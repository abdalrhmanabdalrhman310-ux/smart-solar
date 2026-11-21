package com.smartsolar.app.domain.model

enum class UserRole(val apiValue: String, val arabicLabel: String) {
    CLIENT("client", "عميل"),
    ENGINEER("engineer", "مهندس"),
    VENDOR("vendor", "بائع");

    companion object {
        fun fromApi(value: String?): UserRole {
            return when (value?.trim()?.lowercase()) {
                "engineer", "مهندس" -> ENGINEER
                "vendor", "seller", "بائع" -> VENDOR
                else -> CLIENT
            }
        }
    }
}

enum class ApprovalStatus(val apiValue: String, val arabicLabel: String) {
    APPROVED("approved", "مفعل ومعتمد"),
    PENDING("pending", "بانتظار موافقة الإدارة"),
    REJECTED("rejected", "مرفوض");

    companion object {
        fun fromApi(value: String?): ApprovalStatus {
            return when (value?.trim()?.lowercase()) {
                "pending", "waiting", "0", "false", "inactive" -> PENDING
                "rejected", "blocked", "suspended" -> REJECTED
                else -> APPROVED
            }
        }
    }
}

data class UserSession(
    val userId: Long = 0L,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val address: String = "",
    val avatarUrl: String = "",
    val role: UserRole = UserRole.CLIENT,
    val approvalStatus: ApprovalStatus = ApprovalStatus.APPROVED,
    val token: String = "",
    val rememberSession: Boolean = false,
    // Engineer-specific optional metadata
    val age: Int = 0,
    val specialization: String = "",
    val qualification: String = "",
    val yearsOfExperience: Int = 0,
    val bio: String = "",
    val previousProjects: String = "",
    val availability: EngineerAvailability = EngineerAvailability.AVAILABLE,
    // Vendor-specific optional metadata
    val storeName: String = "",
    val businessType: String = "",
    val storeDescription: String = "",
    val storeLogoUrl: String = "",
    val storeImageUrl: String = "",
    val commercialRegister: String = ""
) {
    val isAuthenticated: Boolean
        get() = userId > 0L && token.isNotBlank()

    val isApprovedForDashboard: Boolean
        get() = role == UserRole.CLIENT || approvalStatus == ApprovalStatus.APPROVED
}

data class ServerConfig(
    val baseUrl: String = "",
    val timeoutSeconds: Int = 30
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && (
            baseUrl.startsWith("https://", ignoreCase = true) ||
                baseUrl.startsWith("http://", ignoreCase = true)
            )

    val isHttps: Boolean
        get() = baseUrl.startsWith("https://", ignoreCase = true)
}
