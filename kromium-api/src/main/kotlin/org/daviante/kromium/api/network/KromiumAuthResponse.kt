package org.daviante.kromium.api.network

/**
 * The response decision for an authentication challenge.
 */
sealed class KromiumAuthResponse {
    /** Cancel the authentication request. */
    data object Cancel : KromiumAuthResponse()

    /** Provide authentication credentials. */
    data class Proceed(val username: String, val password: String) : KromiumAuthResponse()

    companion object {
        @JvmField
        val CANCEL = Cancel

        @JvmStatic
        fun cancel(): KromiumAuthResponse = Cancel

        @JvmStatic
        fun proceed(username: String, password: String): KromiumAuthResponse = Proceed(username, password)
    }
}
