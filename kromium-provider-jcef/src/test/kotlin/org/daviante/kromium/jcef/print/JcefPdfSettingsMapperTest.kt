package org.daviante.kromium.jcef.print

import org.daviante.kromium.api.print.KromiumPaperSize
import org.daviante.kromium.api.print.KromiumPdfMargins
import org.daviante.kromium.api.print.KromiumPdfSettings
import org.cef.misc.CefPdfPrintSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcefPdfSettingsMapperTest {

    @Test
    fun testToCefPdfPrintSettingsMapping() {
        val customMargins = KromiumPdfMargins.fromInches(0.5, 0.5, 0.5, 0.5)
        val settings = KromiumPdfSettings(
            landscape = true,
            printBackground = true,
            scale = 1.25,
            paperSize = KromiumPaperSize.Letter,
            preferCssPageSize = true,
            margins = customMargins,
            pageRanges = "1-3, 5",
            displayHeaderFooter = true,
            headerTemplate = "<span class=\"title\"></span>",
            footerTemplate = "<span class=\"pageNumber\"></span>",
            generateTaggedPdf = true,
            generateDocumentOutline = true
        )

        val cef = JcefPdfSettingsMapper.map(settings)

        assertTrue(cef.landscape)
        assertTrue(cef.print_background)
        assertEquals(1.25, cef.scale)
        assertEquals(8.5, cef.paper_width)
        assertEquals(11.0, cef.paper_height)
        assertTrue(cef.prefer_css_page_size)
        assertEquals("1-3, 5", cef.page_ranges)
        assertTrue(cef.display_header_footer)
        assertEquals("<span class=\"title\"></span>", cef.header_template)
        assertEquals("<span class=\"pageNumber\"></span>", cef.footer_template)
        assertTrue(cef.generate_tagged_pdf)
        assertTrue(cef.generate_document_outline)

        assertEquals(CefPdfPrintSettings.MarginType.CUSTOM, cef.margin_type)
        assertEquals(0.5, cef.margin_top)
        assertEquals(0.5, cef.margin_right)
        assertEquals(0.5, cef.margin_bottom)
        assertEquals(0.5, cef.margin_left)

        // Test None margins mapping
        val noneCef = JcefPdfSettingsMapper.map(KromiumPdfSettings(margins = KromiumPdfMargins.None))
        assertEquals(CefPdfPrintSettings.MarginType.NONE, noneCef.margin_type)

        // Test Minimum margins mapping
        val minCef = JcefPdfSettingsMapper.map(KromiumPdfSettings(margins = KromiumPdfMargins.Minimum))
        assertEquals(CefPdfPrintSettings.MarginType.CUSTOM, minCef.margin_type)
        assertEquals(0.1, minCef.margin_top)

        // Test Default margins mapping
        val defaultCef = JcefPdfSettingsMapper.map(KromiumPdfSettings.Default)
        assertEquals(CefPdfPrintSettings.MarginType.DEFAULT, defaultCef.margin_type)
    }
}
