package com.mobile.driving.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.driving.presentation.driving.DrivingPreviewData
import com.mobile.ride.domain.EarningsPeriod
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.RiderEarnings
import com.mobile.ride.presentation.util.fareText
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.back
import gozipo.feature.driving.presentation.generated.resources.earnings_today
import gozipo.feature.driving.presentation.generated.resources.earnings_trips
import gozipo.feature.driving.presentation.generated.resources.earnings_week
import gozipo.feature.driving.presentation.generated.resources.history_empty
import gozipo.feature.driving.presentation.generated.resources.history_failed
import gozipo.feature.driving.presentation.generated.resources.history_status_cancelled
import gozipo.feature.driving.presentation.generated.resources.history_status_completed
import gozipo.feature.driving.presentation.generated.resources.history_title
import gozipo.feature.driving.presentation.generated.resources.retry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/** Rows from the end at which the next page starts loading. */
private const val LOAD_MORE_THRESHOLD = 5

private val RideTimeFormat = LocalDateTime.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    chars(", ")
    hour()
    char(':')
    minute()
}

@Composable
fun RideHistoryRoot(
    onBackClick: () -> Unit,
    viewModel: RideHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RideHistoryScreen(
        state = state,
        onAction = { action ->
            when (action) {
                RideHistoryAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun RideHistoryScreen(
    state: RideHistoryState,
    onAction: (RideHistoryAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onAction(RideHistoryAction.OnLoadMore) }
    }

    GoZipoSnackbarScaffold {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.padding(
                    horizontal = spacing.screenHorizontal,
                    vertical = spacing.sm
                )
            ) {
                GoZipoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    onClick = { onAction(RideHistoryAction.OnBackClick) }
                )
                Text(
                    text = stringResource(Res.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = extended.textPrimary
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                state.earnings?.let { earnings ->
                    item(key = "earnings") { EarningsRow(earnings) }
                }
                items(state.rides, key = { it.id }) { ride ->
                    HistoryRow(ride)
                    GoZipoDivider(modifier = Modifier.padding(horizontal = spacing.screenHorizontal))
                }
                item(key = "footer") {
                    HistoryFooter(state = state, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun EarningsRow(earnings: RiderEarnings) {
    val spacing = GoZipoTheme.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md)
    ) {
        EarningsCard(
            title = stringResource(Res.string.earnings_today),
            period = earnings.today,
            modifier = Modifier.weight(1f)
        )
        EarningsCard(
            title = stringResource(Res.string.earnings_week),
            period = earnings.last7Days,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EarningsCard(
    title: String,
    period: EarningsPeriod,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        shape = MaterialTheme.shapes.large,
        color = extended.surfaceHigher,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.padding(spacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
            Text(
                text = fareText(period.amount),
                style = MaterialTheme.typography.headlineSmall,
                color = extended.cash
            )
            Text(
                text = stringResource(Res.string.earnings_trips, period.trips),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
        }
    }
}

@Composable
private fun HistoryRow(ride: Ride) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val completed = ride.status == RideStatus.COMPLETED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = (ride.completedAt ?: ride.cancelledAt ?: ride.createdAt).toRideTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
            Text(
                text = ride.dropoff.address,
                style = MaterialTheme.typography.titleMedium,
                color = extended.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ride.pickup.address,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (completed) fareText(ride.agreedFare ?: ride.offeredFare) else "—",
                style = MaterialTheme.typography.titleMedium,
                color = if (completed) extended.textPrimary else extended.textTertiary
            )
            Text(
                text = stringResource(
                    if (completed) Res.string.history_status_completed else Res.string.history_status_cancelled
                ),
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary
            )
        }
    }
}

@Composable
private fun HistoryFooter(
    state: RideHistoryState,
    onAction: (RideHistoryAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.xl)
    ) {
        when {
            state.isLoading || state.isLoadingMore -> CircularProgressIndicator(color = extended.textPrimary)
            state.errorText != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Text(
                    text = stringResource(Res.string.history_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = extended.textSecondary
                )
                GoZipoButton(
                    text = stringResource(Res.string.retry),
                    onClick = { onAction(RideHistoryAction.OnRetryClick) },
                    style = GoZipoButtonStyle.SECONDARY
                )
            }

            state.rides.isEmpty() -> Text(
                text = stringResource(Res.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = extended.textSecondary
            )
        }
    }
}

private fun Instant.toRideTime(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).format(RideTimeFormat)

@Preview
@Composable
private fun RideHistoryPreview() {
    GoZipoTheme(darkTheme = true) {
        RideHistoryScreen(
            state = RideHistoryState(
                earnings = RiderEarnings(EarningsPeriod(3, 1_620), EarningsPeriod(12, 6_480)),
                rides = listOfNotNull(
                    DrivingPreviewData.completed.finishedRide,
                    DrivingPreviewData.cancelledByCustomer.finishedRide?.copy(id = "ride-2"),
                ),
                isLoading = false,
                endReached = true,
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun RideHistoryEmptyPreview() {
    GoZipoTheme(darkTheme = true) {
        RideHistoryScreen(
            state = RideHistoryState(
                earnings = RiderEarnings(EarningsPeriod(0, 0), EarningsPeriod(0, 0)),
                isLoading = false,
                endReached = true,
            ),
            onAction = {}
        )
    }
}
