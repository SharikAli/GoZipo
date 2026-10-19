package com.mobile.core.map.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.map.api.NavigationProvider
import com.mobile.core.map.api.NavigationSettings
import gozipo.core.map.navigation.generated.resources.Res
import gozipo.core.map.navigation.generated.resources.navigation_close
import gozipo.core.map.navigation.generated.resources.navigation_unavailable
import org.jetbrains.compose.resources.stringResource

/**
 * Stands in where the Navigation SDK can't run: MAPBOX_ACCESS_TOKEN is missing, or on iOS the
 * app didn't register the Swift bridge.
 */
internal class PlaceholderNavigationProvider(private val reason: String) : NavigationProvider {
    @Composable
    override fun Navigation(
        destination: GeoPoint,
        settings: NavigationSettings,
        onArrival: () -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.navigation_unavailable, reason),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(Res.string.navigation_close))
                }
            }
        }
    }
}
