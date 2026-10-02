package com.mobile.auth.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.continue_with_passkey
import gozipo.feature.auth.presentation.generated.resources.or
import org.jetbrains.compose.resources.stringResource

/** "──── or ────" between the password form and alternative sign-in options. */
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val extended = MaterialTheme.colorScheme.extended
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.md),
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalDivider(color = extended.divider, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.or),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textTertiary
        )
        HorizontalDivider(color = extended.divider, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PasskeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    GoZipoButton(
        text = stringResource(Res.string.continue_with_passkey),
        onClick = onClick,
        style = GoZipoButtonStyle.SECONDARY,
        isLoading = isLoading,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

/** "New to GoZipo? Create account" */
@Composable
fun AuthFooterLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.extended.textSecondary
        )
        TextButton(onClick = onClick) {
            Text(
                text = action,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
