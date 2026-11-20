package com.smartsolar.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartsolar.app.AppContainer
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.repository.CatalogRepository
import com.smartsolar.app.data.repository.ServicesRepository
import com.smartsolar.app.domain.calculator.SolarSystemCalculator
import com.smartsolar.app.domain.model.ApplianceLoad
import com.smartsolar.app.domain.model.AppNotification
import com.smartsolar.app.domain.model.CartItem
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.EngineerProfile
import com.smartsolar.app.domain.model.EngineerRating
import com.smartsolar.app.domain.model.Order
import com.smartsolar.app.domain.model.Product
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.SavedSolarDesign
import com.smartsolar.app.domain.model.ServiceTask
import com.smartsolar.app.domain.model.ServiceType
import com.smartsolar.app.domain.model.SolarAssumptions
import com.smartsolar.app.domain.model.SolarDesignResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel(
    private val sessionPreferences: SessionPreferencesManager,
    private val catalogRepository: CatalogRepository,
    private val servicesRepository: ServicesRepository
) : ViewModel() {

    // Products & Filtering
    private val _productsState = MutableStateFlow<NetworkResult<List<Product>>>(NetworkResult.Loading)
    val productsState: StateFlow<NetworkResult<List<Product>>> = _productsState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // Cart
    val cartItems: StateFlow<List<CartItem>> = sessionPreferences.cartItemsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _cartMessage = MutableStateFlow<String?>(null)
    val cartMessage: StateFlow<String?> = _cartMessage.asStateFlow()

    private val _stockVerificationState = MutableStateFlow<NetworkResult<List<CartItem>>?>(null)
    val stockVerificationState: StateFlow<NetworkResult<List<CartItem>>?> = _stockVerificationState.asStateFlow()

    private val _checkoutState = MutableStateFlow<NetworkResult<Order>?>(null)
    val checkoutState: StateFlow<NetworkResult<Order>?> = _checkoutState.asStateFlow()

    // Orders & Service Requests
    private val _ordersState = MutableStateFlow<NetworkResult<List<Order>>>(NetworkResult.Loading)
    val ordersState: StateFlow<NetworkResult<List<Order>>> = _ordersState.asStateFlow()

    private val _serviceTasksState = MutableStateFlow<NetworkResult<List<ServiceTask>>>(NetworkResult.Loading)
    val serviceTasksState: StateFlow<NetworkResult<List<ServiceTask>>> = _serviceTasksState.asStateFlow()

    // Solar Calculator
    private val _appliances = MutableStateFlow<List<ApplianceLoad>>(emptyList())
    val appliances: StateFlow<List<ApplianceLoad>> = _appliances.asStateFlow()

    private val _assumptions = MutableStateFlow(SolarAssumptions())
    val assumptions: StateFlow<SolarAssumptions> = _assumptions.asStateFlow()

    private val _designResult = MutableStateFlow(SolarSystemCalculator.calculate(emptyList(), SolarAssumptions()))
    val designResult: StateFlow<SolarDesignResult> = _designResult.asStateFlow()

    private val _saveDesignState = MutableStateFlow<NetworkResult<SavedSolarDesign>?>(null)
    val saveDesignState: StateFlow<NetworkResult<SavedSolarDesign>?> = _saveDesignState.asStateFlow()

    private val _savedDesignsState = MutableStateFlow<NetworkResult<List<SavedSolarDesign>>?>(null)
    val savedDesignsState: StateFlow<NetworkResult<List<SavedSolarDesign>>?> = _savedDesignsState.asStateFlow()

    // Pre-filled system details when navigating from calculator to installation request
    private val _draftInstallationSystemDetails = MutableStateFlow("")
    val draftInstallationSystemDetails: StateFlow<String> = _draftInstallationSystemDetails.asStateFlow()

    // Engineers & Selection
    private val _engineersState = MutableStateFlow<NetworkResult<List<EngineerProfile>>>(NetworkResult.Loading)
    val engineersState: StateFlow<NetworkResult<List<EngineerProfile>>> = _engineersState.asStateFlow()

    private val _engineerAvailabilityFilter = MutableStateFlow<EngineerAvailability?>(null)
    val engineerAvailabilityFilter: StateFlow<EngineerAvailability?> = _engineerAvailabilityFilter.asStateFlow()

    private val _selectedEngineerForRequest = MutableStateFlow<EngineerProfile?>(null)
    val selectedEngineerForRequest: StateFlow<EngineerProfile?> = _selectedEngineerForRequest.asStateFlow()

    private val _inspectedEngineer = MutableStateFlow<EngineerProfile?>(null)
    val inspectedEngineer: StateFlow<EngineerProfile?> = _inspectedEngineer.asStateFlow()

    private val _engineerRatingsState = MutableStateFlow<NetworkResult<List<EngineerRating>>>(NetworkResult.Loading)
    val engineerRatingsState: StateFlow<NetworkResult<List<EngineerRating>>> = _engineerRatingsState.asStateFlow()

    // Installation / Maintenance / Rating submission
    private val _installationSubmitState = MutableStateFlow<NetworkResult<ServiceTask>?>(null)
    val installationSubmitState: StateFlow<NetworkResult<ServiceTask>?> = _installationSubmitState.asStateFlow()

    private val _maintenanceSubmitState = MutableStateFlow<NetworkResult<ServiceTask>?>(null)
    val maintenanceSubmitState: StateFlow<NetworkResult<ServiceTask>?> = _maintenanceSubmitState.asStateFlow()

    private val _ratingSubmitState = MutableStateFlow<NetworkResult<Unit>?>(null)
    val ratingSubmitState: StateFlow<NetworkResult<Unit>?> = _ratingSubmitState.asStateFlow()

    private val _locallyRatedServiceKeys = MutableStateFlow<Set<String>>(emptySet())
    val locallyRatedServiceKeys: StateFlow<Set<String>> = _locallyRatedServiceKeys.asStateFlow()

    // Notifications
    private val _notificationsState = MutableStateFlow<NetworkResult<List<AppNotification>>>(NetworkResult.Loading)
    val notificationsState: StateFlow<NetworkResult<List<AppNotification>>> = _notificationsState.asStateFlow()

    fun loadHomeData() {
        loadProducts()
        loadOrders()
        loadNotifications()
    }

    fun loadProducts(
        category: ProductCategory = _selectedCategory.value,
        query: String = _searchQuery.value
    ) {
        _selectedCategory.value = category
        _searchQuery.value = query
        viewModelScope.launch {
            _productsState.value = NetworkResult.Loading
            _productsState.value = catalogRepository.fetchProducts(
                category = category,
                searchQuery = query
            )
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            if (product.stock <= 0) {
                _cartMessage.value = "هذا المنتج غير متوفر في المخزون حالياً."
                return@launch
            }
            val current = cartItems.value.toMutableList()
            val idx = current.indexOfFirst { it.productId == product.id }
            if (idx >= 0) {
                val existing = current[idx]
                val newQty = existing.quantity + quantity
                if (newQty > product.stock) {
                    _cartMessage.value = "لا يمكن تجاوز الكمية المتوفرة في المخزون (${product.stock})."
                    return@launch
                }
                current[idx] = existing.copy(quantity = newQty, availableStock = product.stock, unitPrice = product.price)
            } else {
                val safeQty = quantity.coerceIn(1, product.stock)
                current.add(
                    CartItem(
                        productId = product.id,
                        productName = product.name,
                        imageUrl = product.imageUrl,
                        unitPrice = product.price,
                        quantity = safeQty,
                        availableStock = product.stock,
                        vendorId = product.vendorId
                    )
                )
            }
            sessionPreferences.saveCartItems(current)
            _cartMessage.value = "تمت إضافة \"${product.name}\" إلى السلة."
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        viewModelScope.launch {
            val current = cartItems.value.toMutableList()
            val idx = current.indexOfFirst { it.productId == productId }
            if (idx >= 0) {
                val item = current[idx]
                if (newQuantity <= 0) {
                    current.removeAt(idx)
                } else if (newQuantity > item.availableStock) {
                    _cartMessage.value = "الحد الأقصى المتوفر في المخزون هو ${item.availableStock}."
                    return@launch
                } else {
                    current[idx] = item.copy(quantity = newQuantity)
                }
                sessionPreferences.saveCartItems(current)
            }
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            val updated = cartItems.value.filterNot { it.productId == productId }
            sessionPreferences.saveCartItems(updated)
        }
    }

    fun consumeCartMessage() {
        _cartMessage.value = null
    }

    fun verifyCartBeforeCheckout() {
        viewModelScope.launch {
            _stockVerificationState.value = NetworkResult.Loading
            val res = catalogRepository.verifyCartStock(cartItems.value)
            if (res is NetworkResult.Success) {
                sessionPreferences.saveCartItems(res.data)
            }
            _stockVerificationState.value = res
        }
    }

    fun resetStockVerification() {
        _stockVerificationState.value = null
    }

    fun submitOrder(shippingAddress: String, phone: String, notes: String) {
        if (shippingAddress.trim().length < 4) {
            _checkoutState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "يرجى إدخال عنوان التوصيل بالكامل."
            )
            return
        }
        if (phone.trim().length < 7) {
            _checkoutState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "يرجى إدخال رقم هاتف صحيح للتواصل."
            )
            return
        }
        val items = cartItems.value
        if (items.isEmpty()) {
            _checkoutState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "سلة المشتريات فارغة."
            )
            return
        }

        viewModelScope.launch {
            _checkoutState.value = NetworkResult.Loading
            // Re-verify live stock with PHP server before submitting order
            when (val stockCheck = catalogRepository.verifyCartStock(items)) {
                is NetworkResult.Error -> {
                    _checkoutState.value = stockCheck
                    return@launch
                }
                is NetworkResult.Success -> {
                    val orderRes = catalogRepository.createOrder(shippingAddress, phone, notes, stockCheck.data)
                    if (orderRes is NetworkResult.Success) {
                        sessionPreferences.saveCartItems(emptyList())
                        loadOrders()
                    }
                    _checkoutState.value = orderRes
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = null
    }

    fun loadOrders() {
        viewModelScope.launch {
            _ordersState.value = NetworkResult.Loading
            when (val res = catalogRepository.fetchOrders("client")) {
                is NetworkResult.Success -> _ordersState.value = NetworkResult.Success(res.data.first)
                is NetworkResult.Error -> _ordersState.value = res
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadClientServiceRequests() {
        viewModelScope.launch {
            _serviceTasksState.value = NetworkResult.Loading
            _serviceTasksState.value = servicesRepository.fetchServiceTasks("client")
        }
    }

    // Solar Calculator Actions
    fun addAppliance(name: String, watts: Double, quantity: Int, hours: Double) {
        if (name.isBlank() || watts <= 0 || quantity <= 0 || hours <= 0) return
        val newList = _appliances.value + ApplianceLoad(
            id = "${System.currentTimeMillis()}_${_appliances.value.size}",
            name = name.trim(),
            powerWatts = watts,
            quantity = quantity,
            dailyHours = hours.coerceAtMost(24.0)
        )
        _appliances.value = newList
        _designResult.value = SolarSystemCalculator.calculate(newList, _assumptions.value)
    }

    fun removeAppliance(id: String) {
        val newList = _appliances.value.filterNot { it.id == id }
        _appliances.value = newList
        _designResult.value = SolarSystemCalculator.calculate(newList, _assumptions.value)
    }

    fun updateAssumptions(updated: SolarAssumptions) {
        _assumptions.value = updated
        _designResult.value = SolarSystemCalculator.calculate(_appliances.value, updated)
    }

    fun saveSolarDesign(title: String) {
        val result = _designResult.value
        if (result.dailyConsumptionKwh <= 0.0) {
            _saveDesignState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "يرجى إضافة جهاز كهربائي واحد على الأقل لحساب وحفظ التصميم."
            )
            return
        }
        viewModelScope.launch {
            _saveDesignState.value = NetworkResult.Loading
            _saveDesignState.value = servicesRepository.saveSolarDesign(title, result)
        }
    }

    fun clearSaveDesignState() {
        _saveDesignState.value = null
    }

    fun loadSavedSolarDesigns() {
        viewModelScope.launch {
            _savedDesignsState.value = NetworkResult.Loading
            _savedDesignsState.value = servicesRepository.fetchSavedSolarDesigns()
        }
    }

    fun prepareInstallationFromCalculator() {
        val res = _designResult.value
        if (res.dailyConsumptionKwh > 0) {
            _draftInstallationSystemDetails.value =
                "منظومة مقترحة بقدرة ${"%.2f".format(res.recommendedSystemKwp)} kWp | " +
                    "${res.panelCount} لوح (${res.panelWattage}W) | " +
                    "${res.batteryCount} بطارية (${res.singleBatterySpecLabel}) | " +
                    "انفرتر ${res.recommendedInverterWatts}W | استهلاك يومي ${"%.2f".format(res.dailyConsumptionKwh)} kWh"
        }
    }

    fun clearDraftInstallationDetails() {
        _draftInstallationSystemDetails.value = ""
    }

    // Engineers
    fun loadEngineers(availability: EngineerAvailability? = _engineerAvailabilityFilter.value) {
        _engineerAvailabilityFilter.value = availability
        viewModelScope.launch {
            _engineersState.value = NetworkResult.Loading
            _engineersState.value = servicesRepository.fetchEngineers(availability)
        }
    }

    fun inspectEngineer(engineer: EngineerProfile) {
        _inspectedEngineer.value = engineer
        viewModelScope.launch {
            _engineerRatingsState.value = NetworkResult.Loading
            _engineerRatingsState.value = servicesRepository.fetchEngineerRatings(engineer.id)
        }
    }

    fun selectEngineerForRequest(engineer: EngineerProfile?) {
        _selectedEngineerForRequest.value = engineer
    }

    // Installation & Maintenance Requests
    fun submitInstallationRequest(
        location: String,
        systemDetails: String,
        description: String,
        requestedDate: String
    ) {
        if (location.trim().length < 3 || systemDetails.trim().length < 3) {
            _installationSubmitState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "يرجى إدخال موقع التركيب وتفاصيل المنظومة المطلوبة."
            )
            return
        }
        viewModelScope.launch {
            _installationSubmitState.value = NetworkResult.Loading
            val res = servicesRepository.createInstallationRequest(
                location = location,
                systemDetails = systemDetails,
                description = description,
                requestedDate = requestedDate,
                engineerId = _selectedEngineerForRequest.value?.id
            )
            if (res is NetworkResult.Success) {
                loadClientServiceRequests()
            }
            _installationSubmitState.value = res
        }
    }

    fun clearInstallationSubmitState() {
        _installationSubmitState.value = null
    }

    fun submitMaintenanceRequest(
        systemDetails: String,
        issueType: String,
        faultDetails: String,
        location: String,
        requestedDate: String
    ) {
        if (systemDetails.trim().length < 3 || faultDetails.trim().length < 5 || location.trim().length < 3) {
            _maintenanceSubmitState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "يرجى استكمال وصف النظام وتفاصيل العطل وموقع الصيانة."
            )
            return
        }
        viewModelScope.launch {
            _maintenanceSubmitState.value = NetworkResult.Loading
            val res = servicesRepository.createMaintenanceRequest(
                systemDetails = systemDetails,
                issueType = issueType,
                faultDetails = faultDetails,
                location = location,
                requestedDate = requestedDate,
                engineerId = _selectedEngineerForRequest.value?.id
            )
            if (res is NetworkResult.Success) {
                loadClientServiceRequests()
            }
            _maintenanceSubmitState.value = res
        }
    }

    fun clearMaintenanceSubmitState() {
        _maintenanceSubmitState.value = null
    }

    // Ratings
    fun submitEngineerRating(
        task: ServiceTask,
        stars: Int,
        comment: String
    ) {
        val engId = task.engineerId ?: 0L
        if (engId <= 0L) {
            _ratingSubmitState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "لا يوجد مهندس معين لهذه الخدمة لتقييمه."
            )
            return
        }
        val key = "${task.serviceType.apiValue}_${task.id}"
        if (task.isRated || key in _locallyRatedServiceKeys.value) {
            _ratingSubmitState.value = NetworkResult.Error(
                NetworkErrorType.VALIDATION_ERROR,
                "تم تقييم هذه الخدمة مسبقاً ولا يمكن تكرار التقييم."
            )
            return
        }
        viewModelScope.launch {
            _ratingSubmitState.value = NetworkResult.Loading
            val res = servicesRepository.submitEngineerRating(
                engineerId = engId,
                serviceId = task.id,
                serviceType = task.serviceType,
                stars = stars,
                comment = comment
            )
            if (res is NetworkResult.Success) {
                _locallyRatedServiceKeys.value = _locallyRatedServiceKeys.value + key
                loadClientServiceRequests()
            }
            _ratingSubmitState.value = res
        }
    }

    fun clearRatingSubmitState() {
        _ratingSubmitState.value = null
    }

    // Notifications
    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = NetworkResult.Loading
            _notificationsState.value = servicesRepository.fetchNotifications()
        }
    }

    fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            val res = servicesRepository.markNotificationRead(notificationId)
            if (res is NetworkResult.Success) {
                val current = (_notificationsState.value as? NetworkResult.Success)?.data ?: return@launch
                _notificationsState.value = NetworkResult.Success(
                    current.map { if (it.id == notificationId) it.copy(isRead = true) else it }
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClientViewModel(
                        container.sessionPreferences,
                        container.catalogRepository,
                        container.servicesRepository
                    ) as T
                }
            }
    }
}
