package com.mobile.core.designsystem.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/**
 * Container for grouped content inside sheets and screens. [selected] adds an
 * outline (selected vehicle, chosen offer).
 */
@Composable
fun GoZipoCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    val border = if (selected) BorderStroke(1.5.dp, extended.surfaceOutline) else null

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = extended.surfaceHigher,
            contentColor = extended.textPrimary,
            border = border,
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            shape = shape,
            color = extended.surfaceHigher,
            contentColor = extended.textPrimary,
            border = border,
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Preview
@Composable
private fun GoZipoCardPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                GoZipoCard(selected = true, onClick = {}) {
                    Text("Your current ride", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ghazi Rd → Aisha Manzil mosque",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.extended.textSecondary
                    )
                }
            }
        }
    }
}
