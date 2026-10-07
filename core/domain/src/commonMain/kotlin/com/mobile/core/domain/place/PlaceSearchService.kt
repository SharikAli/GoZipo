package com.mobile.core.domain.place

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result

/** Place search and reverse geocoding, implemented by the map provider module. */
interface PlaceSearchService {
    /**
     * Autocomplete search for pickup and drop-off.
     *
     * @param near biases results towards this point (usually the user's location)
     */
    suspend fun search(query: String, near: GeoPoint?): Result<List<Place>, DataError.Remote>

    /** The address at [point], or null when nothing is there (e.g. at sea). */
    suspend fun reverseGeocode(point: GeoPoint): Result<Place?, DataError.Remote>
}
