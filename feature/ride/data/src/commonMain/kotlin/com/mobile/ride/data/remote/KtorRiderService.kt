package com.mobile.ride.data.remote

import com.mobile.core.data.network.get
import com.mobile.core.data.network.post
import com.mobile.core.data.network.put
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.core.domain.realtime.RealtimeMessage
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import com.mobile.ride.data.RideJson
import com.mobile.ride.data.dto.EarningsDto
import com.mobile.ride.data.dto.EmptyRequestDto
import com.mobile.ride.data.dto.LocationDto
import com.mobile.ride.data.dto.OutgoingWebSocketType
import com.mobile.ride.data.dto.RiderStatusDto
import com.mobile.ride.data.dto.VehicleDto
import com.mobile.ride.data.mappers.toDto
import com.mobile.ride.data.mappers.toRiderEarnings
import com.mobile.ride.data.mappers.toRiderStatus
import com.mobile.ride.data.mappers.toVehicle
import com.mobile.ride.domain.RiderEarnings
import com.mobile.ride.domain.RiderService
import com.mobile.ride.domain.RiderStatus
import com.mobile.ride.domain.Vehicle
import io.ktor.client.HttpClient
import kotlinx.datetime.TimeZone

class KtorRiderService(
    private val httpClient: HttpClient,
    private val realtimeClient: RealtimeClient,
) : RiderService {

    override suspend fun getStatus(): Result<RiderStatus, DataError.Remote> {
        return httpClient.get<RiderStatusDto>(route = "/riders/me/status")
            .map { it.toRiderStatus() }
    }

    override suspend fun getEarnings(): Result<RiderEarnings, DataError.Remote> {
        return httpClient.get<EarningsDto>(
            route = "/riders/me/earnings",
            queryParams = mapOf("zone" to TimeZone.currentSystemDefault().id)
        ).map { it.toRiderEarnings() }
    }

    override suspend fun saveVehicle(vehicle: Vehicle): Result<Vehicle, DataError.Remote> {
        return httpClient.put<VehicleDto, VehicleDto>(
            route = "/riders/me/vehicle",
            body = vehicle.toDto()
        ).map { it.toVehicle() }
    }

    override suspend fun goOnline(location: GeoPoint): Result<RiderStatus, DataError.Remote> {
        return httpClient.post<LocationDto, RiderStatusDto>(
            route = "/riders/me/online",
            body = location.toDto()
        ).map { it.toRiderStatus() }
    }

    override suspend fun goOffline(): EmptyResult<DataError.Remote> {
        return httpClient.post<EmptyRequestDto, Unit>(
            route = "/riders/me/offline",
            body = EmptyRequestDto()
        )
    }

    override suspend fun shareLocation(location: GeoPoint): EmptyResult<DataError.Remote> {
        if (realtimeClient.connectionState.value == ConnectionState.CONNECTED) {
            val message = RealtimeMessage(
                type = OutgoingWebSocketType.LOCATION_UPDATE.name,
                payload = RideJson.encodeToString(location.toDto())
            )
            if (realtimeClient.send(message) is Result.Success) {
                return Result.Success(Unit)
            }
        }
        return httpClient.put<LocationDto, Unit>(
            route = "/riders/me/location",
            body = location.toDto()
        )
    }
}
