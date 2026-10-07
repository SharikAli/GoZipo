package com.mobile.core.domain.route

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result

/** Driving directions, implemented by the map provider module. */
interface RouteService {
    suspend fun getRoute(from: GeoPoint, to: GeoPoint): Result<Route, DataError.Remote>
}
