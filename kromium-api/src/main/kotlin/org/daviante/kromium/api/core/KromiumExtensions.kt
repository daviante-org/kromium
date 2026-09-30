package org.daviante.kromium.api.core

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework

/**
 * Creates a new zero-dependency headless [KromiumBrowser] instance within a new isolated client session.
 * 
 * @param url The initial URL to load.
 * @param width The initial width of the headless viewport.
 * @param height The initial height of the headless viewport.
 */
fun KromiumEngine.createHeadlessBrowser(
    url: String = "about:blank",
    width: Int = 1280,
    height: Int = 800
): KromiumBrowser = createClient(
    KromiumClientConfig.builder()
        .framework(KromiumUiFramework.HEADLESS)
        .initialWidth(width)
        .initialHeight(height)
        .build()
).createBrowser(url)

/**
 * Creates a new zero-dependency headless [KromiumBrowser] instance on this client.
 *
 * @param url The initial URL to load.
 * @param width The initial width of the headless viewport.
 * @param height The initial height of the headless viewport.
 */
fun KromiumClient.createHeadlessBrowser(
    url: String = "about:blank",
    width: Int = 1280,
    height: Int = 800
): KromiumBrowser = createBrowser(
    url,
    KromiumClientConfig.builder()
        .framework(KromiumUiFramework.HEADLESS)
        .initialWidth(width)
        .initialHeight(height)
        .build()
)
