package com.mobile.core.designsystem.components.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Pickup and destination stops joined by a dashed connector. Used floating over
 * the map (route preview) and inside sheets (current ride details).
 */
@Composable
fun RouteStopsCard(
    pickupText: String,
    destinationText: String,
    modifier: Modifier = Modifier,
    pickupPlaceholder: Boolean = false,
    destinationPlaceholder: Boolean = false,
    onPickupClick: (() -> Unit)? = null,
    onDestinationClick: (() -> Unit)? = null,
    pickupTrailing: @Composable (() -> Unit)? = null,
    destinationTrailing: @Composable (() -> Unit)? = null,
    elevated: Boolean = true,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (elevated) 6.dp else 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            RouteStopRow(
                icon = Icons.Default.EmojiPeople,
                text = pickupText,
                isPlaceholder = pickupPlaceholder,
                onClick = onPickupClick,
                trailing = pickupTrailing,
                showConnectorBelow = true,
            )
            RouteStopRow(
                icon = Icons.Default.Flag,
                text = destinationText,
                isPlaceholder = destinationPlaceholder,
                onClick = onDestinationClick,
                trailing = destinationTrailing,
                showConnectorAbove = true,
            )
        }
    }
}

@Composable
fun RouteStopRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showConnectorAbove: Boolean = false,
    showConnectorBelow: Boolean = false,
) {
    val extended = MaterialTheme.colorScheme.extended
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Connector(visible = showConnectorAbove)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = extended.textPrimary,
                modifier = Modifier.size(26.dp)
            )
            Connector(visible = showConnectorBelow)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (isPlaceholder) extended.textPlaceholder else extended.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun Connector(visible: Boolean) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(14.dp)
            .background(
                if (visible) MaterialTheme.colorScheme.extended.surfaceOutline else Color.Transparent
            )
    )
}

/** Small pill at the end of a stop row, e.g. "Entrance". */
@Composable
fun StopChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.extended.surfaceHigher,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.extended.textPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Preview
@Composable
private fun RouteStopsCardPreview() {
    GoZipoTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            RouteStopsCard(
                pickupText = "Dr Abdus Salam Rd",
                destinationText = "Aisha Manzil mosque ~22 min.",
                pickupTrailing = { StopChip(text = "Entrance", onClick = {}) },
                destinationTrailing = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add stop",
                        tint = MaterialTheme.colorScheme.extended.textPrimary
                    )
                }
            )
        }
    }
}
