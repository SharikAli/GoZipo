package com.mobile.core.designsystem.components.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

enum class GoZipoIconButtonStyle {
    /** Neutral circle inside sheets, e.g. fare stepper -/+ */
    FILLED,

    /** Floating over the map (back, recenter) with a shadow. */
    MAP,

    /** Lime circle. */
    PRIMARY
}

@Composable
fun GoZipoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GoZipoIconButtonStyle = GoZipoIconButtonStyle.FILLED,
    enabled: Boolean = true,
    size: Dp = GoZipoTheme.spacing.iconButtonSize,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = scheme.extended
    val (container, content) = when (style) {
        GoZipoIconButtonStyle.FILLED -> extended.surfaceHighest to extended.textPrimary
        GoZipoIconButtonStyle.MAP -> extended.mapControlContainer to extended.onMapControlContainer
        GoZipoIconButtonStyle.PRIMARY -> scheme.primary to scheme.onPrimary
    }
    val containerColor = if (enabled) container else extended.disabledFill
    val contentColor = if (enabled) content else extended.textDisabled

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = if (style == GoZipoIconButtonStyle.MAP) 6.dp else 0.dp,
        modifier = modifier.size(size)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.padding(size / 4)
        )
    }
}

@Preview
@Composable
private fun GoZipoIconButtonPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                GoZipoIconButton(
                    Icons.Default.Remove,
                    "Decrease",
                    onClick = {},
                    enabled = false
                )
                GoZipoIconButton(
                    Icons.Default.Add,
                    "Increase",
                    onClick = {}
                )
                GoZipoIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back",
                    onClick = {},
                    style = GoZipoIconButtonStyle.MAP
                )
                GoZipoIconButton(
                    Icons.Default.MyLocation,
                    "My location",
                    onClick = {},
                    style = GoZipoIconButtonStyle.PRIMARY
                )
            }
        }
    }
}
