package com.smartsolar.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartsolar.app.AppContainer
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.repository.CatalogRepository
import com.smartsolar.app.domain.model.Order
import com.smartsolar.app.domain.model.Product
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.VendorDashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VendorViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _productsState = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.Loading)
    val productsState: StateFlow<NetworkResult<List<Product>>> = _productsState.asStateFlow()

    private val _ordersState = MutableStateFlow<NetworkResult<List<Order>>>(NetworkResult.Loading)
    val ordersState: StateFlow<NetworkResult<List<Order>>> = _ordersState.asStateFlow()

    private val _stats = MutableStateFlow(VendorDashboardStats())
    val stats: StateFlow<VendorDashboardStats> = _stats.asStateFlow()

    private val _productMutationState = MutableStateFlow<NetworkResult<Product>?>(null)
    val productMutationState: StateFlow<NetworkResult<Product>?> = _productMutationState.asStateFlow()

    private val _deleteProductState = MutableStateFlow<NetworkResult<Unit>?>(null)
    val deleteProductState: StateFlow<NetworkResult<Unit>?> = _deleteProductState.asStateFlow()

    private var lastServerSalesTotal: Double? = null

    fun loadVendorDashboard(vendorId: Long) {
        loadVendorProducts(vendorId)
        loadVendorOrders()
    }

    fun loadVendorProducts(vendorId: Long) {
        viewModelScope.launch {
            _productsState.value = NetworkResult.Loading
            when (val res = catalogRepository.fetchProducts(vendorId = vendorId)) {
                is NetworkResult.Success -> {
                    // Enforce strict vendor isolation: never allow modifying other vendors' products
                    val ownProducts = res.data.filter { it.vendorId == 0L || it.vendorId == vendorId }
                    _productsState.value = NetworkResult.Success(ownProducts)
                    recalculateStats()
                }
                is NetworkResult.Error -> _productsState.value = res
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadVendorOrders() {
        viewModelScope.launch {
            _ordersState.value = NetworkResult.Loading
            when (val res = catalogRepository.fetchOrders("vendor")) {
                is NetworkResult.Success -> {
                    _ordersState.value = NetworkResult.Success(res.data.first)
                    lastServerSalesTotal = res.data.second
                    recalculateStats()
                }
                is NetworkResult.Error -> _ordersState.value = res
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun saveProduct(
        currentVendorId: Long,
        editingProduct: Product?,
        name: String,
        description: String,
        category: ProductCategory,
        priceText: String,
        stockText: String,
        imageUri: Uri?
    ) {
        if (editingProduct != null && editingProduct.vendorId != 0L && editingProduct.vendorId != currentVendorId) {
            _productMutationState.value = NetworkResult.Error(
                NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
                "غير مسموح بتعديل منتجات بائع آخر."
            )
            return
        }
        val price = priceText.trim().toDoubleOrNull()
        val stock = stockText.trim().toIntOrNull()
        val error = when {
            name.trim().length < 2 -> "يرجى إدخال اسم المنتج."
            description.trim().length < 5 -> "يرجى إدخال وصف واضح للمنتج."
            category == ProductCategory.ALL -> "يرجى اختيار تصنيف المنتج."
            price == null || price <= 0.0 -> "يرجى إدخال سعر صحيح."
            stock == null || stock < 0 -> "يرجى إدخال كمية مخزون صحيحة."
            else -> null
        }
        if (error != null) {
            _productMutationState.value = NetworkResult.Error(NetworkErrorType.VALIDATION_ERROR, error)
            return
        }

        viewModelScope.launch {
            _productMutationState.value = NetworkResult.Loading
            val res = catalogRepository.saveVendorProduct(
                productId = editingProduct?.id,
                name = name,
                description = description,
                category = category,
                price = price ?: 0.0,
                stock = stock ?: 0,
                imageUri = imageUri
            )
            if (res is NetworkResult.Success) {
                loadVendorProducts(currentVendorId)
            }
            _productMutationState.value = res
        }
    }

    fun deleteProduct(currentVendorId: Long, product: Product) {
        if (product.vendorId != 0L && product.vendorId != currentVendorId) {
            _deleteProductState.value = NetworkResult.Error(
                NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
                "غير مسموح بحذف منتجات بائع آخر."
            )
            return
        }
        viewModelScope.launch {
            _deleteProductState.value = NetworkResult.Loading
            val res = catalogRepository.deleteVendorProduct(product.id)
            if (res is NetworkResult.Success) {
                loadVendorProducts(currentVendorId)
            }
            _deleteProductState.value = res
        }
    }

    fun clearProductMutationState() {
        _productMutationState.value = null
    }

    fun clearDeleteState() {
        _deleteProductState.value = null
    }

    private fun recalculateStats() {
        val products = (_productsState.value as? NetworkResult.Success)?.data.orEmpty()
        val orders = (_ordersState.value as? NetworkResult.Success)?.data.orEmpty()
        _stats.value = catalogRepository.computeVendorStats(products, orders, lastServerSalesTotal)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VendorViewModel(container.catalogRepository) as T
                }
            }
    }
}
