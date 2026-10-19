package com.mobile.core.payment.api

/**
 * Which of the provider's payment UIs the app uses. A build setting (`STRIPE_PAYMENT_UI` in
 * `local.properties`), never a customer choice; every flow works with each of them.
 */
enum class PaymentUiMode {
    /** One sheet that collects the card (or Google Pay / Apple Pay) and pays. */
    PAYMENT_SHEET,

    /** The app shows the chosen method with a "Change" action and its own pay button. */
    FLOW_CONTROLLER,

    /** Payment methods drawn inline in the app's own UI, with the app's pay button. */
    EMBEDDED;

    /** The app draws its own confirm button (the sheet has its own). */
    val hasOwnConfirmButton: Boolean
        get() = this != PAYMENT_SHEET

    companion object {
        /** Unknown or blank values fall back to [PAYMENT_SHEET]. */
        fun fromSetting(value: String?): PaymentUiMode =
            entries.firstOrNull {
                it.name.equals(value?.trim(), ignoreCase = true)
            } ?: PAYMENT_SHEET
    }
}
