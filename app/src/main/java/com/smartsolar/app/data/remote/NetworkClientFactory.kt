package com.smartsolar.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class ServerUrlNotConfiguredException : IOException("PHP Server URL is not configured")

class SessionExpiredException : IOException("Session expired")

class NetworkClientFactory(
    private val context: Context,
    private val sessionPreferences: SessionPreferencesManager,
    val moshi: Moshi
) {
    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    private val dynamicBaseUrlInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val config = runBlocking { sessionPreferences.getCurrentServerConfig() }
        if (!config.isConfigured) {
            throw ServerUrlNotConfiguredException()
        }

        val targetBase = config.baseUrl.toHttpUrlOrNull()
            ?: throw IOException("تنسيق عنوان خادم PHP غير صالح: ${config.baseUrl}")

        // Placeholder base url used by Retrofit is http://unconfigured.invalid/
        val encodedSegments = originalRequest.url.encodedPathSegments.filter { it.isNotEmpty() }
        val newUrlBuilder = targetBase.newBuilder()
        encodedSegments.forEach { segment ->
            newUrlBuilder.addEncodedPathSegment(segment)
        }
        originalRequest.url.encodedQuery?.let { query ->
            newUrlBuilder.encodedQuery(query)
        }

        val updatedRequest = originalRequest.newBuilder()
            .url(newUrlBuilder.build())
            .build()

        chain.proceed(updatedRequest)
    }

    private val sessionAuthInterceptor = Interceptor { chain ->
        val token = runBlocking { sessionPreferences.getCurrentAuthToken() }
        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")
        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
            requestBuilder.header("X-Session-Token", token)
        }

        val response: Response = chain.proceed(requestBuilder.build())
        if (response.code == 401) {
            _sessionExpiredEvents.tryEmit(Unit)
        }
        response
    }

    private val dynamicTimeoutInterceptor = Interceptor { chain ->
        val config = runBlocking { sessionPreferences.getCurrentServerConfig() }
        val timeout = config.timeoutSeconds.coerceIn(10, 120)
        chain
            .withConnectTimeout(timeout, TimeUnit.SECONDS)
            .withReadTimeout(timeout, TimeUnit.SECONDS)
            .withWriteTimeout(timeout, TimeUnit.SECONDS)
            .proceed(chain.request())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
        redactHeader("X-Session-Token")
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicTimeoutInterceptor)
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(sessionAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://unconfigured.invalid/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: SolarApiService = retrofit.create(SolarApiService::class.java)

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun <T> safeApiCall(
        endpointName: String,
        needsPhpConfirmation: Boolean = true,
        block: suspend () -> T
    ): NetworkResult<T> {
        val config = sessionPreferences.getCurrentServerConfig()
        if (!config.isConfigured) {
            return NetworkResult.Error(
                type = NetworkErrorType.SERVER_URL_NOT_CONFIGURED,
                arabicMessage = "لم يتم ضبط عنوان خادم PHP بعد. يرجى إدخال رابط solar-admin/api/ من إعدادات الخادم.",
                phpEndpoint = endpointName,
                needsPhpConfirmation = false
            )
        }

        if (!isNetworkAvailable()) {
            return NetworkResult.Error(
                type = NetworkErrorType.NO_INTERNET,
                arabicMessage = "لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة والمحاولة مرة أخرى.",
                phpEndpoint = endpointName
            )
        }

        return try {
            val result = block()
            NetworkResult.Success(result)
        } catch (_: ServerUrlNotConfiguredException) {
            NetworkResult.Error(
                type = NetworkErrorType.SERVER_URL_NOT_CONFIGURED,
                arabicMessage = "لم يتم ضبط عنوان خادم PHP بعد.",
                phpEndpoint = endpointName
            )
        } catch (_: SocketTimeoutException) {
            NetworkResult.Error(
                type = NetworkErrorType.TIMEOUT,
                arabicMessage = "انتهت مهلة الاتصال بخادم PHP. يرجى التأكد من سرعة الشبكة أو زيادة المهلة في الإعدادات.",
                phpEndpoint = endpointName
            )
        } catch (e: UnknownHostException) {
            NetworkResult.Error(
                type = NetworkErrorType.NO_INTERNET,
                arabicMessage = "تعذر الوصول إلى اسم النطاق أو عنوان الخادم. تأكد من صحة الرابط والإنترنت.",
                technicalDetail = e.localizedMessage.orEmpty(),
                phpEndpoint = endpointName
            )
        } catch (e: ConnectException) {
            NetworkResult.Error(
                type = NetworkErrorType.NO_INTERNET,
                arabicMessage = "تعذر الاتصال بخادم PHP. تأكد من تشغيل الخادم وصحة المنفذ والرابط.",
                technicalDetail = e.localizedMessage.orEmpty(),
                phpEndpoint = endpointName
            )
        } catch (e: SSLException) {
            NetworkResult.Error(
                type = NetworkErrorType.SSL_OR_SECURITY_ERROR,
                arabicMessage = "فشل التحقق من شهادة الأمان HTTPS للخادم.",
                technicalDetail = e.localizedMessage.orEmpty(),
                phpEndpoint = endpointName
            )
        } catch (e: JsonDataException) {
            NetworkResult.Error(
                type = NetworkErrorType.INVALID_SERVER_RESPONSE,
                arabicMessage = "استجابة الخادم في $endpointName لا تطابق تنسيق JSON المتوقع. يرجى التحقق من توافق كود PHP.",
                technicalDetail = e.localizedMessage.orEmpty(),
                phpEndpoint = endpointName,
                needsPhpConfirmation = needsPhpConfirmation
            )
        } catch (e: JsonEncodingException) {
            NetworkResult.Error(
                type = NetworkErrorType.INVALID_SERVER_RESPONSE,
                arabicMessage = "أعاد الخادم استجابة غير صالحة (ليست JSON صحيحاً، ربما صفحة خطأ HTML من PHP).",
                technicalDetail = e.localizedMessage.orEmpty(),
                phpEndpoint = endpointName,
                needsPhpConfirmation = needsPhpConfirmation
            )
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> {
                    _sessionExpiredEvents.tryEmit(Unit)
                    NetworkResult.Error(
                        type = NetworkErrorType.UNAUTHORIZED_SESSION_EXPIRED,
                        arabicMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً.",
                        technicalDetail = "HTTP 401",
                        phpEndpoint = endpointName
                    )
                }
                403 -> NetworkResult.Error(
                    type = NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
                    arabicMessage = "لا تملك صلاحية تنفيذ هذه العملية أو أن حسابك بانتظار موافقة الإدارة.",
                    technicalDetail = "HTTP 403",
                    phpEndpoint = endpointName
                )
                404 -> NetworkResult.Error(
                    type = NetworkErrorType.HTTP_NOT_FOUND,
                    arabicMessage = "ملف API غير موجود على الخادم ($endpointName). تأكد من مسار solar-admin/api/.",
                    technicalDetail = "HTTP 404",
                    phpEndpoint = endpointName,
                    needsPhpConfirmation = true
                )
                else -> NetworkResult.Error(
                    type = NetworkErrorType.SERVER_ERROR,
                    arabicMessage = "خطأ من الخادم (رمز ${e.code()}) أثناء الاتصال بـ $endpointName.",
                    technicalDetail = e.message(),
                    phpEndpoint = endpointName,
                    needsPhpConfirmation = needsPhpConfirmation
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                type = NetworkErrorType.SERVER_ERROR,
                arabicMessage = "حدث خطأ غير متوقع أثناء الاتصال بالخادم: ${e.localizedMessage ?: "خطأ غير معروف"}",
                technicalDetail = e.javaClass.simpleName,
                phpEndpoint = endpointName,
                needsPhpConfirmation = needsPhpConfirmation
            )
        }
    }

    companion object {
        fun buildMoshi(): Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }
}
