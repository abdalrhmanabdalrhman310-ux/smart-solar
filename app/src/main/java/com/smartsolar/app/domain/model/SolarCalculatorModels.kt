package com.smartsolar.app.domain.model

data class ApplianceLoad(
    val id: String,
    val name: String,
    val powerWatts: Double,
    val quantity: Int,
    val dailyHours: Double
) {
    val totalWatts: Double get() = powerWatts * quantity
    val dailyWattHours: Double get() = totalWatts * dailyHours
}

data class SolarAssumptions(
    val peakSunHours: Double = 5.0,          // ساعات ذروة الإشعاع الشمسي يومياً
    val systemEfficiency: Double = 0.78,     // كفاءة النظام (فواقد الكابلات والانفرتر والغبار)
    val batteryDepthOfDischarge: Double = 0.80, // عمق التفريغ (0.8 لليثيوم، 0.5 للجل/الرصاص)
    val batteryEfficiency: Double = 0.90,    // كفاءة الشحن والتفريغ للبطارية
    val autonomyDays: Double = 1.0,          // أيام الاستقلالية الاحتياطية
    val systemVoltage: Int = 48,             // جهد بنك البطاريات (12V / 24V / 48V)
    val singlePanelWatts: Int = 550,         // قدرة اللوح الشمسي المرجعي بالواط
    val singleBatteryAh: Int = 100,          // سعة البطارية المرجعية بالأمبير-ساعة
    val singleBatteryVoltage: Int = 48,      // جهد البطارية الواحدة
    val inverterSafetyFactor: Double = 1.25  // معامل أمان قدرة الانفرتر
)

data class SolarDesignResult(
    val totalLoadWatts: Double,
    val dailyConsumptionKwh: Double,
    val recommendedSystemKwp: Double,
    val panelCount: Int,
    val panelWattage: Int,
    val requiredBatteryCapacityWh: Double,
    val requiredBatteryAhAtSystemVoltage: Double,
    val batteryCount: Int,
    val singleBatterySpecLabel: String,
    val recommendedInverterWatts: Int,
    val assumptions: SolarAssumptions
)

data class SavedSolarDesign(
    val id: Long = 0L,
    val title: String,
    val totalLoadWatts: Double,
    val dailyKwh: Double,
    val systemKwp: Double,
    val panelCount: Int,
    val panelWatts: Int,
    val batteryCapacityWh: Double,
    val batteryCount: Int,
    val inverterWatts: Int,
    val assumptionsNote: String,
    val createdAt: String = ""
)
