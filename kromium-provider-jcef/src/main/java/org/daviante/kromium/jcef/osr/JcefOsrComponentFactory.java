package org.daviante.kromium.jcef.osr;

import org.cef.browser.CefBrowserOsr;
import org.daviante.kromium.api.config.KromiumClientConfig;
import org.daviante.kromium.api.ui.KromiumUiFramework;

/**
 * Factory SPI for creating framework-native OSR display components.
 * Registered by individual UI modules (kromium-swing, kromium-awt, kromium-javafx, kromium-swt).
 */
public interface JcefOsrComponentFactory {

    /**
     * Checks if this factory supports the given UI framework.
     */
    boolean supports(KromiumUiFramework framework);

    /**
     * Creates and initializes a framework-native OSR component for the given browser instance.
     *
     * @param browser The CefBrowserOsr instance
     * @param config  The KromiumClientConfig session configuration (may contain transparency, DPI, etc.)
     * @return The framework-native JcefOsrComponent
     */
    JcefOsrComponent createComponent(CefBrowserOsr browser, KromiumClientConfig config);
}
