package com.mobile.auth.presentation.passkey_signup

import androidx.compose.foundation.text.input.TextFieldState
import com.mobile.core.presentation.util.UiText

data class PasskeySignupState(
    val step: PasskeySignupStep = PasskeySignupStep.ENTER_EMAIL,
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val codeTextFieldState: TextFieldState = TextFieldState(),
    val codeSentTo: String = "",
    val canSendCode: Boolean = false,
    val canCreatePasskey: Boolean = false,
    val isSendingCode: Boolean = false,
    val isCreatingPasskey: Boolean = false,
    val isCodeError: Boolean = false,
    val error: UiText? = null
)

enum class PasskeySignupStep {
    ENTER_EMAIL,
    ENTER_CODE
}
