package com.example.laburin.data.mapper

import com.example.laburin.data.remote.SalaryDto
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod

/** A salary the app can't read (unknown period) is dropped: the offer is still shown, as "A convenir". */
fun SalaryDto.toDomainOrNull(): Salary? {
    val period = SalaryPeriod.entries.firstOrNull { it.name == period } ?: return null
    return Salary(min, max, period)
}

fun Salary.toDto() = SalaryDto(min, max, period.name)

/** Salary as stored in Room: three nullable columns, all null when there is none. */
fun salaryFromColumns(min: Long?, max: Long?, period: String?): Salary? =
    if (min == null || period == null) null else SalaryDto(min, max, period).toDomainOrNull()
