package com.mobile.core.presentation.util

import androidx.compose.runtime.Composable

/**
 * Platform's native share sheet (Android's `ACTION_SEND` chooser, iOS's `UIActivityViewController`) when invoked.
 */
@Composable
expect fun rememberShareTextLauncher(): (text: String) -> Unit
