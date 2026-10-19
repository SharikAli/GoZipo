package com.mobile.core.presentation.permissions

import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PermissionController(
    private val mokoPermissionsController: PermissionsController
) {
    // The system shows one permission dialog at a time; a concurrent request would be canceled
    private val mutex = Mutex()

    suspend fun requestPermission(permission: Permission): PermissionState = mutex.withLock {
        try {
            mokoPermissionsController.providePermission(permission.toMokoPermission())
            PermissionState.GRANTED
        } catch (_: DeniedAlwaysException) {
            PermissionState.PERMANENTLY_DENIED
        } catch (_: DeniedException) {
            PermissionState.DENIED
        } catch (_: RequestCanceledException) {
            PermissionState.DENIED
        }
    }
}

private fun Permission.toMokoPermission(): dev.icerock.moko.permissions.Permission {
    return when (this) {
        Permission.NOTIFICATIONS -> dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION
        Permission.LOCATION -> dev.icerock.moko.permissions.Permission.LOCATION
    }
}