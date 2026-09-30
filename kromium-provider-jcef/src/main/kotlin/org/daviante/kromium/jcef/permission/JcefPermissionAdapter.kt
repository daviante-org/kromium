package org.daviante.kromium.jcef.permission

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.permission.KromiumPermissionDecision
import org.daviante.kromium.api.permission.KromiumPermissionListener
import org.daviante.kromium.api.permission.KromiumPermissionRequest
import org.daviante.kromium.api.permission.KromiumPermissionType
import org.daviante.kromium.core.logging.KromiumLogger
import org.daviante.kromium.jcef.core.JcefKromiumClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefMediaAccessCallback
import org.cef.handler.CefPermissionHandler

internal class JcefPermissionAdapter(
    private val client: JcefKromiumClient,
    private val config: KromiumClientConfig
) : CefPermissionHandler {

    companion object {
        private const val TAG = "JcefPermissionAdapter"
    }

    override fun onRequestMediaAccessPermission(
        browser: CefBrowser?,
        frame: CefFrame?,
        requestingUrl: String?,
        accessFlags: Int,
        callback: CefMediaAccessCallback?
    ): Boolean {
        val cb = callback ?: return false
        val url = requestingUrl ?: ""
        val request = KromiumPermissionRequest.from(url, accessFlags)

        if (client.rememberPermissions) {
            val cachedMask = client.getPermissionCache()[request.origin]
            if (cachedMask != null) {
                if (cachedMask == 0) {
                    KromiumLogger.d(TAG, "Denied media access for ${request.origin} from session cache")
                    cb.Cancel()
                    return true
                } else {
                    val allowedMask = accessFlags and cachedMask
                    if (allowedMask != 0) {
                        KromiumLogger.d(TAG, "Granted media access (flags: $allowedMask) for ${request.origin} from session cache")
                        cb.Continue(allowedMask)
                        return true
                    }
                }
            }
        }

        val listener: KromiumPermissionListener? = client.permissionListener ?: config.permissionListener
        if (listener == null) {
            cb.Cancel()
            return true
        }

        try {
            val decision = listener.onRequestPermission(request)

            when (decision) {
                is KromiumPermissionDecision.Grant -> {
                    val allowed = decision.allowedTypes
                    val mask = if (allowed != null) {
                        KromiumPermissionType.toFlags(allowed) and accessFlags
                    } else {
                        accessFlags
                    }
                    if (mask != 0) {
                        if (client.rememberPermissions) {
                            client.getPermissionCache().merge(request.origin, mask) { old, new -> old or new }
                        }
                        cb.Continue(mask)
                    } else {
                        if (client.rememberPermissions) {
                            client.getPermissionCache()[request.origin] = 0
                        }
                        KromiumLogger.i(TAG, "Denied media permission for ${request.origin} (no matching allowed types)")
                        cb.Cancel()
                    }
                }
                is KromiumPermissionDecision.Deny -> {
                    if (client.rememberPermissions) {
                        client.getPermissionCache()[request.origin] = 0
                    }
                    cb.Cancel()
                }
            }
            return true
        } catch (e: Throwable) {
            KromiumLogger.e(TAG, "Exception in PermissionListener", e)
            cb.Cancel()
            return true
        }
    }
}
