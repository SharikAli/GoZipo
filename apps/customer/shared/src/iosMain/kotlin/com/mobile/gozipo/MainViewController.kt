package com.mobile.gozipo

import androidx.compose.ui.window.ComposeUIViewController
import com.mobile.apps.common.navigation.ExternalUriHandler

fun MainViewController() = ComposeUIViewController { App() }

/** Called from Swift's onOpenURL; the handler lives in :apps:common, which Swift can't see. */
fun handleDeepLink(uri: String) {
    ExternalUriHandler.onNewUri(uri)
}
