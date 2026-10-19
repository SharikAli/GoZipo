package com.mobile.core.data.realtime

/** The server closed the socket because it rejected [accessToken] (close code 1008). */
class RealtimeAuthenticationException(val accessToken: String) :
    Exception("Realtime socket rejected the access token")

/** The server closed the socket for any other reason (restart, idle timeout). */
class RealtimeConnectionClosedException(reason: String?) :
    Exception("Realtime socket closed by the server: $reason")
