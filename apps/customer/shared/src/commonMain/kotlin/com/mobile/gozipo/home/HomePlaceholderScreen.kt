package com.mobile.gozipo.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mobile.core.designsystem.components.brand.GoZipoSuccessIcon
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.layouts.GoZipoResultLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme

/**
 * Placeholder shown after login until the customer map flow is built.
 * Text is inline on purpose; this screen is replaced, not localized.
 */
@Composable
fun HomePlaceholderScreen(
    onLogoutClick: () -> Unit,
) {
    GoZipoSnackbarScaffold {
        GoZipoResultLayout(
            title = "You're signed in",
            description = "Booking rides is coming next.",
            icon = { GoZipoSuccessIcon() },
            primaryButton = {
                GoZipoButton(
                    text = "Log out",
                    onClick = onLogoutClick,
                    style = GoZipoButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Preview
@Composable
private fun HomePlaceholderScreenPreview() {
    GoZipoTheme(darkTheme = true) {
        HomePlaceholderScreen(onLogoutClick = {})
    }
}
