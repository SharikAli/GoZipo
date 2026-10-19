package com.mobile.ride.data.remote

import com.mobile.core.data.network.delete
import com.mobile.core.data.network.get
import com.mobile.core.data.network.post
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import com.mobile.ride.data.RideJson
import com.mobile.ride.data.dto.CancelRideRequestDto
import com.mobile.ride.data.dto.CreateRideRequestDto
import com.mobile.ride.data.dto.EmptyRequestDto
import com.mobile.ride.data.dto.FareEstimateDto
import com.mobile.ride.data.dto.FareEstimateRequestDto
import com.mobile.ride.data.dto.OfferDto
import com.mobile.ride.data.dto.OpenRideDto
import com.mobile.ride.data.dto.RideDto
import com.mobile.ride.data.dto.SubmitOfferRequestDto
import com.mobile.ride.data.dto.UpdateFareRequestDto
import com.mobile.ride.data.mappers.toDto
import com.mobile.ride.data.mappers.toFareEstimate
import com.mobile.ride.data.mappers.toOpenRide
import com.mobile.ride.data.mappers.toRide
import com.mobile.ride.data.mappers.toRideOffer
import com.mobile.ride.domain.FareEstimate
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideRequest
import com.mobile.ride.domain.RideService
import io.ktor.client.HttpClient
import kotlinx.serialization.SerializationException

class KtorRideService(
    private val httpClient: HttpClient
) : RideService {

    override suspend fun estimateFares(
        pickup: GeoPoint,
        dropoff: GeoPoint
    ): Result<List<FareEstimate>, DataError.Remote> {
        return httpClient.post<FareEstimateRequestDto, List<FareEstimateDto>>(
            route = "/rides/estimate",
            body = FareEstimateRequestDto(
                pickup = pickup.toDto(),
                dropoff = dropoff.toDto()
            )
        ).map { estimates -> estimates.map { it.toFareEstimate() } }
    }

    override suspend fun requestRide(request: RideRequest): Result<Ride, DataError.Remote> {
        return httpClient.post<CreateRideRequestDto, RideDto>(
            route = "/rides",
            body = CreateRideRequestDto(
                pickup = request.pickup.toDto(),
                dropoff = request.dropoff.toDto(),
                vehicleType = request.vehicleType,
                offeredFare = request.offeredFare,
                autoAccept = request.autoAccept
            )
        ).map { it.toRide() }
    }

    override suspend fun updateFare(
        rideId: String,
        offeredFare: Int
    ): Result<Ride, DataError.Remote> {
        return httpClient.post<UpdateFareRequestDto, RideDto>(
            route = "/rides/$rideId/fare",
            body = UpdateFareRequestDto(offeredFare)
        ).map { it.toRide() }
    }

    override suspend fun getPendingOffers(rideId: String): Result<List<RideOffer>, DataError.Remote> {
        return httpClient.get<List<OfferDto>>(
            route = "/rides/$rideId/offers"
        ).map { offers -> offers.map { it.toRideOffer() } }
    }

    override suspend fun acceptOffer(
        rideId: String,
        offerId: String
    ): Result<Ride, DataError.Remote> {
        return httpClient.post<EmptyRequestDto, RideDto>(
            route = "/rides/$rideId/offers/$offerId/accept",
            body = EmptyRequestDto()
        ).map { it.toRide() }
    }

    override suspend fun declineOffer(
        rideId: String,
        offerId: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.post<EmptyRequestDto, Unit>(
            route = "/rides/$rideId/offers/$offerId/decline",
            body = EmptyRequestDto()
        )
    }

    override suspend fun getOpenRides(): Result<List<OpenRide>, DataError.Remote> {
        return httpClient.get<List<OpenRideDto>>(
            route = "/rides/open"
        ).map { rides -> rides.map { it.toOpenRide() } }
    }

    override suspend fun submitOffer(
        rideId: String,
        amount: Int?
    ): Result<RideOffer, DataError.Remote> {
        return httpClient.post<SubmitOfferRequestDto, OfferDto>(
            route = "/rides/$rideId/offers",
            body = SubmitOfferRequestDto(amount)
        ).map { it.toRideOffer() }
    }

    override suspend fun withdrawOffer(
        rideId: String,
        offerId: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.delete<Unit>(route = "/rides/$rideId/offers/$offerId")
    }

    override suspend fun markArrived(rideId: String): Result<Ride, DataError.Remote> =
        tripStep(rideId, "arrived")

    override suspend fun startTrip(rideId: String): Result<Ride, DataError.Remote> =
        tripStep(rideId, "start")

    override suspend fun completeTrip(rideId: String): Result<Ride, DataError.Remote> =
        tripStep(rideId, "complete")

    private suspend fun tripStep(rideId: String, step: String): Result<Ride, DataError.Remote> {
        return httpClient.post<EmptyRequestDto, RideDto>(
            route = "/rides/$rideId/$step",
            body = EmptyRequestDto()
        ).map { it.toRide() }
    }

    override suspend fun cancelRide(
        rideId: String,
        reason: String?
    ): Result<Ride, DataError.Remote> {
        return httpClient.post<CancelRideRequestDto, RideDto>(
            route = "/rides/$rideId/cancel",
            body = CancelRideRequestDto(reason)
        ).map { it.toRide() }
    }

    override suspend fun getActiveRide(): Result<Ride?, DataError.Remote> {
        // 204 No Content when there is none, so read the raw body
        return when (val result = httpClient.get<String>(route = "/rides/active")) {
            is Result.Failure -> result
            is Result.Success -> {
                if (result.data.isBlank()) {
                    Result.Success(null)
                } else {
                    try {
                        Result.Success(RideJson.decodeFromString<RideDto>(result.data).toRide())
                    } catch (_: SerializationException) {
                        Result.Failure(DataError.Remote.SERIALIZATION)
                    } catch (_: IllegalArgumentException) {
                        Result.Failure(DataError.Remote.SERIALIZATION)
                    }
                }
            }
        }
    }

    override suspend fun getRide(rideId: String): Result<Ride, DataError.Remote> {
        return httpClient.get<RideDto>(route = "/rides/$rideId").map { it.toRide() }
    }

    override suspend fun getHistory(page: Int): Result<List<Ride>, DataError.Remote> {
        return httpClient.get<List<RideDto>>(
            route = "/rides/history",
            queryParams = mapOf("page" to page)
        ).map { rides -> rides.map { it.toRide() } }
    }
}
