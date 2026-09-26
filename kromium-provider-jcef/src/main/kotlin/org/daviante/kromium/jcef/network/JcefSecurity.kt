package org.daviante.kromium.jcef.network

import org.daviante.kromium.api.network.KromiumSecurity

internal class JcefSecurity : KromiumSecurity {
    @Volatile override var hostLock: Set<String>? = null
    @Volatile override var hostLockSubresources: Boolean = false
    @Volatile override var hostLockSubframes: Boolean = false
}
