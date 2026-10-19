package com.mobile.payment.presentation.methods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.cards.GoZipoCard
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.PaymentUiMode
import com.mobile.core.payment.api.rememberCheckout
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.payment.domain.SavedCard
import com.mobile.payment.presentation.components.CheckoutPanel
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.add_card
import gozipo.feature.payment.presentation.generated.resources.back
import gozipo.feature.payment.presentation.generated.resources.cancel
import gozipo.feature.payment.presentation.generated.resources.card_ending
import gozipo.feature.payment.presentation.generated.resources.card_expires
import gozipo.feature.payment.presentation.generated.resources.payment_methods_empty
import gozipo.feature.payment.presentation.generated.resources.payment_methods_subtitle
import gozipo.feature.payment.presentation.generated.resources.payment_methods_title
import gozipo.feature.payment.presentation.generated.resources.remove_card
import gozipo.feature.payment.presentation.generated.resources.retry
import gozipo.feature.payment.presentation.generated.resources.save_card
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentMethodsRoot(
    onBackClick: () -> Unit,
    viewModel: PaymentMethodsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val checkout = rememberCheckout(
        createIntent = viewModel::createIntent,
        onResult = { viewModel.onAction(PaymentMethodsAction.OnCheckoutResult(it)) }
    )

    val checkoutState by checkout.state.collectAsState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PaymentMethodsEvent.StartCheckout -> scope.launch {
                val ready = checkout.prepare(event.session)
                viewModel.onAction(
                    PaymentMethodsAction.OnCheckoutPrepared(
                        ready,
                        checkout.state.value.errorMessage
                    )
                )
                if (ready && !checkout.mode.hasOwnConfirmButton) checkout.confirm()
            }

            is PaymentMethodsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    PaymentMethodsScreen(
        state = state,
        mode = checkout.mode,
        checkoutState = checkoutState,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                PaymentMethodsAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        },
        onSaveCardClick = checkout::confirm,
        onChangeMethodClick = checkout::changePaymentOption,
        embeddedContent = { checkout.Content(it) }
    )
}

@Composable
fun PaymentMethodsScreen(
    state: PaymentMethodsState,
    mode: PaymentUiMode,
    checkoutState: CheckoutState,
    snackbarHostState: SnackbarHostState,
    onAction: (PaymentMethodsAction) -> Unit,
    onSaveCardClick: () -> Unit,
    onChangeMethodClick: () -> Unit,
    embeddedContent: @Composable (Modifier) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended

    GoZipoSnackbarScaffold(snackbarHostState = snackbarHostState) {
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
                    onClick = { onAction(PaymentMethodsAction.OnBackClick) }
                )
                Text(
                    text = stringResource(Res.string.payment_methods_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = extended.textPrimary
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
            ) {
                Text(
                    text = stringResource(Res.string.payment_methods_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )

                when {
                    state.isLoading -> Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(spacing.xl)
                    ) {
                        CircularProgressIndicator()
                    }

                    state.errorText != null && state.cards.isEmpty() -> {
                        Text(
                            text = state.errorText.asString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = extended.textSecondary
                        )
                        GoZipoButton(
                            text = stringResource(Res.string.retry),
                            onClick = { onAction(PaymentMethodsAction.OnRetryClick) },
                            style = GoZipoButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    state.cards.isEmpty() -> Text(
                        text = stringResource(Res.string.payment_methods_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = extended.textSecondary
                    )

                    else -> state.cards.forEach { card ->
                        SavedCardItem(
                            card = card,
                            isRemoving = state.removingCardId == card.id,
                            onRemoveClick = { onAction(PaymentMethodsAction.OnRemoveCardClick(card.id)) }
                        )
                    }
                }

                if (state.isAddingCard && state.isCheckoutReady && mode.hasOwnConfirmButton) {
                    CheckoutPanel(
                        mode = mode,
                        checkoutState = checkoutState,
                        confirmText = stringResource(Res.string.save_card),
                        onConfirmClick = onSaveCardClick,
                        onChangeMethodClick = onChangeMethodClick,
                        embeddedContent = embeddedContent,
                        modifier = Modifier.padding(top = spacing.sm)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(
                    horizontal = spacing.screenHorizontal,
                    vertical = spacing.lg
                )
            ) {
                if (state.isAddingCard && mode.hasOwnConfirmButton) {
                    GoZipoButton(
                        text = stringResource(Res.string.cancel),
                        onClick = { onAction(PaymentMethodsAction.OnCancelAddCardClick) },
                        style = GoZipoButtonStyle.TEXT,
                        enabled = !checkoutState.isConfirming,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    GoZipoButton(
                        text = stringResource(Res.string.add_card),
                        onClick = { onAction(PaymentMethodsAction.OnAddCardClick) },
                        isLoading = state.isPreparingCheckout || checkoutState.isConfirming,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedCardItem(
    card: SavedCard,
    isRemoving: Boolean,
    onRemoveClick: () -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    GoZipoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = extended.textSecondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        Res.string.card_ending,
                        card.brand.brandName(),
                        card.last4
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.textPrimary
                )
                Text(
                    text = stringResource(
                        Res.string.card_expires,
                        card.expMonth.toString().padStart(2, '0'),
                        (card.expYear % 100).toString().padStart(2, '0')
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
            }
            if (isRemoving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(spacing.sm)
                )
            } else {
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.remove_card),
                        tint = extended.textSecondary
                    )
                }
            }
        }
    }
}

/** Stripe's brand ids ("visa", "mastercard") as shown on the card. */
private fun String.brandName(): String = when (lowercase()) {
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    else -> replaceFirstChar { it.uppercase() }
}

@Preview
@Composable
private fun PaymentMethodsScreenPreview() {
    GoZipoTheme(darkTheme = true) {
        PaymentMethodsScreen(
            state = PaymentMethodsState(
                isLoading = false,
                cards = listOf(
                    SavedCard("pm_1", "visa", "4242", 12, 2030),
                    SavedCard("pm_2", "mastercard", "4444", 3, 2029),
                )
            ),
            mode = PaymentUiMode.PAYMENT_SHEET,
            checkoutState = CheckoutState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onSaveCardClick = {},
            onChangeMethodClick = {},
            embeddedContent = {}
        )
    }
}
