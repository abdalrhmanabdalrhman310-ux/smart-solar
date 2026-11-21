package com.smartsolar.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.local.StoredCookieDto
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.Inet4Address
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class ServerUrlNotConfiguredException : IOException("PHP Server URL is not configured")

class SessionExpiredException : IOException("Session expired")

/**
 * Lenient Moshi adapters for PHP/MySQL JSON output where numeric and boolean fields
 * are frequently serialized as strings ("42", "185.50", "1", "0") or 0/1 ints.
 */
object LenientPhpMoshiAdapters {

    private fun readScalarAsString(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.NUMBER, JsonReader.Token.STRING -> reader.nextString()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    object BooleanAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Boolean {
            return parseBoolean(readScalarAsString(reader)) ?: false
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Boolean) {
            writer.value(value)
        }
    }

    object NullableBooleanAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Boolean? {
            return parseBoolean(readScalarAsString(reader))
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Boolean?) {
            if (value == null) writer.nullValue() else writer.value(value)
        }
    }

    object IntAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Int {
            val raw = readScalarAsString(reader)?.trim() ?: return 0
            return raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt() ?: 0
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Int) {
            writer.value(value)
        }
    }

    object NullableIntAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Int? {
            val raw = readScalarAsString(reader)?.trim()
            if (raw.isNullOrBlank()) return null
            return raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt()
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Int?) {
            if (value == null) writer.nullValue() else writer.value(value)
        }
    }

    object LongAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Long {
            val raw = readScalarAsString(reader)?.trim() ?: return 0L
            return raw.toLongOrNull() ?: raw.toDoubleOrNull()?.toLong() ?: 0L
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Long) {
            writer.value(value)
        }
    }

    object NullableLongAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Long? {
            val raw = readScalarAsString(reader)?.trim()
            if (raw.isNullOrBlank()) return null
            return raw.toLongOrNull() ?: raw.toDoubleOrNull()?.toLong()
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Long?) {
            if (value == null) writer.nullValue() else writer.value(value)
        }
    }

    object DoubleAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Double {
            val raw = readScalarAsString(reader)?.trim() ?: return 0.0
            return raw.toDoubleOrNull() ?: 0.0
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Double) {
            writer.value(value)
        }
    }

    object NullableDoubleAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Double? {
            val raw = readScalarAsString(reader)?.trim()
            if (raw.isNullOrBlank()) return null
            return raw.toDoubleOrNull()
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Double?) {
            if (value == null) writer.nullValue() else writer.value(value)
        }
    }

    private fun parseBoolean(raw: String?): Boolean? {
        if (raw == null) return null
        return when (raw.trim().lowercase()) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off", "" -> false
            else -> raw.toDoubleOrNull()?.let { it != 0.0 }
        }
    }
}

/**
 * Maintains PHP session cookies (e.g. PHPSESSID) across API calls and persists them
 * in DataStore alongside the active session.
 */
