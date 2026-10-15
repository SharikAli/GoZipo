package com.mobile.booking.presentation.util

import com.mobile.core.domain.place.Place

/** "Dolmen Mall Clifton, Block 4, Clifton"; blank for an unnamed dropped pin. */
fun Place.label(): String = listOf(name, address).filter { it.isNotBlank() }.joinToString(", ")
