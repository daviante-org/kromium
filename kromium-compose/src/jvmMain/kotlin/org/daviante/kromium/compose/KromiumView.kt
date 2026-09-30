package org.daviante.kromium.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import org.daviante.kromium.api.core.KromiumBrowser

/**
 * A Jetpack / Multiplatform Compose wrapper for a [KromiumBrowser] instance with optional loading overlay support.
 *
 * @param browser The active browser instance to display.
 * @param modifier Compose layout modifier applied to the root container.
 * @param loadingContent Optional composable overlay displayed when [browser.navigation.navigationState] indicates loading.
 */
@Composable
fun KromiumView(
    browser: KromiumBrowser,
    modifier: Modifier = Modifier,
    loadingContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val navState by browser.navigation.navigationState.collectAsState()

    val awtComponent = remember(browser) {
        browser.view.surface.unwrap(Component::class) as? Component
    }

    Box(modifier = modifier) {
        if (awtComponent != null) {
            SwingPanel(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    JPanel(BorderLayout()).apply {
                        isFocusable = true
                        awtComponent.isVisible = true
                        add(awtComponent, BorderLayout.CENTER)

                        addMouseListener(object : MouseAdapter() {
                            override fun mousePressed(e: MouseEvent) {
                                awtComponent.requestFocusInWindow()
                                browser.view.setFocus(true)
                            }
                        })
                    }
                },
                update = { panel ->
                    awtComponent.isVisible = true
                    if (panel.componentCount == 0 || panel.getComponent(0) != awtComponent) {
                        panel.removeAll()
                        panel.add(awtComponent, BorderLayout.CENTER)
                        panel.revalidate()
                        panel.repaint()
                    }
                }
            )
        }

        if (navState.isLoading && loadingContent != null) {
            loadingContent()
        }
    }
}
