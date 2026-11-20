package com.smartsolar.app.ui.screens.engineer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.ServiceTask
import com.smartsolar.app.domain.model.TaskStatus
import com.smartsolar.app.domain.model.UserSession
import com.smartsolar.app.ui.components.EmptyStateView
import com.smartsolar.app.ui.components.EngineerAvailabilityBadge
import com.smartsolar.app.ui.components.ErrorStateView
import com.smartsolar.app.ui.components.InlineResultBanner
import com.smartsolar.app.ui.components.LoadingStateView
import com.smartsolar.app.ui.components.RatingStarsDisplay
import com.smartsolar.app.ui.components.SmartSolarTopBar
import com.smartsolar.app.ui.components.SolarCard
import com.smartsolar.app.ui.components.SolarNetworkImage
import com.smartsolar.app.ui.components.TaskStatusBadge
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.EngineerViewModel

@Composable
fun EngineerHomeScreen(
    session: UserSession,
    engineerViewModel: EngineerViewModel,
    onOpenTasks: () -> Unit,
    onOpenTaskDetail: (ServiceTask) -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val tasksState by engineerViewModel.tasksState.collectAsState()
    val availability by engineerViewModel.availabilityState.collectAsState()
    val availabilityResult by engineerViewModel.availabilityUpdateResult.collectAsState()

    LaunchedEffect(session.userId) {
        engineerViewModel.syncInitialAvailability(session.availability)
        engineerViewModel.loadAssignedTasks(session.userId)
    }

    val allTasks = (tasksState as? NetworkResult.Success)?.data.orEmpty()
    val pendingCount = allTasks.count { it.status == TaskStatus.PENDING }
    val activeCount = allTasks.count { it.status == TaskStatus.ACCEPTED || it.status == TaskStatus.IN_PROGRESS }
    val completedCount = allTasks.count { it.status == TaskStatus.COMPLETED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engineer Profile Header Card
        item {
            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SolarNetworkImage(
                            imageUrl = session.avatarUrl,
                            contentDescription = session.fullName,
                            fallbackDrawableRes = R.drawable.img_solar_engineer_service,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.fullName.ifBlank { "مهندس الطاقة الشمسية" },
                                style = MaterialTheme.typography.titleLarge,
                                color = SolarTextPrimary
                            )
                            Text(
                                text = session.specialization.ifBlank { "مهندس تركيب وصيانة" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = SolarGreenDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EngineerAvailabilityBadge(availability)
                        }
                    }

                    HorizontalDivider(color = SolarBorderLight)
                    Text("تعديل حالة التوفر:", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineerAvailability.entries.forEach { option ->
                            FilterChip(
                                selected = availability == option,
                                onClick = { engineerViewModel.updateAvailability(option) },
                                label = { Text(option.arabicLabel) },
                                modifier = Modifier.testTag("eng_avail_${option.apiValue}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SolarGreenPrimary,
                                    selectedLabelColor = SolarWhite
                                )
                            )
                        }
                    }
                    when (val res = availabilityResult) {
                        is NetworkResult.Success -> InlineResultBanner(res.message, isSuccess = true)
                        is NetworkResult.Error -> InlineResultBanner(res.arabicMessage, isSuccess = false)
                        else -> Unit
                    }
                }
            }
        }

        // Dashboard Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineerStatCard("إجمالي المهام", "${allTasks.size}", Modifier.weight(1f))
                EngineerStatCard("طلبات جديدة", "$pendingCount", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineerStatCard("مهام نشطة", "$activeCount", Modifier.weight(1f))
                EngineerStatCard("مهام مكتملة", "$completedCount", Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المهام المسندة إليك", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onOpenTasks) {
                    Text("عرض كل المهام")
                }
            }
        }

        when (val state = tasksState) {
            NetworkResult.Loading -> item { LoadingStateView("جاري تحميل المهام المسندة...") }
            is NetworkResult.Error -> item {
                ErrorStateView(
                    error = state,
                    onRetry = { engineerViewModel.loadAssignedTasks(session.userId) },
                    onOpenServerSettings = onOpenServerSettings
                )
            }
            is NetworkResult.Success -> {
                if (state.data.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "لا توجد مهام مسندة حالياً",
                            subtitle = "ستظهر هنا طلبات التركيب والصيانة المسندة لك من النظام."
                        )
                    }
                } else {
                    items(state.data.take(5), key = { "${it.serviceType.apiValue}_${it.id}" }) { task ->
                        EngineerTaskCard(task = task, onClick = { onOpenTaskDetail(task) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineerStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SolarCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = SolarGreenPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = SolarTextSecondary
            )
        }
    }
}

