package com.example.laburin.ui.components

import kotlin.math.roundToInt

/** Vertical shift that puts the map center in the middle of the part not covered by a bottom panel. */
fun mapCenterOffsetY(mapHeightPx: Int, bottomObstructionFraction: Float): Int =
    -(mapHeightPx * bottomObstructionFraction / 2).roundToInt()
