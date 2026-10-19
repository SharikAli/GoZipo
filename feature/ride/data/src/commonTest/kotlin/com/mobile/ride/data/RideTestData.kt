package com.mobile.ride.data

internal const val RIDE_JSON = """
{
  "id": "6f1c2a52-0000-4000-8000-000000000001",
  "status": "SEARCHING",
  "vehicleType": "MINI",
  "pickup": { "location": { "latitude": 24.9425, "longitude": 67.114 }, "address": "Dr Abdus Salam Rd" },
  "dropoff": { "location": { "latitude": 24.93, "longitude": 67.07 }, "address": "Aisha Manzil" },
  "distanceMeters": 9800,
  "durationSeconds": 1320,
  "recommendedFare": 540,
  "offeredFare": 560,
  "agreedFare": null,
  "autoAccept": false,
  "paymentMethod": "CASH",
  "searchRound": 1,
  "searchExpiresAt": "2026-10-07T11:01:00.123456Z",
  "customer": { "id": "c-1", "username": "sharik" },
  "rider": null,
  "createdAt": "2026-10-07T11:00:00.123456Z",
  "acceptedAt": null,
  "arrivedAt": null,
  "startedAt": null,
  "completedAt": null,
  "cancelledAt": null,
  "cancelledBy": null,
  "cancelReason": null
}
"""

internal const val OFFER_JSON = """
{
  "id": "offer-1",
  "rideId": "6f1c2a52-0000-4000-8000-000000000001",
  "amount": 600,
  "kind": "COUNTER",
  "status": "PENDING",
  "searchRound": 1,
  "rider": {
    "id": "r-1",
    "username": "muhammad",
    "vehicle": { "vehicleType": "MINI", "make": "Suzuki", "model": "Mehran", "color": "Blue", "plateNumber": "APV-809" }
  },
  "pickupDistanceMeters": 850,
  "createdAt": "2026-10-07T11:00:20Z"
}
"""

internal const val OPEN_RIDE_JSON = """
{
  "rideId": "6f1c2a52-0000-4000-8000-000000000001",
  "vehicleType": "MINI",
  "pickup": { "location": { "latitude": 24.9425, "longitude": 67.114 }, "address": "Dr Abdus Salam Rd" },
  "dropoff": { "location": { "latitude": 24.93, "longitude": 67.07 }, "address": "Aisha Manzil" },
  "distanceMeters": 9800,
  "durationSeconds": 1320,
  "offeredFare": 560,
  "maximumCounterFare": 840,
  "fareStep": 10,
  "pickupDistanceMeters": 850,
  "searchRound": 2,
  "searchExpiresAt": "2026-10-07T11:01:00Z",
  "customer": { "id": "c-1", "username": "sharik" },
  "myOffer": null
}
"""
