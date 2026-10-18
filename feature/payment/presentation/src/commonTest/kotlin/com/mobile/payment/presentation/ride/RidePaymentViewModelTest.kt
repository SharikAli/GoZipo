package com.mobile.payment.presentation.ride

import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.IntentCreation
import com.mobile.payment.domain.CheckoutConfig
import com.mobile.payment.domain.PaymentEventSource
import com.mobile.payment.domain.PaymentService
import com.mobile.payment.domain.RidePayment
import com.mobile.payment.domain.RidePaymentStatus
import com.mobile.payment.domain.RidePaymentUpdate
import com.mobile.payment.domain.SavedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class RidePaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val payments = FakePaymentService()
    private val events = FakePaymentEventSource()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): RidePaymentViewModel {
        val viewModel = RidePaymentViewModel("ride-1", 450, payments, events)
        // Start the state flow (loads the payment and subscribes to updates)
        backgroundScope.launch(dispatcher) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun anAlreadyPaidRideShowsPaid() = runTest(dispatcher) {
        payments.payment = payment(RidePaymentStatus.SUCCEEDED, payable = false)
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(RidePaymentStage.PAID, viewModel.state.value.stage)
    }

    @Test
    fun payWithCardStartsACheckoutForTheBackendAmount() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(RidePaymentAction.OnPayWithCardClick)
        val event = viewModel.events.first()

        val intent = assertIs<RidePaymentEvent.StartCheckout>(event).session.intent
        assertEquals(CheckoutIntent.Payment(amountMinor = 45_000, currency = "pkr"), intent)
        assertEquals(RidePaymentStage.PREPARING, viewModel.state.value.stage)
    }

    @Test
    fun aTransientErrorIsRetriedOnceWithTheSameIdempotencyKey() = runTest(dispatcher) {
        payments.intentResults += Result.Failure(DataError.Remote.NO_INTERNET)
        payments.intentResults += Result.Success("pi_1_secret")
        val viewModel = viewModel()

        val created = viewModel.createIntent(savePaymentMethod = true)

        assertEquals(IntentCreation.Created("pi_1_secret"), created)
        assertEquals(2, payments.intentKeys.size)
        assertEquals(payments.intentKeys[0], payments.intentKeys[1])
    }

    @Test
    fun eachConfirmationUsesANewIdempotencyKey() = runTest(dispatcher) {
        payments.intentResults += Result.Success("pi_1_secret")
        payments.intentResults += Result.Success("pi_1_secret")
        val viewModel = viewModel()

        viewModel.createIntent(savePaymentMethod = false)
        viewModel.createIntent(savePaymentMethod = false)

        assertEquals(2, payments.intentKeys.toSet().size)
    }

    @Test
    fun completedCheckoutIsPaidOnlyOnceTheBackendConfirms() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        payments.payment = payment(RidePaymentStatus.PROCESSING, payable = false)
        viewModel.onAction(RidePaymentAction.OnCheckoutResult(CheckoutResult.Completed))
        advanceTimeBy(1_000)
        assertEquals(RidePaymentStage.VERIFYING, viewModel.state.value.stage)

        payments.payment = payment(RidePaymentStatus.SUCCEEDED, payable = false)
        advanceTimeBy(3_000)
        assertEquals(RidePaymentStage.PAID, viewModel.state.value.stage)
    }

    @Test
    fun realtimeSuccessForThisRideMarksItPaid() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        events.updates.emit(RidePaymentUpdate("other-ride", RidePaymentStatus.SUCCEEDED, 450))
        advanceUntilIdle()
        assertEquals(RidePaymentStage.IDLE, viewModel.state.value.stage)

        events.updates.emit(RidePaymentUpdate("ride-1", RidePaymentStatus.SUCCEEDED, 450))
        advanceUntilIdle()
        assertEquals(RidePaymentStage.PAID, viewModel.state.value.stage)
    }

    @Test
    fun aDeclineFoundWhileVerifyingLetsTheCustomerTryAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        payments.payment = payment(RidePaymentStatus.FAILED, payable = true, failure = "Your card was declined.")
        viewModel.onAction(RidePaymentAction.OnCheckoutResult(CheckoutResult.Completed))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(RidePaymentStage.IDLE, state.stage)
        assertEquals(true, state.payable)
    }

    private fun payment(status: RidePaymentStatus, payable: Boolean, failure: String? = null) = RidePayment(
        rideId = "ride-1",
        status = status,
        amount = 450,
        amountMinor = 45_000,
        currency = "pkr",
        payable = payable,
        failureMessage = failure,
    )

    private class FakePaymentEventSource : PaymentEventSource {
        override val updates = MutableSharedFlow<RidePaymentUpdate>()
    }

    private inner class FakePaymentService : PaymentService {
        var payment = payment(RidePaymentStatus.REQUIRES_PAYMENT, payable = true)
        val intentResults = ArrayDeque<Result<String, DataError.Remote>>()
        val intentKeys = mutableListOf<String>()

        override suspend fun getCheckoutConfig(): Result<CheckoutConfig, DataError.Remote> =
            Result.Success(CheckoutConfig("pk_test_x", "cus_1", "cuss_secret", "GoZipo", "US", "pkr"))

        override suspend fun createSetupIntent(idempotencyKey: String): Result<String, DataError.Remote> =
            Result.Success("seti_secret")

        override suspend fun getRidePayment(rideId: String): Result<RidePayment, DataError.Remote> =
            Result.Success(payment)

        override suspend fun createRidePaymentIntent(
            rideId: String,
            savePaymentMethod: Boolean,
            idempotencyKey: String
        ): Result<String, DataError.Remote> {
            intentKeys += idempotencyKey
            return intentResults.removeFirst()
        }

        override suspend fun getSavedCards(): Result<List<SavedCard>, DataError.Remote> =
            Result.Success(emptyList())

        override suspend fun removeCard(cardId: String): EmptyResult<DataError.Remote> = Result.Success(Unit)
    }
}
