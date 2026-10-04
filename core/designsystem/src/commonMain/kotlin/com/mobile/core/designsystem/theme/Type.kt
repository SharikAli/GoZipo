package com.mobile.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gozipo.core.designsystem.generated.resources.Res
import gozipo.core.designsystem.generated.resources.plusjakartasans_bold
import gozipo.core.designsystem.generated.resources.plusjakartasans_light
import gozipo.core.designsystem.generated.resources.plusjakartasans_medium
import gozipo.core.designsystem.generated.resources.plusjakartasans_regular
import gozipo.core.designsystem.generated.resources.plusjakartasans_semibold
import org.jetbrains.compose.resources.Font

val PlusJakartaSans @Composable get() = FontFamily(
    Font(
        resource = Res.font.plusjakartasans_light,
        weight = FontWeight.Light
    ),
    Font(
        resource = Res.font.plusjakartasans_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resource = Res.font.plusjakartasans_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resource = Res.font.plusjakartasans_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resource = Res.font.plusjakartasans_bold,
        weight = FontWeight.Bold
    ),
)

/** Large fare amount, e.g. "PKR542" in the fare stepper. */
val Typography.fareDisplay: TextStyle
    @Composable get() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    )

/** Small caption under list items, e.g. "Lower fares, no AC". */
val Typography.labelXSmall: TextStyle
    @Composable get() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

val Typography @Composable get() = Typography(
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Sheet titles: "Driver is arriving in ~5 min", "Searching for drivers"
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    // List item titles: vehicle name, address
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // Buttons
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
)
