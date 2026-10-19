package com.mobile.core.map.mapbox.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Search Box API `/forward` and `/reverse` response (GeoJSON FeatureCollection). */
@Serializable
data class SearchBoxResponseDto(
    val features: List<SearchBoxFeatureDto> = emptyList()
)

@Serializable
data class SearchBoxFeatureDto(
    val properties: SearchBoxPropertiesDto
)

@Serializable
data class SearchBoxPropertiesDto(
    @SerialName("mapbox_id")
    val mapboxId: String,
    val name: String,
    @SerialName("full_address")
    val fullAddress: String? = null,
    @SerialName("place_formatted")
    val placeFormatted: String? = null,
    val coordinates: CoordinatesDto
)

@Serializable
data class CoordinatesDto(
    val latitude: Double,
    val longitude: Double
)
