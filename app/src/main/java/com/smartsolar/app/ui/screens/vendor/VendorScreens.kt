package com.smartsolar.app.ui.screens.vendor

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.Order
import com.smartsolar.app.domain.model.Product
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.UserSession
import com.smartsolar.app.ui.components.EmptyStateView
import com.smartsolar.app.ui.components.ErrorStateView
import com.smartsolar.app.ui.components.InlineResultBanner
import com.smartsolar.app.ui.components.LoadingStateView
import com.smartsolar.app.ui.components.OrderStatusBadge
import com.smartsolar.app.ui.components.SingleImagePickerField
import com.smartsolar.app.ui.components.SmartSolarTopBar
import com.smartsolar.app.ui.components.SolarCard
import com.smartsolar.app.ui.components.SolarNetworkImage
import com.smartsolar.app.ui.components.StatusPill
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarRedError
import com.smartsolar.app.ui.theme.SolarRedSoft
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.VendorViewModel

@Composable
fun VendorHomeScreen(
    session: UserSession,
    vendorViewModel: VendorViewModel,
    onAddProduct: () -> Unit,
    onManageProducts: () -> Unit,
    onViewOrders: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val stats by vendorViewModel.stats.collectAsState()
    val productsState by vendorViewModel.productsState.collectAsState()
    val ordersState by vendorViewModel.ordersState.collectAsState()

    LaunchedEffect(session.userId) {
        vendorViewModel.loadVendorDashboard(session.userId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.storeLogoUrl.isNotBlank()) {
                        SolarNetworkImage(
                            imageUrl = session.storeLogoUrl,
                            contentDescription = session.storeName,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "شعار المتجر",
                            tint = SolarGreenPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.storeName.ifBlank { "متجر الطاقة الشمسية" },
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarTextPrimary
                        )
                        Text(
                            text = "البائع: ${session.fullName.ifBlank { session.email }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SolarTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VendorStatCard("عدد المنتجات", "${stats.totalProducts}", Modifier.weight(1f))
                VendorStatCard("المنتجات المتاحة", "${stats.availableProducts}", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VendorStatCard("الطلبات", "${stats.totalOrders}", Modifier.weight(1f))
                VendorStatCard(
                    "إجمالي المبيعات",
                    stats.totalSalesAmount?.let { "${"%.2f".format(it)} $" } ?: "غير متوفرة",
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddProduct,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("vendor_home_add_product"),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة منتج")
                }
                OutlinedButton(
                    onClick = onManageProducts,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("إدارة منتجاتي")
                }
            }
        }

        if (productsState is NetworkResult.Error) {
            item {
                ErrorStateView(
                    error = productsState as NetworkResult.Error,
                    onRetry = { vendorViewModel.loadVendorDashboard(session.userId) },
                    onOpenServerSettings = onOpenServerSettings
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أحدث طلبات المتجر", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onViewOrders) { Text("كل الطلبات", color = SolarGreenDark) }
            }
        }

        when (val oState = ordersState) {
            NetworkResult.Loading -> item { LoadingStateView("جاري تحميل الطلبات...") }
            is NetworkResult.Error -> item {
                InlineResultBanner(oState.arabicMessage, isSuccess = false)
            }
            is NetworkResult.Success -> {
                if (oState.data.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "لا توجد طلبات حالياً",
                            subtitle = "ستظهر هنا الطلبات المتعلقة بمنتجات متجرك فور وصولها."
                        )
                    }
                } else {
                    items(oState.data.take(4), key = { it.id }) { order ->
                        VendorOrderCard(order = order, onClick = onViewOrders)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    SolarCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = SolarGreenPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
        }
    }
}

@Composable
fun VendorProductsScreen(
    session: UserSession,
    vendorViewModel: VendorViewModel,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val productsState by vendorViewModel.productsState.collectAsState()
    val deleteState by vendorViewModel.deleteProductState.collectAsState()
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(session.userId) {
        vendorViewModel.loadVendorProducts(session.userId)
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = { SmartSolarTopBar(title = "إدارة منتجات المتجر") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = SolarGreenPrimary,
                contentColor = SolarWhite,
                modifier = Modifier.testTag("vendor_fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val dState = deleteState) {
                is NetworkResult.Success -> InlineResultBanner(dState.message, isSuccess = true)
                is NetworkResult.Error -> InlineResultBanner(dState.arabicMessage, isSuccess = false)
                else -> Unit
            }

            when (val state = productsState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل منتجات المتجر...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { vendorViewModel.loadVendorProducts(session.userId) },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد منتجات في متجرك",
                            subtitle = "أضف أول منتج طاقة شمسية ليظهر للعملاء.",
                            actionLabel = "إضافة منتج الآن",
                            onActionClick = onAddProduct
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.data, key = { it.id }) { product ->
                                SolarCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SolarNetworkImage(
                                            imageUrl = product.imageUrl,
                                            contentDescription = product.name,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                                            Text(product.category.arabicLabel, style = MaterialTheme.typography.labelSmall, color = SolarGreenDark)
                                            Text("${"%.2f".format(product.price)} $", style = MaterialTheme.typography.titleSmall, color = SolarGreenPrimary)
                                            StatusPill(
                                                label = "المخزون: ${product.stock}",
                                                background = if (product.stock > 0) SolarGreenLight else SolarRedSoft,
                                                foreground = if (product.stock > 0) SolarGreenDark else SolarRedError
                                            )
                                        }
                                        IconButton(
                                            onClick = { onEditProduct(product) },
                                            modifier = Modifier.testTag("vendor_edit_product_${product.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل المنتج", tint = SolarGreenDark)
                                        }
                                        IconButton(
                                            onClick = {
                                                vendorViewModel.clearDeleteState()
                                                productToDelete = product
                                            },
                                            modifier = Modifier.testTag("vendor_delete_product_${product.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف المنتج", tint = SolarRedError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("تأكيد حذف المنتج") },
            text = { Text("هل أنت متأكد من حذف \"${product.name}\" نهائياً من متجرك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        vendorViewModel.deleteProduct(session.userId, product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarRedError)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VendorProductFormScreen(
    session: UserSession,
    editingProduct: Product?,
    vendorViewModel: VendorViewModel,
    onBackClick: () -> Unit
) {
    val mutationState by vendorViewModel.productMutationState.collectAsState()

    var name by rememberSaveable(editingProduct?.id) { mutableStateOf(editingProduct?.name.orEmpty()) }
    var description by rememberSaveable(editingProduct?.id) { mutableStateOf(editingProduct?.description.orEmpty()) }
    var category by rememberSaveable(editingProduct?.id) {
        mutableStateOf(editingProduct?.category?.takeIf { it != ProductCategory.ALL } ?: ProductCategory.SOLAR_PANELS)
    }
    var priceText by rememberSaveable(editingProduct?.id) {
        mutableStateOf(editingProduct?.price?.toString().orEmpty())
    }
    var stockText by rememberSaveable(editingProduct?.id) {
        mutableStateOf(editingProduct?.stock?.toString().orEmpty())
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(mutationState) {
        if (mutationState is NetworkResult.Success) {
            vendorViewModel.clearProductMutationState()
            onBackClick()
        }
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(
                title = if (editingProduct == null) "إضافة منتج جديد" else "تعديل المنتج",
                onBackClick = {
                    vendorViewModel.clearProductMutationState()
                    onBackClick()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SolarCard(modifier = Modifier.widthIn(max = 600.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mutationState is NetworkResult.Error) {
                        InlineResultBanner(
                            message = (mutationState as NetworkResult.Error).arabicMessage,
                            isSuccess = false
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنتج") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vendor_product_name_input")
                    )

                    Text("التصنيف", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProductCategory.entries.filter { it != ProductCategory.ALL }.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.arabicLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SolarGreenPrimary,
                                    selectedLabelColor = SolarWhite
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("وصف المنتج والمواصفات") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("vendor_product_desc_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("السعر ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("vendor_product_price_input")
                        )
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("المخزون") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("vendor_product_stock_input")
                        )
                    }

                    SingleImagePickerField(
                        label = "صورة المنتج",
                        selectedUri = imageUri,
                        onImageSelected = { imageUri = it },
                        testTag = "vendor_product_image_picker"
                    )

                    val isSaving = mutationState is NetworkResult.Loading
                    Button(
                        onClick = {
                            vendorViewModel.saveProduct(
                                currentVendorId = session.userId,
                                editingProduct = editingProduct,
                                name = name,
                                description = description,
                                category = category,
                                priceText = priceText,
                                stockText = stockText,
                                imageUri = imageUri
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("vendor_product_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
                        } else {
                            Text(if (editingProduct == null) "نشر المنتج" else "حفظ التعديلات")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorOrdersScreen(
    vendorViewModel: VendorViewModel,
    onOpenServerSettings: () -> Unit
) {
    val ordersState by vendorViewModel.ordersState.collectAsState()
    var inspectedOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        vendorViewModel.loadVendorOrders()
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = { SmartSolarTopBar(title = "طلبات منتجات المتجر") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val state = ordersState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل الطلبات...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { vendorViewModel.loadVendorOrders() },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد طلبات حتى الآن",
                            subtitle = "ستظهر هنا جميع طلبات العملاء التي تتضمن منتجاتك."
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.data, key = { it.id }) { order ->
                                VendorOrderCard(order = order, onClick = { inspectedOrder = order })
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { inspectedOrder = null },
            title = { Text("تفاصيل الطلب ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderStatusBadge(order.status)
                    if (order.clientName.isNotBlank()) {
                        Text("العميل: ${order.clientName}")
                    }
                    if (order.shippingAddress.isNotBlank()) {
                        Text("العنوان: ${order.shippingAddress}")
                    }
                    HorizontalDivider(color = SolarBorderLight)
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.productName} × ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                            Text("${"%.2f".format(item.subtotal)} $", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(color = SolarBorderLight)
                    Text(
                        "الإجمالي: ${"%.2f".format(order.totalAmount)} $",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarGreenPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedOrder = null }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun VendorOrderCard(order: Order, onClick: () -> Unit) {
    SolarCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("طلب ${order.orderNumber}", style = MaterialTheme.typography.titleSmall)
                OrderStatusBadge(order.status)
            }
            order.items.take(2).forEach { item ->
                Text(
                    text = "${item.productName} • الكمية: ${item.quantity} • ${"%.2f".format(item.unitPrice)} $",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextSecondary
                )
            }
            Text(
                text = "الإجمالي: ${"%.2f".format(order.totalAmount)} $",
                style = MaterialTheme.typography.titleSmall,
                color = SolarGreenPrimary
            )
        }
    }
}
