package com.example.laburin.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.example.laburin.R
import com.example.laburin.domain.model.Category
import java.util.Locale

@StringRes
fun Category?.labelRes(): Int = when (this) {
    null -> R.string.category_all
    Category.GASTRONOMY -> R.string.category_gastronomy
    Category.MECHANICS -> R.string.category_mechanics
    Category.STOCK -> R.string.category_stock
    Category.CLOTHING -> R.string.category_clothing
}

fun formatDistance(meters: Int): String =
    if (meters >= 1_000) String.format(Locale.US, "%.1f km", meters / 1_000.0) else "$meters m"

fun Modifier.dashedBorder(color: Color, strokeWidth: Dp, cornerRadius: Dp) = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        size = size,
    )
}
