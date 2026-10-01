package com.mobile.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

val ColorScheme.extended: ExtendedColors
    @ReadOnlyComposable
    @Composable
    get() = LocalExtendedColors.current

/**
 * Semantic colors Material3's [ColorScheme] has no slot for. Read them through
 * `MaterialTheme.colorScheme.extended` - never use raw palette values in features.
 */
@Immutable
data class ExtendedColors(
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textDisabled: Color,

    // Surfaces (lower = behind sheets, higher = cards inside sheets, highest = round controls)
    val surfaceLower: Color,
    val surfaceHigher: Color,
    val surfaceHighest: Color,
    val surfaceOutline: Color,
    val divider: Color,
    val overlay: Color,

    // States
    val disabledFill: Color,
    val disabledOutline: Color,
    val primaryPressed: Color,

    // Status
    val success: Color,
    val onSuccess: Color,
    val cash: Color,
    val info: Color,
    val onInfo: Color,
    val rating: Color,
    val destructiveContainer: Color,
    val onDestructiveContainer: Color,

    // Ride UI
    val plateContainer: Color,
    val onPlateContainer: Color,
    val switchTrackOff: Color,

    // Map
    val mapRoute: Color,
    val mapRouteCasing: Color,
    val mapPickupMarker: Color,
    val mapDestinationMarker: Color,
    val mapMarkerContent: Color,
    val mapUserLocation: Color,
    val mapRiderMarker: Color,
    val mapSearchPulse: Color,
    val mapControlContainer: Color,
    val onMapControlContainer: Color,
)

val DarkExtendedColors = ExtendedColors(
    textPrimary = GoZipoBase0,
    textSecondary = GoZipoBase300,
    textTertiary = GoZipoBase400,
    textPlaceholder = GoZipoBase500,
    textDisabled = GoZipoBase600,

    surfaceLower = GoZipoBase950,
    surfaceHigher = GoZipoBase800,
    surfaceHighest = GoZipoBase750,
    surfaceOutline = GoZipoBase700,
    divider = GoZipoBase800,
    overlay = GoZipoBase1000Alpha60,

    disabledFill = GoZipoBase800,
    disabledOutline = GoZipoBase700,
    primaryPressed = GoZipoLime600,

    success = GoZipoGreen300,
    onSuccess = GoZipoBase1000,
    cash = GoZipoGreen300,
    info = GoZipoBlue500,
    onInfo = GoZipoBase0,
    rating = GoZipoAmber400,
    destructiveContainer = GoZipoBase800,
    onDestructiveContainer = GoZipoRed300,

    plateContainer = GoZipoBase600,
    onPlateContainer = GoZipoBase0,
    switchTrackOff = GoZipoBase750,

    mapRoute = GoZipoRouteDark,
    mapRouteCasing = GoZipoBase900,
    mapPickupMarker = GoZipoBase0,
    mapDestinationMarker = GoZipoBase0,
    mapMarkerContent = GoZipoBase1000,
    mapUserLocation = GoZipoBlue500,
    mapRiderMarker = GoZipoLime500,
    mapSearchPulse = GoZipoBase0Alpha30,
    mapControlContainer = GoZipoBase900,
    onMapControlContainer = GoZipoBase0,
)

