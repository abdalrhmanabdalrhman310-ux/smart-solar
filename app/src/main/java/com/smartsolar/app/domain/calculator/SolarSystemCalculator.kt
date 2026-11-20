package com.smartsolar.app.domain.calculator

import com.smartsolar.app.domain.model.ApplianceLoad
import com.smartsolar.app.domain.model.SolarAssumptions
import com.smartsolar.app.domain.model.SolarDesignResult
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Engineering sizing calculator for off-grid / hybrid solar PV systems.
 *
 * Equations:
 * 1. Total instantaneous load (W) = Sum(Power_W * Quantity)
 * 2. Daily energy demand (Wh) = Sum(Power_W * Quantity * Hours)
 *    Daily consumption (kWh) = Wh / 1000
 * 3. Required PV array power (kWp) = Daily_kWh / (PeakSunHours * SystemEfficiency)
 * 4. Panel count = ceil((Required_kWp * 1000) / SinglePanelWatts)
 * 5. Battery bank gross energy (Wh) = (Daily_Wh * AutonomyDays) / (DoD * BatteryEfficiency)
 *    Battery bank Ah @ SystemVoltage = Battery_Wh / SystemVoltage
 *    Battery count (approx units of singleBatteryVoltage * singleBatteryAh) =
 *        ceil(Battery_Wh / (singleBatteryVoltage * singleBatteryAh))
 * 6. Inverter rating (W) = ceil_to_standard_tier(TotalLoadWatts * InverterSafetyFactor)
 */
object SolarSystemCalculator {

    private val standardInverterTiersWatts = listOf(
        1000, 1500, 2000, 3000, 3500, 5000, 6000, 8000, 10000, 12000, 15000, 20000, 25000, 30000
    )

    fun calculate(
        appliances: List<ApplianceLoad>,
        assumptions: SolarAssumptions
    ): SolarDesignResult {
        val validAppliances = appliances.filter { it.powerWatts > 0 && it.quantity > 0 && it.dailyHours > 0 }

        val totalLoadWatts = validAppliances.sumOf { it.totalWatts }
        val dailyWh = validAppliances.sumOf { it.dailyWattHours }
        val dailyKwh = dailyWh / 1000.0

        val safeSunHours = max(1.0, assumptions.peakSunHours)
        val safeEfficiency = assumptions.systemEfficiency.coerceIn(0.40, 0.98)
        val safeDoD = assumptions.batteryDepthOfDischarge.coerceIn(0.20, 0.95)
        val safeBatEfficiency = assumptions.batteryEfficiency.coerceIn(0.60, 0.98)
        val safePanelWatts = max(50, assumptions.singlePanelWatts)
        val safeSystemVoltage = if (assumptions.systemVoltage in listOf(12, 24, 48)) assumptions.systemVoltage else 48
        val safeBatteryUnitWh = max(120, assumptions.singleBatteryVoltage * assumptions.singleBatteryAh).toDouble()

        val requiredKwp = if (dailyKwh > 0) {
            dailyKwh / (safeSunHours * safeEfficiency)
        } else {
            0.0
        }

        val panelCount = if (requiredKwp > 0) {
            ceil((requiredKwp * 1000.0) / safePanelWatts).toInt()
        } else {
            0
        }

        val requiredBatteryWh = if (dailyWh > 0) {
            (dailyWh * max(0.5, assumptions.autonomyDays)) / (safeDoD * safeBatEfficiency)
        } else {
            0.0
        }

        val requiredBatteryAh = if (requiredBatteryWh > 0) {
            requiredBatteryWh / safeSystemVoltage
        } else {
            0.0
        }

        val batteryCount = if (requiredBatteryWh > 0) {
            ceil(requiredBatteryWh / safeBatteryUnitWh).toInt()
        } else {
            0
        }

        val rawInverterWatts = totalLoadWatts * max(1.1, assumptions.inverterSafetyFactor)
        val recommendedInverterWatts = if (rawInverterWatts <= 0.0) {
            0
        } else {
            standardInverterTiersWatts.firstOrNull { it >= rawInverterWatts }
                ?: (((rawInverterWatts / 1000.0).roundToInt() + 1) * 1000)
        }

        return SolarDesignResult(
            totalLoadWatts = totalLoadWatts,
            dailyConsumptionKwh = dailyKwh,
            recommendedSystemKwp = requiredKwp,
            panelCount = panelCount,
            panelWattage = safePanelWatts,
            requiredBatteryCapacityWh = requiredBatteryWh,
            requiredBatteryAhAtSystemVoltage = requiredBatteryAh,
            batteryCount = batteryCount,
            singleBatterySpecLabel = "${assumptions.singleBatteryVoltage}V / ${assumptions.singleBatteryAh}Ah",
            recommendedInverterWatts = recommendedInverterWatts,
            assumptions = assumptions
        )
    }

    fun buildAssumptionsSummary(assumptions: SolarAssumptions): String {
        val efficiencyPct = (assumptions.systemEfficiency * 100).roundToInt()
        val dodPct = (assumptions.batteryDepthOfDischarge * 100).roundToInt()
        return "ساعات الشمس: ${assumptions.peakSunHours} س/يوم | كفاءة النظام: $efficiencyPct% | عمق التفريغ: $dodPct% | جهد النظام: ${assumptions.systemVoltage}V | أيام الاستقلالية: ${assumptions.autonomyDays}"
    }
}
