package com.smartsolar.app.ui.viewmodel

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartsolar.app.AppContainer
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.repository.AuthRepository
import com.smartsolar.app.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthModeTab(val arabicTitle: String) {
    LOGIN("تسجيل الدخول"),
    CLIENT_REGISTER("حساب عميل"),
    ENGINEER_REGISTER("انضمام مهندس"),
    VENDOR_REGISTER("انضمام بائع")
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authResult = MutableStateFlow<NetworkResult<UserSession>?>(null)
    val authResult: StateFlow<NetworkResult<UserSession>?> = _authResult.asStateFlow()

    fun clearState() {
        _authResult.value = null
    }

    fun login(email: String, password: String, rememberSession: Boolean) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _authResult.value = NetworkResult.Error(
                type = NetworkErrorType.VALIDATION_ERROR,
                arabicMessage = "يرجى إدخال بريد إلكتروني صحيح."
            )
            return
        }
        if (password.length < 6) {
            _authResult.value = NetworkResult.Error(
                type = NetworkErrorType.VALIDATION_ERROR,
                arabicMessage = "كلمة المرور يجب أن تكون 6 أحرف على الأقل."
            )
            return
        }
        viewModelScope.launch {
            _authResult.value = NetworkResult.Loading
            _authResult.value = authRepository.login(email, password, rememberSession)
        }
    }

    fun registerClient(
        fullName: String,
        email: String,
        phone: String,
        city: String,
        password: String,
        confirmPassword: String,
        address: String
    ) {
        val error = validateCommonFields(fullName, email, phone, password)
            ?: when {
                password != confirmPassword -> "كلمة المرور وتأكيد كلمة المرور غير متطابقتين."
                city.trim().length < 2 -> "يرجى إدخال المدينة."
                address.trim().length < 4 -> "يرجى إدخال العنوان بشكل واضح."
                else -> null
            }
        if (error != null) {
            _authResult.value = NetworkResult.Error(NetworkErrorType.VALIDATION_ERROR, error)
            return
        }
        viewModelScope.launch {
            _authResult.value = NetworkResult.Loading
            _authResult.value = authRepository.registerClient(fullName, email, phone, city, password, address)
        }
    }

    fun registerEngineer(
        fullName: String,
        ageText: String,
        email: String,
        phone: String,
        city: String,
        password: String,
        specialization: String,
        qualification: String,
        yearsExperienceText: String,
        bio: String,
        previousProjects: String,
        avatarUri: Uri?,
        portfolioUris: List<Uri>,
        certificateUris: List<Uri>
    ) {
        val age = ageText.trim().toIntOrNull()
        val years = yearsExperienceText.trim().toIntOrNull()
        val error = validateCommonFields(fullName, email, phone, password)
            ?: when {
                age == null || age !in 18..90 -> "يرجى إدخال عمر صحيح (من 18 إلى 90 سنة)."
                city.trim().length < 2 -> "يرجى إدخال المدينة."
                qualification.isBlank() -> "يرجى إدخال المؤهل العلمي."
                specialization.isBlank() -> "يرجى إدخال التخصص الهندسي."
                years == null || years < 0 -> "يرجى إدخال سنوات خبرة صحيحة."
                bio.trim().length < 10 -> "يرجى كتابة نبذة تعريفية عن الخبرات (10 أحرف على الأقل)."
                previousProjects.isBlank() && portfolioUris.isEmpty() ->
                    "يرجى إدخال وصف الأعمال والمشاريع السابقة أو إرفاق صورها."
                else -> null
            }
        if (error != null) {
            _authResult.value = NetworkResult.Error(NetworkErrorType.VALIDATION_ERROR, error)
            return
        }
        viewModelScope.launch {
            _authResult.value = NetworkResult.Loading
            _authResult.value = authRepository.registerEngineer(
                fullName = fullName,
                age = age ?: 0,
                email = email,
                phone = phone,
                city = city,
                password = password,
                specialization = specialization,
                qualification = qualification,
                yearsExperience = years ?: 0,
                bio = bio,
                previousProjects = previousProjects,
                avatarUri = avatarUri,
                portfolioUris = portfolioUris,
                certificateUris = certificateUris
            )
        }
    }

    fun registerVendor(
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
    ) {
        val error = validateCommonFields(vendorName, email, phone, password)
            ?: when {
                storeName.trim().length < 2 -> "يرجى إدخال اسم المحل أو المؤسسة."
                businessType.trim().length < 2 -> "يرجى إدخال نوع النشاط التجاري."
                city.trim().length < 2 -> "يرجى إدخال المدينة."
                storeAddress.trim().length < 4 -> "يرجى إدخال عنوان المحل بشكل واضح."
                storeDescription.trim().length < 6 -> "يرجى إدخال وصف المحل والمنتجات التي يقدمها."
                else -> null
            }
        if (error != null) {
            _authResult.value = NetworkResult.Error(NetworkErrorType.VALIDATION_ERROR, error)
            return
        }
        viewModelScope.launch {
            _authResult.value = NetworkResult.Loading
            _authResult.value = authRepository.registerVendor(
                vendorName = vendorName,
                email = email,
                phone = phone,
                city = city,
                storeAddress = storeAddress,
                password = password,
                storeName = storeName,
                businessType = businessType,
                storeDescription = storeDescription,
                commercialRegister = commercialRegister,
                storeLogoUri = storeLogoUri,
                storeImageUri = storeImageUri
            )
        }
    }

    private fun validateCommonFields(
        name: String,
        email: String,
        phone: String,
        password: String
    ): String? {
        return when {
            name.trim().length < 3 -> "يرجى إدخال الاسم الكامل (3 أحرف على الأقل)."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "يرجى إدخال بريد إلكتروني صحيح."
            phone.trim().length < 7 -> "يرجى إدخال رقم هاتف صحيح."
            password.length < 6 -> "كلمة المرور يجب أن تكون 6 أحرف على الأقل."
            else -> null
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(container.authRepository) as T
                }
            }
    }
}
