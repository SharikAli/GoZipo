package com.mobile.core.designsystem.components.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * The map's zoom in (+) / zoom out (−) buttons: one floating pill styled like the other map
 * controls. The design system has no strings, so callers pass the content descriptions.
 */
@Composable
fun GoZipoMapZoomControls(
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    zoomInContentDescription: String,
    zoomOutContentDescription: String,
    modifier: Modifier = Modifier,
    zoomInEnabled: Boolean = true,
    zoomOutEnabled: Boolean = true,
) {
    val extended = MaterialTheme.colorScheme.extended
    val size = GoZipoTheme.spacing.iconButtonSize
    Surface(
        shape = RoundedCornerShape(size / 2),
        color = extended.mapControlContainer,
        contentColor = extended.onMapControlContainer,
        shadowElevation = 6.dp,
        modifier = modifier.width(size)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZoomButton(Icons.Default.Add, zoomInContentDescription, zoomInEnabled, onZoomInClick)
            HorizontalDivider(
                color = extended.divider,
                modifier = Modifier.width(size / 2)
            )
            ZoomButton(Icons.Default.Remove, zoomOutContentDescription, zoomOutEnabled, onZoomOutClick)
        }
    }
}

@Composable
private fun ZoomButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    val size = GoZipoTheme.spacing.iconButtonSize
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = extended.mapControlContainer,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) extended.onMapControlContainer else extended.textDisabled,
                modifier = Modifier.height(size / 2).width(size / 2)
            )
        }
    }
}

@Preview
@Composable
private fun GoZipoMapZoomControlsPreview() {
    GoZipoTheme(darkTheme = true) {
        GoZipoMapZoomControls(
            onZoomInClick = {},
            onZoomOutClick = {},
            zoomInContentDescription = "Zoom in",
            zoomOutContentDescription = "Zoom out",
            zoomOutEnabled = false
        )
    }
}
