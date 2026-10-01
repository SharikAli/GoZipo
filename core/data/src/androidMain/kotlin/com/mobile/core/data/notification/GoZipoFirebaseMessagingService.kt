package com.mobile.core.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.mobile.core.data.util.PlatformUtils
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.notification.DeviceTokenService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GoZipoFirebaseMessagingService : FirebaseMessagingService() {

    private val deviceTokenService by inject<DeviceTokenService>()
    private val sessionStorage by inject<SessionStorage>()
    private val applicationScope by inject<CoroutineScope>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        applicationScope.launch {
            val authInfo = sessionStorage.observeAuthInfo().first()
            if (authInfo != null) {
                deviceTokenService.registerToken(
                    token = token,
                    platform = PlatformUtils.getOSName()
                )
            }
        }
    }
}
