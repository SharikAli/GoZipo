package com.mobile.auth.data.passkey

import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

internal class AuthorizationDelegate(
    private val onResult: (credential: Any?, error: NSError?) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        onResult(didCompleteWithAuthorization.credential, null)
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        onResult(null, didCompleteWithError)
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController
    ): ASPresentationAnchor = keyWindow() ?: UIWindow()

    private fun keyWindow(): UIWindow? = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstNotNullOfOrNull { scene ->
            scene.keyWindow ?: scene.windows.filterIsInstance<UIWindow>().firstOrNull()
        }
}
