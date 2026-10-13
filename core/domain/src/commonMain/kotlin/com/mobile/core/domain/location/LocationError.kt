package com.mobile.core.domain.location

import com.mobile.core.domain.util.Error

enum class LocationError : Error {
    /** The app has no location permission; ask for it through the presentation layer. */
    PERMISSION_DENIED,

    /** Location services are turned off in the device settings. */
    SERVICES_DISABLED,

    /** No fix could be obtained right now. */
    UNAVAILABLE,

    /** This platform has no location implementation. */
    UNSUPPORTED
}
