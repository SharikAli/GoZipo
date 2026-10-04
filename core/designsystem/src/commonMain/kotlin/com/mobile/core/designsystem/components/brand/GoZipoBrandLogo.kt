package com.mobile.core.designsystem.components.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

/** Lime app mark followed by the "GoZipo" wordmark. */
@Composable
fun GoZipoBrandLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 44.dp,
    showWordmark: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.md),
        modifier = modifier
    ) {
        GoZipoBrandMark(size = markSize)
        if (showWordmark) {
            Text(
                text = "GoZipo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.extended.textPrimary
            )
        }
    }
}

@Composable
fun GoZipoBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.size(size)
    ) {
        Icon(
            imageVector = Icons.Default.LocalTaxi,
            contentDescription = null,
            modifier = Modifier.padding(size / 5)
        )
    }
}

@Preview
@Composable
private fun GoZipoBrandLogoPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                GoZipoBrandLogo()
                GoZipoBrandMark(size = 64.dp)
            }
        }
    }
}
