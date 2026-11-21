package com.smartsolar.app.data.repository

import android.content.Context
import android.net.Uri
import com.smartsolar.app.data.remote.CreateOrderItemRequestDto
import com.smartsolar.app.data.remote.CreateOrderRequestDto
import com.smartsolar.app.data.remote.GenericActionRequestDto
import com.smartsolar.app.data.remote.MultipartUploadHelper
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.remote.OrderDto
import com.smartsolar.app.data.remote.PhpEndpointContract
import com.smartsolar.app.data.remote.ProductDto
import com.smartsolar.app.domain.model.CartItem
import com.smartsolar.app.domain.model.Order
import com.smartsolar.app.domain.model.OrderItem
import com.smartsolar.app.domain.model.OrderStatus
import com.smartsolar.app.domain.model.Product
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.VendorDashboardStats
import okhttp3.MultipartBody

class CatalogRepository(
    private val context: Context,
    private val networkFactory: NetworkClientFactory
) {
    private val api get() = networkFactory.apiService

    suspend fun fetchProducts(
        category: ProductCategory? = null,
        searchQuery: String? = null,
        vendorId: Long? = null
    ): NetworkResult<List<Product>> {
        val catParam = category?.takeIf { it != ProductCategory.ALL }?.apiValue
        val searchParam = searchQuery?.trim()?.takeIf { it.isNotBlank() }
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_PRODUCTS) {
            api.getProducts(category = catParam, search = searchParam, vendorId = vendorId)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (resp.sessionExpired == true) {
                    NetworkResult.Error(
                        type = NetworkErrorType.UNAUTHORIZED_SESSION_EXPIRED,
                        arabicMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً.",
                        phpEndpoint = PhpEndpointContract.FILE_PRODUCTS
                    )
                } else if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر جلب المنتجات من الخادم." },
                        phpEndpoint = PhpEndpointContract.FILE_PRODUCTS
                    )
                } else {
                    val rawList = resp.data?.products ?: resp.data?.items.orEmpty()
                    NetworkResult.Success(rawList.map { it.toDomain() })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun verifyCartStock(cartItems: List<CartItem>): NetworkResult<List<CartItem>> {
        if (cartItems.isEmpty()) {
            return NetworkResult.Error(
                type = NetworkErrorType.VALIDATION_ERROR,
                arabicMessage = "سلة المشتريات فارغة.",
                phpEndpoint = PhpEndpointContract.FILE_PRODUCTS,
                needsPhpConfirmation = false
            )
        }
        return when (val productsRes = fetchProducts()) {
            is NetworkResult.Success -> {
                val liveById = productsRes.data.associateBy { it.id }
                val updated = mutableListOf<CartItem>()
                for (item in cartItems) {
                    val live = liveById[item.productId]
                    if (live == null) {
                        return NetworkResult.Error(
                            type = NetworkErrorType.VALIDATION_ERROR,
                            arabicMessage = "المنتج \"${item.productName}\" لم يعد متوفراً في المتجر.",
                            phpEndpoint = PhpEndpointContract.FILE_PRODUCTS,
                            needsPhpConfirmation = false
                        )
                    }
                    if (live.stock < item.quantity) {
                        return NetworkResult.Error(
                            type = NetworkErrorType.VALIDATION_ERROR,
                            arabicMessage = "الكمية المطلوبة من \"${live.name}\" (${item.quantity}) تتجاوز المتاح في المخزون (${live.stock}).",
                            phpEndpoint = PhpEndpointContract.FILE_PRODUCTS,
                            needsPhpConfirmation = false
                        )
                    }
                    updated.add(
                        item.copy(
                            unitPrice = live.price,
                            availableStock = live.stock,
                            productName = live.name,
                            imageUrl = live.imageUrl.ifBlank { item.imageUrl }
                        )
                    )
                }
                NetworkResult.Success(updated, "تم التحقق من توفر الكميات في المخزون بنجاح.")
            }
            is NetworkResult.Error -> productsRes
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun createOrder(
        shippingAddress: String,
        phone: String,
        notes: String,
        cartItems: List<CartItem>
    ): NetworkResult<Order> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_ORDERS) {
            api.createOrder(
                CreateOrderRequestDto(
                    shippingAddress = shippingAddress.trim(),
                    phone = phone.trim(),
                    notes = notes.trim(),
                    items = cartItems.map { CreateOrderItemRequestDto(it.productId, it.quantity) }
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر إتمام الطلب على الخادم." },
                        phpEndpoint = PhpEndpointContract.FILE_ORDERS
                    )
                } else {
                    val order = resp.data?.toDomain() ?: Order(
                        id = 0L,
                        orderNumber = "-",
                        shippingAddress = shippingAddress,
                        clientPhone = phone,
                        notes = notes,
                        totalAmount = cartItems.sumOf { it.lineTotal },
                        status = OrderStatus.PENDING,
                        createdAt = ""
                    )
                    NetworkResult.Success(order, resp.resolvedMessage().ifBlank { "تم إرسال الطلب بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun fetchOrders(role: String): NetworkResult<Pair<List<Order>, Double?>> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_ORDERS) {
            api.getOrders(role = role)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر تحميل قائمة الطلبات." },
                        phpEndpoint = PhpEndpointContract.FILE_ORDERS
                    )
                } else {
                    val list = (resp.data?.orders ?: resp.data?.items.orEmpty()).map { it.toDomain() }
                    NetworkResult.Success(list to resp.data?.totalSales)
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun saveVendorProduct(
        productId: Long?,
        name: String,
        description: String,
        category: ProductCategory,
        price: Double,
        stock: Int,
        imageUri: Uri?
    ): NetworkResult<Product> {
        var imagePart: MultipartBody.Part? = null
        if (imageUri != null) {
            when (val prepared = MultipartUploadHelper.prepareImagePart(context, imageUri, "image")) {
                is MultipartUploadHelper.PreparedPart.Valid -> imagePart = prepared.part
                is MultipartUploadHelper.PreparedPart.Invalid -> return NetworkResult.Error(
                    type = NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = prepared.arabicReason,
                    phpEndpoint = PhpEndpointContract.FILE_PRODUCTS,
                    needsPhpConfirmation = false
                )
            }
        }

        val actionName = if (productId == null || productId <= 0L) "create" else "update"
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_PRODUCTS) {
            api.saveVendorProductMultipart(
                action = MultipartUploadHelper.textPart(actionName),
                id = productId?.takeIf { it > 0L }?.let { MultipartUploadHelper.textPart(it.toString()) },
                name = MultipartUploadHelper.textPart(name.trim()),
                description = MultipartUploadHelper.textPart(description.trim()),
                category = MultipartUploadHelper.textPart(category.apiValue),
                price = MultipartUploadHelper.textPart(price.toString()),
                stock = MultipartUploadHelper.textPart(stock.toString()),
                image = imagePart
            )
        }

        return when (result) {
            is NetworkResult.Success -> {
                val resp = result.data
                if (!resp.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = resp.resolvedMessage().ifBlank { "تعذر حفظ المنتج." },
                        phpEndpoint = PhpEndpointContract.FILE_PRODUCTS
                    )
                } else {
                    val saved = resp.data?.toDomain() ?: Product(
                        id = productId ?: 0L,
                        name = name,
                        description = description,
                        category = category,
                        price = price,
                        stock = stock
                    )
                    NetworkResult.Success(saved, resp.resolvedMessage().ifBlank { "تم حفظ المنتج بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun deleteVendorProduct(productId: Long): NetworkResult<Unit> {
        val result = networkFactory.safeApiCall(PhpEndpointContract.FILE_PRODUCTS) {
            api.deleteVendorProduct(GenericActionRequestDto(action = "delete", id = productId))
        }
        return when (result) {
            is NetworkResult.Success -> {
                if (!result.data.isOperationSuccess()) {
                    NetworkResult.Error(
                        type = NetworkErrorType.SERVER_ERROR,
                        arabicMessage = result.data.resolvedMessage().ifBlank { "تعذر حذف المنتج." },
                        phpEndpoint = PhpEndpointContract.FILE_PRODUCTS
                    )
                } else {
                    NetworkResult.Success(Unit, result.data.resolvedMessage().ifBlank { "تم حذف المنتج بنجاح." })
                }
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    fun computeVendorStats(
        products: List<Product>,
        orders: List<Order>,
        serverTotalSales: Double?
    ): VendorDashboardStats {
        return VendorDashboardStats(
            totalProducts = products.size,
            availableProducts = products.count { it.stock > 0 },
            totalOrders = orders.size,
            totalSalesAmount = serverTotalSales
        )
    }

    private suspend fun ProductDto.toDomain(): Product {
        val cat = ProductCategory.fromApi(category)
        return Product(
            id = id,
            vendorId = vendorId ?: 0L,
            vendorName = vendorName.orEmpty(),
            storeName = storeName.orEmpty(),
            name = name ?: title ?: "منتج طاقة شمسية",
            description = description.orEmpty(),
            category = cat,
            rawCategory = category ?: cat.arabicLabel,
            price = price ?: 0.0,
            stock = stock ?: quantity ?: 0,
            imageUrl = networkFactory.resolveAssetUrl(imageUrl ?: image),
            isFeatured = isFeatured ?: false,
            createdAt = createdAt.orEmpty()
        )
    }

    private suspend fun OrderDto.toDomain(): Order {
        return Order(
            id = id,
            orderNumber = orderNumber?.takeIf { it.isNotBlank() } ?: "#$id",
            clientId = clientId ?: 0L,
            clientName = clientName.orEmpty(),
            clientPhone = clientPhone.orEmpty(),
            shippingAddress = shippingAddress ?: address.orEmpty(),
            notes = notes.orEmpty(),
            totalAmount = totalAmount ?: total ?: 0.0,
            status = OrderStatus.fromApi(status),
            createdAt = createdAt.orEmpty(),
            items = items.orEmpty().map { item ->
                val price = item.unitPrice ?: item.price ?: 0.0
                OrderItem(
                    id = item.id ?: 0L,
                    productId = item.productId,
                    productName = item.productName ?: "منتج #${item.productId}",
                    quantity = item.quantity,
                    unitPrice = price,
                    subtotal = price * item.quantity,
                    imageUrl = networkFactory.resolveAssetUrl(item.imageUrl)
                )
            }
        )
    }
}
