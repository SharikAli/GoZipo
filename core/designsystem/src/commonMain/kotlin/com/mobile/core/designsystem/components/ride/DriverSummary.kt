package com.mobile.core.designsystem.components.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Header of the "driver assigned" sheet:
 * "Driver is arriving in ~5 min / Blue Suzuki Mehran / [APV-809]" + vehicle image.
 */
@Composable
fun DriverSummary(
    title: String,
    vehicleText: String,
    plateText: String,
    modifier: Modifier = Modifier,
    vehicleImage: @Composable () -> Unit = { DefaultVehicleImage() },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            Text(
                text = vehicleText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
            PlateChip(text = plateText)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 96.dp, height = 64.dp)
        ) {
            vehicleImage()
        }
    }
}

@Composable
fun PlateChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = extended.plateContainer,
        contentColor = extended.onPlateContainer,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Driver/rider avatar with an optional rating badge ("★ 5"). */
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    ratingText: String? = null,
    image: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.extended.textSecondary,
            modifier = Modifier.padding(12.dp)
        )
    },
) {
    val extended = MaterialTheme.colorScheme.extended
    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = extended.surfaceHigher,
            modifier = Modifier.size(64.dp)
        ) {
            image()
        }
        if (ratingText != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-4).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = extended.rating,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(text = ratingText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Preview
@Composable
private fun DriverSummaryPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column {
                DriverSummary(
                    title = "Driver is arriving in ~5 min",
                    vehicleText = "Blue Suzuki Mehran",
                    plateText = "APV-809"
                )
                ProfileAvatar(ratingText = "5", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
