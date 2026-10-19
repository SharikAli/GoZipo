package com.mobile.core.map.mapbox.ios

import platform.UIKit.UIView

/*
 * Bridge to the Mapbox Maps iOS SDK, which is Swift-only and can't be called from Kotlin.
 * Swift implements these protocols (apps/ios-common/Mapbox/GoZipoMapboxBridge.swift) and
 * hands the factory to Kotlin at startup (`doInitKoin(mapboxFactory:)`). Only plain types
 * cross the bridge, so the Swift side never sees Compose or domain classes.
 */

/** Creates one native map per `GoZipoMap` on screen. */
interface GoZipoMapboxFactory {
    fun createMap(
        accessToken: String,
        camera: GoZipoMapboxCamera,
        listener: GoZipoMapboxListener
    ): GoZipoMapboxMap
}

/** One native `MapView`. All calls come from the main thread. */
interface GoZipoMapboxMap {
    fun view(): UIView

    fun setCamera(camera: GoZipoMapboxCamera, animated: Boolean)

    /** Frames [coordinates] with [padding] on top of the content insets, zooming no closer than [maxZoom]. */
    fun fitBounds(
        coordinates: List<GoZipoMapboxCoordinate>,
        padding: GoZipoMapboxInsets,
        maxZoom: Double,
        animated: Boolean
    )

    /** Area covered by app UI (in points). The camera centers inside the rest; ornaments stay visible. */
    fun setContentInsets(insets: GoZipoMapboxInsets)

    fun setDarkTheme(dark: Boolean)

    fun setShowUserLocation(show: Boolean)

    /** Replaces all lines; drawn in list order. */
    fun setPolylines(polylines: List<GoZipoMapboxPolyline>)

    /** Where [coordinate] is on the map view, in points from its top-left corner. */
    fun screenPoint(coordinate: GoZipoMapboxCoordinate): GoZipoMapboxPoint

    fun destroy()
}

/** Calls from Swift back into Kotlin, on the main thread. */
interface GoZipoMapboxListener {
    fun onCameraChanged(camera: GoZipoMapboxCamera, isMoving: Boolean)
    fun onMapTap(coordinate: GoZipoMapboxCoordinate)
}

data class GoZipoMapboxCoordinate(
    val latitude: Double,
    val longitude: Double
)

data class GoZipoMapboxCamera(
    val center: GoZipoMapboxCoordinate,
    val zoom: Double,
    val bearing: Double
)

/** In points. */
data class GoZipoMapboxInsets(
    val top: Double,
    val left: Double,
    val bottom: Double,
    val right: Double
)

data class GoZipoMapboxPoint(
    val x: Double,
    val y: Double
)

/**
 * @param color ARGB, as `androidx.compose.ui.graphics.Color.toArgb()`
 * @param casingColor ARGB outline under the line, or null for none
 */
data class GoZipoMapboxPolyline(
    val id: String,
    val coordinates: List<GoZipoMapboxCoordinate>,
    val color: Int,
    val width: Double,
    val casingColor: Int?
)
