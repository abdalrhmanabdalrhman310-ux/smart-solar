package com.smartsolar.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartsolar.app.data.remote.PhpEndpointContract
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.ui.components.InlineResultBanner
import com.smartsolar.app.ui.components.SmartSolarTopBar
import com.smartsolar.app.ui.components.SolarCard
import com.smartsolar.app.ui.theme.SolarAmberAccent
import com.smartsolar.app.ui.theme.SolarAmberSoft
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.AppSessionViewModel
import com.smartsolar.app.ui.viewmodel.ConnectionTestState

@Composable
fun ServerSettingsScreen(
    appSessionViewModel: AppSessionViewModel,
    serverConfig: ServerConfig,
    onBackClick: () -> Unit
) {
    var baseUrl by rememberSaveable(serverConfig.baseUrl) { mutableStateOf(serverConfig.baseUrl) }
    var timeoutText by rememberSaveable(serverConfig.timeoutSeconds) {
        mutableStateOf(serverConfig.timeoutSeconds.toString())
    }
    var savedMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val testState by appSessionViewModel.connectionTestState.collectAsState()

    LaunchedEffect(Unit) {
        appSessionViewModel.resetConnectionTestState()
    }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(
                title = "إعدادات خادم PHP وقاعدة البيانات",
                onBackClick = onBackClick
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
            Column(
                modifier = Modifier.widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = SolarGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "عنوان خادم PHP API (solar-admin/api/)",
                                style = MaterialTheme.typography.titleMedium,
                                color = SolarTextPrimary
                            )
                        }
                        Text(
                            text = "يتصل التطبيق بخادم PHP عبر الرابط المحفوظ في الإعدادات (الافتراضي للتطوير المحلي: http://10.46.17.35/solar-admin/api/) للوصول إلى قاعدة البيانات solar_platform.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextSecondary
                        )

                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = {
                                baseUrl = it
                                savedMessage = null
                            },
                            label = { Text("رابط مجلد API (ينتهي بـ /solar-admin/api/)") },
                            placeholder = { Text("http://10.46.17.35/solar-admin/api/") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_url_input")
                        )

                        val trimmedUrl = baseUrl.trim()
                        if (trimmedUrl.startsWith("http://", ignoreCase = true)) {
                            Surface(
                                color = SolarAmberSoft,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = SolarAmberAccent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "وضع التطوير المحلي HTTP: الاتصال مسموح للشبكة المحلية (مثل 10.46.17.35 و 10.0.2.2). يُوصى باستخدام HTTPS عند النشر الفعلي.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarTextPrimary
                                    )
                                }
                            }
                        } else if (trimmedUrl.startsWith("https://", ignoreCase = true)) {
                            Surface(
                                color = SolarGreenLight,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = SolarGreenDark)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "اتصال مشفر آمن عبر HTTPS.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarGreenDark
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = timeoutText,
                            onValueChange = { timeoutText = it },
                            label = { Text("مهلة الاتصال بالثواني (10 - 120)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_timeout_input")
                        )

                        if (savedMessage != null) {
                            InlineResultBanner(message = savedMessage!!, isSuccess = true)
                        }

                        when (val state = testState) {
                            ConnectionTestState.Idle -> Unit
                            ConnectionTestState.Testing -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = SolarGreenPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("جاري اختبار الاتصال بـ settings.php ...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            is ConnectionTestState.Success -> {
                                InlineResultBanner(message = state.message, isSuccess = true)
                            }
                            is ConnectionTestState.Failure -> {
                                InlineResultBanner(
                                    message = if (state.detail.isNotBlank()) "${state.message} (${state.detail})" else state.message,
                                    isSuccess = false
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timeout = timeoutText.toIntOrNull() ?: 30
                                    appSessionViewModel.saveServerSettings(baseUrl, timeout)
                                    savedMessage = "تم حفظ عنوان الخادم وإعدادات المهلة."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("server_save_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ الإعدادات")
                            }

                            OutlinedButton(
                                onClick = {
                                    val timeout = timeoutText.toIntOrNull() ?: 30
                                    appSessionViewModel.testServerConnection(baseUrl, timeout)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("server_test_button")
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص الاتصال")
                            }
                        }
                    }
                }

                // Transparent PHP API integration contract map (all 11 endpoints)
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "نقاط التكامل مع خادم PHP (11 ملفاً)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarTextPrimary
                        )
                        Text(
                            text = "تم بناء طبقة تكامل مركزية لملفات solar-admin/api/ المرتبطة بجداول قاعدة البيانات solar_platform. البنود الموضحة أدناه تحدد العمليات التي تحتاج لمطابقة أسماء المعاملات مع كود PHP الأصلي على الخادم:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextSecondary
                        )
                        HorizontalDivider(color = SolarBorderLight)
                        PhpEndpointContract.allEndpoints.forEach { spec ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = spec.phpFile,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SolarGreenDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = spec.supportedMethods,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarTextSecondary
                                    )
                                }
                                Text(
                                    text = "الجداول: ${spec.mysqlTables}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarTextSecondary
                                )
                                Text(
                                    text = spec.purposeArabic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SolarTextPrimary
                                )
                                Text(
                                    text = "ملاحظة التوافق: ${spec.requiresPhpConfirmationNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarAmberAccent
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = SolarBorderLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
