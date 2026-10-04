package com.mobile.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GoZipoShapes = Shapes(
    // Plate chip, small tags
    extraSmall = RoundedCornerShape(6.dp),
    // Text fields, small buttons
    small = RoundedCornerShape(12.dp),
    // Cards, list containers, primary buttons
    medium = RoundedCornerShape(16.dp),
    // Selected vehicle card, floating location card
    large = RoundedCornerShape(24.dp),
    // Bottom sheet top corners
    extraLarge = RoundedCornerShape(28.dp),
)

/** Bottom sheets only round their top corners. */
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
