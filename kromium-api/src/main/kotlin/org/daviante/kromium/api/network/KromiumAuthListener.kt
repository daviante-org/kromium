package org.daviante.kromium.api.network

/**
 * Listener for handling HTTP Basic, Digest, or Proxy authentication challenges.
 */
fun interface KromiumAuthListener {
    /**
     * Called when an authentication challenge is received.
     *
     * @param request The authentication challenge details.
     * @return [KromiumAuthResponse.Proceed] with credentials, or [KromiumAuthResponse.Cancel].
     */
    fun onAuthRequired(request: KromiumAuthRequest): KromiumAuthResponse
}
