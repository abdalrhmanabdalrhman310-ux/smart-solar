package com.smartsolar.app

import com.smartsolar.app.domain.calculator.SolarSystemCalculator
import com.smartsolar.app.domain.model.ApplianceLoad
import com.smartsolar.app.domain.model.SolarAssumptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarSystemCalculatorTest {

    @Test
    fun `empty appliances yields zero design`() {
        val result = SolarSystemCalculator.calculate(emptyList(), SolarAssumptions())
        assertEquals(0.0, result.totalLoadWatts, 0.001)
        assertEquals(0.0, result.dailyConsumptionKwh, 0.001)
        assertEquals(0, result.panelCount)
        assertEquals(0, result.batteryCount)
        assertEquals(0, result.recommendedInverterWatts)
    }

    @Test
    fun `engineering sizing computes accurate panels batteries and inverter`() {
        val appliances = listOf(
            ApplianceLoad("1", "ثلاجة", 200.0, 1, 10.0),    // 2000 Wh/day, 200 W
            ApplianceLoad("2", "إنارة", 20.0, 10, 5.0),      // 1000 Wh/day, 200 W
            ApplianceLoad("3", "مكيف", 1200.0, 1, 4.0)       // 4800 Wh/day, 1200 W
        )
        // Total load = 1600 W -> 1600 * 1.25 = 2000 W inverter
        // Daily energy = 7800 Wh = 7.8 kWh
        // With 5.0 sun hours & 0.78 efficiency => 7.8 / (5 * 0.78) = 2.0 kWp => ceil(2000 / 550) = 4 panels
        val assumptions = SolarAssumptions(
            peakSunHours = 5.0,
            systemEfficiency = 0.78,
            batteryDepthOfDischarge = 0.80,
            batteryEfficiency = 0.90,
            autonomyDays = 1.0,
            systemVoltage = 48,
            singlePanelWatts = 550,
            singleBatteryAh = 100,
            singleBatteryVoltage = 48,
            inverterSafetyFactor = 1.25
        )
        val result = SolarSystemCalculator.calculate(appliances, assumptions)

        assertEquals(1600.0, result.totalLoadWatts, 0.01)
        assertEquals(7.8, result.dailyConsumptionKwh, 0.01)
        assertEquals(2.0, result.recommendedSystemKwp, 0.01)
        assertEquals(4, result.panelCount)
        assertEquals(2000, result.recommendedInverterWatts)
        // Battery Wh = 7800 / (0.8 * 0.9) = 10833.33 Wh -> ceil(10833.33 / 4800) = 3 batteries
        assertTrue(result.requiredBatteryCapacityWh > 10800.0)
        assertEquals(3, result.batteryCount)
    }
}
