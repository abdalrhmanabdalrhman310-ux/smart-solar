package com.smartsolar.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiBaseResponse<T>(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val error: String? = null,
    val token: String? = null,
    val data: T? = null,
    @Json(name = "session_expired") val sessionExpired: Boolean? = null
) {
    fun isOperationSuccess(): Boolean {
        if (sessionExpired == true) return false
        if (success != null) return success
        val st = status?.trim()?.lowercase()
        return st == "success" || st == "ok" || st == "1" || st == "true"
    }

    fun resolvedMessage(): String {
        return message?.takeIf { it.isNotBlank() }
            ?: error?.takeIf { it.isNotBlank() }
            ?: ""
    }
}

@JsonClass(generateAdapter = true)
data class AuthUserDto(
    val id: Long? = null,
    @Json(name = "user_id") val userId: Long? = null,
    @Json(name = "full_name") val fullName: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val avatar: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val role: String? = null,
    @Json(name = "approval_status") val approvalStatus: String? = null,
    @Json(name = "is_approved") val isApproved: Boolean? = null,
    val token: String? = null,
    val specialization: String? = null,
    val qualification: String? = null,
    @Json(name = "years_experience") val yearsExperience: Int? = null,
    val bio: String? = null,
    val availability: String? = null,
    @Json(name = "store_name") val storeName: String? = null,
    @Json(name = "store_logo") val storeLogo: String? = null,
    @Json(name = "store_image") val storeImage: String? = null,
    @Json(name = "commercial_register") val commercialRegister: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthPayloadDto(
    val token: String? = null,
    val user: AuthUserDto? = null,
    @Json(name = "approval_status") val approvalStatus: String? = null,
    val requiresApproval: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val action: String = "login",
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterClientRequestDto(
    val action: String = "register",
    val role: String = "client",
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val address: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    val action: String = "update_profile",
    @Json(name = "full_name") val fullName: String,
    val phone: String,
    val address: String
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: Long = 0L,
    @Json(name = "vendor_id") val vendorId: Long? = null,
    @Json(name = "vendor_name") val vendorName: String? = null,
    @Json(name = "store_name") val storeName: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val stock: Int? = null,
    val quantity: Int? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val image: String? = null,
    @Json(name = "is_featured") val isFeatured: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ProductsPayloadDto(
    val products: List<ProductDto>? = null,
    val items: List<ProductDto>? = null
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    val id: Long? = null,
    @Json(name = "product_id") val productId: Long = 0L,
    @Json(name = "product_name") val productName: String? = null,
    val quantity: Int = 1,
    @Json(name = "unit_price") val unitPrice: Double? = null,
    val price: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    val id: Long = 0L,
    @Json(name = "order_number") val orderNumber: String? = null,
    @Json(name = "client_id") val clientId: Long? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "client_phone") val clientPhone: String? = null,
    @Json(name = "shipping_address") val shippingAddress: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @Json(name = "total_amount") val totalAmount: Double? = null,
    val total: Double? = null,
    val status: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val items: List<OrderItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class OrdersPayloadDto(
    val orders: List<OrderDto>? = null,
    val items: List<OrderDto>? = null,
    @Json(name = "total_sales") val totalSales: Double? = null
)

@JsonClass(generateAdapter = true)
data class CreateOrderRequestDto(
    val action: String = "create",
    @Json(name = "shipping_address") val shippingAddress: String,
    val phone: String,
    val notes: String,
    val items: List<CreateOrderItemRequestDto>
)

@JsonClass(generateAdapter = true)
data class CreateOrderItemRequestDto(
    @Json(name = "product_id") val productId: Long,
    val quantity: Int
)

@JsonClass(generateAdapter = true)
data class EngineerDto(
    val id: Long = 0L,
    @Json(name = "user_id") val userId: Long? = null,
    @Json(name = "full_name") val fullName: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val specialization: String? = null,
    val qualification: String? = null,
    @Json(name = "years_experience") val yearsExperience: Int? = null,
    val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val avatar: String? = null,
    @Json(name = "portfolio_images") val portfolioImages: List<String>? = null,
    @Json(name = "certificate_images") val certificateImages: List<String>? = null,
    @Json(name = "average_rating") val averageRating: Double? = null,
    val rating: Double? = null,
    @Json(name = "ratings_count") val ratingsCount: Int? = null,
    val availability: String? = null
)

@JsonClass(generateAdapter = true)
data class EngineersPayloadDto(
    val engineers: List<EngineerDto>? = null,
    val items: List<EngineerDto>? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAvailabilityRequestDto(
    val action: String = "update_availability",
    val availability: String
)

@JsonClass(generateAdapter = true)
data class ServiceTaskDto(
    val id: Long = 0L,
    @Json(name = "request_number") val requestNumber: String? = null,
    @Json(name = "client_id") val clientId: Long? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "client_phone") val clientPhone: String? = null,
    val location: String? = null,
    @Json(name = "system_details") val systemDetails: String? = null,
    @Json(name = "issue_type") val issueType: String? = null,
    val description: String? = null,
    @Json(name = "fault_details") val faultDetails: String? = null,
    @Json(name = "engineer_id") val engineerId: Long? = null,
    @Json(name = "engineer_name") val engineerName: String? = null,
    val status: String? = null,
    @Json(name = "technical_report") val technicalReport: String? = null,
    @Json(name = "requested_date") val requestedDate: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "is_rated") val isRated: Boolean? = null,
    @Json(name = "can_accept") val canAccept: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ServiceTasksPayloadDto(
    val installations: List<ServiceTaskDto>? = null,
    val maintenance: List<ServiceTaskDto>? = null,
    val tasks: List<ServiceTaskDto>? = null,
    val items: List<ServiceTaskDto>? = null
)

@JsonClass(generateAdapter = true)
data class CreateInstallationRequestDto(
    val action: String = "create",
    val location: String,
    @Json(name = "system_details") val systemDetails: String,
    val description: String,
    @Json(name = "requested_date") val requestedDate: String,
    @Json(name = "engineer_id") val engineerId: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateMaintenanceRequestDto(
    val action: String = "create",
    @Json(name = "system_details") val systemDetails: String,
    @Json(name = "issue_type") val issueType: String,
    @Json(name = "fault_details") val faultDetails: String,
    val location: String,
    @Json(name = "requested_date") val requestedDate: String,
    @Json(name = "engineer_id") val engineerId: Long? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTaskStatusRequestDto(
    val action: String = "update_status",
    val id: Long,
    val status: String,
    @Json(name = "technical_report") val technicalReport: String = ""
)

@JsonClass(generateAdapter = true)
data class SaveSolarDesignRequestDto(
    val action: String = "save",
    val title: String,
    @Json(name = "total_load_watts") val totalLoadWatts: Double,
    @Json(name = "daily_kwh") val dailyKwh: Double,
    @Json(name = "system_kwp") val systemKwp: Double,
    @Json(name = "panel_count") val panelCount: Int,
    @Json(name = "panel_watts") val panelWatts: Int,
    @Json(name = "battery_capacity_wh") val batteryCapacityWh: Double,
    @Json(name = "battery_count") val batteryCount: Int,
    @Json(name = "inverter_watts") val inverterWatts: Int,
    @Json(name = "assumptions_note") val assumptionsNote: String
)

@JsonClass(generateAdapter = true)
data class SolarDesignDto(
    val id: Long = 0L,
    val title: String? = null,
    @Json(name = "total_load_watts") val totalLoadWatts: Double? = null,
    @Json(name = "daily_kwh") val dailyKwh: Double? = null,
    @Json(name = "system_kwp") val systemKwp: Double? = null,
    @Json(name = "panel_count") val panelCount: Int? = null,
    @Json(name = "panel_watts") val panelWatts: Int? = null,
    @Json(name = "battery_capacity_wh") val batteryCapacityWh: Double? = null,
    @Json(name = "battery_count") val batteryCount: Int? = null,
    @Json(name = "inverter_watts") val inverterWatts: Int? = null,
    @Json(name = "assumptions_note") val assumptionsNote: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SolarDesignsPayloadDto(
    val designs: List<SolarDesignDto>? = null,
    val items: List<SolarDesignDto>? = null
)

@JsonClass(generateAdapter = true)
data class EngineerRatingDto(
    val id: Long = 0L,
    @Json(name = "engineer_id") val engineerId: Long = 0L,
    @Json(name = "client_id") val clientId: Long? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "service_id") val serviceId: Long? = null,
    @Json(name = "service_type") val serviceType: String? = null,
    val stars: Int = 5,
    val comment: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RatingsPayloadDto(
    val ratings: List<EngineerRatingDto>? = null,
    val items: List<EngineerRatingDto>? = null,
    @Json(name = "average_rating") val averageRating: Double? = null,
    val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class SubmitRatingRequestDto(
    val action: String = "create",
    @Json(name = "engineer_id") val engineerId: Long,
    @Json(name = "service_id") val serviceId: Long,
    @Json(name = "service_type") val serviceType: String,
    val stars: Int,
    val comment: String
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Long = 0L,
    val title: String? = null,
    val message: String? = null,
    val type: String? = null,
    @Json(name = "is_read") val isRead: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationsPayloadDto(
    val notifications: List<NotificationDto>? = null,
    val items: List<NotificationDto>? = null
)

@JsonClass(generateAdapter = true)
data class MarkNotificationReadRequestDto(
    val action: String = "mark_read",
    val id: Long
)

@JsonClass(generateAdapter = true)
data class PlatformSettingsDto(
    @Json(name = "site_name") val siteName: String? = null,
    @Json(name = "require_engineer_approval") val requireEngineerApproval: Boolean? = null,
    @Json(name = "require_vendor_approval") val requireVendorApproval: Boolean? = null,
    @Json(name = "require_commercial_register") val requireCommercialRegister: Boolean? = null,
    val currency: String? = null
)

@JsonClass(generateAdapter = true)
data class GenericActionRequestDto(
    val action: String,
    val id: Long? = null
)
