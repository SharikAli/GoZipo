package com.mobile.core.designsystem.components.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * A vehicle type in the "Select vehicle" sheet (Car, AC Car, Premium).
 * When [selected], it shows an outlined card and reveals [selectedContent]
 * (usually a [FareStepper]) below the vehicle info.
 */
@Composable
fun VehicleOptionItem(
    title: String,
    capacityText: String,
    etaText: String?,
    description: String,
    priceText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit = { DefaultVehicleImage() },
    titleTrailing: @Composable (() -> Unit)? = null,
    selectedContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(1.dp, extended.surfaceOutline) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (selected) 6.dp else 0.dp)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (selected) extended.surfaceHigher else MaterialTheme.colorScheme.surface,
            ) {
                VehicleInfoRow(
                    title = title,
                    capacityText = capacityText,
                    etaText = etaText,
                    description = description,
                    priceText = if (selected) null else priceText,
                    image = image,
                    titleTrailing = titleTrailing,
                )
            }
            AnimatedVisibility(visible = selected && selectedContent != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    selectedContent?.invoke(this)
                }
            }
        }
    }
}

@Composable
private fun VehicleInfoRow(
    title: String,
    capacityText: String,
    etaText: String?,
    description: String,
    priceText: String?,
    image: @Composable () -> Unit,
    titleTrailing: @Composable (() -> Unit)?,
) {
    val extended = MaterialTheme.colorScheme.extended
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 72.dp, height = 48.dp)
        ) {
            image()
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                titleTrailing?.invoke()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = extended.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (etaText != null) "$capacityText • $etaText" else capacityText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textPrimary
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (priceText != null) {
            Text(
                text = priceText,
                style = MaterialTheme.typography.titleMedium,
                color = extended.textPrimary,
                modifier = Modifier.align(Alignment.Top)
            )
        }
    }
}

@Composable
fun DefaultVehicleImage(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.DirectionsCar,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.extended.textPrimary,
        modifier = modifier.size(40.dp)
    )
}

@Preview
@Composable
private fun VehicleOptionItemPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            var selected by remember { mutableStateOf("car") }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                VehicleOptionItem(
                    title = "Car",
                    capacityText = "4",
                    etaText = "4 min",
                    description = "Lower fares, no AC",
                    priceText = "~PKR588",
                    selected = selected == "car",
                    onClick = { selected = "car" },
                    selectedContent = {
                        FareStepper(
                            fareText = "PKR588",
                            caption = "Recommended fare",
                            onDecrease = {},
                            onIncrease = {}
                        )
                    }
                )
                VehicleOptionItem(
                    title = "AC Car",
                    capacityText = "4",
                    etaText = "4 min",
                    description = "Cars with AC",
                    priceText = "~PKR647",
                    selected = selected == "ac",
                    onClick = { selected = "ac" },
                    image = { DefaultVehicleImage(icon = Icons.Default.AcUnit) }
                )
                VehicleOptionItem(
                    title = "Premium",
                    capacityText = "4",
                    etaText = "5 min",
                    description = "Sedans with AC",
                    priceText = "~PKR1,000",
                    selected = selected == "premium",
                    onClick = { selected = "premium" },
                    image = { DefaultVehicleImage(icon = Icons.Default.LocalTaxi) }
                )
            }
        }
    }
}
