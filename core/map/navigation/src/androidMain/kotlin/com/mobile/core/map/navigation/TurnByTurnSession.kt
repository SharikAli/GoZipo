package com.mobile.core.map.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeOfArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechVolume
import com.mobile.core.domain.location.GeoPoint
import java.util.Date
import java.util.Locale

/**
 * One guidance session to a single destination on the Mapbox Navigation SDK, wired like
 * Mapbox's turn-by-turn example: navigation camera, maneuver banner, trip progress, route line
 * and arrows, voice. Registered with `MapboxNavigationApp` as an observer, so it runs while the
 * screen is attached. Compose shows [mapView], [maneuverView] and [tripProgressView] and reads
 * the observable state below for its own buttons.
 *
 * @param simulate drive a replayed location along the route instead of the device's GPS
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class TurnByTurnSession(
    private val context: Context,
    private val destination: GeoPoint,
    private val origin: GeoPoint?,
    private val simulate: Boolean,
    private val onArrival: () -> Unit,
) : MapboxNavigationObserver {

    enum class RouteState { FINDING, READY, FAILED }

    var routeState by mutableStateOf(RouteState.FINDING)
        private set
    var isMuted by mutableStateOf(false)
        private set
    var isFollowing by mutableStateOf(true)
        private set

    private val density = Resources.getSystem().displayMetrics.density
    private val language = Locale.getDefault().language

    val mapView = MapView(context, MapInitOptions(context)).apply {
        compass.enabled = false
        scalebar.enabled = false
    }
    val maneuverView = MapboxManeuverView(context).apply { visibility = View.INVISIBLE }
    val tripProgressView = MapboxTripProgressView(context)

    private val navigationLocationProvider = NavigationLocationProvider()
    private val viewportDataSource = MapboxNavigationViewportDataSource(mapView.mapboxMap).apply {
        // Room for the maneuver banner on top and the trip card at the bottom
        followingPadding =
            EdgeInsets(180.0 * density, 40.0 * density, 150.0 * density, 40.0 * density)
        overviewPadding =
            EdgeInsets(160.0 * density, 40.0 * density, 160.0 * density, 40.0 * density)
    }
    private val navigationCamera =
        NavigationCamera(mapView.mapboxMap, mapView.camera, viewportDataSource)

    private val distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()
    private val maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))
    private val tripProgressApi = MapboxTripProgressApi(
        TripProgressUpdateFormatter.Builder(context)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(context))
            .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
            // Arrival time on the trip card (replaces the deprecated estimatedTimeToArrivalFormatter)
            .estimatedTimeOfArrivalFormatter(
                EstimatedTimeOfArrivalFormatter(
                    context,
                    TimeFormat.NONE_SPECIFIED
                )
            )
            .build()
    )
    private val routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
    private val routeLineView = MapboxRouteLineView(
        MapboxRouteLineViewOptions.Builder(context)
            .routeLineBelowLayerId("road-label-navigation")
            .build()
    )
    private val routeArrowApi = MapboxRouteArrowApi()
    private val routeArrowView = MapboxRouteArrowView(RouteArrowOptions.Builder(context).build())

    private val speechApi = MapboxSpeechApi(context, language)

    // Created up front: the on-device TTS engine takes a moment to start
    private val voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(context, language)

    private var mapboxNavigation: MapboxNavigation? = null
    private var replayProgressObserver: ReplayProgressObserver? = null
    private var hasRequestedRoute = false
    private var hasArrived = false
    private var isDark: Boolean? = null

    init {
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(
                navigationCamera
            )
        )
        navigationCamera.registerNavigationCameraStateChangeObserver { state ->
            isFollowing = state == NavigationCameraState.FOLLOWING ||
                    state == NavigationCameraState.TRANSITION_TO_FOLLOWING
        }
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.from(
                    com.mapbox.navigation.ui.components.R.drawable.mapbox_navigation_puck_icon
                )
            )
            puckBearingEnabled = true
            enabled = true
        }
    }

    fun setDarkTheme(dark: Boolean) {
        if (dark == isDark) return
        isDark = dark
        mapView.mapboxMap.loadStyle(
            if (dark) NavigationStyles.NAVIGATION_NIGHT_STYLE else NavigationStyles.NAVIGATION_DAY_STYLE
        ) { style ->
            // The route line layers must exist before the arrows
            routeLineView.initializeLayers(style)
            mapboxNavigation?.getNavigationRoutes()?.takeIf { it.isNotEmpty() }?.let { routes ->
                routeLineApi.setNavigationRoutes(routes) {
                    routeLineView.renderRouteDrawData(
                        style,
                        it
                    )
                }
            }
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        voiceInstructionsPlayer.volume(SpeechVolume(if (isMuted) 0f else 1f))
    }

    fun showOverview() = navigationCamera.requestNavigationCameraToOverview()

    fun recenter() = navigationCamera.requestNavigationCameraToFollowing()

    fun retry() {
        hasRequestedRoute = false
        routeState = RouteState.FINDING
        val start =
            navigationLocationProvider.lastLocation?.let { GeoPoint(it.latitude, it.longitude) }
                ?: origin
        start?.let { requestRoute(it, navigationLocationProvider.lastLocation?.bearing) }
    }

    // MapboxNavigationObserver

    @SuppressLint("MissingPermission") // Checked before starting the live session
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)

        if (simulate) {
            val replayer = mapboxNavigation.mapboxReplayer
            ReplayProgressObserver(replayer).also {
                replayProgressObserver = it
                mapboxNavigation.registerRouteProgressObserver(it)
            }
            mapboxNavigation.startReplayTripSession()
            origin?.let { start ->
                replayer.pushEvents(
                    listOf(
                        ReplayRouteMapper.mapToUpdateLocation(
                            Date().time.toDouble(),
                            start.toPoint()
                        )
                    )
                )
                replayer.playFirstLocation()
                replayer.play()
            }
        } else if (hasLocationPermission()) {
            mapboxNavigation.startTripSession()
        }

        // Route from the caller's last known position right away; the SDK reroutes if it's off
        origin?.let { requestRoute(it, bearing = null) }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        replayProgressObserver?.let { mapboxNavigation.unregisterRouteProgressObserver(it) }
        mapboxNavigation.setNavigationRoutes(emptyList())
        if (simulate) mapboxNavigation.mapboxReplayer.finish()
        mapboxNavigation.stopTripSession()
        this.mapboxNavigation = null
    }

    /** Releases the SDK helpers; the map view itself follows its lifecycle owner. */
    fun destroy() {
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    // Route

    private fun requestRoute(start: GeoPoint, bearing: Double?) {
        val navigation = mapboxNavigation ?: return
        if (hasRequestedRoute) return
        hasRequestedRoute = true
        routeState = RouteState.FINDING

        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .coordinatesList(listOf(start.toPoint(), destination.toPoint()))
            .apply {
                // Start in the direction the driver is already moving
                bearing?.let {
                    bearingsList(
                        listOf(
                            Bearing.builder().angle(it).degrees(BEARING_TOLERANCE).build(), null
                        )
                    )
                }
            }
            .layersList(listOf(navigation.getZLevel(), null))
            .build()

        navigation.requestRoutes(options, object : NavigationRouterCallback {
            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                navigation.setNavigationRoutes(routes)
                routeState = RouteState.READY
                navigationCamera.requestNavigationCameraToFollowing()
                if (simulate) replayRoute(navigation, routes.first())
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                hasRequestedRoute = false
                routeState = RouteState.FAILED
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                hasRequestedRoute = false
            }
        })
    }

    private fun replayRoute(navigation: MapboxNavigation, route: NavigationRoute) {
        val replayer = navigation.mapboxReplayer
        val events = ReplayRouteMapper().mapDirectionsRouteGeometry(route.directionsRoute)
        replayer.stop()
        replayer.clearEvents()
        replayer.pushEvents(events)
        replayer.seekTo(events.first())
        replayer.play()
    }

    // Observers

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) = Unit

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val location = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(location, locationMatcherResult.keyPoints)
            viewportDataSource.onLocationChanged(location)
            viewportDataSource.evaluate()
            // No origin from the caller: route from the first fix
            if (!hasRequestedRoute && routeState != RouteState.FAILED) {
                requestRoute(GeoPoint(location.latitude, location.longitude), location.bearing)
            }
        }
    }

    private val routesObserver = RoutesObserver { result ->
        val style = mapView.mapboxMap.style
        if (result.navigationRoutes.isNotEmpty()) {
            routeLineApi.setNavigationRoutes(result.navigationRoutes) { value ->
                style?.let { routeLineView.renderRouteDrawData(it, value) }
            }
            viewportDataSource.onRouteChanged(result.navigationRoutes.first())
        } else {
            style?.let {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        it,
                        value
                    )
                }
                routeArrowView.render(it, routeArrowApi.clearArrows())
            }
            viewportDataSource.clearRouteData()
        }
        viewportDataSource.evaluate()
    }

    private val routeProgressObserver = RouteProgressObserver { progress ->
        viewportDataSource.onRouteProgressChanged(progress)
        viewportDataSource.evaluate()

        mapView.mapboxMap.style?.let { style ->
            routeArrowView.renderManeuverUpdate(
                style,
                routeArrowApi.addUpcomingManeuverArrow(progress)
            )
        }
        val maneuvers = maneuverApi.getManeuvers(progress)
        if (maneuvers.isValue) maneuverView.visibility = View.VISIBLE
        maneuverView.renderManeuvers(maneuvers)
        tripProgressView.render(tripProgressApi.getTripProgress(progress))
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { instructions ->
        speechApi.generate(instructions) { result ->
            result.fold(
                { error -> voiceInstructionsPlayer.play(error.fallback) { speechApi.clean(it) } },
                { value -> voiceInstructionsPlayer.play(value.announcement) { speechApi.clean(it) } }
            )
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) = Unit

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) = Unit

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            if (hasArrived) return
            hasArrived = true
            onArrival()
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun GeoPoint.toPoint(): Point = Point.fromLngLat(longitude, latitude)

    private companion object {
        const val BEARING_TOLERANCE = 45.0
    }
}
