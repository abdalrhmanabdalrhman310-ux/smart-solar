package com.smartsolar.app.ui.screens.client

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.calculator.SolarSystemCalculator
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
import com.smartsolar.app.ui.theme.SolarRedError
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.ClientViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SolarCalculatorScreen(
    clientViewModel: ClientViewModel,
    onRequestInstallationWithDesign: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val appliances by clientViewModel.appliances.collectAsState()
    val assumptions by clientViewModel.assumptions.collectAsState()
    val result by clientViewModel.designResult.collectAsState()
    val saveState by clientViewModel.saveDesignState.collectAsState()

    var deviceName by rememberSaveable { mutableStateOf("") }
    var powerWattsText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var designTitle by rememberSaveable { mutableStateOf("تصميم منظومة المنزل") }
    var showAssumptionsEditor by rememberSaveable { mutableStateOf(false) }

    var sunHoursInput by rememberSaveable(assumptions.peakSunHours) { mutableStateOf(assumptions.peakSunHours.toString()) }
    var efficiencyPctInput by rememberSaveable(assumptions.systemEfficiency) { mutableStateOf((assumptions.systemEfficiency * 100).toInt().toString()) }
    var dodPctInput by rememberSaveable(assumptions.batteryDepthOfDischarge) { mutableStateOf((assumptions.batteryDepthOfDischarge * 100).toInt().toString()) }
    var panelWattsInput by rememberSaveable(assumptions.singlePanelWatts) { mutableStateOf(assumptions.singlePanelWatts.toString()) }

    Scaffold(
        containerColor = SolarBackground,
        topBar = {
            SmartSolarTopBar(
                title = "حاسبة المنظومة الشمسية",
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
                // Header art
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_solar_calculator_art),
                            contentDescription = "حاسبة الطاقة الشمسية",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حساب الأحمال وتصميم المنظومة",
                                style = MaterialTheme.typography.titleMedium,
                                color = SolarTextPrimary
                            )
                            Text(
                                text = "أضف أجهزتك الكهربائية مع القدرة بالواط وساعات التشغيل لحساب المتطلبات الهندسية التقريبية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarTextSecondary
                            )
                        }
                    }
                }

                // Add Appliance Form
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إضافة جهاز كهربائي", style = MaterialTheme.typography.titleMedium)

                        // Quick helper presets to fill inputs (never auto-added!)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("ثلاجة", "180", "12"),
                                Triple("مكيف انفرتر", "1200", "6"),
                                Triple("إنارة LED", "15", "8"),
                                Triple("تلفاز", "120", "5"),
                                Triple("غسالة", "500", "2")
                            ).forEach { (name, watts, hrs) ->
                                AssistChip(
                                    onClick = {
                                        deviceName = name
                                        powerWattsText = watts
                                        hoursText = hrs
                                    },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = { Text("اسم الجهاز الكهربائي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("calc_device_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = powerWattsText,
                                onValueChange = { powerWattsText = it },
                                label = { Text("القدرة (واط)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("calc_watts_input")
                            )
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("العدد") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.7f).testTag("calc_qty_input")
                            )
                            OutlinedTextField(
                                value = hoursText,
                                onValueChange = { hoursText = it },
                                label = { Text("ساعات/يوم") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("calc_hours_input")
                            )
                        }

                        Button(
                            onClick = {
                                val w = powerWattsText.toDoubleOrNull() ?: 0.0
                                val q = quantityText.toIntOrNull() ?: 0
                                val h = hoursText.toDoubleOrNull() ?: 0.0
                                if (deviceName.isNotBlank() && w > 0 && q > 0 && h > 0) {
                                    clientViewModel.addAppliance(deviceName, w, q, h)
                                    deviceName = ""
                                    powerWattsText = ""
                                    quantityText = "1"
                                    hoursText = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("calc_add_appliance_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة الجهاز للحسبة")
                        }
                    }
                }

                // Appliances List
                if (appliances.isNotEmpty()) {
                    SolarCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("قائمة الأحمال المضافة (${appliances.size})", style = MaterialTheme.typography.titleMedium)
                            appliances.forEach { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${app.name} (×${app.quantity})",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = SolarTextPrimary
                                        )
                                        Text(
                                            text = "${app.powerWatts.toInt()} واط × ${app.dailyHours} ساعة = ${"%.2f".format(app.dailyWattHours / 1000.0)} kWh/يوم",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SolarTextSecondary
                                        )
                                    }
                                    IconButton(onClick = { clientViewModel.removeAppliance(app.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الجهاز", tint = SolarRedError)
                                    }
                                }
                                HorizontalDivider(color = SolarBorderLight)
                            }
                        }
                    }
                }

                // Engineering Assumptions Card
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الافتراضات الهندسية للحساب", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(
                                onClick = { showAssumptionsEditor = !showAssumptionsEditor },
                                modifier = Modifier.testTag("calc_toggle_assumptions")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showAssumptionsEditor) "إغلاق" else "تعديل")
                            }
                        }
                        Text(
                            text = SolarSystemCalculator.buildAssumptionsSummary(assumptions),
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextSecondary
                        )

                        if (showAssumptionsEditor) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sunHoursInput,
                                    onValueChange = { sunHoursInput = it },
                                    label = { Text("ساعات الشمس") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = efficiencyPctInput,
                                    onValueChange = { efficiencyPctInput = it },
                                    label = { Text("كفاءة النظام %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = dodPctInput,
                                    onValueChange = { dodPctInput = it },
                                    label = { Text("عمق التفريغ DoD %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = panelWattsInput,
                                    onValueChange = { panelWattsInput = it },
                                    label = { Text("قدرة اللوح (W)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    val sh = sunHoursInput.toDoubleOrNull() ?: assumptions.peakSunHours
                                    val eff = ((efficiencyPctInput.toDoubleOrNull() ?: 78.0) / 100.0).coerceIn(0.4, 0.98)
                                    val dod = ((dodPctInput.toDoubleOrNull() ?: 80.0) / 100.0).coerceIn(0.2, 0.95)
                                    val pw = panelWattsInput.toIntOrNull() ?: assumptions.singlePanelWatts
                                    clientViewModel.updateAssumptions(
                                        assumptions.copy(
                                            peakSunHours = sh,
                                            systemEfficiency = eff,
                                            batteryDepthOfDischarge = dod,
                                            singlePanelWatts = pw
                                        )
                                    )
                                    showAssumptionsEditor = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenDark)
                            ) {
                                Text("تطبيق الافتراضات")
                            }
                        }
                    }
                }

                // Calculated Output Dashboard
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("النتائج الهندسية التقديرية", style = MaterialTheme.typography.titleMedium)

                        CalcMetricRow("إجمالي الأحمال اللحظية", "${result.totalLoadWatts.toInt()} واط")
                        CalcMetricRow("الاستهلاك اليومي", "${"%.2f".format(result.dailyConsumptionKwh)} كيلوواط/ساعة")
                        CalcMetricRow("قدرة المنظومة الشمسية المقترحة", "${"%.2f".format(result.recommendedSystemKwp)} kWp")
                        CalcMetricRow("عدد الألواح الشمسية", "${result.panelCount} لوح (قدرة اللوح ${result.panelWattage}W)")
                        CalcMetricRow(
                            "سعة البطاريات المقترحة",
                            "${"%.2f".format(result.requiredBatteryCapacityWh / 1000.0)} kWh (${result.requiredBatteryAhAtSystemVoltage.toInt()}Ah @ ${result.assumptions.systemVoltage}V)"
                        )
                        CalcMetricRow("عدد البطاريات المقترح", "${result.batteryCount} بطارية (${result.singleBatterySpecLabel})")
                        CalcMetricRow("قدرة الانفرتر المناسبة", "${result.recommendedInverterWatts} واط")

                        // Mandatory Engineering Non-Guarantee Disclaimer
                        Surface(
                            color = SolarAmberSoft,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = SolarAmberAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيه هندسي: هذه الأرقام تقديرية استرشادية مبنية على الافتراضات الموضحة أعلاه، ولا تُعد مواصفات نهائية مضمونة إلا بعد المعاينة الهندسية الميدانية لموقع التركيب.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SolarTextPrimary
                                )
                            }
                        }

                        HorizontalDivider(color = SolarBorderLight)

                        OutlinedTextField(
                            value = designTitle,
                            onValueChange = { designTitle = it },
                            label = { Text("اسم التصميم للحفظ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("calc_design_title_input")
                        )

                        when (val sState = saveState) {
                            is NetworkResult.Success -> InlineResultBanner(message = sState.message, isSuccess = true)
                            is NetworkResult.Error -> InlineResultBanner(message = sState.arabicMessage, isSuccess = false)
                            else -> Unit
                        }

                        val isSaving = saveState is NetworkResult.Loading
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { clientViewModel.saveSolarDesign(designTitle) },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("calc_save_design_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(color = SolarWhite, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ التصميم")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    clientViewModel.prepareInstallationFromCalculator()
                                    onRequestInstallationWithDesign()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("calc_request_install_button")
                            ) {
                                Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب تركيب")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcMetricRow(label: String, value: String) {
    Surface(
        color = SolarGreenLight.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SolarTextPrimary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = SolarGreenDark, fontWeight = FontWeight.Bold)
        }
    }
}
