package com.smartsolar.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartsolar.app.domain.model.ApprovalStatus
import com.smartsolar.app.domain.model.CartItem
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.domain.model.UserRole
import com.smartsolar.app.domain.model.UserSession
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.smartSolarDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_solar_session_prefs"
)

@JsonClass(generateAdapter = true)
data class StoredCartItemDto(
    val productId: Long,
    val productName: String,
    val imageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val availableStock: Int,
    val vendorId: Long
)

class SessionPreferencesManager(
    private val context: Context,
    private val moshi: Moshi
) {
    private object Keys {
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REMEMBER_SESSION = booleanPreferencesKey("remember_session")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = longPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val EMAIL = stringPreferencesKey("email")
        val PHONE = stringPreferencesKey("phone")
        val ADDRESS = stringPreferencesKey("address")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val ROLE = stringPreferencesKey("role")
        val APPROVAL_STATUS = stringPreferencesKey("approval_status")
        val SPECIALIZATION = stringPreferencesKey("specialization")
        val QUALIFICATION = stringPreferencesKey("qualification")
        val YEARS_EXPERIENCE = intPreferencesKey("years_experience")
        val BIO = stringPreferencesKey("bio")
        val AVAILABILITY = stringPreferencesKey("availability")
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_LOGO_URL = stringPreferencesKey("store_logo_url")
        val STORE_IMAGE_URL = stringPreferencesKey("store_image_url")
        val COMMERCIAL_REGISTER = stringPreferencesKey("commercial_register")
        val CART_JSON = stringPreferencesKey("cart_json")
    }

    // In-memory active session when "Remember Session" is unchecked (never persisted across kills)
    private val _ephemeralSession = MutableStateFlow<UserSession?>(null)
    val ephemeralSession: StateFlow<UserSession?> = _ephemeralSession.asStateFlow()

    private val cartListType = Types.newParameterizedType(List::class.java, StoredCartItemDto::class.java)
    private val cartAdapter: JsonAdapter<List<StoredCartItemDto>> = moshi.adapter(cartListType)

    val serverConfigFlow: Flow<ServerConfig> = context.smartSolarDataStore.data.map { prefs ->
        ServerConfig(
            baseUrl = prefs[Keys.SERVER_BASE_URL].orEmpty(),
            timeoutSeconds = (prefs[Keys.TIMEOUT_SECONDS] ?: 30).coerceIn(10, 120)
        )
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.smartSolarDataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val persistedSessionFlow: Flow<UserSession> = context.smartSolarDataStore.data.map { prefs ->
        val remember = prefs[Keys.REMEMBER_SESSION] ?: false
        if (!remember) {
            UserSession()
        } else {
            UserSession(
                userId = prefs[Keys.USER_ID] ?: 0L,
                fullName = prefs[Keys.FULL_NAME].orEmpty(),
                email = prefs[Keys.EMAIL].orEmpty(),
                phone = prefs[Keys.PHONE].orEmpty(),
                address = prefs[Keys.ADDRESS].orEmpty(),
                avatarUrl = prefs[Keys.AVATAR_URL].orEmpty(),
                role = UserRole.fromApi(prefs[Keys.ROLE]),
                approvalStatus = ApprovalStatus.fromApi(prefs[Keys.APPROVAL_STATUS]),
                token = prefs[Keys.AUTH_TOKEN].orEmpty(),
                rememberSession = true,
                specialization = prefs[Keys.SPECIALIZATION].orEmpty(),
                qualification = prefs[Keys.QUALIFICATION].orEmpty(),
                yearsOfExperience = prefs[Keys.YEARS_EXPERIENCE] ?: 0,
                bio = prefs[Keys.BIO].orEmpty(),
                availability = EngineerAvailability.fromApi(prefs[Keys.AVAILABILITY]),
                storeName = prefs[Keys.STORE_NAME].orEmpty(),
                storeLogoUrl = prefs[Keys.STORE_LOGO_URL].orEmpty(),
                storeImageUrl = prefs[Keys.STORE_IMAGE_URL].orEmpty(),
                commercialRegister = prefs[Keys.COMMERCIAL_REGISTER].orEmpty()
            )
        }
    }

    val cartItemsFlow: Flow<List<CartItem>> = context.smartSolarDataStore.data.map { prefs ->
        val json = prefs[Keys.CART_JSON].orEmpty()
        if (json.isBlank()) {
            emptyList()
        } else {
            runCatching {
                cartAdapter.fromJson(json).orEmpty().map {
                    CartItem(
                        productId = it.productId,
                        productName = it.productName,
                        imageUrl = it.imageUrl,
                        unitPrice = it.unitPrice,
                        quantity = it.quantity,
                        availableStock = it.availableStock,
                        vendorId = it.vendorId
                    )
                }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun getCurrentServerConfig(): ServerConfig = serverConfigFlow.first()

    suspend fun getCurrentAuthToken(): String {
        val ephemeral = _ephemeralSession.value
        if (ephemeral != null && ephemeral.token.isNotBlank()) return ephemeral.token
        return persistedSessionFlow.first().token
    }

    suspend fun getCurrentSession(): UserSession {
        val ephemeral = _ephemeralSession.value
        if (ephemeral != null && ephemeral.isAuthenticated) return ephemeral
        return persistedSessionFlow.first()
    }

    suspend fun saveServerConfig(rawBaseUrl: String, timeoutSeconds: Int) {
        val normalized = normalizeBaseUrl(rawBaseUrl)
        context.smartSolarDataStore.edit { prefs ->
            prefs[Keys.SERVER_BASE_URL] = normalized
            prefs[Keys.TIMEOUT_SECONDS] = timeoutSeconds.coerceIn(10, 120)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.smartSolarDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveUserSession(session: UserSession) {
        if (session.rememberSession) {
            _ephemeralSession.value = session
            context.smartSolarDataStore.edit { prefs ->
                prefs[Keys.REMEMBER_SESSION] = true
                prefs[Keys.AUTH_TOKEN] = session.token
                prefs[Keys.USER_ID] = session.userId
                prefs[Keys.FULL_NAME] = session.fullName
                prefs[Keys.EMAIL] = session.email
                prefs[Keys.PHONE] = session.phone
                prefs[Keys.ADDRESS] = session.address
                prefs[Keys.AVATAR_URL] = session.avatarUrl
                prefs[Keys.ROLE] = session.role.apiValue
                prefs[Keys.APPROVAL_STATUS] = session.approvalStatus.apiValue
                prefs[Keys.SPECIALIZATION] = session.specialization
                prefs[Keys.QUALIFICATION] = session.qualification
                prefs[Keys.YEARS_EXPERIENCE] = session.yearsOfExperience
                prefs[Keys.BIO] = session.bio
                prefs[Keys.AVAILABILITY] = session.availability.apiValue
                prefs[Keys.STORE_NAME] = session.storeName
                prefs[Keys.STORE_LOGO_URL] = session.storeLogoUrl
                prefs[Keys.STORE_IMAGE_URL] = session.storeImageUrl
                prefs[Keys.COMMERCIAL_REGISTER] = session.commercialRegister
            }
        } else {
            _ephemeralSession.value = session
            clearPersistedAuthFields()
        }
    }

    suspend fun clearSession() {
        _ephemeralSession.value = null
        clearPersistedAuthFields()
    }

    private suspend fun clearPersistedAuthFields() {
        context.smartSolarDataStore.edit { prefs ->
            prefs[Keys.REMEMBER_SESSION] = false
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PHONE)
            prefs.remove(Keys.ADDRESS)
            prefs.remove(Keys.AVATAR_URL)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.APPROVAL_STATUS)
            prefs.remove(Keys.SPECIALIZATION)
            prefs.remove(Keys.QUALIFICATION)
            prefs.remove(Keys.YEARS_EXPERIENCE)
            prefs.remove(Keys.BIO)
            prefs.remove(Keys.AVAILABILITY)
            prefs.remove(Keys.STORE_NAME)
            prefs.remove(Keys.STORE_LOGO_URL)
            prefs.remove(Keys.STORE_IMAGE_URL)
            prefs.remove(Keys.COMMERCIAL_REGISTER)
        }
    }

    suspend fun saveCartItems(items: List<CartItem>) {
        val dtoList = items.map {
            StoredCartItemDto(
                productId = it.productId,
                productName = it.productName,
                imageUrl = it.imageUrl,
                unitPrice = it.unitPrice,
                quantity = it.quantity,
                availableStock = it.availableStock,
                vendorId = it.vendorId
            )
        }
        val json = cartAdapter.toJson(dtoList)
        context.smartSolarDataStore.edit { prefs ->
            prefs[Keys.CART_JSON] = json
        }
    }

    companion object {
        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) return ""
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
