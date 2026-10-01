package com.mobile.core.data.network

/**
 * Masks credentials in Ktor log output (headers and JSON bodies) so tokens,
 * passwords, codes and passkey credentials never reach the logs.
 */
object HttpLogSanitizer {

    const val MASK = "***"

    val sensitiveHeaders = setOf(
        "authorization",
        "proxy-authorization",
        "x-api-key",
        "cookie",
        "set-cookie"
    )

    private val sensitiveFields = setOf(
        "password",
        "oldPassword",
        "newPassword",
        "currentPassword",
        "accessToken",
        "refreshToken",
        "token",
        "credentialJson",
        "requestJson"
    )

    private val fieldAlternation = sensitiveFields.joinToString("|")

    /** `"field": "value"` (escaped quotes included) or `"field": 123`. */
    private val jsonFieldRegex = Regex(
        "(\"(?:$fieldAlternation)\"\\s*:\\s*)(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]\\s]+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * `"code": "123456"`: the email sign-up code. Only numeric values are masked so
     * error responses (`{"code": "INVALID_CREDENTIALS", ...}`) stay readable.
     */
    private val numericCodeRegex = Regex(
        "(\"code\"\\s*:\\s*)(\"\\d+\"|\\d+)",
        RegexOption.IGNORE_CASE
    )

    /** `field=value` in query strings. */
    private val queryParamRegex = Regex(
        "([?&](?:$fieldAlternation)=)[^&\\s]*",
        RegexOption.IGNORE_CASE
    )

    /** `Authorization: Bearer ...` style header lines, as a fallback to Ktor's sanitizeHeader. */
    private val headerLineRegex = Regex(
        "((?:${sensitiveHeaders.joinToString("|")})\\s*:\\s*).+",
        RegexOption.IGNORE_CASE
    )

    fun isSensitiveHeader(name: String): Boolean = name.lowercase() in sensitiveHeaders

    /** Masks a full Ktor log message (request line, headers and body). */
    fun sanitize(message: String): String {
        return message
            .replace(jsonFieldRegex) { match -> "${match.groupValues[1]}\"$MASK\"" }
            .replace(numericCodeRegex) { match -> "${match.groupValues[1]}\"$MASK\"" }
            .replace(queryParamRegex) { match -> "${match.groupValues[1]}$MASK" }
            .replace(headerLineRegex) { match -> "${match.groupValues[1]}$MASK" }
    }
}
