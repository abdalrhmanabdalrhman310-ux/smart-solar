package com.smartsolar.app.ui.screens.client

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.Order
import com.smartsolar.app.domain.model.ServiceTask
import com.smartsolar.app.domain.model.TaskStatus
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
import com.smartsolar.app.ui.components.TaskStatusBadge
import com.smartsolar.app.ui.theme.SolarAmberAccent
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarRedError
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.AppSessionViewModel
import com.smartsolar.app.ui.viewmodel.ClientViewModel

@Composable
fun ClientOrdersScreen(
    clientViewModel: ClientViewModel,
    onOpenServerSettings: () -> Unit
) {
    val ordersState by clientViewModel.ordersState.collectAsState()
    val serviceTasksState by clientViewModel.serviceTasksState.collectAsState()
    val ratingSubmitState by clientViewModel.ratingSubmitState.collectAsState()
    val locallyRatedKeys by clientViewModel.locallyRatedServiceKeys.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Orders, 1 = Services
    var inspectedOrder by remember { mutableStateOf<Order?>(null) }
    var ratingTargetTask by remember { mutableStateOf<ServiceTask?>(null) }

    LaunchedEffect(Unit) {
        clientViewModel.loadOrders()
        clientViewModel.loadClientServiceRequests()
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "طلباتي وخدماتي")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("طلبات المنتجات") },
                    modifier = Modifier.testTag("orders_tab_products"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarGreenPrimary,
                        selectedLabelColor = SolarWhite
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("التركيب والصيانة") },
                    modifier = Modifier.testTag("orders_tab_services"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarGreenPrimary,
                        selectedLabelColor = SolarWhite
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                when (val state = ordersState) {
                    NetworkResult.Loading -> LoadingStateView("جاري تحميل طلبات المنتجات...")
                    is NetworkResult.Error -> ErrorStateView(
                        error = state,
                        onRetry = { clientViewModel.loadOrders() },
                        onOpenServerSettings = onOpenServerSettings
                    )
                    is NetworkResult.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyStateView(
                                title = "لا توجد طلبات شراء",
                                subtitle = "ستظهر هنا جميع طلباتك للمنتجات وتفاصيل حالتها."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(state.data, key = { it.id }) { order ->
                                    OrderSummaryCard(
                                        order = order,
                                        onClick = { inspectedOrder = order }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = serviceTasksState) {
                    NetworkResult.Loading -> LoadingStateView("جاري تحميل طلبات التركيب والصيانة...")
                    is NetworkResult.Error -> ErrorStateView(
                        error = state,
                        onRetry = { clientViewModel.loadClientServiceRequests() },
                        onOpenServerSettings = onOpenServerSettings
                    )
                    is NetworkResult.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyStateView(
                                title = "لا توجد طلبات تركيب أو صيانة",
                                subtitle = "يمكنك تقديم طلب تركيب أو صيانة من الصفحة الرئيسية."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(state.data, key = { "${it.serviceType.apiValue}_${it.id}" }) { task ->
                                    val key = "${task.serviceType.apiValue}_${task.id}"
                                    val alreadyRated = task.isRated || key in locallyRatedKeys
                                    ClientServiceTaskCard(
                                        task = task,
                                        alreadyRated = alreadyRated,
                                        onRateEngineer = {
                                            clientViewModel.clearRatingSubmitState()
                                            ratingTargetTask = task
                                        }
                                    )
                                }
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
                    Text("التاريخ: ${order.createdAt.ifBlank { "-" }}")
                    Text("العنوان: ${order.shippingAddress.ifBlank { "-" }}")
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

    ratingTargetTask?.let { task ->
        RateEngineerDialog(
            task = task,
            submitState = ratingSubmitState,
            onSubmit = { stars, comment ->
                clientViewModel.submitEngineerRating(task, stars, comment)
            },
            onDismiss = {
                clientViewModel.clearRatingSubmitState()
                ratingTargetTask = null
            }
        )
    }
}

@Composable
private fun ClientServiceTaskCard(
    task: ServiceTask,
    alreadyRated: Boolean,
    onRateEngineer: () -> Unit
) {
    SolarCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${task.serviceType.arabicLabel} (${task.requestNumber})", style = MaterialTheme.typography.titleSmall)
                TaskStatusBadge(task.status)
            }
            if (task.engineerName.isNotBlank()) {
                Text("المهندس: ${task.engineerName}", style = MaterialTheme.typography.bodySmall, color = SolarGreenDark)
            }
            if (task.location.isNotBlank()) {
                Text("الموقع: ${task.location}", style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
            }
            if (task.description.isNotBlank()) {
                Text(task.description, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
            }
            if (task.technicalReport.isNotBlank()) {
                Text(
                    "التقرير الفني: ${task.technicalReport}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextPrimary
                )
            }

            if (task.status == TaskStatus.COMPLETED && (task.engineerId ?: 0L) > 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                if (alreadyRated) {
                    StatusPill(
                        label = "تم تقييم المهندس",
                        background = SolarGreenLight,
                        foreground = SolarGreenDark
                    )
                } else {
                    OutlinedButton(
                        onClick = onRateEngineer,
                        modifier = Modifier.fillMaxWidth().testTag("rate_engineer_button_${task.id}")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SolarAmberAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تقييم المهندس")
                    }
                }
            }
        }
    }
}

@Composable
private fun RateEngineerDialog(
    task: ServiceTask,
    submitState: NetworkResult<Unit>?,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var comment by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقييم المهندس ${task.engineerName.ifBlank { "" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختر التقييم من 1 إلى 5 نجوم:")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { stars = i },
                            modifier = Modifier.testTag("rating_star_$i")
                        ) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$i نجوم",
                                tint = SolarAmberAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("تعليقك حول الخدمة") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("rating_comment_input")
                )
                when (submitState) {
                    is NetworkResult.Success -> InlineResultBanner(submitState.message, isSuccess = true)
                    is NetworkResult.Error -> InlineResultBanner(submitState.arabicMessage, isSuccess = false)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            if (submitState is NetworkResult.Success) {
                Button(onClick = onDismiss) { Text("تم") }
            } else {
                Button(
                    onClick = { onSubmit(stars, comment) },
                    enabled = submitState !is NetworkResult.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                ) {
                    Text(if (submitState is NetworkResult.Loading) "جاري الإرسال..." else "إرسال التقييم")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun NotificationsScreen(
    clientViewModel: ClientViewModel,
    onBackClick: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val state by clientViewModel.notificationsState.collectAsState()

    LaunchedEffect(Unit) {
        clientViewModel.loadNotifications()
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "الإشعارات", onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val current = state) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل الإشعارات...")
                is NetworkResult.Error -> ErrorStateView(
                    error = current,
                    onRetry = { clientViewModel.loadNotifications() },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    if (current.data.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد إشعارات حالياً",
                            subtitle = "ستصلك هنا تنبيهات الطلبات والمهام والتحديثات من المنصة."
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(current.data, key = { it.id }) { notif ->
                                SolarCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { if (!notif.isRead) clientViewModel.markNotificationRead(notif.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (notif.isRead) SolarBackground else SolarGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (notif.isRead) Icons.Default.MarkEmailRead else Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = if (notif.isRead) SolarTextSecondary else SolarGreenDark
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = notif.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold
                                                )
                                                if (!notif.isRead) {
                                                    StatusPill("جديد", SolarGreenLight, SolarGreenDark)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                                            if (notif.createdAt.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(notif.createdAt, style = MaterialTheme.typography.labelSmall, color = SolarTextSecondary)
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
    }
}

@Composable
fun ProfileScreen(
    session: UserSession,
    appSessionViewModel: AppSessionViewModel,
    onOpenServerSettings: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val actionState by appSessionViewModel.profileActionState.collectAsState()
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable(session.fullName) { mutableStateOf(session.fullName) }
    var phone by rememberSaveable(session.phone) { mutableStateOf(session.phone) }
    var address by rememberSaveable(session.address) { mutableStateOf(session.address) }

    LaunchedEffect(actionState) {
        if (actionState is NetworkResult.Success) {
            isEditing = false
        }
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "الملف الشخصي")
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
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (session.avatarUrl.isNotBlank()) {
                            SolarNetworkImage(
                                imageUrl = session.avatarUrl,
                                contentDescription = session.fullName,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(SolarGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "الصورة الشخصية",
                                    tint = SolarGreenDark,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                        Text(
                            text = session.fullName.ifBlank { "مستخدم Smart Solar" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = SolarTextPrimary
                        )
                        StatusPill(
                            label = session.role.arabicLabel,
                            background = SolarGreenLight,
                            foreground = SolarGreenDark
                        )
                        Text(session.email, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
                    }
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("البيانات الشخصية", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = {
                                    isEditing = !isEditing
                                    appSessionViewModel.clearProfileActionState()
                                },
                                modifier = Modifier.testTag("profile_edit_toggle_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditing) "إلغاء" else "تعديل البيانات")
                            }
                        }

                        when (val state = actionState) {
                            is NetworkResult.Success -> InlineResultBanner(state.message, isSuccess = true)
                            is NetworkResult.Error -> InlineResultBanner(state.arabicMessage, isSuccess = false)
                            else -> Unit
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("الاسم الكامل") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                            )
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("العنوان") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_address_input")
                            )
                            val saving = actionState is NetworkResult.Loading
                            Button(
                                onClick = { appSessionViewModel.updateProfile(fullName, phone, address) },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_save_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                            ) {
                                if (saving) {
                                    CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("حفظ التعديلات")
                                }
                            }
                        } else {
                            ProfileInfoRow("البريد الإلكتروني", session.email.ifBlank { "-" })
                            ProfileInfoRow("رقم الهاتف", session.phone.ifBlank { "-" })
                            ProfileInfoRow("العنوان", session.address.ifBlank { "-" })
                            if (session.storeName.isNotBlank()) {
                                ProfileInfoRow("اسم المتجر", session.storeName)
                            }
                            if (session.specialization.isNotBlank()) {
                                ProfileInfoRow("التخصص", session.specialization)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenServerSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_server_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات خادم PHP")
                }

                Button(
                    onClick = {
                        appSessionViewModel.logout()
                        onLoggedOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_logout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarRedError)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج")
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SolarTextPrimary, fontWeight = FontWeight.Medium)
    }
}
