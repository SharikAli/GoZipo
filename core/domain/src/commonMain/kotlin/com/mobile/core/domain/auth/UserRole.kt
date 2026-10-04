package com.mobile.core.domain.auth

/**
 * Which app an account belongs to. An account is either a customer or a
 * rider, never both. Each app registers the role it signs in as in Koin, and
 * the backend rejects a login from the other app.
 */
enum class UserRole {
    CUSTOMER,
    RIDER
}
