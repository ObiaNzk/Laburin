package com.example.laburin.ui.screens.publish

import androidx.lifecycle.SavedStateHandle
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.SalaryPeriod

/*
 * The offer form lives in memory, so Android killing the process in the background (low RAM while the business
 * checks WhatsApp) would empty it. Each change is also written to the SavedStateHandle as plain strings, which
 * the system restores into the next ViewModel.
 */

private const val TITLE = "offerForm.title"
private const val CATEGORY = "offerForm.category"
private const val SCHEDULE = "offerForm.schedule"
private const val DESCRIPTION = "offerForm.description"
private const val SALARY_MIN = "offerForm.salaryMin"
private const val SALARY_MAX = "offerForm.salaryMax"
private const val SALARY_PERIOD = "offerForm.salaryPeriod"

internal fun SavedStateHandle.saveOfferForm(draft: OfferDraft) {
    this[TITLE] = draft.title
    this[CATEGORY] = draft.category?.name
    this[SCHEDULE] = draft.schedule
    this[DESCRIPTION] = draft.description
    this[SALARY_MIN] = draft.salaryMin
    this[SALARY_MAX] = draft.salaryMax
    this[SALARY_PERIOD] = draft.salaryPeriod.name
}

/** Null when nothing was saved, i.e. the form was never touched. */
internal fun SavedStateHandle.restoreOfferForm(): OfferDraft? {
    val title = get<String>(TITLE) ?: return null
    return OfferDraft(
        title = title,
        category = get<String>(CATEGORY)?.let { name -> Category.entries.firstOrNull { it.name == name } },
        schedule = get<String>(SCHEDULE).orEmpty(),
        description = get<String>(DESCRIPTION).orEmpty(),
        salaryMin = get<String>(SALARY_MIN).orEmpty(),
        salaryMax = get<String>(SALARY_MAX).orEmpty(),
        salaryPeriod = SalaryPeriod.entries.firstOrNull { it.name == get<String>(SALARY_PERIOD) } ?: SalaryPeriod.MONTH,
    )
}
