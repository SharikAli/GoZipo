package com.mobile.core.designsystem.components.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Map-library independent marker visuals. The map layer positions them
 * (e.g. as view annotations); the marker's anchor is its bottom center.
 */
@Composable
fun PickupMarker(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    PinMarker(
        icon = Icons.Default.EmojiPeople,
        containerColor = MaterialTheme.colorScheme.extended.mapPickupMarker,
        label = label,
        modifier = modifier
    )
}

@Composable
fun DestinationMarker(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    PinMarker(
        icon = Icons.Default.Flag,
        containerColor = MaterialTheme.colorScheme.extended.mapDestinationMarker,
        label = label,
        modifier = modifier
    )
}

@Composable
private fun PinMarker(
    icon: ImageVector,
    containerColor: Color,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.colorScheme.extended
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 2.dp,
                    color = extended.mapMarkerContent,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = extended.mapMarkerContent,
                modifier = Modifier.size(28.dp)
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.mapMarkerContent,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        // Stem pointing at the exact coordinate
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .background(containerColor)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(containerColor, CircleShape)
        )
    }
}

/** A rider's vehicle on the map, rotated to its [bearing] in degrees. Anchor: center. */
@Composable
fun RiderMarker(
    bearing: Float,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.DirectionsCar,
) {
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        shape = CircleShape,
        color = extended.mapRiderMarker,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp,
        modifier = modifier
            .size(36.dp)
            .rotate(bearing)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(6.dp)
        )
    }
}

/** The user's own position: blue dot with a white ring. Anchor: center. */
@Composable
fun UserLocationDot(modifier: Modifier = Modifier) {
    val extended = MaterialTheme.colorScheme.extended
    Box(
        modifier = modifier
            .size(20.dp)
            .background(MaterialTheme.colorScheme.inverseOnSurface, CircleShape)
            .padding(3.dp)
            .background(extended.mapUserLocation, CircleShape)
    )
}

/**
 * Expanding rings drawn around the pickup point while searching for drivers.
 * Place it centered on the pickup coordinate.
 */
@Composable
fun SearchingPulse(
    modifier: Modifier = Modifier,
    maxRadius: Dp = 140.dp,
    rings: Int = 2,
) {
    val color = MaterialTheme.colorScheme.extended.mapSearchPulse
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.size(maxRadius * 2)) {
        val maxRadiusPx = maxRadius.toPx()
        repeat(rings) { index ->
            val ringProgress = (progress + index.toFloat() / rings) % 1f
            drawCircle(
                color = color.copy(alpha = color.alpha * (1f - ringProgress)),
                radius = maxRadiusPx * (0.25f + 0.75f * ringProgress)
            )
        }
    }
}

@Preview
@Composable
private fun MapMarkersPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.extended.surfaceLower) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                SearchingPulse()
                PickupMarker()
            }
        }
    }
}

@Preview
@Composable
private fun MapMarkersRowPreview() {
    GoZipoTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.extended.surfaceLower) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                PickupMarker(label = "3 min")
                DestinationMarker()
                RiderMarker(bearing = 45f)
                UserLocationDot()
            }
        }
    }
}
