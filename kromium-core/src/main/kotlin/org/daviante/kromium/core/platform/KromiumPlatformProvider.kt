package org.daviante.kromium.core.platform

/**
 * Adapter interface for operating system detection.
 * 
 * Providing this as an interface allows testing environments to mock the
 * operating system and architecture without relying on physical host properties.
 */
interface KromiumPlatformProvider {
    /**
     * Returns the current [KromiumPlatformInfo].
     * @throws IllegalStateException if the OS or architecture is unsupported.
     */
    @Throws(IllegalStateException::class)
    fun current(): KromiumPlatformInfo

    companion object {
        /** Global instance used by Kromium internals. Can be replaced for testing. */
        @Volatile
        @JvmStatic
        var instance: KromiumPlatformProvider = KromiumDefaultPlatformProvider()
    }
}



