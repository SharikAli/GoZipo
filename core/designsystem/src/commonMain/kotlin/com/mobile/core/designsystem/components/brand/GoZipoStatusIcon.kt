package com.mobile.core.designsystem.components.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/** Large circular icon at the top of result screens (success, failure, email sent). */
@Composable
fun GoZipoStatusIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.size(size)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(size / 4)
        )
    }
}

@Composable
fun GoZipoSuccessIcon(modifier: Modifier = Modifier) {
    GoZipoStatusIcon(
        icon = Icons.Default.Check,
        containerColor = MaterialTheme.colorScheme.extended.success,
        contentColor = MaterialTheme.colorScheme.extended.onSuccess,
        modifier = modifier
    )
}

@Composable
fun GoZipoFailureIcon(modifier: Modifier = Modifier) {
    GoZipoStatusIcon(
        icon = Icons.Default.Close,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = modifier
    )
}

@Composable
fun GoZipoEmailSentIcon(modifier: Modifier = Modifier) {
    GoZipoStatusIcon(
        icon = Icons.Default.MarkEmailRead,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
    )
}

@Preview
@Composable
private fun GoZipoStatusIconPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                GoZipoSuccessIcon()
                GoZipoFailureIcon()
                GoZipoEmailSentIcon()
            }
        }
    }
}
