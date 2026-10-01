package com.mobile.core.designsystem.components.ride

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.designsystem.theme.fareDisplay

/**
 * "− PKR542 +" control for adjusting the offered fare. The amount and bounds
 * come from the backend; this only renders them and reports taps.
 */
@Composable
fun FareStepper(
    fareText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
    decreaseContentDescription: String? = null,
    increaseContentDescription: String? = null,
) {
    val spacing = GoZipoTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        GoZipoIconButton(
            icon = Icons.Default.Remove,
            contentDescription = decreaseContentDescription,
            onClick = onDecrease,
            enabled = canDecrease,
            size = spacing.stepperButtonSize
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.sm)
        ) {
            AnimatedContent(
                targetState = fareText,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.fareDisplay,
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.extended.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        GoZipoIconButton(
            icon = Icons.Default.Add,
            contentDescription = increaseContentDescription,
            onClick = onIncrease,
            enabled = canIncrease,
            size = spacing.stepperButtonSize
        )
    }
}

@Preview
@Composable
private fun FareStepperPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            FareStepper(
                fareText = "PKR542",
                caption = "Recommended fare",
                onDecrease = {},
                onIncrease = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
