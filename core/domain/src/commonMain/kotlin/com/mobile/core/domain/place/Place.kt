package com.mobile.core.domain.place

import com.mobile.core.domain.location.GeoPoint

/**
 * A searchable or reverse-geocoded location.
 *
 * @param name short label ("Dolmen Mall Clifton")
 * @param address the rest of the address ("Block 4, Clifton, Karachi")
 */
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val point: GeoPoint
)
