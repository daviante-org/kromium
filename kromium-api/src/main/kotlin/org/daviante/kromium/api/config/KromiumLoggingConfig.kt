package org.daviante.kromium.api.config

/**
 * Logging configuration for the engine.
 */
@ConsistentCopyVisibility
data class KromiumLoggingConfig internal constructor(
    val logSeverity: KromiumLogSeverity
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var logSeverity: KromiumLogSeverity = KromiumLogSeverity.DEFAULT

        fun logSeverity(severity: KromiumLogSeverity) = apply { this.logSeverity = severity }

        fun build(): KromiumLoggingConfig = KromiumLoggingConfig(logSeverity)
    }
}

