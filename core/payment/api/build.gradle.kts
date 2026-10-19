plugins {
    alias(libs.plugins.convention.cmp.library)
}

// Card payment UI contract. Features open payment UI only through rememberCheckout and
// these types; the provider (today :core:payment:stripe) is bound in the customer app's Koin.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.koin.compose)
            }
        }
    }

}