val LightExtendedColors = ExtendedColors(
    textPrimary = GoZipoBase1000,
    textSecondary = GoZipoBase600,
    textTertiary = GoZipoBase500,
    textPlaceholder = GoZipoBase400,
    textDisabled = GoZipoBase300,

    surfaceLower = GoZipoBase50,
    surfaceHigher = GoZipoBase100,
    surfaceHighest = GoZipoBase150,
    surfaceOutline = GoZipoBase200,
    divider = GoZipoBase150,
    overlay = GoZipoBase1000Alpha60,

    disabledFill = GoZipoBase100,
    disabledOutline = GoZipoBase200,
    primaryPressed = GoZipoLime600,

    success = GoZipoGreen500,
    onSuccess = GoZipoBase0,
    cash = GoZipoGreen500,
    info = GoZipoBlue500,
    onInfo = GoZipoBase0,
    rating = GoZipoAmber600,
    destructiveContainer = GoZipoRed50,
    onDestructiveContainer = GoZipoRed500,

    plateContainer = GoZipoBase150,
    onPlateContainer = GoZipoBase1000,
    switchTrackOff = GoZipoBase200,

    mapRoute = GoZipoRouteLight,
    mapRouteCasing = GoZipoBase0,
    mapPickupMarker = GoZipoBase1000,
    mapDestinationMarker = GoZipoBase1000,
    mapMarkerContent = GoZipoBase0,
    mapUserLocation = GoZipoBlue500,
    mapRiderMarker = GoZipoLime800,
    mapSearchPulse = GoZipoBase1000Alpha10,
    mapControlContainer = GoZipoBase0,
    onMapControlContainer = GoZipoBase1000,
)

val DarkColorScheme = darkColorScheme(
    primary = GoZipoLime500,
    onPrimary = GoZipoBase1000,
    primaryContainer = GoZipoLime900,
    onPrimaryContainer = GoZipoLime300,

    secondary = GoZipoBase300,
    onSecondary = GoZipoBase1000,
    secondaryContainer = GoZipoBase800,
    onSecondaryContainer = GoZipoBase0,

    // Text-on-surface accent (links, text buttons) - lime reads well on dark
    tertiary = GoZipoLime500,
    onTertiary = GoZipoBase1000,
    tertiaryContainer = GoZipoLime900,
    onTertiaryContainer = GoZipoLime300,

    error = GoZipoRed300,
    onError = GoZipoRed900,
    errorContainer = GoZipoRed700,
    onErrorContainer = GoZipoRed200,

    background = GoZipoBase950,
    onBackground = GoZipoBase0,
    surface = GoZipoBase900,
    onSurface = GoZipoBase0,
    surfaceVariant = GoZipoBase800,
    onSurfaceVariant = GoZipoBase300,
    surfaceContainerLowest = GoZipoBase1000,
    surfaceContainerLow = GoZipoBase950,
    surfaceContainer = GoZipoBase900,
    surfaceContainerHigh = GoZipoBase850,
    surfaceContainerHighest = GoZipoBase800,
    inverseSurface = GoZipoBase0,
    inverseOnSurface = GoZipoBase1000,

    outline = GoZipoBase700,
    outlineVariant = GoZipoBase800,
    scrim = GoZipoBase1000,
)

val LightColorScheme = lightColorScheme(
    primary = GoZipoLime500,
    onPrimary = GoZipoBase1000,
    primaryContainer = GoZipoLime100,
    onPrimaryContainer = GoZipoLime900,

    secondary = GoZipoBase600,
    onSecondary = GoZipoBase0,
    secondaryContainer = GoZipoBase100,
    onSecondaryContainer = GoZipoBase1000,

    // Lime fails contrast as text on white, so text accents use a deep olive
    tertiary = GoZipoLime800,
    onTertiary = GoZipoBase0,
    tertiaryContainer = GoZipoLime100,
    onTertiaryContainer = GoZipoLime900,

    error = GoZipoRed500,
    onError = GoZipoBase0,
    errorContainer = GoZipoRed50,
    onErrorContainer = GoZipoRed700,

    background = GoZipoBase50,
    onBackground = GoZipoBase1000,
    surface = GoZipoBase0,
    onSurface = GoZipoBase1000,
    surfaceVariant = GoZipoBase100,
    onSurfaceVariant = GoZipoBase600,
    surfaceContainerLowest = GoZipoBase0,
    surfaceContainerLow = GoZipoBase50,
    surfaceContainer = GoZipoBase0,
    surfaceContainerHigh = GoZipoBase100,
    surfaceContainerHighest = GoZipoBase150,
    inverseSurface = GoZipoBase900,
    inverseOnSurface = GoZipoBase0,

    outline = GoZipoBase200,
    outlineVariant = GoZipoBase150,
    scrim = GoZipoBase1000,
)
