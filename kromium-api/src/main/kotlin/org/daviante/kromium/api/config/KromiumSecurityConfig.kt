package org.daviante.kromium.api.config

/**
 * Handles security boundaries, privacy, and background telemetry.
 */
@ConsistentCopyVisibility
data class KromiumSecurityConfig internal constructor(
    val sandboxEnabled: Boolean,
    val blockRegistryAndTelemetry: Boolean,
    val doNotTrack: Boolean,
    val webrtcIpHandlingPolicy: String?,
    val authServerAllowlist: List<String>,
    val authNegotiateDelegateAllowlist: List<String>
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var sandboxEnabled: Boolean = true
        private var blockRegistryAndTelemetry: Boolean = true
        private var doNotTrack: Boolean = true
        private var webrtcIpHandlingPolicy: String? = null
        private var authServerAllowlist: List<String> = emptyList()
        private var authNegotiateDelegateAllowlist: List<String> = emptyList()

        fun sandboxEnabled(enabled: Boolean) = apply { this.sandboxEnabled = enabled }
        fun blockRegistryAndTelemetry(block: Boolean) = apply { this.blockRegistryAndTelemetry = block }
        fun doNotTrack(enabled: Boolean) = apply { this.doNotTrack = enabled }
        fun webrtcIpHandlingPolicy(policy: String?) = apply { this.webrtcIpHandlingPolicy = policy }
        fun authServerAllowlist(allowlist: List<String>) = apply { this.authServerAllowlist = allowlist.toList() }
        fun authNegotiateDelegateAllowlist(allowlist: List<String>) = apply { this.authNegotiateDelegateAllowlist = allowlist.toList() }

        fun build(): KromiumSecurityConfig = KromiumSecurityConfig(
            sandboxEnabled, blockRegistryAndTelemetry, doNotTrack, webrtcIpHandlingPolicy,
            authServerAllowlist, authNegotiateDelegateAllowlist
        )
    }
}

