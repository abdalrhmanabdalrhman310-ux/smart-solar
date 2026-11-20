package com.smartsolar.app.domain.model

enum class ProductCategory(val apiValue: String, val arabicLabel: String) {
    ALL("all", "الكل"),
    SOLAR_PANELS("solar_panels", "الألواح الشمسية"),
    BATTERIES("batteries", "البطاريات"),
    INVERTERS("inverters", "الانفرترات"),
    CHARGE_CONTROLLERS("charge_controllers", "منظمات الشحن"),
    ACCESSORIES("accessories", "ملحقات الطاقة الشمسية");

    companion object {
        fun fromApi(value: String?): ProductCategory {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when {
                normalized.contains("panel") || normalized.contains("ألواح") || normalized.contains("لوح") -> SOLAR_PANELS
                normalized.contains("batter") || normalized.contains("بطار") -> BATTERIES
                normalized.contains("invert") || normalized.contains("انفرتر") || normalized.contains("محول") -> INVERTERS
                normalized.contains("control") || normalized.contains("منظم") -> CHARGE_CONTROLLERS
                normalized.contains("access") || normalized.contains("ملحق") -> ACCESSORIES
                else -> ACCESSORIES
            }
        }
    }
}

data class Product(
    val id: Long,
    val vendorId: Long = 0L,
    val vendorName: String = "",
    val storeName: String = "",
    val name: String,
    val description: String,
    val category: ProductCategory,
    val rawCategory: String = category.arabicLabel,
    val price: Double,
    val stock: Int,
    val imageUrl: String = "",
    val isFeatured: Boolean = false,
    val createdAt: String = ""
) {
    val isAvailable: Boolean get() = stock > 0
}

data class CartItem(
    val productId: Long,
    val productName: String,
    val imageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val availableStock: Int,
    val vendorId: Long = 0L
) {
    val lineTotal: Double get() = unitPrice * quantity
}

enum class OrderStatus(val apiValue: String, val arabicLabel: String) {
    PENDING("pending", "قيد الانتظار"),
    CONFIRMED("confirmed", "مؤكد"),
    PROCESSING("processing", "قيد التجهيز"),
    SHIPPED("shipped", "تم الشحن"),
    DELIVERED("delivered", "مكتمل"),
    CANCELLED("cancelled", "ملغي");

    companion object {
        fun fromApi(value: String?): OrderStatus {
            return when (value?.trim()?.lowercase()) {
                "confirmed", "accepted", "مؤكد" -> CONFIRMED
                "processing", "in_progress", "قيد التجهيز", "قيد التنفيذ" -> PROCESSING
                "shipped", "on_the_way", "تم الشحن" -> SHIPPED
                "delivered", "completed", "done", "مكتمل" -> DELIVERED
                "cancelled", "canceled", "rejected", "ملغي" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

data class OrderItem(
    val id: Long = 0L,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double = quantity * unitPrice,
    val imageUrl: String = ""
)

data class Order(
    val id: Long,
    val orderNumber: String,
    val clientId: Long = 0L,
    val clientName: String = "",
    val clientPhone: String = "",
    val shippingAddress: String = "",
    val notes: String = "",
    val totalAmount: Double,
    val status: OrderStatus,
    val createdAt: String,
    val items: List<OrderItem> = emptyList()
)

data class VendorDashboardStats(
    val totalProducts: Int = 0,
    val availableProducts: Int = 0,
    val totalOrders: Int = 0,
    val totalSalesAmount: Double? = null
)
