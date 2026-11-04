package com.example.laburin.ui.components

import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryFormattingTest {

    @Test
    fun `pesos use a dot for thousands`() {
        assertEquals("$ 600.000", formatPesos(600_000))
        assertEquals("$ 5.000", formatPesos(5_000))
        assertEquals("$ 950", formatPesos(950))
        assertEquals("$ 1.250.000", formatPesos(1_250_000))
    }

    @Test
    fun `a single amount shows just that amount`() {
        assertEquals("$ 600.000", salaryAmounts(Salary(600_000, null, SalaryPeriod.MONTH)))
    }

    @Test
    fun `a range shows both ends`() {
        assertEquals("$ 5.000 – $ 7.000", salaryAmounts(Salary(5_000, 7_000, SalaryPeriod.HOUR)))
    }
}
