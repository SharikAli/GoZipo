package com.mobile.core.map.navigation.ios

import platform.UIKit.UIViewController

// The Mapbox Navigation SDK for iOS is Swift-only, so Swift implements these
// (apps/rider/iosApp/iosApp/Navigation/GoZipoNavigationBridge.swift) and the rider app passes
// the factory to Koin at startup. Changing a member here means updating the Swift side too.

/** Creates guidance sessions. One per app; the SDK allows a single navigation core. */
interface GoZipoNavigationFactory {
    /**
     * @param origin where to route from before the SDK has its own fix
     * @param simulate drive a simulated location along the route (desk testing)
     */
    fun createNavigation(
        accessToken: String,
        destination: GoZipoNavigationCoordinate,
        origin: GoZipoNavigationCoordinate?,
        destinationName: String?,
        darkTheme: Boolean,
        simulate: Boolean,
        listener: GoZipoNavigationListener
    ): GoZipoNavigationSession
}

/** One guidance session; [viewController] shows routing progress, then the SDK's navigation UI. */
interface GoZipoNavigationSession {
    fun viewController(): UIViewController

    /** Ends guidance and releases the session. */
    fun destroy()
}

interface GoZipoNavigationListener {
    /** Reached the destination. */
    fun onArrival()

    /** The driver ended guidance with the SDK's own cancel button. */
    fun onClose()
}

data class GoZipoNavigationCoordinate(
    val latitude: Double,
    val longitude: Double
)
