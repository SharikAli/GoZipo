package com.mobile.core.designsystem.components.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Single line text input backed by [TextFieldState], matching the MVI screens
 * that keep `TextFieldState` in their UI state.
 */
@Composable
fun GoZipoTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    title: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    inputTransformation: InputTransformation? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    GoZipoTextFieldLayout(
        title = title,
        supportingText = supportingText,
        isError = isError,
        modifier = modifier
    ) { textStyle, containerModifier ->
        BasicTextField(
            state = state,
            enabled = enabled,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            inputTransformation = inputTransformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
            modifier = containerModifier(isFocused, enabled),
            decorator = { innerTextField ->
                GoZipoTextFieldDecoration(
                    isEmpty = state.text.isEmpty(),
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    innerTextField = innerTextField
                )
            }
        )
    }
}

@Composable
fun GoZipoPasswordTextField(
    state: TextFieldState,
    isPasswordVisible: Boolean,
    onToggleVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    showPasswordContentDescription: String? = null,
    hidePasswordContentDescription: String? = null,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    GoZipoTextFieldLayout(
        title = title,
        supportingText = supportingText,
        isError = isError,
        modifier = modifier
    ) { textStyle, containerModifier ->
        BasicSecureTextField(
            state = state,
            enabled = enabled,
            textStyle = textStyle,
            textObfuscationMode = if (isPasswordVisible) {
                TextObfuscationMode.Visible
            } else {
                TextObfuscationMode.RevealLastTyped
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onKeyboardAction = onKeyboardAction,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
            modifier = containerModifier(isFocused, enabled),
            decorator = { innerTextField ->
                GoZipoTextFieldDecoration(
                    isEmpty = state.text.isEmpty(),
                    placeholder = placeholder,
                    leadingIcon = null,
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibilityClick) {
                            Icon(
                                imageVector = if (isPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (isPasswordVisible) {
                                    hidePasswordContentDescription
                                } else {
                                    showPasswordContentDescription
                                },
                                tint = MaterialTheme.colorScheme.extended.textTertiary
                            )
                        }
                    },
                    innerTextField = innerTextField
                )
            }
        )
    }
}

@Composable
private fun GoZipoTextFieldLayout(
    title: String?,
    supportingText: String?,
    isError: Boolean,
    modifier: Modifier = Modifier,
    textField: @Composable (
        textStyle: TextStyle,
        containerModifier: @Composable (isFocused: Boolean, enabled: Boolean) -> Modifier
    ) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val extended = scheme.extended
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = extended.textPrimary)
    val shape = MaterialTheme.shapes.small

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = extended.textSecondary
            )
        }
        textField(textStyle) { isFocused, enabled ->
            val borderColor = when {
                isError -> scheme.error
                isFocused -> scheme.tertiary
                else -> Color.Transparent
            }
            Modifier
                .fillMaxWidth()
                .background(
                    color = if (enabled) extended.surfaceHigher else extended.disabledFill,
                    shape = shape
                )
                .border(width = 1.dp, color = borderColor, shape = shape)
                .defaultMinSize(minHeight = FieldMinHeight)
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) scheme.error else extended.textTertiary
            )
        }
    }
}

private val FieldMinHeight = 56.dp

@Composable
private fun GoZipoTextFieldDecoration(
    isEmpty: Boolean,
    placeholder: String?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    innerTextField: @Composable () -> Unit,
) {
    // Fixed min height with centered content: a 48dp IconButton (trailing/leading)
    // fits inside it, so fields with and without icons are the same height.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .heightIn(min = FieldMinHeight)
            .padding(
                start = 16.dp,
                end = if (trailingIcon != null) 4.dp else 16.dp,
                top = 4.dp,
                bottom = 4.dp
            )
    ) {
        leadingIcon?.invoke()
        Box(modifier = Modifier.weight(1f)) {
            if (isEmpty && placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.extended.textPlaceholder
                )
            }
            innerTextField()
        }
        trailingIcon?.invoke()
    }
}

// apiLevel 36: on API 37 BasicSecureTextField reads android.text.ShowSecretsSetting,
// which the preview renderer (layoutlib) doesn't have yet. Devices are unaffected.
@Preview(apiLevel = 36)
@Composable
private fun GoZipoTextFieldPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                GoZipoTextField(
                    state = rememberTextFieldState(),
                    title = "Email",
                    placeholder = "you@example.com",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.extended.textTertiary
                        )
                    }
                )
                GoZipoTextField(
                    state = rememberTextFieldState("not-an-email"),
                    title = "Email",
                    isError = true,
                    supportingText = "Enter a valid email address"
                )
                GoZipoPasswordTextField(
                    state = rememberTextFieldState("Secret123"),
                    isPasswordVisible = false,
                    onToggleVisibilityClick = { },
                    title = "Password",
                    supportingText = "At least 9 characters"
                )
                GoZipoPasswordTextField(
                    state = rememberTextFieldState("Secret123"),
                    isPasswordVisible = true,
                    onToggleVisibilityClick = { },
                    title = "Password",
                    supportingText = "At least 9 characters"
                )
            }
        }
    }
}