@Composable
fun EngineerTasksScreen(
    session: UserSession,
    engineerViewModel: EngineerViewModel,
    onOpenTaskDetail: (ServiceTask) -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val tasksState by engineerViewModel.tasksState.collectAsState()
    var statusFilter by rememberSaveable { mutableStateOf<TaskStatus?>(null) }

    LaunchedEffect(session.userId) {
        engineerViewModel.loadAssignedTasks(session.userId)
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = { SmartSolarTopBar(title = "المهام المسندة") }
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
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("الكل") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarGreenPrimary,
                        selectedLabelColor = SolarWhite
                    )
                )
                listOf(TaskStatus.PENDING, TaskStatus.ACCEPTED, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED).forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st.arabicLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGreenPrimary,
                            selectedLabelColor = SolarWhite
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (val state = tasksState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل المهام...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { engineerViewModel.loadAssignedTasks(session.userId) },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    val filtered = state.data.filter { statusFilter == null || it.status == statusFilter }
                    if (filtered.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد مهام في هذه الحالة",
                            subtitle = "اسحب أو اختر تصفية أخرى لعرض المهام."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered, key = { "${it.serviceType.apiValue}_${it.id}" }) { task ->
                                EngineerTaskCard(task = task, onClick = { onOpenTaskDetail(task) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineerTaskCard(
    task: ServiceTask,
    onClick: () -> Unit
) {
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
                    text = "${task.serviceType.arabicLabel} (${task.requestNumber})",
                    style = MaterialTheme.typography.titleSmall,
                    color = SolarTextPrimary
                )
                TaskStatusBadge(task.status)
            }
            if (task.clientName.isNotBlank()) {
                Text("العميل: ${task.clientName}", style = MaterialTheme.typography.bodyMedium)
            }
            if (task.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = SolarTextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(task.location, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                }
            }
            if (!task.clientPhone.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = SolarGreenDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الهاتف: ${task.clientPhone}", style = MaterialTheme.typography.bodySmall, color = SolarGreenDark)
                }
            }
            if (task.requestedDate.isNotBlank()) {
                Text("تاريخ الطلب: ${task.requestedDate}", style = MaterialTheme.typography.labelSmall, color = SolarTextSecondary)
            }
        }
    }
}

