package com.mobile.core.map.navigation

import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.map.api.NavigationProvider
import com.mobile.core.map.api.NavigationSettings
import gozipo.core.map.navigation.generated.resources.Res
import gozipo.core.map.navigation.generated.resources.navigation_close
import gozipo.core.map.navigation.generated.resources.navigation_finding_route
import gozipo.core.map.navigation.generated.resources.navigation_mute
import gozipo.core.map.navigation.generated.resources.navigation_overview
import gozipo.core.map.navigation.generated.resources.navigation_recenter
import gozipo.core.map.navigation.generated.resources.navigation_retry
import gozipo.core.map.navigation.generated.resources.navigation_route_failed
import gozipo.core.map.navigation.generated.resources.navigation_unmute
import org.jetbrains.compose.resources.stringResource

/**
 * [NavigationProvider] on the Mapbox Navigation SDK v3 for Android. The SDK's own views show
 * the map, the maneuver banner and trip progress; the buttons are Compose.
 *
 * @param simulate replay a drive along the route (`MAPBOX_SIMULATE_NAVIGATION`, desk testing)
 */
class MapboxTurnByTurnProvider(
    accessToken: String,
    private val simulate: Boolean,
) : NavigationProvider {

    init {
        MapboxOptions.accessToken = accessToken
    }

    @Composable
    override fun Navigation(
        destination: GeoPoint,
        settings: NavigationSettings,
        onArrival: () -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentOnArrival by rememberUpdatedState(onArrival)

        // The SDK views use Material Components styles, which the Compose activity theme lacks
        val session = remember(destination) {
            TurnByTurnSession(
                context = ContextThemeWrapper(
                    context,
                    com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
                ),
                destination = destination,
                origin = settings.origin,
                simulate = simulate,
                onArrival = { currentOnArrival() }
            )
        }

        DisposableEffect(session, lifecycleOwner) {
            if (!MapboxNavigationApp.isSetup()) {
                MapboxNavigationApp.setup(
                    NavigationOptions.Builder(context.applicationContext).build()
                )
            }
            MapboxNavigationApp.attach(lifecycleOwner)
            MapboxNavigationApp.registerObserver(session)
            onDispose {
                MapboxNavigationApp.unregisterObserver(session)
                MapboxNavigationApp.detach(lifecycleOwner)
                session.destroy()
            }
        }

        // Drivers glance at the screen; don't let it sleep mid-route
        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        LaunchedEffect(session, settings.darkTheme) {
            session.setDarkTheme(settings.darkTheme)
        }

        Box(modifier = modifier) {
            AndroidView(factory = { session.mapView }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                AndroidView(factory = { session.maneuverView }, modifier = Modifier.fillMaxWidth())
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    MapButton(
                        icon = if (session.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(
                            if (session.isMuted) Res.string.navigation_unmute else Res.string.navigation_mute
                        ),
                        onClick = session::toggleMute
                    )
                    if (session.isFollowing) {
                        MapButton(
                            Icons.Default.Route,
                            stringResource(Res.string.navigation_overview),
                            session::showOverview
                        )
                    } else {
                        MapButton(
                            Icons.Default.Navigation,
                            stringResource(Res.string.navigation_recenter),
                            session::recenter
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (session.routeState) {
                            TurnByTurnSession.RouteState.READY -> AndroidView(
                                factory = { session.tripProgressView },
                                update = { it.visibility = View.VISIBLE },
                                modifier = Modifier.fillMaxWidth()
                            )

                            TurnByTurnSession.RouteState.FINDING -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.navigation_finding_route),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            TurnByTurnSession.RouteState.FAILED -> Column(
                                modifier = Modifier.padding(
                                    12.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.navigation_route_failed),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = session::retry) {
                                    Text(stringResource(Res.string.navigation_retry))
                                }
                            }
                        }
                    }
                    MapButton(
                        Icons.Default.Close,
                        stringResource(Res.string.navigation_close),
                        onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun MapButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
