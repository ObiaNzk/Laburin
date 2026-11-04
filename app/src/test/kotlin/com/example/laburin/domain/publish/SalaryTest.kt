package com.example.laburin.domain.publish

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The salary is optional: a single amount or a range, per hour, day or month, in whole pesos. */
class SalaryTest {

    private val draft = OfferDraft("Mozo", Category.GASTRONOMY, "Sábados", "Atención en salón")

    private fun salaryErrors(min: String, max: String = "") =
        OfferDraftValidator.validate(draft.copy(salaryMin = min, salaryMax = max)).filter { it == PublishField.SALARY }

    @Test
    fun `an offer without salary is valid and has no salary`() {
        assertTrue(OfferDraftValidator.validate(draft).isEmpty())
        assertNull(draft.salary())
    }

    @Test
    fun `a single amount is a salary without maximum`() {
        val withSalary = draft.copy(salaryMin = "600000", salaryPeriod = SalaryPeriod.MONTH)

        assertEquals(Salary(600_000, null, SalaryPeriod.MONTH), withSalary.salary())
    }

    @Test
    fun `a range keeps both ends and the period`() {
        val withSalary = draft.copy(salaryMin = "5000", salaryMax = "7000", salaryPeriod = SalaryPeriod.HOUR)

        assertEquals(Salary(5_000, 7_000, SalaryPeriod.HOUR), withSalary.salary())
    }

    @Test
    fun `amounts are read the way people write them in Argentina`() {
        val withSalary = draft.copy(salaryMin = " $ 600.000 ", salaryMax = "750.000")

        assertEquals(Salary(600_000, 750_000, SalaryPeriod.MONTH), withSalary.salary())
    }

    @Test
    fun `the same amount at both ends is a single amount`() {
        assertEquals(Salary(600_000, null, SalaryPeriod.MONTH), draft.copy(salaryMin = "600000", salaryMax = "600.000").salary())
    }

    @Test
    fun `valid salaries pass validation`() {
        assertTrue(salaryErrors("600.000").isEmpty())
        assertTrue(salaryErrors("5000", "7000").isEmpty())
    }

    @Test
    fun `a maximum without a minimum is rejected`() {
        assertEquals(listOf(PublishField.SALARY), salaryErrors("", "700000"))
    }

    @Test
    fun `something that is not a whole amount of pesos is rejected`() {
        listOf("mucho", "600,50", "1.500,50", "-5000", "0", "12a").forEach {
            assertEquals(it, listOf(PublishField.SALARY), salaryErrors(it))
        }
    }

    @Test
    fun `a maximum lower than the minimum is rejected`() {
        assertEquals(listOf(PublishField.SALARY), salaryErrors("700000", "600000"))
    }

    @Test
    fun `an invalid maximum is rejected`() {
        assertEquals(listOf(PublishField.SALARY), salaryErrors("600000", "mucho"))
    }
}
