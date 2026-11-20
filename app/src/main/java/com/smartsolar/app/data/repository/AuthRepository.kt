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
            is NetworkResult.Success -> mapAuthResponse(result.data, rememberSession)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerClient(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: String
    ): NetworkResult<UserSession> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.registerClient(
                RegisterClientRequestDto(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    password = password,
                    address = address.trim()
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(result.data, rememberSession = true, defaultRole = UserRole.CLIENT)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerEngineer(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        specialization: String,
        qualification: String,
        yearsExperience: Int,
        bio: String,
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
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, uri, "certificate_images[]")) {
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
                email = MultipartUploadHelper.textPart(email.trim()),
                phone = MultipartUploadHelper.textPart(phone.trim()),
                password = MultipartUploadHelper.textPart(password),
                specialization = MultipartUploadHelper.textPart(specialization.trim()),
                qualification = MultipartUploadHelper.textPart(qualification.trim()),
                yearsExperience = MultipartUploadHelper.textPart(yearsExperience.toString()),
                bio = MultipartUploadHelper.textPart(bio.trim()),
                avatar = avatarPart,
                portfolioImages = portfolioParts,
                certificateImages = certificateParts
            )
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                result.data,
                rememberSession = true,
                defaultRole = UserRole.ENGINEER,
                defaultApproval = ApprovalStatus.PENDING
            )
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun registerVendor(
        vendorName: String,
        email: String,
        phone: String,
        password: String,
        storeName: String,
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
                password = MultipartUploadHelper.textPart(password),
                storeName = MultipartUploadHelper.textPart(storeName.trim()),
                commercialRegister = MultipartUploadHelper.textPart(commercialRegister.trim()),
                storeLogo = logoPart,
                storeImage = storeImgPart
            )
        }
        return when (result) {
            is NetworkResult.Success -> mapAuthResponse(
                result.data,
                rememberSession = true,
                defaultRole = UserRole.VENDOR,
                defaultApproval = ApprovalStatus.PENDING
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
            is NetworkResult.Success -> mapAuthResponse(result.data, rememberSession = current.rememberSession, fallbackToken = current.token)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        address: String
    ): NetworkResult<UserSession> {
        val current = sessionPreferences.getCurrentSession()
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_AUTH) {
            api.updateProfile(
                UpdateProfileRequestDto(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    address = address.trim()
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val mapped = mapAuthResponse(result.data, current.rememberSession, fallbackToken = current.token)
                if (mapped is NetworkResult.Success) {
                    mapped
                } else {
                    mapped
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
        sessionPreferences.clearSession()
    }

    private suspend fun mapAuthResponse(
        response: ApiBaseResponse<AuthPayloadDto>,
        rememberSession: Boolean,
        defaultRole: UserRole = UserRole.CLIENT,
        defaultApproval: ApprovalStatus = ApprovalStatus.APPROVED,
        fallbackToken: String = ""
    ): NetworkResult<UserSession> {
        if (response.sessionExpired == true) {
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
        val userDto: AuthUserDto? = payload?.user
        val resolvedToken = payload?.token?.takeIf { it.isNotBlank() }
            ?: userDto?.token?.takeIf { it.isNotBlank() }
            ?: response.token?.takeIf { it.isNotBlank() }
            ?: fallbackToken

        val approvalStr = userDto?.approvalStatus ?: payload?.approvalStatus
        val approvalStatus = when {
            userDto?.isApproved == false || payload?.requiresApproval == true -> ApprovalStatus.PENDING
            approvalStr != null -> ApprovalStatus.fromApi(approvalStr)
            else -> defaultApproval
        }

        val session = UserSession(
            userId = userDto?.id ?: userDto?.userId ?: 0L,
            fullName = userDto?.fullName ?: userDto?.name ?: "",
            email = userDto?.email.orEmpty(),
            phone = userDto?.phone.orEmpty(),
            address = userDto?.address.orEmpty(),
            avatarUrl = userDto?.avatarUrl ?: userDto?.avatar.orEmpty(),
            role = if (userDto?.role != null) UserRole.fromApi(userDto.role) else defaultRole,
            approvalStatus = approvalStatus,
            token = resolvedToken,
            rememberSession = rememberSession,
            specialization = userDto?.specialization.orEmpty(),
            qualification = userDto?.qualification.orEmpty(),
            yearsOfExperience = userDto?.yearsExperience ?: 0,
            bio = userDto?.bio.orEmpty(),
            availability = EngineerAvailability.fromApi(userDto?.availability),
            storeName = userDto?.storeName.orEmpty(),
            storeLogoUrl = userDto?.storeLogo.orEmpty(),
            storeImageUrl = userDto?.storeImage.orEmpty(),
            commercialRegister = userDto?.commercialRegister.orEmpty()
        )

        if (!session.isAuthenticated && session.approvalStatus == ApprovalStatus.APPROVED) {
            return NetworkResult.Error(
                type = NetworkErrorType.INVALID_SERVER_RESPONSE,
                arabicMessage = "لم يُرجع الخادم بيانات الجلسة أو رمز التحقق (Token). تحقق من استجابة auth.php.",
                phpEndpoint = PhpEndpointContract.FILE_AUTH,
                needsPhpConfirmation = true
            )
        }

        sessionPreferences.saveUserSession(session)
        return NetworkResult.Success(
            data = session,
            message = response.resolvedMessage().ifBlank { "تمت العملية بنجاح" }
        )
    }
}
