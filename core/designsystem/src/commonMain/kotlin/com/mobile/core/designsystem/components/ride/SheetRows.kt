package com.mobile.core.designsystem.components.ride

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

@Composable
fun GoZipoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = scheme.extended
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = scheme.onPrimary,
            checkedTrackColor = scheme.primary,
            checkedBorderColor = scheme.primary,
            uncheckedThumbColor = extended.textPrimary,
            uncheckedTrackColor = extended.switchTrackOff,
            uncheckedBorderColor = extended.switchTrackOff,
        ),
        modifier = modifier
    )
}

/** "Auto-accept offer of PKR542 [switch]". The whole row toggles. */
@Composable
fun GoZipoSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.extended.textPrimary,
            modifier = Modifier.weight(1f)
        )
        GoZipoSwitch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

/** "Share my ride >" / "Call 15 >" rows. [destructive] colors the row for emergency actions. */
@Composable
fun ListActionRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    showChevron: Boolean = true,
) {
    val extended = MaterialTheme.colorScheme.extended
    val color = if (destructive) extended.onDestructiveContainer else extended.textPrimary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = extended.textTertiary
            )
        }
    }
}

/** "Payment: [cash] PKR593 Cash" summary row. */
@Composable
fun PaymentMethodRow(
    amountText: String,
    methodText: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector = Icons.Default.Payments,
    onClick: (() -> Unit)? = null,
) {
    val extended = MaterialTheme.colorScheme.extended
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (label != null) {
            SectionLabel(text = label)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = extended.cash)
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                color = extended.textPrimary
            )
            Text(
                text = methodText,
                style = MaterialTheme.typography.titleMedium,
                color = extended.textSecondary
            )
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.extended.textSecondary,
        modifier = modifier
    )
}

@Composable
fun GoZipoDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.extended.divider,
        modifier = modifier
    )
}

@Preview
@Composable
private fun SheetRowsPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column {
                var autoAccept by remember { mutableStateOf(false) }
                GoZipoSwitchRow(
                    text = "Auto-accept offer of PKR542",
                    checked = autoAccept,
                    onCheckedChange = { autoAccept = it },
                    icon = Icons.AutoMirrored.Filled.Send
                )
                GoZipoDivider()
                PaymentMethodRow(label = "Payment", amountText = "PKR593", methodText = "Cash")
                GoZipoDivider()
                ListActionRow(text = "Share my ride", icon = Icons.Default.Share, onClick = {})
                ListActionRow(
                    text = "Call 15",
                    icon = Icons.Default.NotificationsActive,
                    onClick = {},
                    destructive = true
                )
            }
        }
    }
}
