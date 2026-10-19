package com.mobile.core.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpLogSanitizerTest {

    private val mask = HttpLogSanitizer.MASK

    @Test
    fun masksTokensAndPasswordsInJsonBodies() {
        val message = """BODY START
            |{"email":"a@b.com","password":"Secret123!","accessToken":"aaa.bbb","refreshToken":"ccc"}
            |BODY END""".trimMargin()

        val sanitized = HttpLogSanitizer.sanitize(message)

        assertTrue(sanitized.contains(""""password":"$mask""""))
        assertTrue(sanitized.contains(""""accessToken":"$mask""""))
        assertTrue(sanitized.contains(""""refreshToken":"$mask""""))
        assertTrue(sanitized.contains("a@b.com"))
        assertFalse(sanitized.contains("Secret123!"))
    }

    @Test
    fun masksPasskeyCredentialJsonWithEscapedQuotes() {
        val message = """{"token":"t1","credentialJson":"{\"id\":\"cred-id\",\"rawId\":\"x\"}"}"""

        val sanitized = HttpLogSanitizer.sanitize(message)

        assertEquals("""{"token":"$mask","credentialJson":"$mask"}""", sanitized)
    }

    @Test
    fun masksNumericSignUpCodeButKeepsErrorCodes() {
        assertEquals(
            """{"email":"a@b.com","code":"$mask"}""",
            HttpLogSanitizer.sanitize("""{"email":"a@b.com","code":"123456"}""")
        )
        val error = """{"code":"INVALID_CREDENTIALS","message":"Invalid credentials"}"""
        assertEquals(error, HttpLogSanitizer.sanitize(error))
    }

    @Test
    fun masksSensitiveQueryParameters() {
        val sanitized = HttpLogSanitizer.sanitize("REQUEST: http://localhost:8080/api/auth/verify?token=abc123&lang=en")

        assertEquals("REQUEST: http://localhost:8080/api/auth/verify?token=$mask&lang=en", sanitized)
    }

    @Test
    fun masksSensitiveHeaderLines() {
        val sanitized = HttpLogSanitizer.sanitize("-> Authorization: Bearer abc.def\n-> x-api-key: key123\n-> Accept: */*")

        assertEquals("-> Authorization: $mask\n-> x-api-key: $mask\n-> Accept: */*", sanitized)
        assertTrue(HttpLogSanitizer.isSensitiveHeader("X-Api-Key"))
        assertFalse(HttpLogSanitizer.isSensitiveHeader("Content-Type"))
    }

    @Test
    fun masksStripeClientSecrets() {
        val sanitized = HttpLogSanitizer.sanitize(
            """{"customerId":"cus_1","customerSessionClientSecret":"cuss_secret_x","clientSecret":"pi_1_secret_y"}"""
        )

        assertEquals(
            """{"customerId":"cus_1","customerSessionClientSecret":"$mask","clientSecret":"$mask"}""",
            sanitized
        )
    }
}
