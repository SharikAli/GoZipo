package com.mobile.core.designsystem.components.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * One-time code input drawn as [length] boxes, backed by a single
 * [TextFieldState] so paste and the keyboard work as in a normal field.
 * Only digits are accepted.
 */
@Composable
fun GoZipoCodeTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
    supportingText: String? = null,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = scheme.extended
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val digitsOnly = remember {
        InputTransformation {
            if (!asCharSequence().all { it.isDigit() }) {
                revertAllChanges()
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        BasicTextField(
            state = state,
            enabled = enabled,
            inputTransformation = InputTransformation.maxLength(length).then(digitsOnly),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = onKeyboardAction,
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = interactionSource,
            // The boxes draw the text; hide the real cursor
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
            modifier = Modifier.fillMaxWidth(),
            decorator = { innerTextField ->
                Box {
                    // The real (transparent) field stays in the tree for focus, IME and paste
                    Box(modifier = Modifier.padding(1.dp)) { innerTextField() }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val text = state.text
                        repeat(length) { index ->
                            val char = text.getOrNull(index)
                            val isCurrent = isFocused && (index == text.length ||
                                    (text.length == length && index == length - 1))
                            val borderColor = when {
                                isError -> scheme.error
                                isCurrent -> scheme.tertiary
                                else -> Color.Transparent
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(max = 56.dp)
                                    .aspectRatio(0.85f)
                                    .background(
                                        color = if (enabled) extended.surfaceHigher else extended.disabledFill,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .border(1.dp, borderColor, MaterialTheme.shapes.small)
                            ) {
                                Text(
                                    text = char?.toString().orEmpty(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = extended.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) scheme.error else extended.textTertiary
            )
        }
    }
}

@Preview
@Composable
private fun GoZipoCodeTextFieldPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                GoZipoCodeTextField(state = rememberTextFieldState("482"))
                GoZipoCodeTextField(
                    state = rememberTextFieldState("123456"),
                    isError = true,
                    supportingText = "The code is invalid or has expired"
                )
            }
        }
    }
}
