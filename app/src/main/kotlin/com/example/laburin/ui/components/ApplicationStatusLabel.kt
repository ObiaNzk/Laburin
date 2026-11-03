package com.example.laburin.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.R
import com.example.laburin.domain.model.ApplicationStatus

@StringRes
fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.PENDING_SEND -> R.string.status_pending_send
    ApplicationStatus.SENT -> R.string.status_sent
    ApplicationStatus.APPROVED -> R.string.status_approved
    ApplicationStatus.REJECTED -> R.string.status_rejected
    ApplicationStatus.OFFER_CLOSED -> R.string.status_offer_closed
}

/** Extra line explaining what happens next, when there is something useful to say. */
@StringRes
fun ApplicationStatus.hintRes(): Int? = when (this) {
    ApplicationStatus.PENDING_SEND -> R.string.status_hint_pending
    ApplicationStatus.APPROVED -> R.string.status_hint_approved
    else -> null
}

@Composable
fun ApplicationStatusChip(status: ApplicationStatus, modifier: Modifier = Modifier) {
    val highlighted = status == ApplicationStatus.APPROVED
    Text(
        stringResource(status.labelRes()),
        fontSize = 12.sp,
        color = if (highlighted) Color.White else Color.Black,
        modifier = modifier
            .background(if (highlighted) Color.DarkGray else Color.Transparent, RoundedCornerShape(50))
            .border(1.dp, Color.Gray, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
