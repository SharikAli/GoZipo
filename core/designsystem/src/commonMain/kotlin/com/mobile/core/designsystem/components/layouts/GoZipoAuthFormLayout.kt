package com.mobile.core.designsystem.components.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.components.brand.GoZipoBrandLogo
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.presentation.util.DeviceConfiguration
import com.mobile.core.presentation.util.clearFocusOnTap
import com.mobile.core.presentation.util.currentDeviceConfiguration

private val FormMaxWidth = 480.dp

/**
 * Adaptive layout for auth forms: logo, title, optional subtitle and error,
 * then [content]. Phones in portrait get a scrolling column, phones in
 * landscape put the header beside the form, tablets get a centered card.
 */
@Composable
fun GoZipoAuthFormLayout(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    errorText: String? = null,
    onBackClick: (() -> Unit)? = null,
    backContentDescription: String? = null,
    logo: @Composable () -> Unit = { GoZipoBrandLogo() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val configuration = currentDeviceConfiguration()

    val topBar: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth().height(spacing.iconButtonSize)) {
            if (onBackClick != null) {
                GoZipoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                    onClick = onBackClick
                )
            }
        }
    }

    when (configuration) {
        DeviceConfiguration.MOBILE_PORTRAIT -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
            ) {
                topBar()
                Spacer(modifier = Modifier.height(spacing.lg))
                logo()
                Spacer(modifier = Modifier.height(spacing.xxl))
                AuthHeader(
                    title = title,
                    subtitle = subtitle,
                    errorText = errorText
                )
                Spacer(modifier = Modifier.height(spacing.xl))
                content()
                Spacer(modifier = Modifier.height(spacing.xl))
            }
        }

        DeviceConfiguration.MOBILE_LANDSCAPE -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                modifier = modifier
                    .fillMaxSize()
                    .clearFocusOnTap()
                    .padding(horizontal = spacing.xl, vertical = spacing.sm)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    topBar()
                    logo()
                    AuthHeader(
                        title = title,
                        subtitle = subtitle,
                        errorText = errorText
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = spacing.lg)
                ) {
                    content()
                }
            }
        }

        DeviceConfiguration.TABLET_PORTRAIT,
        DeviceConfiguration.TABLET_LANDSCAPE,
        DeviceConfiguration.DESKTOP -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.xl)
            ) {
                Box(modifier = Modifier.widthIn(max = FormMaxWidth)) {
                    topBar()
                }
                Spacer(modifier = Modifier.height(spacing.lg))
                logo()
                Spacer(modifier = Modifier.height(spacing.xl))
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .widthIn(max = FormMaxWidth)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = spacing.xl,
                            vertical = spacing.xxl
                        )
                    ) {
                        AuthHeader(
                            title = title,
                            subtitle = subtitle,
                            errorText = errorText
                        )
                        Spacer(modifier = Modifier.height(spacing.xl))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthHeader(
    title: String,
    subtitle: String?,
    errorText: String?,
    textAlign: TextAlign = TextAlign.Start,
) {
    val extended = MaterialTheme.colorScheme.extended
    Column(
        verticalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = extended.textPrimary,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = extended.textSecondary,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(visible = errorText != null) {
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun GoZipoAuthFormLayoutPreview() {
    GoZipoTheme(darkTheme = true) {
        GoZipoSnackbarScaffold {
            GoZipoAuthFormLayout(
                title = "Welcome back",
                subtitle = "Log in to book your next ride",
                errorText = "The email or password is incorrect",
                onBackClick = {}
            ) {
                GoZipoTextField(
                    state = rememberTextFieldState(),
                    title = "Email",
                    placeholder = "you@example.com",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                GoZipoButton(
                    text = "Log in",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