@Composable
fun EngineerTaskDetailScreen(
    session: UserSession,
    engineerViewModel: EngineerViewModel,
    onBackClick: () -> Unit
) {
    val task by engineerViewModel.selectedTask.collectAsState()
    val actionResult by engineerViewModel.taskActionResult.collectAsState()

    var technicalReport by rememberSaveable(task?.id) {
        mutableStateOf(task?.technicalReport.orEmpty())
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "تفاصيل المهمة", onBackClick = {
                engineerViewModel.clearTaskActionResult()
                onBackClick()
            })
        }
    ) { innerPadding ->
        val currentTask = task
        if (currentTask == null) {
            EmptyStateView(
                title = "لم يتم اختيار مهمة",
                subtitle = "عد إلى قائمة المهام واختر مهمة لعرض تفاصيلها.",
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
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("رقم الطلب: ${currentTask.requestNumber}", style = MaterialTheme.typography.titleMedium)
                            TaskStatusBadge(currentTask.status)
                        }
                        Text("نوع الخدمة: ${currentTask.serviceType.arabicLabel}", style = MaterialTheme.typography.bodyMedium, color = SolarGreenDark)
                        Text("اسم العميل: ${currentTask.clientName.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                        Text("موقع العميل: ${currentTask.location.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                        if (!currentTask.clientPhone.isNullOrBlank()) {
                            Text("رقم الهاتف: ${currentTask.clientPhone}", style = MaterialTheme.typography.bodyMedium, color = SolarGreenDark)
                        } else {
                            Text("رقم الهاتف: محجوب حسب صلاحيات النظام", style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                        }
                        Text("تاريخ الطلب: ${currentTask.requestedDate.ifBlank { currentTask.createdAt }}", style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                        HorizontalDivider(color = SolarBorderLight)
                        if (currentTask.systemDetails.isNotBlank()) {
                            Text("تفاصيل المنظومة: ${currentTask.systemDetails}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (currentTask.issueType.isNotBlank()) {
                            Text("نوع المشكلة: ${currentTask.issueType}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("الوصف / العمل المطلوب:", style = MaterialTheme.typography.titleSmall)
                        Text(currentTask.description.ifBlank { "لا يوجد وصف إضافي." }, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
                    }
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("التقرير الفني وتحديث الحالة", style = MaterialTheme.typography.titleMedium)

                        when (val res = actionResult) {
                            is NetworkResult.Success -> InlineResultBanner(res.message, isSuccess = true)
                            is NetworkResult.Error -> InlineResultBanner(res.arabicMessage, isSuccess = false)
                            else -> Unit
                        }

                        OutlinedTextField(
                            value = technicalReport,
                            onValueChange = { technicalReport = it },
                            label = { Text("التقرير الفني / الملاحظات الهندسية") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("eng_task_report_input")
                        )

                        val isUpdating = actionResult is NetworkResult.Loading
                        if (isUpdating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = SolarGreenPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التحديث...")
                            }
                        }

                        if (currentTask.status == TaskStatus.PENDING && currentTask.canAccept) {
                            Button(
                                onClick = { engineerViewModel.acceptTask(currentTask, session.userId) },
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("eng_accept_task_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قبول المهمة")
                            }
                        }

                        if (currentTask.status == TaskStatus.ACCEPTED || currentTask.status == TaskStatus.PENDING) {
                            OutlinedButton(
                                onClick = {
                                    engineerViewModel.updateTaskProgress(
                                        currentTask,
                                        TaskStatus.IN_PROGRESS,
                                        technicalReport,
                                        session.userId
                                    )
                                },
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("eng_start_task_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحديث إلى: قيد التنفيذ")
                            }
                        }

                        if (currentTask.status != TaskStatus.COMPLETED && currentTask.status != TaskStatus.CANCELLED) {
                            Button(
                                onClick = {
                                    engineerViewModel.updateTaskProgress(
                                        currentTask,
                                        TaskStatus.COMPLETED,
                                        technicalReport,
                                        session.userId
                                    )
                                },
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("eng_complete_task_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenDark)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إكمال المهمة وحفظ التقرير")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EngineerRatingsScreen(
    session: UserSession,
    engineerViewModel: EngineerViewModel,
    onOpenServerSettings: () -> Unit
) {
    val ratingsState by engineerViewModel.ratingsState.collectAsState()

    LaunchedEffect(session.userId) {
        engineerViewModel.loadEngineerRatings(session.userId)
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = { SmartSolarTopBar(title = "تقييمات العملاء") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val state = ratingsState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل التقييمات...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { engineerViewModel.loadEngineerRatings(session.userId) },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    val ratings = state.data
                    val avg = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
                    SolarCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("متوسط التقييم العام", style = MaterialTheme.typography.titleMedium)
                                Text("عدد التقييمات: ${ratings.size}", style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                            }
                            RatingStarsDisplay(rating = avg, count = ratings.size)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (ratings.isEmpty()) {
                        EmptyStateView(
                            title = "لا توجد تقييمات بعد",
                            subtitle = "ستظهر هنا تقييمات وتعليقات العملاء بعد إكمال خدماتهم."
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(ratings, key = { it.id }) { rating ->
                                SolarCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(rating.clientName, style = MaterialTheme.typography.titleSmall)
                                            RatingStarsDisplay(rating = rating.stars.toDouble())
                                        }
                                        if (rating.comment.isNotBlank()) {
                                            Text(rating.comment, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
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
