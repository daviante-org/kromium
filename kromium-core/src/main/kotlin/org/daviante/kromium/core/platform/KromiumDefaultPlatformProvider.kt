package org.daviante.kromium.core.platform

/**
 * Default implementation that relies on standard JVM system properties.
 */
class KromiumDefaultPlatformProvider : KromiumPlatformProvider {
    @Volatile
    private var cachedPlatform: KromiumPlatformInfo? = null

    @Throws(IllegalStateException::class)
    override fun current(): KromiumPlatformInfo {
        cachedPlatform?.let { return it }

        val osName = System.getProperty("os.name") ?: ""
        val osArch = System.getProperty("os.arch") ?: ""

        val os = KromiumOperatingSystem.fromSystem(osName)
            ?: throw IllegalStateException("Unsupported operating system: $osName")
        val arch = KromiumArchitecture.fromSystem(osArch)
            ?: throw IllegalStateException("Unsupported CPU architecture: $osArch")

        return KromiumPlatformInfo(os, arch).also { cachedPlatform = it }
    }
}

