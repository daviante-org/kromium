package org.daviante.kromium.jcef.print

import org.daviante.kromium.api.print.KromiumPdfMargins
import org.daviante.kromium.api.print.KromiumPdfSettings
import org.cef.misc.CefPdfPrintSettings

/**
 * Maps engine-agnostic [KromiumPdfSettings] to native JCEF [CefPdfPrintSettings].
 */
internal object JcefPdfSettingsMapper {

    fun map(settings: KromiumPdfSettings): CefPdfPrintSettings {
        val cefSettings = CefPdfPrintSettings()
        cefSettings.landscape = settings.landscape
        cefSettings.print_background = settings.printBackground
        cefSettings.scale = settings.scale
        cefSettings.paper_width = settings.paperSize.widthInches
        cefSettings.paper_height = settings.paperSize.heightInches
        cefSettings.prefer_css_page_size = settings.preferCssPageSize
        cefSettings.page_ranges = settings.pageRanges
        cefSettings.display_header_footer = settings.displayHeaderFooter
        cefSettings.header_template = settings.headerTemplate
        cefSettings.footer_template = settings.footerTemplate
        cefSettings.generate_tagged_pdf = settings.generateTaggedPdf
        cefSettings.generate_document_outline = settings.generateDocumentOutline

        when (val margins = settings.margins) {
            is KromiumPdfMargins.Default -> {
                cefSettings.margin_type = CefPdfPrintSettings.MarginType.DEFAULT
            }
            is KromiumPdfMargins.None -> {
                cefSettings.margin_type = CefPdfPrintSettings.MarginType.NONE
            }
            is KromiumPdfMargins.Minimum -> {
                cefSettings.margin_type = CefPdfPrintSettings.MarginType.CUSTOM
                cefSettings.margin_top = 0.1
                cefSettings.margin_right = 0.1
                cefSettings.margin_bottom = 0.1
                cefSettings.margin_left = 0.1
            }
            is KromiumPdfMargins.Custom -> {
                cefSettings.margin_type = CefPdfPrintSettings.MarginType.CUSTOM
                cefSettings.margin_top = margins.topInches
                cefSettings.margin_right = margins.rightInches
                cefSettings.margin_bottom = margins.bottomInches
                cefSettings.margin_left = margins.leftInches
            }
        }

        return cefSettings
    }
}
