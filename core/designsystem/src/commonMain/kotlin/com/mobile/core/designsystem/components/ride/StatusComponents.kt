package com.mobile.core.designsystem.components.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/** "[shield] Your driver is verified." style banner, optionally dismissible. */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.VerifiedUser,
    onDismiss: (() -> Unit)? = null,
    dismissContentDescription: String? = null,
) {
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        shape = MaterialTheme.shapes.large,
        color = extended.surfaceHigher,
        contentColor = extended.textPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = extended.info,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = dismissContentDescription
                    )
                }
            }
        }
    }
}

/** "[shield] All drivers verified" inline label. */
@Composable
fun VerifiedLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.extended.info,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.extended.textPrimary
        )
    }
}

/**
 * "Searching for drivers 0:58" header with a draining progress bar.
 * [progress] runs from 1 (just started) to 0 (timed out).
 */
@Composable
fun CountdownHeader(
    title: String,
    timeText: String,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
) {
    val extended = MaterialTheme.colorScheme.extended
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = extended.textPrimary
                )
                subtitle?.invoke()
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary
            )
        }
        LinearProgressIndicator(
            progress = progress,
            color = extended.textPrimary,
            trackColor = extended.surfaceHigher,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
        )
    }
}

@Preview
@Composable
private fun StatusComponentsPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                CountdownHeader(
                    title = "Searching for drivers",
                    timeText = "0:58",
                    progress = { 0.95f },
                    subtitle = { VerifiedLabel(text = "All drivers verified") }
                )
                InfoBanner(
                    text = "For your safety, keep communication in this chat and don't share personal details",
                    onDismiss = {},
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
