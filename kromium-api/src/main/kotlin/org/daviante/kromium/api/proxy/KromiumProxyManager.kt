package org.daviante.kromium.api.proxy

/**
 * Facade for managing the proxy settings of the browser at runtime.
 */
interface KromiumProxyManager {
    /**
     * Dynamically updates the proxy for this browser session.
     * @param proxy The new proxy configuration to apply.
     * @return true if the proxy was successfully updated, false otherwise.
     */
    fun updateProxy(proxy: KromiumProxy): Boolean
}
