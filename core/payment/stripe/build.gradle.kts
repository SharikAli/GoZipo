plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

// Stripe implementation of the payment contract (:core:payment:api): PaymentSheet,
// PaymentSheet.FlowController and the Embedded Payment Element, with cards, Google Pay
// (Android) and Apple Pay (iOS). Only the customer app depends on this. iOS goes through a
// Swift bridge (StripeIosBridge.kt ↔ apps/customer/iosApp/iosApp/Stripe/).
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.payment.api)
                implementation(projects.core.domain)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.stripe.paymentsheet)
            }
        }
    }

}
