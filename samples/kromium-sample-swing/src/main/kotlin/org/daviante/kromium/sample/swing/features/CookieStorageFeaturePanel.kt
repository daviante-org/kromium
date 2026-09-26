package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumSwitch
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class CookieStorageFeaturePanel(
    private val browser: KromiumBrowser
) : JPanel() {

    private val statusCard = KromiumCard(BorderLayout(), padding = 8)
    private val statusLabel = KromiumLabel("", isMuted = false, fontSize = 11f)

    private val cookieNameInput = KromiumTextField(placeholder = "Name (e.g. token)", fontSize = 11f)
    private val cookieValueInput = KromiumTextField(placeholder = "Value (e.g. abc123)", fontSize = 11f)
    private val cookieDomainInput = KromiumTextField(placeholder = "Domain (optional)", fontSize = 11f)
    private val cookiePathInput = KromiumTextField(placeholder = "Path (default /)", fontSize = 11f)

    private val secureCheckBox = JCheckBox("Secure")
    private val httpOnlyCheckBox = JCheckBox("HttpOnly")
    private val setCookieButton = KromiumButton("Set Cookie", isPrimary = true, fontSize = 11f)

    private val cookiesCountLabel = KromiumLabel("Current Page Cookies (0)", isBold = true, fontSize = 11f)
    private val refreshCookiesButton = KromiumButton("Refresh", fontSize = 10f)
    private val cookiesListPanel = JPanel()

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        mainContainer.add(KromiumLabel("Cookie & Storage Engine", isBold = true, fontSize = 14f))
        mainContainer.add(Box.createVerticalStrut(4))
        mainContainer.add(KromiumLabel(
            "Inspect and mutate HTTP cookies, session storage, and origin-scoped localStorage directly through the Kromium Storage facade.",
            isMuted = true,
            fontSize = 11f
        ))
        mainContainer.add(Box.createVerticalStrut(8))

        // Status Card
        statusCard.isVisible = false
        statusCard.add(statusLabel, BorderLayout.CENTER)
        mainContainer.add(statusCard)
        mainContainer.add(Box.createVerticalStrut(8))

        // Card 1: Storage Purge Actions
        val purgeCard = KromiumCard(padding = 10)
        purgeCard.layout = BoxLayout(purgeCard, BoxLayout.Y_AXIS)
        purgeCard.add(KromiumLabel("Storage Purge Actions", isBold = true, fontSize = 11f))
        purgeCard.add(Box.createVerticalStrut(8))

        val purgeRow = JPanel(GridLayout(1, 3, 6, 0))
        purgeRow.isOpaque = false
        val clearCookiesBtn = KromiumButton("Clear Cookies", fontSize = 10f)
        clearCookiesBtn.addActionListener {
            val success = browser.storage.clearCookies()
            showStatus(if (success) "Cleared all session cookies." else "Failed to clear cookies.")
            refreshCookies()
        }
        val clearStorageBtn = KromiumButton("Clear Storage", fontSize = 10f)
        clearStorageBtn.addActionListener {
            browser.storage.clearWebStorageAsync().thenAccept { ok ->
                SwingUtilities.invokeLater {
                    showStatus(if (ok) "Origin localStorage and sessionStorage purged." else "Storage clear failed.")
                }
            }
        }
        val purgeAllBtn = KromiumButton("Purge All", isPrimary = true, fontSize = 10f)
        purgeAllBtn.addActionListener {
            browser.storage.clearBrowsingDataAsync(clearCookies = true, clearStorage = true).thenAccept { ok ->
                SwingUtilities.invokeLater {
                    showStatus(if (ok) "Purged both cookies and origin storage." else "Data purge failed.")
                    refreshCookies()
                }
            }
        }
        purgeRow.add(clearCookiesBtn)
        purgeRow.add(clearStorageBtn)
        purgeRow.add(purgeAllBtn)
        purgeCard.add(purgeRow)
        mainContainer.add(purgeCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: Inject New Cookie
        val injectCard = KromiumCard(padding = 10)
        injectCard.layout = BoxLayout(injectCard, BoxLayout.Y_AXIS)
        injectCard.add(KromiumLabel("Inject New Cookie", isBold = true, fontSize = 11f))
        injectCard.add(Box.createVerticalStrut(8))

        val nameValRow = JPanel(GridLayout(1, 2, 6, 0))
        nameValRow.isOpaque = false
        nameValRow.add(cookieNameInput)
        nameValRow.add(cookieValueInput)
        injectCard.add(nameValRow)
        injectCard.add(Box.createVerticalStrut(6))

        val domPathRow = JPanel(GridLayout(1, 2, 6, 0))
        domPathRow.isOpaque = false
        domPathRow.add(cookieDomainInput)
        cookiePathInput.text = "/"
        domPathRow.add(cookiePathInput)
        injectCard.add(domPathRow)
        injectCard.add(Box.createVerticalStrut(8))

        val optionsRow = JPanel(BorderLayout())
        optionsRow.isOpaque = false

        val checkBoxes = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        checkBoxes.isOpaque = false
        secureCheckBox.isOpaque = false
        httpOnlyCheckBox.isOpaque = false
        checkBoxes.add(secureCheckBox)
        checkBoxes.add(httpOnlyCheckBox)
        optionsRow.add(checkBoxes, BorderLayout.WEST)

        setCookieButton.preferredSize = Dimension(85, 28)
        setCookieButton.addActionListener {
            val name = cookieNameInput.text.trim()
            if (name.isNotEmpty()) {
                val value = cookieValueInput.text
                val domain = cookieDomainInput.text.trim().ifEmpty { null }
                val path = cookiePathInput.text.trim().ifEmpty { "/" }
                val success = browser.storage.setCookie(
                    name = name,
                    value = value,
                    domain = domain,
                    path = path,
                    isSecure = secureCheckBox.isSelected,
                    isHttpOnly = httpOnlyCheckBox.isSelected
                )
                showStatus(if (success) "Cookie '$name' set." else "Failed to set cookie.")
                if (success) {
                    cookieNameInput.text = ""
                    cookieValueInput.text = ""
                    refreshCookies()
                }
            }
        }
        optionsRow.add(setCookieButton, BorderLayout.EAST)
        injectCard.add(optionsRow)
        mainContainer.add(injectCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 3: Current Page Cookies
        val listCard = KromiumCard(padding = 10)
        listCard.layout = BoxLayout(listCard, BoxLayout.Y_AXIS)

        val listHeader = JPanel(BorderLayout())
        listHeader.isOpaque = false
        listHeader.add(cookiesCountLabel, BorderLayout.WEST)

        refreshCookiesButton.preferredSize = Dimension(65, 26)
        refreshCookiesButton.addActionListener { refreshCookies() }
        listHeader.add(refreshCookiesButton, BorderLayout.EAST)
        listCard.add(listHeader)
        listCard.add(Box.createVerticalStrut(8))

        cookiesListPanel.layout = BoxLayout(cookiesListPanel, BoxLayout.Y_AXIS)
        cookiesListPanel.isOpaque = false

        val cookiesScroll = JScrollPane(cookiesListPanel)
        cookiesScroll.preferredSize = Dimension(100, 160)
        cookiesScroll.isOpaque = false
        cookiesScroll.viewport.isOpaque = false
        cookiesScroll.border = EmptyBorder(0, 0, 0, 0)
        listCard.add(cookiesScroll)
        mainContainer.add(listCard)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
        refreshCookies()
    }

    private fun updateColors() {
        secureCheckBox.foreground = SwingTheme.textPrimary
        secureCheckBox.font = SwingTheme.fontSans(11f, false)
        httpOnlyCheckBox.foreground = SwingTheme.textPrimary
        httpOnlyCheckBox.font = SwingTheme.fontSans(11f, false)
    }

    private fun showStatus(message: String) {
        statusLabel.text = message
        statusCard.isVisible = true
        revalidate()
        repaint()
    }

    private fun refreshCookies() {
        refreshCookiesButton.isEnabled = false
        refreshCookiesButton.text = "..."

        browser.storage.getCookiesAsync().thenAccept { map ->
            SwingUtilities.invokeLater {
                refreshCookiesButton.isEnabled = true
                refreshCookiesButton.text = "Refresh"
                cookiesCountLabel.text = "Current Page Cookies (${map.size})"
                cookiesListPanel.removeAll()

                if (map.isEmpty()) {
                    val emptyLbl = KromiumLabel("No cookies found for current origin.", isMuted = true, fontSize = 11f)
                    emptyLbl.horizontalAlignment = SwingConstants.CENTER
                    cookiesListPanel.add(emptyLbl)
                } else {
                    map.forEach { (name, value) ->
                        cookiesListPanel.add(createCookieRow(name, value))
                        cookiesListPanel.add(Box.createVerticalStrut(4))
                    }
                }
                cookiesListPanel.revalidate()
                cookiesListPanel.repaint()
            }
        }.exceptionally { ex ->
            SwingUtilities.invokeLater {
                refreshCookiesButton.isEnabled = true
                refreshCookiesButton.text = "Refresh"
                showStatus("Error fetching cookies: ${ex.message}")
            }
            null
        }
    }

    private fun createCookieRow(name: String, value: String): JPanel {
        val row = KromiumCard(BorderLayout(6, 0), padding = 6)

        val textCol = JPanel()
        textCol.layout = BoxLayout(textCol, BoxLayout.Y_AXIS)
        textCol.isOpaque = false

        val nameLbl = KromiumLabel(name, isBold = true, isMonospace = true, fontSize = 11f)
        val valLbl = KromiumLabel(value, isMuted = true, isMonospace = true, fontSize = 10f)
        textCol.add(nameLbl)
        textCol.add(valLbl)
        row.add(textCol, BorderLayout.CENTER)

        val copyBtn = KromiumButton("Copy", fontSize = 10f)
        copyBtn.preferredSize = Dimension(55, 24)
        copyBtn.addActionListener {
            val selection = StringSelection("$name=$value")
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            showStatus("Copied '$name' to clipboard.")
        }
        row.add(copyBtn, BorderLayout.EAST)
        return row
    }
}
