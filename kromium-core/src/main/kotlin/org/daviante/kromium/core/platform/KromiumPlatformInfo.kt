package org.daviante.kromium.core.platform

/**
 * Encapsulates the resolved runtime platform.
 */
data class KromiumPlatformInfo(
    val os: KromiumOperatingSystem,
    val arch: KromiumArchitecture
)

