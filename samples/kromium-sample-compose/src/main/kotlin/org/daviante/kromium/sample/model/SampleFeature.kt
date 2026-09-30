package org.daviante.kromium.sample.model

/**
 * Enumeration of all interactive features demonstrated in the Kromium Compose Sample.
 */
enum class SampleFeature(
    val title: String,
    val category: String,
    val description: String,
    val iconTag: String
) {
    NAVIGATION(
        title = "Navigation",
        category = "Core",
        description = "URL loading, back/forward history, cache reload, and live reactive state flow",
        iconTag = "NAV"
    ),
    DOM_EXTRACTION(
        title = "DOM Extraction",
        category = "Inspection",
        description = "Extract full outerHTML, inner text, page title, and live favicon URL",
        iconTag = "DOM"
    ),
    DOWNLOADS(
        title = "Downloads",
        category = "IO",
        description = "Real-time download tracking, speed, progress, pause, resume & cancel",
        iconTag = "DL"
    ),
    VIEW_TOOLS(
        title = "View & Graphics",
        category = "Rendering",
        description = "Smooth zoom levels, anti-aliasing hints, image interpolation & screenshot capture",
        iconTag = "VIEW"
    ),
    AUTOMATION(
        title = "Automation",
        category = "Testing",
        description = "Selector clicking, input typing, element counts, visibility & desktop emulation",
        iconTag = "AUTO"
    ),
    COOKIE_STORAGE(
        title = "Cookies & Storage",
        category = "Storage",
        description = "Cookie management, localStorage and sessionStorage clearing",
        iconTag = "DATA"
    ),
    PDF_PRINT(
        title = "PDF Export",
        category = "Export",
        description = "High-fidelity PDF generation with custom margins, scale, and page options",
        iconTag = "PDF"
    ),
    DEVTOOLS(
        title = "DevTools & Console",
        category = "Diagnostics",
        description = "Chromium native DevTools inspector and real-time console log monitoring",
        iconTag = "LOGS"
    ),
    SECURITY_NETWORK(
        title = "Security & Assets",
        category = "Security",
        description = "Selective asset filtering (images/media/fonts) and domain host-locking",
        iconTag = "SEC"
    )
}
