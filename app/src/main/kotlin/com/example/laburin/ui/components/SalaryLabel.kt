package com.example.laburin.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.laburin.R
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod

/** "$ 600.000 – $ 750.000 por mes", or "A convenir" when the offer has no salary. */
@Composable
fun salaryText(salary: Salary?): String =
    if (salary == null) {
        stringResource(R.string.salary_to_agree)
    } else {
        stringResource(R.string.salary_with_period, salaryAmounts(salary), stringResource(salary.period.perLabelRes()))
    }

@StringRes
private fun SalaryPeriod.perLabelRes() = when (this) {
    SalaryPeriod.HOUR -> R.string.salary_per_hour
    SalaryPeriod.DAY -> R.string.salary_per_day
    SalaryPeriod.MONTH -> R.string.salary_per_month
}

@StringRes
fun SalaryPeriod.optionLabelRes() = when (this) {
    SalaryPeriod.HOUR -> R.string.salary_option_hour
    SalaryPeriod.DAY -> R.string.salary_option_day
    SalaryPeriod.MONTH -> R.string.salary_option_month
}
