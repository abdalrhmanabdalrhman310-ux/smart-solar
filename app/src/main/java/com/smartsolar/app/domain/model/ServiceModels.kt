package com.smartsolar.app.domain.model

enum class EngineerAvailability(val apiValue: String, val arabicLabel: String) {
    AVAILABLE("available", "متاح"),
    BUSY("busy", "مشغول"),
    UNAVAILABLE("unavailable", "غير متاح");

    companion object {
        fun fromApi(value: String?): EngineerAvailability {
            return when (value?.trim()?.lowercase()) {
                "busy", "occupied", "مشغول" -> BUSY
                "unavailable", "offline", "inactive", "غير متاح" -> UNAVAILABLE
                else -> AVAILABLE
            }
        }
    }
}

data class EngineerProfile(
    val id: Long,
    val userId: Long = 0L,
    val fullName: String,
    val email: String = "",
    val phone: String = "",
    val specialization: String,
    val qualification: String,
    val yearsOfExperience: Int,
    val bio: String,
    val avatarUrl: String = "",
    val portfolioImages: List<String> = emptyList(),
    val certificateImages: List<String> = emptyList(),
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val availability: EngineerAvailability = EngineerAvailability.AVAILABLE
)

data class EngineerRating(
    val id: Long,
    val engineerId: Long,
    val clientId: Long = 0L,
    val clientName: String = "عميل",
    val serviceId: Long = 0L,
    val serviceType: String = "",
    val stars: Int,
    val comment: String,
    val createdAt: String = ""
)

enum class TaskStatus(val apiValue: String, val arabicLabel: String) {
    PENDING("pending", "قيد الانتظار"),
    ACCEPTED("accepted", "مقبول"),
    IN_PROGRESS("in_progress", "قيد التنفيذ"),
    COMPLETED("completed", "مكتمل"),
    CANCELLED("cancelled", "ملغي");

    companion object {
        fun fromApi(value: String?): TaskStatus {
            return when (value?.trim()?.lowercase()) {
                "accepted", "approved", "مقبول" -> ACCEPTED
                "in_progress", "working", "active", "قيد التنفيذ" -> IN_PROGRESS
                "completed", "done", "finished", "مكتمل" -> COMPLETED
                "cancelled", "rejected", "ملغي" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

enum class ServiceType(val apiValue: String, val arabicLabel: String) {
    INSTALLATION("installation", "تركيب منظومة شمسية"),
    MAINTENANCE("maintenance", "صيانة منظومة شمسية")
}

data class ServiceTask(
    val id: Long,
    val requestNumber: String,
    val serviceType: ServiceType,
    val clientId: Long = 0L,
    val clientName: String = "",
    val clientPhone: String? = null, // Only shown when permitted by PHP policy
    val location: String = "",
    val systemDetails: String = "",
    val issueType: String = "",
    val description: String = "",
    val engineerId: Long? = null,
    val engineerName: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val technicalReport: String = "",
    val requestedDate: String = "",
    val createdAt: String = "",
    val isRated: Boolean = false,
    val canAccept: Boolean = true
)

data class AppNotification(
    val id: Long,
    val title: String,
    val message: String,
    val type: String = "",
    val isRead: Boolean = false,
    val createdAt: String = ""
)
