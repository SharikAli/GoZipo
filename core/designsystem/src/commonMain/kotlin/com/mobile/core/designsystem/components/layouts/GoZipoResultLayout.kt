package com.mobile.core.designsystem.components.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.components.brand.GoZipoSuccessIcon
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Centered icon, title, description and actions, for outcomes such as
 * "Account created" or "Email verified". [secondaryError] shows under the
 * secondary button (e.g. a failed resend).
 */
@Composable
fun GoZipoResultLayout(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    primaryButton: (@Composable () -> Unit)? = null,
    secondaryButton: (@Composable () -> Unit)? = null,
    secondaryError: String? = null,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xl)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            icon()
            Spacer(modifier = Modifier.height(spacing.xl))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = extended.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.xxl))
            primaryButton?.invoke()
            if (secondaryButton != null) {
                Spacer(modifier = Modifier.height(spacing.md))
                secondaryButton()
            }
            if (secondaryError != null) {
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = secondaryError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
private fun GoZipoResultLayoutPreview() {
    GoZipoTheme(darkTheme = true) {
        GoZipoSnackbarScaffold {
            GoZipoResultLayout(
                title = "Account created",
                description = "We've sent a verification email to you@example.com",
                icon = { GoZipoSuccessIcon() },
                primaryButton = {
                    GoZipoButton(text = "Log in", onClick = {}, modifier = Modifier.fillMaxWidth())
                },
                secondaryButton = {
                    GoZipoButton(
                        text = "Resend verification email",
                        onClick = {},
                        style = GoZipoButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}
