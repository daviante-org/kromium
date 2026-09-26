package org.daviante.kromium.api.config

import org.daviante.kromium.api.proxy.KromiumProxy
import org.daviante.kromium.api.scheme.KromiumAssetHandler
import org.daviante.kromium.api.scheme.KromiumCustomScheme
import org.daviante.kromium.api.scheme.KromiumSchemeRegistration

/**
 * General browser behavior and storage configuration.
 */
@ConsistentCopyVisibility
data class KromiumBrowserConfig internal constructor(
    val cachePath: String?,
    val isHeadless: Boolean,
    val remoteDebuggingPort: Int,
    val commandLineArgs: List<String>,
    val customSchemes: List<KromiumCustomScheme>,
    val schemeRegistrations: List<KromiumSchemeRegistration> = emptyList(),
    val proxy: KromiumProxy? = null
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var cachePath: String? = null
        private var isHeadless: Boolean = false
        private var remoteDebuggingPort: Int = 0
        private val commandLineArgs: MutableList<String> = mutableListOf()
        private val customSchemes: MutableList<KromiumCustomScheme> = mutableListOf()
        private val schemeRegistrations: MutableList<KromiumSchemeRegistration> = mutableListOf()
        private var proxy: KromiumProxy? = null

        fun cachePath(cachePath: String?) = apply { this.cachePath = cachePath }
        fun isHeadless(headless: Boolean) = apply { this.isHeadless = headless }
        fun remoteDebuggingPort(port: Int) = apply { this.remoteDebuggingPort = port }
        fun proxy(proxy: KromiumProxy?) = apply { this.proxy = proxy }
        
        fun addArg(arg: String) = apply { this.commandLineArgs.add(arg) }
        fun addArgs(vararg args: String) = apply { this.commandLineArgs.addAll(args) }

        fun addCustomScheme(scheme: KromiumCustomScheme) = apply { this.customSchemes.add(scheme) }
        fun addCustomSchemes(vararg schemes: KromiumCustomScheme) = apply { this.customSchemes.addAll(schemes) }

        fun addSchemeRegistration(registration: KromiumSchemeRegistration) = apply { this.schemeRegistrations.add(registration) }
        fun addSchemeRegistrations(vararg registrations: KromiumSchemeRegistration) = apply { this.schemeRegistrations.addAll(registrations) }
        fun registerSchemeHandler(schemeName: String, domainName: String? = null, handler: KromiumAssetHandler) = apply {
            this.schemeRegistrations.add(KromiumSchemeRegistration(schemeName, domainName, handler))
        }

        fun build(): KromiumBrowserConfig = KromiumBrowserConfig(
            cachePath, isHeadless, remoteDebuggingPort, commandLineArgs.toList(), customSchemes.toList(), schemeRegistrations.toList(), proxy
        )
    }
}

