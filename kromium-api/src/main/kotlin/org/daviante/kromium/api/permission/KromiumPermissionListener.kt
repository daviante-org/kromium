package org.daviante.kromium.api.permission

/**
 * Functional interface for intercepting and deciding on media/hardware permission requests.
 *
 * Compatible with Kotlin lambdas `{ request -> ... }` and Java lambdas `request -> ...`.
 */
fun interface KromiumPermissionListener {

    /**
     * Evaluates an incoming [KromiumPermissionRequest] and returns a [KromiumPermissionDecision].
     *
     * @param request Details of the requesting web frame, origin, and requested permission types.
     * @return [KromiumPermissionDecision.GRANT] to permit access, or [KromiumPermissionDecision.DENY] to reject.
     */
    fun onRequestPermission(request: KromiumPermissionRequest): KromiumPermissionDecision

    companion object {
        /**
         * Creates a listener that automatically grants all media and device permission requests.
         */
        @JvmStatic
        fun grantAll(): KromiumPermissionListener =
            KromiumPermissionListener { KromiumPermissionDecision.GRANT }

        /**
         * Creates a listener that automatically denies all media and device permission requests.
         */
        @JvmStatic
        fun denyAll(): KromiumPermissionListener =
            KromiumPermissionListener { KromiumPermissionDecision.DENY }

        /**
         * Creates a listener that grants permissions only if the origin is explicitly allowed.
         */
        @JvmStatic
        fun forOrigins(allowedOrigins: Set<String>): KromiumPermissionListener =
            KromiumPermissionListener { request ->
                // Simple strict match for allowed origins
                val isAllowed = allowedOrigins.any { allowed -> 
                    val originLower = request.origin.lowercase()
                    val allowedLower = allowed.lowercase()
                    originLower == allowedLower || originLower.endsWith(".$allowedLower")
                }
                if (isAllowed) KromiumPermissionDecision.GRANT else KromiumPermissionDecision.DENY
            }

        @JvmStatic
        fun forOrigins(vararg allowedOrigins: String): KromiumPermissionListener =
            forOrigins(allowedOrigins.toSet())
    }
}