class PhpSessionCookieJar(
    private val sessionPreferences: SessionPreferencesManager
) : CookieJar {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var loadedFromStore = false
    private val cookieStore = mutableListOf<Cookie>()

    private fun ensureLoaded() {
        if (!loadedFromStore) {
            synchronized(lock) {
                if (!loadedFromStore) {
                    val saved = runBlocking { sessionPreferences.getSavedCookies() }
                    val now = System.currentTimeMillis()
                    cookieStore.clear()
                    saved.filter { it.expiresAt > now }.forEach { dto ->
                        val builder = Cookie.Builder()
                            .name(dto.name)
                            .value(dto.value)
                            .path(dto.path)
                            .expiresAt(dto.expiresAt)
                        if (dto.hostOnly) builder.hostOnlyDomain(dto.domain) else builder.domain(dto.domain)
                        if (dto.secure) builder.secure()
                        if (dto.httpOnly) builder.httpOnly()
                        cookieStore.add(builder.build())
                    }
                    loadedFromStore = true
                }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        ensureLoaded()
        val snapshot: List<StoredCookieDto>
        synchronized(lock) {
            val now = System.currentTimeMillis()
            cookieStore.removeAll { it.expiresAt <= now }
            for (incoming in cookies) {
                cookieStore.removeAll {
                    it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path
                }
                if (incoming.expiresAt > now && incoming.value.isNotEmpty()) {
                    cookieStore.add(incoming)
                }
            }
            snapshot = cookieStore.map {
                StoredCookieDto(
                    name = it.name,
                    value = it.value,
                    domain = it.domain,
                    path = it.path,
                    expiresAt = it.expiresAt,
                    secure = it.secure,
                    httpOnly = it.httpOnly,
                    hostOnly = it.hostOnly
                )
            }
        }
        scope.launch {
            sessionPreferences.saveCookies(snapshot)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        ensureLoaded()
        synchronized(lock) {
            val now = System.currentTimeMillis()
            cookieStore.removeAll { it.expiresAt <= now }
            return cookieStore.filter { it.matches(url) }
        }
    }

    fun clear() {
        synchronized(lock) {
            cookieStore.clear()
            loadedFromStore = true
        }
        scope.launch {
            sessionPreferences.clearCookies()
        }
    }
}

class NetworkClientFactory(
    private val context: Context,
    private val sessionPreferences: SessionPreferencesManager,
    val moshi: Moshi
) {
    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    val cookieJar = PhpSessionCookieJar(sessionPreferences)

    private val dynamicBaseUrlInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val config = runBlocking { sessionPreferences.getCurrentServerConfig() }
        if (!config.isConfigured) {
            throw ServerUrlNotConfiguredException()
        }

        val targetBase = config.baseUrl.toHttpUrlOrNull()
            ?: throw IOException("تنسيق عنوان خادم PHP غير صالح: ${config.baseUrl}")

        // Placeholder base url used by Retrofit is https://unconfigured.invalid/
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
        if (token.isNotBlank() && token != "php_session") {
            requestBuilder.header("Authorization", "Bearer $token")
            requestBuilder.header("X-Session-Token", token)
        }

        val response: Response = chain.proceed(requestBuilder.build())
        if (response.code == 401) {
            _sessionExpiredEvents.tryEmit(Unit)
        }
        response
    }

    /**
     * Normalizes common PHP JSON response shapes without touching non-JSON / error HTML bodies:
     * - Strips UTF-8 BOM if emitted by PHP files
     * - Wraps direct array payloads `{"success": true, "data": [...]}` into `{"items": [...]}`
     *   for list endpoints, or promotes top-level list keys into `data`.
     */
    private val phpResponseNormalizationInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val body = response.body ?: return@Interceptor response
        val contentType = body.contentType()
        val rawString = body.string()
        val normalized = normalizePhpJsonBody(
            path = response.request.url.encodedPath,
            rawBody = rawString
        )
        val newBody = normalized.toResponseBody(contentType ?: "application/json; charset=utf-8".toMediaTypeOrNull())
        response.newBuilder().body(newBody).build()
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
        .cookieJar(cookieJar)
        .addInterceptor(dynamicTimeoutInterceptor)
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(sessionAuthInterceptor)
        .addInterceptor(phpResponseNormalizationInterceptor)
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

    fun isNetworkAvailable(targetBaseUrl: String = ""): Boolean {
        if (isLocalDevUrl(targetBaseUrl)) {
            return true
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    suspend fun resolveAssetUrl(rawUrl: String?): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return ""
        if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }
        val baseUrl = sessionPreferences.getCurrentServerConfig().baseUrl
        val httpBase = baseUrl.toHttpUrlOrNull() ?: return trimmed
        if (trimmed.startsWith("/")) {
            return httpBase.newBuilder()
                .encodedPath(trimmed)
                .query(null)
                .build()
                .toString()
        }
        // Standard PHP setup: base URL is http://host/solar-admin/api/ and uploads live under /solar-admin/uploads/
        val baseForRelative = if (
            trimmed.startsWith("uploads/", ignoreCase = true) &&
            httpBase.encodedPath.endsWith("/api/", ignoreCase = true)
        ) {
            httpBase.resolve("../") ?: httpBase
        } else {
            httpBase
        }
        return baseForRelative.resolve(trimmed)?.toString() ?: trimmed
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

        if (!isNetworkAvailable(config.baseUrl)) {
            return NetworkResult.Error(
                type = NetworkErrorType.NO_INTERNET,
                arabicMessage = "لا يوجد اتصال بالشبكة. يرجى التحقق من اتصال الواي فاي أو البيانات والمحاولة مرة أخرى.",
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
                arabicMessage = "تعذر الوصول إلى اسم النطاق أو عنوان الخادم. تأكد من صحة الرابط والشبكة.",
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
            val rawErrorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
            val extracted = extractServerErrorDetails(rawErrorBody)
            val serverMsg = extracted.message
            val detail = rawErrorBody.take(300).ifBlank { "HTTP ${e.code()} ${e.message()}" }

            if (e.code() == 401 || extracted.sessionExpired) {
                _sessionExpiredEvents.tryEmit(Unit)
                return NetworkResult.Error(
                    type = NetworkErrorType.UNAUTHORIZED_SESSION_EXPIRED,
                    arabicMessage = serverMsg.ifBlank { "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً." },
                    technicalDetail = detail,
                    phpEndpoint = endpointName
                )
            }

            when (e.code()) {
                400, 409, 422 -> NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = if (serverMsg.isNotBlank()) {
                        "$serverMsg (رمز ${e.code()})"
                    } else {
                        "طلب غير مكتمل أو بيانات غير صالحة (رمز ${e.code()}) أثناء الاتصال بـ $endpointName."
                    },
                    technicalDetail = detail,
                    phpEndpoint = endpointName,
                    needsPhpConfirmation = serverMsg.isBlank() && needsPhpConfirmation
                )
                403 -> NetworkResult.Error(
                    type = NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
                    arabicMessage = serverMsg.ifBlank {
                        "لا تملك صلاحية تنفيذ هذه العملية أو أن حسابك بانتظار موافقة الإدارة."
                    },
                    technicalDetail = detail,
                    phpEndpoint = endpointName
                )
                404 -> NetworkResult.Error(
                    type = NetworkErrorType.HTTP_NOT_FOUND,
                    arabicMessage = serverMsg.ifBlank {
                        "ملف API غير موجود على الخادم ($endpointName). تأكد من مسار solar-admin/api/."
                    },
                    technicalDetail = detail,
                    phpEndpoint = endpointName,
                    needsPhpConfirmation = true
                )
                else -> NetworkResult.Error(
                    type = NetworkErrorType.SERVER_ERROR,
                    arabicMessage = if (serverMsg.isNotBlank()) {
                        "$serverMsg (رمز ${e.code()})"
                    } else {
                        "خطأ من الخادم (رمز ${e.code()}) أثناء الاتصال بـ $endpointName."
                    },
                    technicalDetail = detail,
                    phpEndpoint = endpointName,
                    needsPhpConfirmation = serverMsg.isBlank() && needsPhpConfirmation
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
            .add(LenientPhpMoshiAdapters.BooleanAdapter)
            .add(LenientPhpMoshiAdapters.NullableBooleanAdapter)
            .add(LenientPhpMoshiAdapters.IntAdapter)
            .add(LenientPhpMoshiAdapters.NullableIntAdapter)
            .add(LenientPhpMoshiAdapters.LongAdapter)
            .add(LenientPhpMoshiAdapters.NullableLongAdapter)
            .add(LenientPhpMoshiAdapters.DoubleAdapter)
            .add(LenientPhpMoshiAdapters.NullableDoubleAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()

        internal fun isLocalDevUrl(baseUrl: String): Boolean {
            val host = baseUrl.toHttpUrlOrNull()?.host?.lowercase() ?: return false
            if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2") return true
            val parts = host.split(".")
            if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
                val a = parts[0].toInt()
                val b = parts[1].toInt()
                return a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
            }
            return runCatching {
                val addr = Inet4Address.getByName(host)
                addr.isSiteLocalAddress || addr.isLoopbackAddress
            }.getOrDefault(false)
        }

        private val genericJsonAdapter = Moshi.Builder().build().adapter<Any>(Any::class.java)

        @Suppress("UNCHECKED_CAST")
        internal fun normalizePhpJsonBody(path: String, rawBody: String): String {
            val clean = rawBody.trimStart('\uFEFF').trim()
            if (clean.isEmpty()) return rawBody
            if (!clean.startsWith("[") && !clean.startsWith("{")) return rawBody

            return runCatching {
                val parsed = genericJsonAdapter.fromJson(clean) ?: return@runCatching clean
                when (parsed) {
                    is List<*> -> genericJsonAdapter.toJson(
                        mapOf(
                            "success" to true,
                            "data" to mapOf("items" to parsed)
                        )
                    )
                    is Map<*, *> -> {
                        val mutableObj = parsed.entries.associate { (k, v) -> k.toString() to v }.toMutableMap()
                        val dataVal = mutableObj["data"]
                        var modified = clean != rawBody
                        when {
                            dataVal is List<*> -> {
                                mutableObj["data"] = mapOf("items" to dataVal)
                                modified = true
                            }
                            dataVal == null -> {
                                val listKeys = listOf(
                                    "products", "orders", "engineers",
                                    "installations", "maintenance", "tasks",
                                    "designs", "ratings", "notifications", "items"
                                )
                                val foundListKey = listKeys.firstOrNull { mutableObj[it] is List<*> }
                                if (foundListKey != null) {
                                    val wrapper = mutableMapOf<String, Any?>(
                                        foundListKey to mutableObj[foundListKey]
                                    )
                                    if (mutableObj.containsKey("total_sales")) {
                                        wrapper["total_sales"] = mutableObj["total_sales"]
                                    }
                                    mutableObj["data"] = wrapper
                                    modified = true
                                } else if (path.endsWith(PhpEndpointContract.FILE_SETTINGS) && mutableObj.containsKey("site_name")) {
                                    val wrapper = mutableMapOf<String, Any?>()
                                    listOf(
                                        "site_name",
                                        "require_engineer_approval",
                                        "require_vendor_approval",
                                        "require_commercial_register",
                                        "currency"
                                    ).forEach { key ->
                                        if (mutableObj.containsKey(key)) wrapper[key] = mutableObj[key]
                                    }
                                    mutableObj["data"] = wrapper
                                    modified = true
                                }
                            }
                        }
                        if (modified) genericJsonAdapter.toJson(mutableObj) else clean
                    }
                    else -> clean
                }
            }.getOrDefault(clean)
        }

        internal data class ExtractedServerError(
            val message: String,
            val sessionExpired: Boolean = false
        )

        internal fun extractServerErrorDetails(rawBody: String): ExtractedServerError {
            val clean = rawBody.trimStart('\uFEFF').trim()
            if (clean.isBlank()) return ExtractedServerError("")
            if (clean.startsWith("{")) {
                val parsed = runCatching { genericJsonAdapter.fromJson(clean) as? Map<*, *> }.getOrNull()
                if (parsed != null) {
                    val expired = parsed["session_expired"].let {
                        it == true || it?.toString()?.trim()?.lowercase() in setOf("true", "1")
                    }
                    val direct = listOf("message", "error", "msg", "detail")
                        .firstNotNullOfOrNull { key ->
                            parsed[key]?.toString()?.trim()?.takeIf { it.isNotBlank() && it != "null" }
                        }
                    val errorsText = when (val errs = parsed["errors"]) {
                        is List<*> -> errs.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                            .joinToString(" - ")
                        is Map<*, *> -> errs.values.flatMap { value ->
                            when (value) {
                                is List<*> -> value.mapNotNull { it?.toString()?.trim() }
                                else -> listOfNotNull(value?.toString()?.trim())
                            }
                        }.filter(String::isNotBlank).joinToString(" - ")
                        else -> ""
                    }
                    val combined = listOfNotNull(
                        direct,
                        errorsText.takeIf { it.isNotBlank() && it != direct }
                    ).joinToString(" — ")
                    return ExtractedServerError(message = combined, sessionExpired = expired)
                }
            }
            // Non-JSON plain text (avoid dumping raw HTML error pages to the user)
            if (!clean.startsWith("<")) {
                return ExtractedServerError(message = clean.take(240))
            }
            return ExtractedServerError("")
        }
    }
}
