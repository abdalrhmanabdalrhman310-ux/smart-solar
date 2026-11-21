package com.smartsolar.app.ui.screens.auth

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.domain.model.UserSession
import com.smartsolar.app.ui.components.InlineResultBanner
import com.smartsolar.app.ui.components.MultiAttachmentPickerField
import com.smartsolar.app.ui.components.MultiImagePickerField
import com.smartsolar.app.ui.components.SingleImagePickerField
import com.smartsolar.app.ui.components.SolarCard
import com.smartsolar.app.ui.theme.SolarAmberAccent
import com.smartsolar.app.ui.theme.SolarAmberSoft
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.AuthModeTab
import com.smartsolar.app.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    serverConfig: ServerConfig,
    sessionExpiredBanner: String?,
    onDismissSessionExpiredBanner: () -> Unit,
    onAuthSuccess: (UserSession) -> Unit,
    onOpenServerSettings: () -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(AuthModeTab.LOGIN) }
    val authResult by authViewModel.authResult.collectAsState()

    LaunchedEffect(authResult) {
        val success = authResult as? NetworkResult.Success
        if (success != null) {
            authViewModel.clearState()
            onAuthSuccess(success.data)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_smart_solar_mark),
                        contentDescription = "شعار Smart Solar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "الطاقة الشمسية الذكية",
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarTextPrimary
                        )
                        Text(
                            text = "بوابة الدخول والتسجيل",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onOpenServerSettings,
                    modifier = Modifier.testTag("auth_server_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "إعدادات خادم PHP",
                        tint = SolarGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Server connection indicator
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenServerSettings)
                    .testTag("auth_server_status_banner"),
                shape = RoundedCornerShape(12.dp),
                color = if (serverConfig.isConfigured) SolarGreenLight else SolarAmberSoft
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (serverConfig.isConfigured) Icons.Default.Settings else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (serverConfig.isConfigured) SolarGreenDark else SolarAmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (serverConfig.isConfigured) {
                                "متصل بالخادم: ${serverConfig.baseUrl}"
                            } else {
                                "لم يتم تحديد عنوان خادم PHP بعد — اضغط هنا لضبطه"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextPrimary
                        )
                    }
                }
            }

            if (sessionExpiredBanner != null) {
                Spacer(modifier = Modifier.height(10.dp))
                InlineResultBanner(message = sessionExpiredBanner, isSuccess = false)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AuthModeTab.entries) { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = {
                            activeTab = tab
                            authViewModel.clearState()
                            onDismissSessionExpiredBanner()
                        },
                        label = { Text(tab.arabicTitle) },
                        modifier = Modifier.testTag("auth_tab_${tab.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGreenPrimary,
                            selectedLabelColor = SolarWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isLoading = authResult is NetworkResult.Loading
            val errorMsg = (authResult as? NetworkResult.Error)?.arabicMessage

            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (errorMsg != null) {
                        InlineResultBanner(message = errorMsg, isSuccess = false)
                    }

                    when (activeTab) {
                        AuthModeTab.LOGIN -> LoginForm(
                            isLoading = isLoading,
                            onSubmit = { email, password, remember ->
                                authViewModel.login(email, password, remember)
                            }
                        )
                        AuthModeTab.CLIENT_REGISTER -> ClientRegisterForm(
                            isLoading = isLoading,
                            onSubmit = { name, email, phone, city, pass, confirmPass, address ->
                                authViewModel.registerClient(name, email, phone, city, pass, confirmPass, address)
                            }
                        )
                        AuthModeTab.ENGINEER_REGISTER -> EngineerRegisterForm(
                            isLoading = isLoading,
                            onSubmit = { name, age, email, phone, city, pass, spec, qual, years, bio, projects, avatar, portfolio, certs ->
                                authViewModel.registerEngineer(
                                    name, age, email, phone, city, pass, spec, qual, years, bio, projects, avatar, portfolio, certs
                                )
                            }
                        )
                        AuthModeTab.VENDOR_REGISTER -> VendorRegisterForm(
                            isLoading = isLoading,
                            onSubmit = { vendorName, email, phone, city, storeAddress, pass, storeName, businessType, storeDesc, commReg, logo, storeImg ->
                                authViewModel.registerVendor(
                                    vendorName, email, phone, city, storeAddress, pass, storeName, businessType, storeDesc, commReg, logo, storeImg
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    onSubmit: (String, String, Boolean) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var rememberSession by rememberSaveable { mutableStateOf(true) }

    Text(
        text = "تسجيل الدخول إلى حسابك",
        style = MaterialTheme.typography.headlineSmall,
        color = SolarTextPrimary
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("البريد الإلكتروني") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_email_input")
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.testTag("login_toggle_password_button")
            ) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_password_input")
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { rememberSession = !rememberSession }
    ) {
        Checkbox(
            checked = rememberSession,
            onCheckedChange = { rememberSession = it },
            modifier = Modifier.testTag("login_remember_checkbox")
        )
        Text(
            text = "تذكر الجلسة على هذا الجهاز",
            style = MaterialTheme.typography.bodyMedium,
            color = SolarTextPrimary
        )
    }
    Button(
        onClick = { onSubmit(email, password, rememberSession) },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("login_submit_button"),
        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
        } else {
            Text("تسجيل الدخول", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ClientRegisterForm(
    isLoading: Boolean,
    onSubmit: (String, String, String, String, String, String, String) -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Text(
        text = "إنشاء حساب عميل جديد",
        style = MaterialTheme.typography.headlineSmall,
        color = SolarTextPrimary
    )
    OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("الاسم الكامل") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_name_input")
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("البريد الإلكتروني") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_email_input")
    )
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("رقم الهاتف") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_phone_input")
    )
    OutlinedTextField(
        value = city,
        onValueChange = { city = it },
        label = { Text("المدينة") },
        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_city_input")
    )
    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text("العنوان") },
        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_address_input")
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_password_input")
    )
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("تأكيد كلمة المرور") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("client_reg_confirm_password_input")
    )
    Button(
        onClick = { onSubmit(fullName, email, phone, city, password, confirmPassword, address) },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("client_reg_submit_button"),
        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
        } else {
            Text("إنشاء حساب العميل", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EngineerRegisterForm(
    isLoading: Boolean,
    onSubmit: (String, String, String, String, String, String, String, String, String, String, String, Uri?, List<Uri>, List<Uri>) -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var specialization by rememberSaveable { mutableStateOf("") }
    var qualification by rememberSaveable { mutableStateOf("") }
    var yearsExperience by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var previousProjects by rememberSaveable { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var portfolioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var certificateUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    Text(
        text = "الانضمام كمهندس طاقة شمسية",
        style = MaterialTheme.typography.headlineSmall,
        color = SolarTextPrimary
    )
    Text(
        text = "ملاحظة: يخضع حساب المهندس لمراجعة وموافقة الإدارة قبل تفعيل لوحة المهام.",
        style = MaterialTheme.typography.bodySmall,
        color = SolarTextSecondary
    )
    OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("الاسم الكامل") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_name_input")
    )
    OutlinedTextField(
        value = age,
        onValueChange = { age = it },
        label = { Text("العمر") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_age_input")
    )
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("رقم الهاتف") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_phone_input")
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("البريد الإلكتروني") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_email_input")
    )
    OutlinedTextField(
        value = city,
        onValueChange = { city = it },
        label = { Text("المدينة") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_city_input")
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_password_input")
    )
    OutlinedTextField(
        value = qualification,
        onValueChange = { qualification = it },
        label = { Text("المؤهل العلمي") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_qual_input")
    )
    OutlinedTextField(
        value = specialization,
        onValueChange = { specialization = it },
        label = { Text("التخصص (مثلاً: تصميم وتركيب الأنظمة الهجينة)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_spec_input")
    )
    OutlinedTextField(
        value = yearsExperience,
        onValueChange = { yearsExperience = it },
        label = { Text("سنوات الخبرة") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_years_input")
    )
    OutlinedTextField(
        value = bio,
        onValueChange = { bio = it },
        label = { Text("نبذة عن الخبرات") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_bio_input")
    )
    OutlinedTextField(
        value = previousProjects,
        onValueChange = { previousProjects = it },
        label = { Text("الأعمال والمشاريع السابقة") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("eng_reg_projects_input")
    )

    SingleImagePickerField(
        label = "الصورة الشخصية",
        selectedUri = avatarUri,
        onImageSelected = { avatarUri = it },
        testTag = "eng_reg_avatar_picker"
    )
    MultiImagePickerField(
        label = "صور الأعمال السابقة",
        selectedUris = portfolioUris,
        onImagesChanged = { portfolioUris = it },
        testTag = "eng_reg_portfolio_picker"
    )
    MultiAttachmentPickerField(
        label = "الشهادات والمرفقات (إن وجدت)",
        selectedUris = certificateUris,
        onAttachmentsChanged = { certificateUris = it },
        testTag = "eng_reg_certificates_picker"
    )

    Button(
        onClick = {
            onSubmit(
                fullName, age, email, phone, city, password, specialization,
                qualification, yearsExperience, bio, previousProjects, avatarUri, portfolioUris, certificateUris
            )
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("eng_reg_submit_button"),
        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
        } else {
            Text("إرسال طلب الانضمام كمهندس", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun VendorRegisterForm(
    isLoading: Boolean,
    onSubmit: (String, String, String, String, String, String, String, String, String, String, Uri?, Uri?) -> Unit
) {
    var vendorName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var storeAddress by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf("") }
    var businessType by rememberSaveable { mutableStateOf("") }
    var storeDescription by rememberSaveable { mutableStateOf("") }
    var commercialRegister by rememberSaveable { mutableStateOf("") }
    var storeLogoUri by remember { mutableStateOf<Uri?>(null) }
    var storeImageUri by remember { mutableStateOf<Uri?>(null) }

    Text(
        text = "الانضمام كبائع منتجات طاقة شمسية",
        style = MaterialTheme.typography.headlineSmall,
        color = SolarTextPrimary
    )
    Text(
        text = "ملاحظة: يخضع حساب المتجر لمراجعة وموافقة الإدارة قبل إدارة المنتجات والطلبات.",
        style = MaterialTheme.typography.bodySmall,
        color = SolarTextSecondary
    )
    OutlinedTextField(
        value = vendorName,
        onValueChange = { vendorName = it },
        label = { Text("اسم البائع") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_name_input")
    )
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("رقم الهاتف") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_phone_input")
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("البريد الإلكتروني") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_email_input")
    )
    OutlinedTextField(
        value = storeName,
        onValueChange = { storeName = it },
        label = { Text("اسم المحل أو المؤسسة") },
        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_store_input")
    )
    OutlinedTextField(
        value = businessType,
        onValueChange = { businessType = it },
        label = { Text("نوع النشاط التجاري (مثلاً: ألواح وبطاريات / مستلزمات شمسية)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_business_type_input")
    )
    OutlinedTextField(
        value = city,
        onValueChange = { city = it },
        label = { Text("المدينة") },
        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_city_input")
    )
    OutlinedTextField(
        value = storeAddress,
        onValueChange = { storeAddress = it },
        label = { Text("عنوان المحل") },
        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_address_input")
    )
    OutlinedTextField(
        value = commercialRegister,
        onValueChange = { commercialRegister = it },
        label = { Text("رقم السجل التجاري (إن وجد)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_cr_input")
    )
    OutlinedTextField(
        value = storeDescription,
        onValueChange = { storeDescription = it },
        label = { Text("وصف المحل") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_description_input")
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("كلمة المرور") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vendor_reg_password_input")
    )

    SingleImagePickerField(
        label = "صورة المحل",
        selectedUri = storeImageUri,
        onImageSelected = { storeImageUri = it },
        testTag = "vendor_reg_image_picker"
    )
    SingleImagePickerField(
        label = "شعار المحل (إن وجد)",
        selectedUri = storeLogoUri,
        onImageSelected = { storeLogoUri = it },
        testTag = "vendor_reg_logo_picker"
    )

    Button(
        onClick = {
            onSubmit(
                vendorName, email, phone, city, storeAddress, password, storeName,
                businessType, storeDescription, commercialRegister, storeLogoUri, storeImageUri
            )
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("vendor_reg_submit_button"),
        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(22.dp))
        } else {
            Text("إرسال طلب الانضمام كبائع", style = MaterialTheme.typography.labelLarge)
        }
    }
}
