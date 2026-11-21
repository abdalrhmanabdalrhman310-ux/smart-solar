package com.smartsolar.app.ui.screens.client

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.EngineerProfile
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
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.ClientViewModel

@Composable
fun InstallationRequestScreen(
    session: UserSession,
    clientViewModel: ClientViewModel,
    onSelectEngineer: () -> Unit,
    onBackClick: () -> Unit,
    onOpenOrders: () -> Unit
) {
    val selectedEngineer by clientViewModel.selectedEngineerForRequest.collectAsState()
    val draftSystemDetails by clientViewModel.draftInstallationSystemDetails.collectAsState()
    val submitState by clientViewModel.installationSubmitState.collectAsState()

    var location by rememberSaveable(session.address) { mutableStateOf(session.address) }
    var systemDetails by rememberSaveable(draftSystemDetails) { mutableStateOf(draftSystemDetails) }
    var description by rememberSaveable { mutableStateOf("") }
    var requestedDate by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "طلب تركيب منظومة شمسية", onBackClick = onBackClick)
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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("بيانات العميل", style = MaterialTheme.typography.titleMedium)
                        Text("الاسم: ${session.fullName.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                        Text("الهاتف: ${session.phone.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("تفاصيل طلب التركيب", style = MaterialTheme.typography.titleMedium)

                        if (submitState is NetworkResult.Success) {
                            InlineResultBanner(
                                message = (submitState as NetworkResult.Success).message,
                                isSuccess = true
                            )
                            OutlinedButton(
                                onClick = {
                                    clientViewModel.clearInstallationSubmitState()
                                    onOpenOrders()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("متابعة حالة الطلب")
                            }
                        } else if (submitState is NetworkResult.Error) {
                            InlineResultBanner(
                                message = (submitState as NetworkResult.Error).arabicMessage,
                                isSuccess = false
                            )
                        }

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("موقع التركيب (المدينة / الحي / العنوان)") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("install_location_input")
                        )

                        OutlinedTextField(
                            value = systemDetails,
                            onValueChange = { systemDetails = it },
                            label = { Text("تفاصيل المنظومة (القدرة، عدد الألواح، البطاريات)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("install_system_input")
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف الطلب وملاحظات الموقع") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("install_description_input")
                        )

                        OutlinedTextField(
                            value = requestedDate,
                            onValueChange = { requestedDate = it },
                            label = { Text("التاريخ المفضل (مثال: 2026-12-01)") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("install_date_input")
                        )

                        // Selected engineer card
                        SolarCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("المهندس المختار (اختياري)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = selectedEngineer?.fullName ?: "لم يتم تحديد مهندس (يُعين من الإدارة)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarTextSecondary
                                    )
                                }
                                Row {
                                    if (selectedEngineer != null) {
                                        IconButton(onClick = { clientViewModel.selectEngineerForRequest(null) }) {
                                            Icon(Icons.Default.Close, contentDescription = "إلغاء اختيار المهندس")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onSelectEngineer,
                                        modifier = Modifier.testTag("install_pick_engineer_button")
                                    ) {
                                        Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (selectedEngineer == null) "اختيار" else "تغيير")
                                    }
                                }
                            }
                        }

                        val isLoading = submitState is NetworkResult.Loading
                        Button(
                            onClick = {
                                clientViewModel.submitInstallationRequest(
                                    location = location,
                                    systemDetails = systemDetails,
                                    description = description,
                                    requestedDate = requestedDate
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("install_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
                            } else {
                                Text("إرسال طلب التركيب")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineersScreen(
    clientViewModel: ClientViewModel,
    onEngineerSelectedAndReturn: () -> Unit,
    onBackClick: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    val engineersState by clientViewModel.engineersState.collectAsState()
    val availabilityFilter by clientViewModel.engineerAvailabilityFilter.collectAsState()
    val inspectedEngineer by clientViewModel.inspectedEngineer.collectAsState()
    val ratingsState by clientViewModel.engineerRatingsState.collectAsState()

    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        clientViewModel.loadEngineers()
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "المهندسون المتخصصون", onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = availabilityFilter == null,
                        onClick = { clientViewModel.loadEngineers(null) },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGreenPrimary,
                            selectedLabelColor = SolarWhite
                        )
                    )
                }
                items(EngineerAvailability.entries) { avail ->
                    FilterChip(
                        selected = availabilityFilter == avail,
                        onClick = { clientViewModel.loadEngineers(avail) },
                        label = { Text(avail.arabicLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGreenPrimary,
                            selectedLabelColor = SolarWhite
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (val state = engineersState) {
                NetworkResult.Loading -> LoadingStateView("جاري تحميل قائمة المهندسين...")
                is NetworkResult.Error -> ErrorStateView(
                    error = state,
                    onRetry = { clientViewModel.loadEngineers() },
                    onOpenServerSettings = onOpenServerSettings
                )
                is NetworkResult.Success -> {
                    val list = state.data.filter {
                        availabilityFilter == null || it.availability == availabilityFilter
                    }
                    if (list.isEmpty()) {
                        EmptyStateView(
                            title = "لا يوجد مهندسون مطابقون",
                            subtitle = "جرّب تغيير تصفية حالة التوفر."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(list, key = { it.id }) { engineer ->
                                EngineerCardItem(
                                    engineer = engineer,
                                    onInspect = {
                                        clientViewModel.inspectEngineer(engineer)
                                        showProfileSheet = true
                                    },
                                    onSelect = {
                                        clientViewModel.selectEngineerForRequest(engineer)
                                        onEngineerSelectedAndReturn()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileSheet && inspectedEngineer != null) {
        val eng = inspectedEngineer!!
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SolarWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SolarNetworkImage(
                        imageUrl = eng.avatarUrl,
                        contentDescription = eng.fullName,
                        fallbackDrawableRes = R.drawable.img_solar_engineer_service,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(eng.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(eng.specialization, style = MaterialTheme.typography.bodyMedium, color = SolarGreenDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        EngineerAvailabilityBadge(eng.availability)
                    }
                }

                RatingStarsDisplay(rating = eng.averageRating, count = eng.ratingsCount)
                if (eng.city.isNotBlank()) {
                    Text("المدينة: ${eng.city}", style = MaterialTheme.typography.bodyMedium)
                }
                if (eng.age > 0) {
                    Text("العمر: ${eng.age} سنة", style = MaterialTheme.typography.bodyMedium)
                }
                Text("المؤهل العلمي: ${eng.qualification.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                Text("سنوات الخبرة: ${eng.yearsOfExperience} سنة", style = MaterialTheme.typography.bodyMedium)
                if (eng.bio.isNotBlank()) {
                    Text("النبذة التعريفية:", style = MaterialTheme.typography.titleSmall)
                    Text(eng.bio, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
                }
                if (eng.previousProjects.isNotBlank()) {
                    Text("الأعمال والمشاريع السابقة:", style = MaterialTheme.typography.titleSmall)
                    Text(eng.previousProjects, style = MaterialTheme.typography.bodyMedium, color = SolarTextSecondary)
                }

                if (eng.portfolioImages.isNotEmpty()) {
                    Text("صور الأعمال السابقة", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(eng.portfolioImages) { url ->
                            SolarNetworkImage(
                                imageUrl = url,
                                contentDescription = "عمل سابق",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                if (eng.certificateImages.isNotEmpty()) {
                    Text("الشهادات", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(eng.certificateImages) { url ->
                            SolarNetworkImage(
                                imageUrl = url,
                                contentDescription = "شهادة المهندس",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                HorizontalDivider(color = SolarBorderLight)
                Text("آراء وتقييمات العملاء", style = MaterialTheme.typography.titleSmall)
                when (val rState = ratingsState) {
                    NetworkResult.Loading -> LoadingStateView("جاري تحميل التقييمات...")
                    is NetworkResult.Error -> Text(rState.arabicMessage, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                    is NetworkResult.Success -> {
                        if (rState.data.isEmpty()) {
                            Text("لا توجد تقييمات مسجلة بعد.", style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                        } else {
                            rState.data.forEach { review ->
                                SolarCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(review.clientName, style = MaterialTheme.typography.titleSmall)
                                            RatingStarsDisplay(rating = review.stars.toDouble())
                                        }
                                        if (review.comment.isNotBlank()) {
                                            Text(review.comment, style = MaterialTheme.typography.bodySmall, color = SolarTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        clientViewModel.selectEngineerForRequest(eng)
                        showProfileSheet = false
                        onEngineerSelectedAndReturn()
                    },
                    enabled = eng.availability != EngineerAvailability.UNAVAILABLE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("engineer_sheet_select_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                ) {
                    Text(
                        if (eng.availability == EngineerAvailability.UNAVAILABLE) {
                            "المهندس غير متاح حالياً"
                        } else {
                            "اختيار هذا المهندس"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EngineerCardItem(
    engineer: EngineerProfile,
    onInspect: () -> Unit,
    onSelect: () -> Unit
) {
    SolarCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onInspect
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolarNetworkImage(
                    imageUrl = engineer.avatarUrl,
                    contentDescription = engineer.fullName,
                    fallbackDrawableRes = R.drawable.img_solar_engineer_service,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = engineer.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarTextPrimary
                        )
                        EngineerAvailabilityBadge(engineer.availability)
                    }
                    Text(
                        text = engineer.specialization,
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarGreenDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "خبرة ${engineer.yearsOfExperience} سنوات",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolarTextSecondary
                        )
                        RatingStarsDisplay(rating = engineer.averageRating, count = engineer.ratingsCount)
                    }
                }
            }
            if (engineer.bio.isNotBlank()) {
                Text(
                    text = engineer.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextSecondary,
                    maxLines = 2
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ملف المهندس")
                }
                Button(
                    onClick = onSelect,
                    enabled = engineer.availability != EngineerAvailability.UNAVAILABLE,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اختيار")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceRequestScreen(
    session: UserSession,
    clientViewModel: ClientViewModel,
    onSelectEngineer: () -> Unit,
    onBackClick: () -> Unit,
    onOpenOrders: () -> Unit
) {
    val selectedEngineer by clientViewModel.selectedEngineerForRequest.collectAsState()
    val submitState by clientViewModel.maintenanceSubmitState.collectAsState()

    val issueTypes = listOf(
        "انخفاض الإنتاجية",
        "عطل في الانفرتر",
        "مشكلة في البطاريات",
        "توصيلات وأسلاك",
        "تنظيف وفحص دوري",
        "عطل آخر"
    )
    var selectedIssueType by rememberSaveable { mutableStateOf(issueTypes.first()) }
    var systemDetails by rememberSaveable { mutableStateOf("") }
    var faultDetails by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable(session.address) { mutableStateOf(session.address) }
    var requestedDate by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(title = "طلب صيانة منظومة شمسية", onBackClick = onBackClick)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_solar_engineer_service),
                            contentDescription = "خدمة الصيانة",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الدعم الفني والصيانة", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "صف العطل بدقة ليتمكن المهندس المختص من تشخيص المشكلة وتجهيز القطع اللازمة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarTextSecondary
                            )
                        }
                    }
                }

                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (submitState is NetworkResult.Success) {
                            InlineResultBanner(
                                message = (submitState as NetworkResult.Success).message,
                                isSuccess = true
                            )
                            OutlinedButton(
                                onClick = {
                                    clientViewModel.clearMaintenanceSubmitState()
                                    onOpenOrders()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("عرض طلبات الصيانة")
                            }
                        } else if (submitState is NetworkResult.Error) {
                            InlineResultBanner(
                                message = (submitState as NetworkResult.Error).arabicMessage,
                                isSuccess = false
                            )
                        }

                        OutlinedTextField(
                            value = systemDetails,
                            onValueChange = { systemDetails = it },
                            label = { Text("وصف النظام الشمسي الحالي (القدرة / النوع)") },
                            modifier = Modifier.fillMaxWidth().testTag("maint_system_input")
                        )

                        Text("نوع المشكلة", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            issueTypes.forEach { issue ->
                                FilterChip(
                                    selected = selectedIssueType == issue,
                                    onClick = { selectedIssueType = issue },
                                    label = { Text(issue) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SolarGreenPrimary,
                                        selectedLabelColor = SolarWhite
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = faultDetails,
                            onValueChange = { faultDetails = it },
                            label = { Text("تفاصيل العطل والأعراض") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("maint_fault_input")
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("الموقع") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("maint_location_input")
                        )

                        OutlinedTextField(
                            value = requestedDate,
                            onValueChange = { requestedDate = it },
                            label = { Text("التاريخ المفضل للزيارة") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("maint_date_input")
                        )

                        SolarCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("اختيار مهندس (اختياري)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = selectedEngineer?.fullName ?: "تعيين تلقائي لأقرب مهندس متاح",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarTextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSelectEngineer,
                                    modifier = Modifier.testTag("maint_pick_engineer_button")
                                ) {
                                    Text(if (selectedEngineer == null) "اختيار" else "تغيير")
                                }
                            }
                        }

                        val isLoading = submitState is NetworkResult.Loading
                        Button(
                            onClick = {
                                clientViewModel.submitMaintenanceRequest(
                                    systemDetails = systemDetails,
                                    issueType = selectedIssueType,
                                    faultDetails = faultDetails,
                                    location = location,
                                    requestedDate = requestedDate
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("maint_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
                            } else {
                                Text("إرسال طلب الصيانة")
                            }
                        }
                    }
                }
            }
        }
    }
}
