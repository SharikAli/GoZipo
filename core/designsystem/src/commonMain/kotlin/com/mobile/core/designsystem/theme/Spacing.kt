package com.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** Horizontal padding of sheet and screen content. */
    val screenHorizontal: Dp = 16.dp,
    /** Minimum touch target and height of primary buttons. */
    val buttonHeight: Dp = 56.dp,
    val iconButtonSize: Dp = 48.dp,
    val stepperButtonSize: Dp = 56.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
