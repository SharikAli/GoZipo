@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.mobile.core.data.realtime

import com.mobile.core.data.network.platformWebSocketBaseUrl
import com.mobile.core.data.network.refreshSession
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.core.domain.realtime.RealtimeMessage
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds

class KtorRealtimeClient(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val connectionErrorHandler: ConnectionErrorHandler,
    private val connectionRetryHandler: ConnectionRetryHandler,
    appLifecycleObserver: AppLifecycleObserver,
    connectivityObserver: ConnectivityObserver,
    applicationScope: CoroutineScope,
    private val logger: GoZipoLogger,
    private val socketUrl: String = "${platformWebSocketBaseUrl()}$RIDES_PATH",
) : RealtimeClient {

    private companion object {
        const val RIDES_PATH = "/rides"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    @Volatile
    private var currentSession: WebSocketSession? = null

    private val isSignedIn = sessionStorage
        .observeAuthInfo()
        .map { it != null }
        .distinctUntilChanged()

    private val isOnline = connectivityObserver
        .isConnected
        .debounce(1.seconds)
        .distinctUntilChanged()

    private val isInForeground = appLifecycleObserver
        .isInForeground
        .distinctUntilChanged()
        .onEach { inForeground ->
            if (inForeground) {
                connectionRetryHandler.resetDelay()
            }
        }

    override val messages: Flow<RealtimeMessage> = combine(
        isSignedIn,
        isInForeground,
        isOnline
    ) { signedIn, inForeground, online ->
        // null: nothing stops us from connecting
        when {
            !signedIn -> ConnectionState.DISCONNECTED
            !inForeground -> ConnectionState.DISCONNECTED
            !online -> ConnectionState.ERROR_NETWORK
            else -> null
        }
    }.distinctUntilChanged().flatMapLatest { blockedState ->
        if (blockedState != null) {
            logger.info("Realtime socket idle: $blockedState")
            _connectionState.value = blockedState
            emptyFlow()
        } else {
            socketFlow()
                // Map platform exceptions (iOS wraps network errors in cancellations)
                .catch { e -> throw connectionErrorHandler.transformException(e) }
                .retryWhen { cause, _ ->
                    val shouldRetry = connectionRetryHandler.shouldRetry(cause)
                    logger.warn("Realtime socket failed (${cause::class.simpleName}), retry: $shouldRetry")
                    if (!shouldRetry) {
                        return@retryWhen false
                    }
                    _connectionState.value = ConnectionState.CONNECTING
                    if (cause is RealtimeAuthenticationException) {
                        // No session left means the user was signed out: stop here
                        httpClient.refreshSession(sessionStorage, cause.accessToken)
                            ?: return@retryWhen false
                    }
                    connectionRetryHandler.applyRetryDelay()
                    true
                }
                .catch { e ->
                    logger.error("Realtime socket stopped", e)
                    _connectionState.value = connectionErrorHandler.getConnectionStateForError(e)
                }
        }
    }.shareIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(5_000)
    )

    private fun socketFlow(): Flow<RealtimeMessage> = flow {
        val accessToken = sessionStorage.observeAuthInfo().firstOrNull()?.accessToken
            ?: return@flow

        _connectionState.value = ConnectionState.CONNECTING
        val session = httpClient.webSocketSession(urlString = socketUrl) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        currentSession = session

        try {
            _connectionState.value = ConnectionState.CONNECTED
            connectionRetryHandler.onConnected()
            logger.info("Realtime socket connected")

            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    parse(frame.readText())?.let { emit(it) }
                }
            }

            // incoming ends when the server closes the socket
            val reason = session.closeReason.await()
            if (reason?.code == CloseReason.Codes.VIOLATED_POLICY.code) {
                throw RealtimeAuthenticationException(accessToken)
            }
            throw RealtimeConnectionClosedException(reason?.toString())
        } finally {
            currentSession = null
            _connectionState.value = ConnectionState.DISCONNECTED
            withContext(NonCancellable) {
                session.close()
            }
        }
    }

    private fun parse(text: String): RealtimeMessage? {
        return try {
            val dto = json.decodeFromString<RealtimeMessageDto>(text)
            // Never log payloads: they carry locations and personal details
            logger.debug("Realtime message received: ${dto.type}")
            RealtimeMessage(type = dto.type, payload = dto.payload)
        } catch (e: SerializationException) {
            logger.warn("Ignoring malformed realtime message")
            null
        } catch (e: IllegalArgumentException) {
            logger.warn("Ignoring malformed realtime message")
            null
        }
    }

    override suspend fun send(message: RealtimeMessage): EmptyResult<DataError.Connection> {
        val session = currentSession
        if (session == null || connectionState.value != ConnectionState.CONNECTED) {
            return Result.Failure(DataError.Connection.NOT_CONNECTED)
        }

        return try {
            val dto = RealtimeMessageDto(type = message.type, payload = message.payload)
            session.send(Frame.Text(json.encodeToString(dto)))
            Result.Success(Unit)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.error("Unable to send realtime message ${message.type}", e)
            Result.Failure(DataError.Connection.MESSAGE_SEND_FAILED)
        }
    }
}
