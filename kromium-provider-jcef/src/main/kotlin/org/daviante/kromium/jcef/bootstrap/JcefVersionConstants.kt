package org.daviante.kromium.jcef.bootstrap

/**
 * Constants representing the specific JCEF build versions this provider is compiled against.
 * The provider relies on these exact versions for its bundled native libraries and jar files.
 */
internal object JcefVersionConstants {
    const val JCEF_VERSION = "150.0.14"
    const val CEF_VERSION = "150.0.14+g7c1aa68+chromium-150.0.7871.129"
    const val CHROMIUM_VERSION = "150.0.7871.129"
}
