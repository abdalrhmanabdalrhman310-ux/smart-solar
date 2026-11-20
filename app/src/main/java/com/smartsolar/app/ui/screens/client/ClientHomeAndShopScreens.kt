package com.smartsolar.app.ui.screens.client

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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
import com.smartsolar.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientHomeScreen(
    session: UserSession,
    clientViewModel: ClientViewModel,
    cartCount: Int,
    onOpenProducts: () -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onOpenCart: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenInstallation: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenEngineers: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val productsState by clientViewModel.productsState.collectAsState()
    val ordersState by clientViewModel.ordersState.collectAsState()
    val notificationsState by clientViewModel.notificationsState.collectAsState()
    val unreadCount = (notificationsState as? NetworkResult.Success)?.data?.count { !it.isRead } ?: 0

    LaunchedEffect(Unit) {
        clientViewModel.loadHomeData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مرحباً بك، ${session.fullName.ifBlank { "عميلنا العزيز" }}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SolarTextPrimary
                    )
                    Text(
                        text = "خطط منظومتك الشمسية بذكاء واحترافية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarTextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text("$unreadCount") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "الإشعارات",
                                tint = SolarTextPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenCart,
                        modifier = Modifier.testTag("home_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text("$cartCount") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "سلة المشتريات",
                                tint = SolarTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Hero Banner
        item {
            SolarCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_solar_hero_banner),
                        contentDescription = "الطاقة الشمسية الذكية",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SolarTextPrimary.copy(alpha = 0.12f),
                                        SolarTextPrimary.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "احسب منظومتك الشمسية بدقة",
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تقدير عدد الألواح والبطاريات والانفرتر المناسب لمنزلك",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarWhite.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenCalculator,
                            modifier = Modifier.testTag("home_hero_calculator_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حاسبة الطاقة الشمسية")
                        }
                    }
                }
            }
        }

        // Service Shortcuts
        item {
            Text(
                text = "اختصارات الخدمات",
                style = MaterialTheme.typography.titleMedium,
                color = SolarTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ServiceShortcutTile(
                    title = "حاسبة الطاقة",
                    icon = Icons.Default.Calculate,
                    modifier = Modifier.weight(1f).testTag("shortcut_calculator"),
                    onClick = onOpenCalculator
                )
                ServiceShortcutTile(
                    title = "طلب تركيب",
                    icon = Icons.Default.Construction,
                    modifier = Modifier.weight(1f).testTag("shortcut_installation"),
                    onClick = onOpenInstallation
                )
                ServiceShortcutTile(
                    title = "طلب صيانة",
                    icon = Icons.Default.Build,
                    modifier = Modifier.weight(1f).testTag("shortcut_maintenance"),
                    onClick = onOpenMaintenance
                )
                ServiceShortcutTile(
                    title = "المهندسون",
                    icon = Icons.Default.Engineering,
                    modifier = Modifier.weight(1f).testTag("shortcut_engineers"),
                    onClick = onOpenEngineers
                )
            }
        }

        // Featured & Latest Products from PHP API
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المنتجات المميزة والأحدث",
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarTextPrimary
                )
                TextButton(
                    onClick = onOpenProducts,
                    modifier = Modifier.testTag("home_see_all_products")
                ) {
                    Text("عرض الكل", color = SolarGreenDark)
                }
            }
        }

        when (val state = productsState) {
            NetworkResult.Loading -> item { LoadingStateView("جاري جلب المنتجات من الخادم...") }
            is NetworkResult.Error -> item {
                ErrorStateView(
                    error = state,
                    onRetry = { clientViewModel.loadProducts() },
                    onOpenServerSettings = onOpenServerSettings
                )
            }
            is NetworkResult.Success -> {
                val list = state.data
                if (list.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "لا توجد منتجات متاحة حالياً",
                            subtitle = "سيتم عرض الألواح الشمسية والبطاريات والانفرترات فور إضافتها من البائعين في الخادم."
                        )
                    }
                } else {
                    val featured = list.filter { it.isFeatured }.ifEmpty { list.take(5) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(featured, key = { it.id }) { product ->
                                ProductCompactCard(
                                    product = product,
                                    onClick = { onOpenProductDetail(product) },
                                    onAddToCart = { clientViewModel.addToCart(product) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الطلبات",
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarTextPrimary
                )
                TextButton(onClick = onOpenOrders) {
                    Text("جميع الطلبات", color = SolarGreenDark)
                }
            }
        }

        when (val oState = ordersState) {
            NetworkResult.Loading -> item { LoadingStateView("جاري جلب آخر الطلبات...") }
            is NetworkResult.Error -> item {
                InlineResultBanner(message = oState.arabicMessage, isSuccess = false)
            }
            is NetworkResult.Success -> {
                val recent = oState.data.take(3)
                if (recent.isEmpty()) {
                    item {
                        SolarCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "لا توجد طلبات شراء مسجلة حتى الآن.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SolarTextSecondary
                            )
                        }
                    }
                } else {
                    items(recent, key = { it.id }) { order ->
                        OrderSummaryCard(order = order, onClick = onOpenOrders)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceShortcutTile(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SolarCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SolarGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = SolarGreenDark)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = SolarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductCompactCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    SolarCard(
        modifier = Modifier.width(196.dp),
        onClick = onClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column {
            SolarNetworkImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.category.arabicLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SolarGreenDark
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = SolarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${"%.2f".format(product.price)} $",
                    style = MaterialTheme.typography.titleSmall,
                    color = SolarGreenPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onAddToCart,
                    enabled = product.isAvailable
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "إضافة للسلة",
                        tint = if (product.isAvailable) SolarGreenDark else SolarTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ProductsScreen(
    clientViewModel: ClientViewModel,
    cartCount: Int,
    onOpenProductDetail: (Product) -> Unit,
    onOpenCart: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val productsState by clientViewModel.productsState.collectAsState()
    val selectedCategory by clientViewModel.selectedCategory.collectAsState()
    val searchQuery by clientViewModel.searchQuery.collectAsState()
    val cartMessage by clientViewModel.cartMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (productsState !is NetworkResult.Success) {
            clientViewModel.loadProducts()
        }
    }

    LaunchedEffect(cartMessage) {
        cartMessage?.let {
            snackbarHostState.showSnackbar(it)
            clientViewModel.consumeCartMessage()
        }
    }

    Scaffold(
        containerColor = SolarBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SmartSolarTopBar(
                title = "منتجات الطاقة الشمسية",
                actions = {
                    IconButton(
                        onClick = onOpenCart,
                        modifier = Modifier.testTag("products_cart_button")
                    ) {
                        BadgedBox(badge = { if (cartCount > 0) Badge { Text("$cartCount") } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "السلة")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { clientViewModel.loadProducts(selectedCategory, it) },
                placeholder = { Text("ابحث عن ألواح، بطاريات، انفرترات...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("products_search_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProductCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { clientViewModel.loadProducts(cat, searchQuery) },
                        label = { Text(cat.arabicLabel) },
                        modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGreenPrimary,
                            selectedLabelColor = SolarWhite
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (val state = productsState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل المنتجات...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { clientViewModel.loadProducts() },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    val filtered = state.data.filter { product ->
                        val catMatch = selectedCategory == ProductCategory.ALL || product.category == selectedCategory
                        val textMatch = searchQuery.isBlank() ||
                            product.name.contains(searchQuery, ignoreCase = true) ||
                            product.description.contains(searchQuery, ignoreCase = true)
                        catMatch && textMatch
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد منتجات مطابقة",
                            subtitle = "جرّب تغيير التصنيف أو كلمة البحث."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered, key = { it.id }) { product ->
                                ProductListCard(
                                    product = product,
                                    onClick = { onOpenProductDetail(product) },
                                    onAddToCart = { clientViewModel.addToCart(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    SolarCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SolarNetworkImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.category.arabicLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarGreenDark
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${"%.2f".format(product.price)} $",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(
                            label = if (product.isAvailable) "متوفر: ${product.stock}" else "نفد المخزون",
                            background = if (product.isAvailable) SolarGreenLight else SolarRedSoft,
                            foreground = if (product.isAvailable) SolarGreenDark else SolarRedError
                        )
                    }
                    Button(
                        onClick = onAddToCart,
                        enabled = product.isAvailable,
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("للسلة", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailsScreen(
    product: Product?,
    clientViewModel: ClientViewModel,
    onBackClick: () -> Unit,
    onOpenCart: () -> Unit
) {
    val cartMessage by clientViewModel.cartMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var quantity by rememberSaveable(product?.id) { mutableIntStateOf(1) }

    LaunchedEffect(cartMessage) {
        cartMessage?.let {
            snackbarHostState.showSnackbar(it)
            clientViewModel.consumeCartMessage()
        }
    }

    Scaffold(
        containerColor = SolarBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SmartSolarTopBar(
                title = "تفاصيل المنتج",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onOpenCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "السلة")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (product == null) {
            EmptyStateView(
                title = "لم يتم اختيار منتج",
                subtitle = "عد إلى قائمة المنتجات واختر منتجاً لعرض تفاصيله.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SolarCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    SolarNetworkImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = product.category.arabicLabel,
                                background = SolarGreenLight,
                                foreground = SolarGreenDark
                            )
                            StatusPill(
                                label = if (product.isAvailable) "المتوفر: ${product.stock} قطعة" else "نفد المخزون",
                                background = if (product.isAvailable) SolarGreenLight else SolarRedSoft,
                                foreground = if (product.isAvailable) SolarGreenDark else SolarRedError
                            )
                        }

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = SolarTextPrimary
                        )
                        if (product.storeName.isNotBlank() || product.vendorName.isNotBlank()) {
                            Text(
                                text = "البائع: ${product.storeName.ifBlank { product.vendorName }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarTextSecondary
                            )
                        }
                        Text(
                            text = "${"%.2f".format(product.price)} $",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SolarGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = SolarBorderLight)
                        Text(
                            text = "الوصف الفني والمواصفات",
                            style = MaterialTheme.typography.titleSmall,
                            color = SolarTextPrimary
                        )
                        Text(
                            text = product.description.ifBlank { "لا يوجد وصف إضافي لهذا المنتج." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = SolarTextSecondary
                        )

                        if (product.isAvailable) {
                            HorizontalDivider(color = SolarBorderLight)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الكمية المطلوبة:", style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.testTag("detail_qty_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "إنقاص الكمية")
                                    }
                                    Text(
                                        text = "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    IconButton(
                                        onClick = { if (quantity < product.stock) quantity++ },
                                        modifier = Modifier.testTag("detail_qty_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة الكمية")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { clientViewModel.addToCart(product, quantity) },
                            enabled = product.isAvailable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("detail_add_to_cart_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (product.isAvailable) "إضافة إلى سلة المشتريات" else "غير متوفر حالياً")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    clientViewModel: ClientViewModel,
    onBackClick: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onExploreProducts: () -> Unit
) {
    val cartItems by clientViewModel.cartItems.collectAsState()
    val stockCheckState by clientViewModel.stockVerificationState.collectAsState()
    val cartMessage by clientViewModel.cartMessage.collectAsState()

    LaunchedEffect(stockCheckState) {
        if (stockCheckState is NetworkResult.Success) {
            clientViewModel.resetStockVerification()
            onProceedToCheckout()
        }
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "سلة المشتريات", onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (cartMessage != null) {
                InlineResultBanner(message = cartMessage!!, isSuccess = false)
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (stockCheckState is NetworkResult.Error) {
                InlineResultBanner(
                    message = (stockCheckState as NetworkResult.Error).arabicMessage,
                    isSuccess = false
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (cartItems.isEmpty()) {
                EmptyStateView(
                    title = "سلة المشتريات فارغة",
                    subtitle = "أضف الألواح والبطاريات والانفرترات التي تحتاجها إلى السلة.",
                    actionLabel = "تصفح المنتجات",
                    onActionClick = onExploreProducts
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.productId }) { item ->
                        SolarCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SolarNetworkImage(
                                    imageUrl = item.imageUrl,
                                    contentDescription = item.productName,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SolarTextPrimary
                                    )
                                    Text(
                                        text = "سعر الوحدة: ${"%.2f".format(item.unitPrice)} $",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarTextSecondary
                                    )
                                    Text(
                                        text = "الإجمالي: ${"%.2f".format(item.lineTotal)} $",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SolarGreenPrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            clientViewModel.updateCartItemQuantity(item.productId, item.quantity - 1)
                                        }) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص")
                                        }
                                        Text("${item.quantity}", style = MaterialTheme.typography.titleSmall)
                                        IconButton(onClick = {
                                            clientViewModel.updateCartItemQuantity(item.productId, item.quantity + 1)
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة")
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = { clientViewModel.removeFromCart(item.productId) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف المنتج",
                                                tint = SolarRedError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val total = cartItems.sumOf { it.lineTotal }
                val isVerifying = stockCheckState is NetworkResult.Loading
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي الكلي:", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${"%.2f".format(total)} $",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SolarGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { clientViewModel.verifyCartBeforeCheckout() },
                            enabled = !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("cart_checkout_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التحقق من المخزون...")
                            } else {
                                Text("التحقق من المخزون وإتمام الطلب")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    session: UserSession,
    clientViewModel: ClientViewModel,
    onBackClick: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val cartItems by clientViewModel.cartItems.collectAsState()
    val checkoutState by clientViewModel.checkoutState.collectAsState()

    var address by rememberSaveable(session.address) { mutableStateOf(session.address) }
    var phone by rememberSaveable(session.phone) { mutableStateOf(session.phone) }
    var notes by rememberSaveable { mutableStateOf("") }

    val total = cartItems.sumOf { it.lineTotal }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "إتمام الطلب", onBackClick = onBackClick)
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
            Column(
                modifier = Modifier.widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (checkoutState is NetworkResult.Success) {
                    val order = (checkoutState as NetworkResult.Success<Order>).data
                    SolarCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SolarGreenPrimary,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "تم تأكيد إرسال طلبك بنجاح!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SolarTextPrimary
                            )
                            Text(
                                text = "رقم الطلب: ${order.orderNumber} | الإجمالي: ${"%.2f".format(order.totalAmount)} $",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SolarTextSecondary
                            )
                            Button(
                                onClick = {
                                    clientViewModel.resetCheckoutState()
                                    onOrderPlaced()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                            ) {
                                Text("عرض طلباتي")
                            }
                        }
                    }
                    return@Column
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ملخص المنتجات (${cartItems.sumOf { it.quantity }})", style = MaterialTheme.typography.titleMedium)
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.productName} × ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                Text("${"%.2f".format(item.lineTotal)} $", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider(color = SolarBorderLight)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي النهائي", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${"%.2f".format(total)} $",
                                style = MaterialTheme.typography.titleLarge,
                                color = SolarGreenPrimary
                            )
                        }
                    }
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("بيانات التوصيل", style = MaterialTheme.typography.titleMedium)
                        if (checkoutState is NetworkResult.Error) {
                            InlineResultBanner(
                                message = (checkoutState as NetworkResult.Error).arabicMessage,
                                isSuccess = false
                            )
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("عنوان التوصيل بالتفصيل") },
                            modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف للتواصل") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("ملاحظات إضافية (اختياري)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("checkout_notes_input")
                        )
                        val isSubmitting = checkoutState is NetworkResult.Loading
                        Button(
                            onClick = { clientViewModel.submitOrder(address, phone, notes) },
                            enabled = !isSubmitting && cartItems.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("checkout_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
                            } else {
                                Text("تأكيد وإرسال الطلب للخادم")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(order: Order, onClick: () -> Unit) {
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
                Text(
                    text = "طلب رقم ${order.orderNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SolarTextPrimary
                )
                OrderStatusBadge(status = order.status)
            }
            Text(
                text = "عدد الأصناف: ${order.items.sumOf { it.quantity }} | الإجمالي: ${"%.2f".format(order.totalAmount)} $",
                style = MaterialTheme.typography.bodyMedium,
                color = SolarTextSecondary
            )
            if (order.createdAt.isNotBlank()) {
                Text(
                    text = "التاريخ: ${order.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextSecondary
                )
            }
        }
    }
}
