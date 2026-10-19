package com.mobile.gozipo

import androidx.compose.ui.window.ComposeUIViewController
import com.mobile.apps.common.navigation.ExternalUriHandler
import com.mobile.core.data.notification.IosDeviceTokenHolder

fun MainViewController() = ComposeUIViewController { App() }

/** Called from Swift's onOpenURL; the handler lives in :apps:common, which Swift can't see. */
fun handleDeepLink(uri: String) {
    ExternalUriHandler.onNewUri(uri)
}

/** Called from Swift's `GoZipoAppDelegate` whenever Firebase issues an FCM token. */
fun updatePushToken(token: String) {
    IosDeviceTokenHolder.updateToken(token)
}
