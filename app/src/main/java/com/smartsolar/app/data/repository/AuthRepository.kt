package com.smartsolar.app.data.repository

import android.content.Context
import android.net.Uri
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.remote.ApiBaseResponse
import com.smartsolar.app.data.remote.AuthPayloadDto
import com.smartsolar.app.data.remote.AuthUserDto
import com.smartsolar.app.data.remote.LoginRequestDto
import com.smartsolar.app.data.remote.MultipartUploadHelper
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.remote.PhpEndpointContract
import com.smartsolar.app.data.remote.RegisterClientRequestDto
import com.smartsolar.app.data.remote.UpdateProfileRequestDto
import com.smartsolar.app.domain.model.ApprovalStatus
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.UserRole
import com.smartsolar.app.domain.model.UserSession
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MultipartBody

class AuthRepository(
    private val context: Context,
    private val networkFactory: NetworkClientFactory,
    private val sessionPreferences: SessionPreferencesManager
) {
    private val api get() = networkFactory.apiService

    suspend fun login(
        email: String,
        password: String,
        rememberSession: Boolean
    ): NetworkResult<UserSession> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.login(LoginRequestDto(email = email.trim(), password = password))
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                response = result.data,
                rememberSession = rememberSession,
                isLoginFlow = true
            )
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerClient(
        fullName: String,
        email: String,
        phone: String,
        city: String,
        password: String,
        address: String
    ): NetworkResult<UserSession> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.registerClient(
                RegisterClientRequestDto(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    city = city.trim(),
                    password = password,
                    address = address.trim()
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val mapped = mapAuthResponse(
                    response = result.data,
                    rememberSession = true,
                    defaultRole = UserRole.CLIENT,
                    defaultApproval = ApprovalStatus.APPROVED,
                    allowMissingTokenOnRegister = true
                )
                if (mapped is NetworkResult.Success && !mapped.data.isAuthenticated) {
                    // Registration saved in users table without returning a session token; log in right away.
                    when (val loginRes = login(email.trim(), password, rememberSession = true)) {
                        is NetworkResult.Success -> loginRes
                        is NetworkResult.Error -> NetworkResult.Error(
                            type = loginRes.type,
                            arabicMessage = "تم إنشاء الحساب بنجاح، لكن تعذر تسجيل الدخول التلقائي: ${loginRes.arabicMessage}",
                            phpEndpoint = PhpEndpointContract.FILE_AUTH
                        )
                        NetworkResult.Loading -> NetworkResult.Loading
                    }
                } else {
                    mapped
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerEngineer(
        fullName: String,
        age: Int,
        email: String,
        phone: String,
        city: String,
        password: String,
        specialization: String,
        qualification: String,
        yearsExperience: Int,
        bio: String,
        previousProjects: String,
        avatarUri: Uri?,
        portfolioUris: List<Uri>,
        certificateUris: List<Uri>
    ): NetworkResult<UserSession> {
        var avatarPart: MultipartBody.Part? = null
        if (avatarUri != null) {
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, avatarUri, "avatar")) {
                is MultipartUploadHelper.PreparedPart.Valid -> avatarPart = prepared.part
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_AUTH,
                    needsPhpConfirmation = false
                )
            }
        }

        val portfolioParts = mutableListOf<MultipartBody.Part>()
        for (uri in portfolioUris) {
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, uri, "portfolio_images[]")) {
                is MultipartUploadHelper.PreparedPart.Valid -> portfolioParts.add(prepared.part)
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_AUTH,
                    needsPhpConfirmation = false
                )
            }
        }

        val certificateParts = mutableListOf<MultipartBody.Part>()
        for (uri in certificateUris) {
            when (val prepared = MultipartUploadHelper.prepareAttachmentPart(context, uri, "certificate_images[]")) {
                is MultipartUploadHelper.PreparedPart.Valid -> certificateParts.add(prepared.part)
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_AUTH,
                    needsPhpConfirmation = false
                )
            }
        }

        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.registerEngineerMultipart(
                action = MultipartUploadHelper.textPart("register"),
                role = MultipartUploadHelper.textPart("engineer"),
                fullName = MultipartUploadHelper.textPart(fullName.trim()),
                age = MultipartUploadHelper.textPart(age.toString()),
                email = MultipartUploadHelper.textPart(email.trim()),
                phone = MultipartUploadHelper.textPart(phone.trim()),
                city = MultipartUploadHelper.textPart(city.trim()),
                password = MultipartUploadHelper.textPart(password),
                specialization = MultipartUploadHelper.textPart(specialization.trim()),
                qualification = MultipartUploadHelper.textPart(qualification.trim()),
                yearsExperience = MultipartUploadHelper.textPart(yearsExperience.toString()),
                bio = MultipartUploadHelper.textPart(bio.trim()),
                previousProjects = MultipartUploadHelper.textPart(previousProjects.trim()),
                avatar = avatarPart,
                portfolioImages = portfolioParts,
                certificateImages = certificateParts
            )
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                response = result.data,
                rememberSession = false,
                defaultRole = UserRole.ENGINEER,
                defaultApproval = ApprovalStatus.PENDING,
                allowMissingTokenOnRegister = true,
                fallbackPendingSession = UserSession(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    city = city.trim(),
                    age = age,
                    specialization = specialization.trim(),
                    qualification = qualification.trim(),
                    yearsOfExperience = yearsExperience,
                    bio = bio.trim(),
                    previousProjects = previousProjects.trim(),
                    role = UserRole.ENGINEER,
                    approvalStatus = ApprovalStatus.PENDING
                )
            )
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerVendor(
        vendorName: String,
        email: String,
        phone: String,
        city: String,
        storeAddress: String,
        password: String,
        storeName: String,
        businessType: String,
        storeDescription: String,
        commercialRegister: String,
        storeLogoUri: Uri?,
        storeImageUri: Uri?
    ): NetworkResult<UserSession> {
        var logoPart: MultipartBody.Part? = null
        if (storeLogoUri != null) {
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, storeLogoUri, "store_logo")) {
                is MultipartUploadHelper.PreparedPart.Valid -> logoPart = prepared.part
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_AUTH,
                    needsPhpConfirmation = false
                )
            }
        }

        var storeImgPart: MultipartBody.Part? = null
        if (storeImageUri != null) {
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, storeImageUri, "store_image")) {
                is MultipartUploadHelper.PreparedPart.Valid -> storeImgPart = prepared.part
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_AUTH,
                    needsPhpConfirmation = false
                )
            }
        }

        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.registerVendorMultipart(
                action = MultipartUploadHelper.textPart("register"),
                role = MultipartUploadHelper.textPart("vendor"),
                fullName = MultipartUploadHelper.textPart(vendorName.trim()),
                email = MultipartUploadHelper.textPart(email.trim()),
                phone = MultipartUploadHelper.textPart(phone.trim()),
                city = MultipartUploadHelper.textPart(city.trim()),
                address = MultipartUploadHelper.textPart(storeAddress.trim()),
                storeAddress = MultipartUploadHelper.textPart(storeAddress.trim()),
                password = MultipartUploadHelper.textPart(password),
                storeName = MultipartUploadHelper.textPart(storeName.trim()),
                businessType = MultipartUploadHelper.textPart(businessType.trim()),
                storeDescription = MultipartUploadHelper.textPart(storeDescription.trim()),
                commercialRegister = MultipartUploadHelper.textPart(commercialRegister.trim()),
                storeLogo = logoPart,
                storeImage = storeImgPart
            )
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                response = result.data,
                rememberSession = false,
                defaultRole = UserRole.VENDOR,
                defaultApproval = ApprovalStatus.PENDING,
                allowMissingTokenOnRegister = true,
                fallbackPendingSession = UserSession(
                    fullName = vendorName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    city = city.trim(),
                    address = storeAddress.trim(),
                    storeName = storeName.trim(),
                    businessType = businessType.trim(),
                    storeDescription = storeDescription.trim(),
                    commercialRegister = commercialRegister.trim(),
                    role = UserRole.VENDOR,
                    approvalStatus = ApprovalStatus.PENDING
                )
            )
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun refreshProfile(): NetworkResult<UserSession> {
        val current = sessionPreferences.getCurrentSession()
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.fetchProfile()
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                response = result.data,
                rememberSession = current.rememberSession,
                defaultRole = current.role,
                defaultApproval = current.approvalStatus,
                fallbackToken = current.token
            )
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        city: String,
        address: String
    ): NetworkResult<UserSession> {
        val current = sessionPreferences.getCurrentSession()
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.updateProfile(
                UpdateProfileRequestDto(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    city = city.trim(),
                    address = address.trim()
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    return NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر تحديث الملف الشخصي." },
                        phpEndpoint = PhpEndpointContract.FILE_AUTH
                    )
                }
                val hasReturnedUser = resp.data?.resolvedUser() != null || resp.user != null
                if (hasReturnedUser) {
                    mapAuthResponse(
                        response = resp,
                        rememberSession = current.rememberSession,
                        defaultRole = current.role,
                        defaultApproval = current.approvalStatus,
                        fallbackToken = current.token
                    )
                } else {
                    val updatedSession = current.copy(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        city = city.trim(),
                        address = address.trim()
                    )
                    sessionPreferences.saveUserSession(updatedSession)
                    NetworkResult.Success(
                        data = updatedSession,
                        message = resp.resolvedMessage().ifBlank { "تم تحديث بيانات الملف الشخصي بنجاح." }
                    )
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun logout() {
        runCatching {
            val config = sessionPreferences.getCurrentServerConfig()
            if (config.isConfigured) {
                api.logout()
            }
        }
        networkFactory.cookieJar.clear()
        sessionPreferences.clearSession()
    }

    private suspend fun hasServerCookie(): Boolean {
        val baseUrl = sessionPreferences.getCurrentServerConfig().baseUrl.toHttpUrlOrNull() ?: return false
        return networkFactory.cookieJar.loadForRequest(baseUrl).isNotEmpty()
    }

    private suspend fun mapAuthResponse(
        response: ApiBaseResponse<AuthPayloadDto>,
        rememberSession: Boolean,
        defaultRole: UserRole = UserRole.CLIENT,
        defaultApproval: ApprovalStatus = ApprovalStatus.APPROVED,
        fallbackToken: String = "",
        isLoginFlow: Boolean = false,
        allowMissingTokenOnRegister: Boolean = false,
        fallbackPendingSession: UserSession? = null
    ): NetworkResult<UserSession> {
        if (response.sessionExpired == true) {
            networkFactory.cookieJar.clear()
            sessionPreferences.clearSession()
            return NetworkResult.Error(
                type = NetworkErrorType.UNAUTHORIZED_SESSION_EXPIRED,
                arabicMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً.",
                phpEndpoint = PhpEndpointContract.FILE_AUTH
            )
        }

        if (!response.isOperationSuccess()) {
            return NetworkResult.Error(
                type = NetworkErrorType.SERVER_ERROR,
                arabicMessage = response.resolvedMessage().ifBlank { "تعذر إتمام عملية المصادقة من الخادم." },
                phpEndpoint = PhpEndpointContract.FILE_AUTH,
                needsPhpConfirmation = false
            )
        }

        val payload = response.data
        val userDto: AuthUserDto? = payload?.resolvedUser() ?: response.user
        val rawToken = payload?.token?.takeIf { it.isNotBlank() }
            ?: userDto?.token?.takeIf { it.isNotBlank() }
            ?: response.token?.takeIf { it.isNotBlank() }
            ?: fallbackToken

        val cookiePresent = hasServerCookie()
        val resolvedToken = when {
            rawToken.isNotBlank() -> rawToken
            cookiePresent && (userDto?.id ?: userDto?.userId ?: 0L) > 0L -> PHP_COOKIE_SESSION_TOKEN
            else -> ""
        }

        val approvalStr = userDto?.approvalStatus ?: userDto?.status ?: payload?.approvalStatus ?: payload?.status
        val approvalStatus = when {
            userDto?.isApproved == false || userDto?.requiresApproval == true ||
                payload?.requiresApproval == true || payload?.requiresApprovalSnake == true -> ApprovalStatus.PENDING
            approvalStr != null -> ApprovalStatus.fromApi(approvalStr)
            else -> defaultApproval
        }

        val resolvedRole = if (userDto?.role != null) UserRole.fromApi(userDto.role) else defaultRole

        // Enforce strict approval gate during login: pending or rejected engineer/vendor accounts must never enter dashboards
        if (isLoginFlow && resolvedRole != UserRole.CLIENT && approvalStatus != ApprovalStatus.APPROVED) {
            networkFactory.cookieJar.clear()
            sessionPreferences.clearSession()
            val gateMsg = if (approvalStatus == ApprovalStatus.REJECTED) {
                "تم رفض طلب انضمام حسابك (${resolvedRole.arabicLabel}) من الإدارة ولا يمكنك تسجيل الدخول."
            } else {
                "طلب انضمامك (${resolvedRole.arabicLabel}) ما زال قيد المراجعة من الإدارة. يرجى الانتظار حتى موافقة المدير."
            }
            return NetworkResult.Error(
                type = NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
                arabicMessage = response.resolvedMessage().takeIf { it.isNotBlank() && it != "تمت العملية بنجاح" } ?: gateMsg,
                phpEndpoint = PhpEndpointContract.FILE_AUTH
            )
        }

        val baseFallback = fallbackPendingSession
        val session = UserSession(
            userId = userDto?.id ?: userDto?.userId ?: baseFallback?.userId ?: 0L,
            fullName = userDto?.fullName ?: userDto?.name ?: baseFallback?.fullName ?: "",
            email = userDto?.email ?: baseFallback?.email ?: "",
            phone = userDto?.phone ?: baseFallback?.phone ?: "",
            city = userDto?.city ?: baseFallback?.city ?: "",
            address = userDto?.address ?: userDto?.storeAddress ?: baseFallback?.address ?: "",
            avatarUrl = networkFactory.resolveAssetUrl(userDto?.avatarUrl ?: userDto?.avatar),
            role = resolvedRole,
            approvalStatus = approvalStatus,
            token = if (resolvedRole != UserRole.CLIENT && approvalStatus != ApprovalStatus.APPROVED) "" else resolvedToken,
            rememberSession = rememberSession && (resolvedRole == UserRole.CLIENT || approvalStatus == ApprovalStatus.APPROVED),
            age = userDto?.age ?: baseFallback?.age ?: 0,
            specialization = userDto?.specialization ?: baseFallback?.specialization ?: "",
            qualification = userDto?.qualification ?: baseFallback?.qualification ?: "",
            yearsOfExperience = userDto?.yearsExperience ?: baseFallback?.yearsOfExperience ?: 0,
            bio = userDto?.bio ?: baseFallback?.bio ?: "",
            previousProjects = userDto?.previousProjects ?: baseFallback?.previousProjects ?: "",
            availability = EngineerAvailability.fromApi(userDto?.availability),
            storeName = userDto?.storeName ?: baseFallback?.storeName ?: "",
            businessType = userDto?.businessType ?: baseFallback?.businessType ?: "",
            storeDescription = userDto?.storeDescription ?: baseFallback?.storeDescription ?: "",
            storeLogoUrl = networkFactory.resolveAssetUrl(userDto?.storeLogo),
            storeImageUrl = networkFactory.resolveAssetUrl(userDto?.storeImage),
            commercialRegister = userDto?.commercialRegister ?: baseFallback?.commercialRegister ?: ""
        )

        if (!allowMissingTokenOnRegister && !session.isAuthenticated && session.approvalStatus == ApprovalStatus.APPROVED) {
            return NetworkResult.Error(
                type = NetworkErrorType.INVALID_SERVER_RESPONSE,
                arabicMessage = "لم يُرجع الخادم بيانات الجلسة أو رمز التحقق (Token). تحقق من استجابة auth.php.",
                phpEndpoint = PhpEndpointContract.FILE_AUTH,
                needsPhpConfirmation = true
            )
        }

        if (session.approvalStatus != ApprovalStatus.APPROVED) {
            // Do not keep active auth cookies for pending/rejected accounts
            networkFactory.cookieJar.clear()
        }
        sessionPreferences.saveUserSession(session)
        return NetworkResult.Success(
            data = session,
            message = response.resolvedMessage().ifBlank { "تمت العملية بنجاح" }
        )
    }

    companion object {
        const val PHP_COOKIE_SESSION_TOKEN = "php_session"
    }
}
