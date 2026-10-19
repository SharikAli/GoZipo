package com.mobile.booking.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.PlaceSearch
import com.mobile.booking.presentation.booking.SearchTarget
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.components.ride.ListActionRow
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.domain.place.Place
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.back
import gozipo.feature.booking.presentation.generated.resources.clear
import gozipo.feature.booking.presentation.generated.resources.search_choose_on_map
import gozipo.feature.booking.presentation.generated.resources.search_hint_dropoff
import gozipo.feature.booking.presentation.generated.resources.search_hint_pickup
import gozipo.feature.booking.presentation.generated.resources.search_no_results
import org.jetbrains.compose.resources.stringResource

/** Full-screen place search for the pickup or the destination. */
@Composable
fun PlaceSearchOverlay(
    search: PlaceSearch,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .imePadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.sm)
            ) {
                IconButton(onClick = { onAction(BookingAction.OnCloseSearch) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = extended.textPrimary
                    )
                }
                GoZipoTextField(
                    state = search.query,
                    placeholder = stringResource(
                        when (search.target) {
                            SearchTarget.PICKUP -> Res.string.search_hint_pickup
                            SearchTarget.DROPOFF -> Res.string.search_hint_dropoff
                        }
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (search.query.text.isNotEmpty()) {
                        {
                            IconButton(onClick = { search.query.edit { replace(0, length, "") } }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(Res.string.clear)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.md)
                        .focusRequester(focusRequester)
                )
            }
            if (search.isSearching) {
                LinearProgressIndicator(
                    color = extended.textPrimary,
                    trackColor = extended.surfaceHigher,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    ListActionRow(
                        text = stringResource(Res.string.search_choose_on_map),
                        icon = Icons.Default.Map,
                        onClick = { onAction(BookingAction.OnChooseOnMapClick) },
                        showChevron = false
                    )
                    GoZipoDivider()
                }
                items(search.results, key = { it.id }) { place ->
                    PlaceRow(
                        place = place,
                        onClick = { onAction(BookingAction.OnPlaceSelected(place)) })
                }
                val showNoResults = !search.isSearching && search.results.isEmpty() &&
                        search.query.text.trim().length >= 2
                if (search.error != null || showNoResults) {
                    item {
                        Text(
                            text = search.error?.asString()
                                ?: stringResource(Res.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                            modifier = Modifier.padding(spacing.lg)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: Place,
    onClick: () -> Unit
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = extended.textSecondary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyLarge,
                color = extended.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (place.address.isNotBlank()) {
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
