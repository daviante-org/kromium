package org.daviante.kromium.jcef.osr;

import org.cef.browser.CefBrowserOsr;
import org.daviante.kromium.api.config.KromiumClientConfig;
import org.daviante.kromium.api.error.KromiumException;
import org.daviante.kromium.api.ui.KromiumUiFramework;

import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified factory in JCEF provider to determine and create the framework-native OSR panel for a browser.
 * Supports standard Java SPI (ServiceLoader) as well as classpath-discovered UI modules.
 */
public final class JcefOsrPanelFactory {

    private static final Map<KromiumUiFramework, JcefOsrComponentFactory> factories = new ConcurrentHashMap<>();

    /**
     * Single source of truth for framework canvas class names.
     */
    private static final Map<KromiumUiFramework, String> FRAMEWORK_CANVAS_CLASSES = new EnumMap<>(KromiumUiFramework.class);

    static {
        FRAMEWORK_CANVAS_CLASSES.put(KromiumUiFramework.SWING, "org.daviante.kromium.swing.KromiumSwingCanvas");
        FRAMEWORK_CANVAS_CLASSES.put(KromiumUiFramework.COMPOSE, "org.daviante.kromium.compose.KromiumComposeCanvas");
        FRAMEWORK_CANVAS_CLASSES.put(KromiumUiFramework.AWT, "org.daviante.kromium.awt.KromiumAwtCanvas");
        FRAMEWORK_CANVAS_CLASSES.put(KromiumUiFramework.JAVAFX, "org.daviante.kromium.javafx.KromiumJavaFxCanvas");
        FRAMEWORK_CANVAS_CLASSES.put(KromiumUiFramework.SWT, "org.daviante.kromium.swt.KromiumSwtCanvas");

        // 1. Discover factories via Java SPI (ServiceLoader)
        loadServiceFactories();

        // 2. Automatically load registered framework modules if present on classpath
        for (String className : FRAMEWORK_CANVAS_CLASSES.values()) {
            tryLoadClass(className);
        }
    }

    private JcefOsrPanelFactory() {}

    /**
     * Registers an OSR component factory for the given framework.
     */
    public static void register(KromiumUiFramework framework, JcefOsrComponentFactory factory) {
        if (framework != null && factory != null) {
            factories.put(framework, factory);
        }
    }

    /**
     * Unregisters an OSR component factory for the given framework.
     */
    public static void unregister(KromiumUiFramework framework) {
        if (framework != null) {
            factories.remove(framework);
        }
    }

    /**
     * Checks if a factory is registered for the given framework.
     */
    public static boolean hasFactory(KromiumUiFramework framework) {
        if (framework == null) {
            return false;
        }
        if (!factories.containsKey(framework)) {
            triggerClassLoad(framework);
        }
        return factories.containsKey(framework);
    }

    /**
     * Decides and creates the appropriate framework-native OSR panel for the given browser and session config.
     */
    public static JcefOsrComponent createPanel(KromiumUiFramework framework, CefBrowserOsr browser, KromiumClientConfig config) {
        if (framework == null) {
            throw new KromiumException.FrameworkRequired();
        }
        if (framework == KromiumUiFramework.HEADLESS) {
            return null;
        }

        // 1. Check pre-registered factory
        JcefOsrComponentFactory factory = factories.get(framework);
        if (factory == null) {
            triggerClassLoad(framework);
            factory = factories.get(framework);
        }

        if (factory != null) {
            JcefOsrComponent component = factory.createComponent(browser, config);
            if (component == null) {
                throw new KromiumException.CanvasNotAvailable(framework);
            }
            return component;
        }

        throw new KromiumException.CanvasNotAvailable(framework);
    }

    private static void triggerClassLoad(KromiumUiFramework framework) {
        if (framework == null) return;
        String className = FRAMEWORK_CANVAS_CLASSES.get(framework);
        if (className != null) {
            tryLoadClass(className);
        }
        loadServiceFactories();
    }

    private static void loadServiceFactories() {
        try {
            ServiceLoader<JcefOsrComponentFactory> loader = ServiceLoader.load(
                JcefOsrComponentFactory.class,
                Thread.currentThread().getContextClassLoader()
            );
            for (JcefOsrComponentFactory factory : loader) {
                for (KromiumUiFramework framework : KromiumUiFramework.values()) {
                    if (framework != KromiumUiFramework.HEADLESS && factory.supports(framework)) {
                        factories.putIfAbsent(framework, factory);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void tryLoadClass(String className) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                Class.forName(className, true, cl);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            Class.forName(className, true, JcefOsrPanelFactory.class.getClassLoader());
        } catch (Throwable ignored) {}
    }
}
