package com.mobile.core.map.api

/**
 * Marks members meant for [MapProvider] implementations only (they feed camera
 * changes back into [MapCameraState]). Features never need them.
 */
@RequiresOptIn(
    message = "Only for MapProvider implementations.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MapProviderApi
