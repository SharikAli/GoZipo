package com.mobile.apps.common.navigation

import com.mobile.core.data.network.UrlConstants
import com.mobile.core.domain.auth.UserRole

/**
 * Base URL of the email links (verify email, reset password) for one app.
 *
 * Each app claims only its own path (`/link/customer/...` or `/link/rider/...`) as a
 * verified Android App Link, so a tapped email link opens the right app directly
 * instead of a chooser. The backend builds the same URLs from the account's role.
 */
fun emailLinkBaseUrl(role: UserRole): String =
    "https://${UrlConstants.APP_LINK_HOST}/link/${role.name.lowercase()}"
