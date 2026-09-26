package org.daviante.kromium.sample.swing.features

import org.daviante.kromium.api.core.KromiumBrowser
import org.daviante.kromium.api.core.KromiumClient
import org.daviante.kromium.api.download.KromiumDownloadItem
import org.daviante.kromium.api.download.KromiumDownloadListener
import org.daviante.kromium.sample.swing.ui.KromiumBadge
import org.daviante.kromium.sample.swing.ui.KromiumButton
import org.daviante.kromium.sample.swing.ui.KromiumCard
import org.daviante.kromium.sample.swing.ui.KromiumLabel
import org.daviante.kromium.sample.swing.ui.SwingTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class DownloadsFeaturePanel(
    private val browser: KromiumBrowser,
    private val client: KromiumClient
) : JPanel() {

    private val downloadItems = ConcurrentHashMap<Int, KromiumDownloadItem>()
    private val itemsContainer = JPanel()
    private val emptyStatePanel = JPanel(BorderLayout())
    private val countLabel = KromiumLabel("Downloads (0):", isBold = true, fontSize = 11f)

    init {
        layout = BorderLayout()
        isOpaque = false
        border = EmptyBorder(12, 12, 12, 12)

        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.isOpaque = false

        val titleLabel = KromiumLabel("Download Manager", isBold = true, fontSize = 14f)
        val descLabel = KromiumLabel(
            "Track download progress, speed, pause/resume, and cancellation in real time.",
            isMuted = true,
            fontSize = 11f
        )
        topPanel.add(titleLabel)
        topPanel.add(Box.createVerticalStrut(4))
        topPanel.add(descLabel)
        topPanel.add(Box.createVerticalStrut(10))

        // Trigger buttons
        val btnRow = JPanel(GridLayout(1, 2, 6, 0))
        btnRow.isOpaque = false
        val samplePdfBtn = KromiumButton("Sample PDF", isPrimary = true, fontSize = 11f)
        samplePdfBtn.addActionListener {
            browser.navigation.loadUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
        }
        val sample1MbBtn = KromiumButton("1MB Test File", fontSize = 11f)
        sample1MbBtn.addActionListener {
            browser.navigation.loadUrl("https://proof.ovh.net/files/1Mb.dat")
        }
        btnRow.add(samplePdfBtn)
        btnRow.add(sample1MbBtn)
        topPanel.add(btnRow)
        topPanel.add(Box.createVerticalStrut(10))

        // Target folder
        val folderCard = KromiumCard(BorderLayout(6, 0), padding = 6)
        val folderPrefix = KromiumLabel("Folder: ", isBold = true, fontSize = 10f)
        val folderPath = KromiumLabel(client.downloadDirectory.absolutePath, isMuted = true, isMonospace = true, fontSize = 10f)
        folderCard.add(folderPrefix, BorderLayout.WEST)
        folderCard.add(folderPath, BorderLayout.CENTER)
        topPanel.add(folderCard)
        topPanel.add(Box.createVerticalStrut(10))

        topPanel.add(countLabel)
        topPanel.add(Box.createVerticalStrut(6))

        add(topPanel, BorderLayout.NORTH)

        // Empty state
        emptyStatePanel.isOpaque = false
        val emptyCard = KromiumCard(BorderLayout(), padding = 16)
        val emptyLabel = KromiumLabel(
            "<html><center>No active or recent downloads.<br>Click one of the buttons above to test.</center></html>",
            isMuted = true,
            fontSize = 11f
        )
        emptyLabel.horizontalAlignment = SwingConstants.CENTER
        emptyCard.add(emptyLabel, BorderLayout.CENTER)
        emptyStatePanel.add(emptyCard, BorderLayout.CENTER)

        // List container
        itemsContainer.layout = BoxLayout(itemsContainer, BoxLayout.Y_AXIS)
        itemsContainer.isOpaque = false

        val scrollPane = JScrollPane(itemsContainer)
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        scrollPane.border = EmptyBorder(0, 0, 0, 0)

        val centerHolder = JPanel(BorderLayout())
        centerHolder.isOpaque = false
        centerHolder.add(emptyStatePanel, BorderLayout.CENTER)
        add(centerHolder, BorderLayout.CENTER)

        // Register download listener
        val originalListener = client.downloadListener
        client.downloadListener = object : KromiumDownloadListener {
            override fun onDownloadUpdated(item: KromiumDownloadItem) {
                downloadItems[item.id] = item
                originalListener?.onDownloadUpdated(item)

                SwingUtilities.invokeLater {
                    countLabel.text = "Downloads (${downloadItems.size}):"
                    if (centerHolder.components.contains(emptyStatePanel)) {
                        centerHolder.remove(emptyStatePanel)
                        centerHolder.add(scrollPane, BorderLayout.CENTER)
                    }
                    rebuildDownloadCards()
                    centerHolder.revalidate()
                    centerHolder.repaint()
                }
            }
        }
    }

    private fun rebuildDownloadCards() {
        itemsContainer.removeAll()
        val sorted = downloadItems.values.toList().sortedByDescending { it.id }
        for (item in sorted) {
            itemsContainer.add(createDownloadCard(item))
            itemsContainer.add(Box.createVerticalStrut(6))
        }
        itemsContainer.revalidate()
        itemsContainer.repaint()
    }

    private fun createDownloadCard(item: KromiumDownloadItem): JPanel {
        val card = KromiumCard(padding = 8)
        card.layout = BoxLayout(card, BoxLayout.Y_AXIS)

        // Header: File name & Status badge
        val headerRow = JPanel(BorderLayout(6, 0))
        headerRow.isOpaque = false
        val nameLabel = KromiumLabel(item.suggestedFileName, isBold = true, fontSize = 11f)
        headerRow.add(nameLabel, BorderLayout.CENTER)

        val statusLabel = when {
            item.isComplete -> "DONE"
            item.isCanceled -> "CANCELLED"
            item.isPaused -> "PAUSED"
            item.isInProgress -> "ACTIVE"
            else -> "PENDING"
        }
        val badge = KromiumBadge(statusLabel, isActive = item.isInProgress)
        headerRow.add(badge, BorderLayout.EAST)
        card.add(headerRow)
        card.add(Box.createVerticalStrut(6))

        // Progress bar
        val progressBar = JProgressBar(0, 100)
        progressBar.value = item.percentComplete
        progressBar.isStringPainted = false
        progressBar.preferredSize = Dimension(progressBar.preferredSize.width, 6)
        progressBar.background = SwingTheme.surfaceVariant
        progressBar.foreground = SwingTheme.primary
        card.add(progressBar)
        card.add(Box.createVerticalStrut(6))

        // Details: Bytes & Speed
        val receivedMb = item.receivedBytes.toDouble() / (1024 * 1024)
        val totalMb = if (item.totalBytes > 0) item.totalBytes.toDouble() / (1024 * 1024) else 0.0
        val speedKb = item.speed.toDouble() / 1024

        val detailsRow = JPanel(BorderLayout(6, 0))
        detailsRow.isOpaque = false
        val bytesText = String.format("%.2f MB / %.2f MB (%d%%)", receivedMb, totalMb, item.percentComplete)
        val bytesLabel = KromiumLabel(bytesText, isMuted = true, isMonospace = true, fontSize = 10f)
        detailsRow.add(bytesLabel, BorderLayout.WEST)

        if (item.isInProgress && !item.isPaused) {
            val speedText = String.format("%.1f KB/s", speedKb)
            val speedLabel = KromiumLabel(speedText, isBold = true, isMonospace = true, fontSize = 10f)
            detailsRow.add(speedLabel, BorderLayout.EAST)
        }
        card.add(detailsRow)

        // Actions: Pause / Resume / Cancel
        if (item.isInProgress) {
            card.add(Box.createVerticalStrut(6))
            val actionRow = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
            actionRow.isOpaque = false

            if (item.isPaused) {
                val resumeBtn = KromiumButton("Resume", fontSize = 10f)
                resumeBtn.preferredSize = Dimension(60, 24)
                resumeBtn.addActionListener { browser.downloads.resumeDownload(item.id) }
                actionRow.add(resumeBtn)
            } else {
                val pauseBtn = KromiumButton("Pause", fontSize = 10f)
                pauseBtn.preferredSize = Dimension(55, 24)
                pauseBtn.addActionListener { browser.downloads.pauseDownload(item.id) }
                actionRow.add(pauseBtn)
            }

            val cancelBtn = KromiumButton("Cancel", fontSize = 10f)
            cancelBtn.preferredSize = Dimension(60, 24)
            cancelBtn.addActionListener { browser.downloads.cancelDownload(item.id) }
            actionRow.add(cancelBtn)

            card.add(actionRow)
        }

        return card
    }
}
