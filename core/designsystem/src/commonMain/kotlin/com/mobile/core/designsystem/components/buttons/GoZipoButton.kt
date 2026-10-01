package com.mobile.core.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

enum class GoZipoButtonStyle {
    /** Lime filled - the main action on a screen ("Find offers"). */
    PRIMARY,

    /** Neutral filled - secondary actions inside sheets. */
    SECONDARY,

    /** Transparent with outline. */
    OUTLINED,

    /** Neutral filled with destructive text ("Cancel the ride"). */
    DESTRUCTIVE,

    /** Text only, accent colored. */
    TEXT
}

@Composable
fun GoZipoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GoZipoButtonStyle = GoZipoButtonStyle.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val extended = MaterialTheme.colorScheme.extended
    val colors = style.colors()
    val border = when {
        style == GoZipoButtonStyle.OUTLINED -> BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.outline else extended.disabledOutline
        )

        else -> null
    }

    Button(
        // A loading button keeps its colors but ignores clicks
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = GoZipoTheme.spacing.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        elevation = null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .alpha(if (isLoading) 1f else 0f),
                strokeWidth = 2.dp,
                color = if (enabled) colors.contentColor else colors.disabledContentColor
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (isLoading) 0f else 1f)
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GoZipoButtonStyle.colors(): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    val extended = scheme.extended
    return when (this) {
        GoZipoButtonStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            disabledContainerColor = extended.disabledFill,
            disabledContentColor = extended.textDisabled
        )

        GoZipoButtonStyle.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = extended.surfaceHigher,
            contentColor = extended.textPrimary,
            disabledContainerColor = extended.disabledFill,
            disabledContentColor = extended.textDisabled
        )

        GoZipoButtonStyle.OUTLINED -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = extended.textPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = extended.textDisabled
        )

        GoZipoButtonStyle.DESTRUCTIVE -> ButtonDefaults.buttonColors(
            containerColor = extended.destructiveContainer,
            contentColor = extended.onDestructiveContainer,
            disabledContainerColor = extended.disabledFill,
            disabledContentColor = extended.textDisabled
        )

        GoZipoButtonStyle.TEXT -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = scheme.tertiary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = extended.textDisabled
        )
    }
}

@Composable
private fun ButtonsPreviewContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        GoZipoButton(
            text = "Find offers",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Find offers",
            onClick = {},
            isLoading = true,
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Confirm",
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Continue with passkey",
            onClick = {},
            style = GoZipoButtonStyle.SECONDARY,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Create account",
            onClick = {},
            style = GoZipoButtonStyle.OUTLINED,
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Cancel the ride",
            onClick = {},
            style = GoZipoButtonStyle.DESTRUCTIVE,
            modifier = Modifier.fillMaxWidth()
        )
        GoZipoButton(
            text = "Forgot password?",
            onClick = {},
            style = GoZipoButtonStyle.TEXT
        )
    }
}

@Preview
@Composable
private fun GoZipoButtonDarkPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface { ButtonsPreviewContent() }
    }
}

@Preview
@Composable
private fun GoZipoButtonLightPreview() {
    GoZipoTheme(darkTheme = false) {
        Surface { ButtonsPreviewContent() }
    }
}
