package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.network.KromiumAssetFilter
import org.daviante.kromium.api.network.KromiumResourceType
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.KromiumSwitch
import org.daviante.kromium.sample.swing.ui.KromiumTextField
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.border.EmptyBorder

class SecurityNetworkFeaturePanel(
    private val browser: KromiumBrowser,
    private val client: KromiumClient
) : JPanel() {

    private val statusCard = KromiumCard(BorderLayout(), padding = 8)
    private val statusLabel = KromiumLabel("", isMuted = false, fontSize = 11f)

    private val blockImagesSwitch = KromiumSwitch(checked = false)
    private val blockMediaSwitch = KromiumSwitch(checked = false)
    private val blockFontsSwitch = KromiumSwitch(checked = false)

    private val hostLockSwitch = KromiumSwitch(checked = browser.security.hostLock != null)
    private val hostLockDomainsInput = KromiumTextField(
        placeholder = "Whitelist (e.g. example.com, wikipedia.org)",
        fontSize = 11f
    )
    private val restrictSubresourcesBox = JCheckBox("Restrict Subresources (XHR / Fetch / Scripts)")
    private val restrictSubframesBox = JCheckBox("Restrict Subframes (iFrames)")
    private val hostLockDetailsPanel = JPanel()

    private val dntSwitch = KromiumSwitch(checked = client.doNotTrack)
    private val rememberPermsSwitch = KromiumSwitch(checked = client.rememberPermissions)

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val mainContainer = JPanel()
        mainContainer.layout = BoxLayout(mainContainer, BoxLayout.Y_AXIS)
        mainContainer.isOpaque = false

        // Header
        mainContainer.add(KromiumLabel("Security & Network Policies", isBold = true, fontSize = 14f))
        mainContainer.add(Box.createVerticalStrut(4))
        mainContainer.add(KromiumLabel(
            "Enforce domain-level host locking, restrict subresources, block bandwidth-heavy media and web fonts, and control privacy headers.",
            isMuted = true,
            fontSize = 11f
        ))
        mainContainer.add(Box.createVerticalStrut(8))

        // Status Card
        statusCard.isVisible = false
        statusCard.add(statusLabel, BorderLayout.CENTER)
        mainContainer.add(statusCard)
        mainContainer.add(Box.createVerticalStrut(8))

        // Card 1: Asset Filtering
        val assetCard = KromiumCard(padding = 10)
        assetCard.layout = BoxLayout(assetCard, BoxLayout.Y_AXIS)
        assetCard.add(KromiumLabel("Bandwidth & Resource Filtering", isBold = true, fontSize = 11f))
        assetCard.add(Box.createVerticalStrut(8))

        assetCard.add(createSwitchRow("Block Images", "Suppresses image downloads (png, jpg, webp, svg)", blockImagesSwitch))
        assetCard.add(Box.createVerticalStrut(6))
        assetCard.add(createSwitchRow("Block Audio & Video", "Suppresses HTML5 video streams and audio media", blockMediaSwitch))
        assetCard.add(Box.createVerticalStrut(6))
        assetCard.add(createSwitchRow("Block Web Fonts", "Blocks woff, woff2, and ttf font network requests", blockFontsSwitch))
        assetCard.add(Box.createVerticalStrut(8))

        val applyAssetsBtn = KromiumButton("Apply Asset Filters", isPrimary = true, fontSize = 11f)
        applyAssetsBtn.preferredSize = Dimension(125, 28)
        applyAssetsBtn.addActionListener {
            val blockedTypes = mutableSetOf<KromiumResourceType>()
            if (blockImagesSwitch.isChecked) blockedTypes.add(KromiumResourceType.IMAGE)
            if (blockMediaSwitch.isChecked) blockedTypes.add(KromiumResourceType.MEDIA)
            if (blockFontsSwitch.isChecked) blockedTypes.add(KromiumResourceType.FONT_RESOURCE)
            
            browser.assets.filter = if (blockedTypes.isEmpty()) {
                null
            } else {
                KromiumAssetFilter(blockedResourceTypes = blockedTypes)
            }
            showStatus("Asset filter rules updated: images=${blockImagesSwitch.isChecked}, media=${blockMediaSwitch.isChecked}, fonts=${blockFontsSwitch.isChecked}")
        }
        val assetBtnRow = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        assetBtnRow.isOpaque = false
        assetBtnRow.add(applyAssetsBtn)
        assetCard.add(assetBtnRow)
        mainContainer.add(assetCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 2: Host Lock
        val hostCard = KromiumCard(padding = 10)
        hostCard.layout = BoxLayout(hostCard, BoxLayout.Y_AXIS)

        val hostHeaderRow = createSwitchRow("Domain Host Lock (Sandbox)", "Reject any top-level navigation outside whitelist", hostLockSwitch)
        hostCard.add(hostHeaderRow)
        hostCard.add(Box.createVerticalStrut(8))

        hostLockDetailsPanel.layout = BoxLayout(hostLockDetailsPanel, BoxLayout.Y_AXIS)
        hostLockDetailsPanel.isOpaque = false
        hostLockDomainsInput.text = browser.security.hostLock?.joinToString(", ") ?: "wikipedia.org, example.com"
        hostLockDetailsPanel.add(hostLockDomainsInput)
        hostLockDetailsPanel.add(Box.createVerticalStrut(6))

        restrictSubresourcesBox.isSelected = browser.security.hostLockSubresources
        restrictSubresourcesBox.isOpaque = false
        hostLockDetailsPanel.add(restrictSubresourcesBox)
        hostLockDetailsPanel.add(Box.createVerticalStrut(4))

        restrictSubframesBox.isSelected = browser.security.hostLockSubframes
        restrictSubframesBox.isOpaque = false
        hostLockDetailsPanel.add(restrictSubframesBox)
        hostLockDetailsPanel.add(Box.createVerticalStrut(6))

        hostLockDetailsPanel.isVisible = hostLockSwitch.isChecked
        hostCard.add(hostLockDetailsPanel)

        hostLockSwitch.onCheckedChanged = { checked ->
            hostLockDetailsPanel.isVisible = checked
            revalidate()
            repaint()
        }

        val applyHostBtn = KromiumButton("Apply Host Lock", isPrimary = true, fontSize = 11f)
        applyHostBtn.preferredSize = Dimension(110, 28)
        applyHostBtn.addActionListener {
            if (hostLockSwitch.isChecked) {
                val domains = hostLockDomainsInput.text.split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                browser.security.hostLock = domains
                browser.security.hostLockSubresources = restrictSubresourcesBox.isSelected
                browser.security.hostLockSubframes = restrictSubframesBox.isSelected
                showStatus("Host lock activated for: ${domains.joinToString(", ")}")
            } else {
                browser.security.hostLock = null
                showStatus("Host lock deactivated. All domains allowed.")
            }
        }
        val hostBtnRow = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        hostBtnRow.isOpaque = false
        hostBtnRow.add(applyHostBtn)
        hostCard.add(hostBtnRow)
        mainContainer.add(hostCard)
        mainContainer.add(Box.createVerticalStrut(10))

        // Card 3: Privacy & Permissions
        val privacyCard = KromiumCard(padding = 10)
        privacyCard.layout = BoxLayout(privacyCard, BoxLayout.Y_AXIS)
        privacyCard.add(KromiumLabel("Client Privacy & Device Permissions", isBold = true, fontSize = 11f))
        privacyCard.add(Box.createVerticalStrut(8))

        dntSwitch.onCheckedChanged = {
            client.doNotTrack = it
            showStatus("Do Not Track header set to $it")
        }
        privacyCard.add(createSwitchRow("Do Not Track (DNT) / Sec-GPC", "Assert DNT=1 and Global Privacy Control headers", dntSwitch))
        privacyCard.add(Box.createVerticalStrut(6))

        rememberPermsSwitch.onCheckedChanged = {
            client.rememberPermissions = it
            showStatus("Remember permissions set to $it")
        }
        privacyCard.add(createSwitchRow("Remember Permission Decisions", "Cache media/device approvals for this session", rememberPermsSwitch))

        mainContainer.add(privacyCard)

        val scroll = JScrollPane(mainContainer)
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.border = EmptyBorder(0, 0, 0, 0)
        add(scroll, BorderLayout.CENTER)

        SwingTheme.addThemeListener {
            updateColors()
        }
        updateColors()
    }

    private fun updateColors() {
        restrictSubresourcesBox.foreground = SwingTheme.textPrimary
        restrictSubresourcesBox.font = SwingTheme.fontSans(10f, false)
        restrictSubframesBox.foreground = SwingTheme.textPrimary
        restrictSubframesBox.font = SwingTheme.fontSans(10f, false)
    }

    private fun createSwitchRow(title: String, subtitle: String, toggle: KromiumSwitch): JPanel {
        val row = JPanel(BorderLayout(8, 0))
        row.isOpaque = false

        val textCol = JPanel()
        textCol.layout = BoxLayout(textCol, BoxLayout.Y_AXIS)
        textCol.isOpaque = false
        textCol.add(KromiumLabel(title, isBold = true, fontSize = 11f))
        textCol.add(KromiumLabel(subtitle, isMuted = true, fontSize = 10f))

        row.add(textCol, BorderLayout.CENTER)
        row.add(toggle, BorderLayout.EAST)
        return row
    }

    private fun showStatus(message: String) {
        statusLabel.text = message
        statusCard.isVisible = true
        revalidate()
        repaint()
    }
}
