package com.mobile.core.presentation.util

import androidx.compose.runtime.Composable
import com.oklahoma.sonnershelpingsonners.appViewController
import platform.UIKit.UIActivityViewController

@Composable
actual fun rememberShareTextLauncher(): (text: String) -> Unit {
    return { text ->
        val activityViewController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
//        controller.presentViewController(
//            viewControllerToPresent = activityViewController,
//            animated = true,
//            completion = null
//        )
    }
}