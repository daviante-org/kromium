package org.daviante.kromium.api.print

import org.daviante.kromium.api.error.KromiumException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KromiumPdfSettingsTest {

    @Test
    fun testPaperSizeDimensionsAndConversions() {
        assertEquals(8.27, KromiumPaperSize.A4.widthInches)
        assertEquals(11.69, KromiumPaperSize.A4.heightInches)

        assertEquals(8.5, KromiumPaperSize.Letter.widthInches)
        assertEquals(11.0, KromiumPaperSize.Letter.heightInches)

        assertEquals(8.5, KromiumPaperSize.Legal.widthInches)
        assertEquals(14.0, KromiumPaperSize.Legal.heightInches)

        assertEquals(11.0, KromiumPaperSize.Tabloid.widthInches)
        assertEquals(17.0, KromiumPaperSize.Tabloid.heightInches)

        // Millimeter conversion: 210mm x 297mm (A4)
        val convertedA4 = KromiumPaperSize.fromMillimeters(210.0, 297.0)
        assertTrue(abs(convertedA4.widthInches - 8.2677) < 0.01)
        assertTrue(abs(convertedA4.heightInches - 11.6929) < 0.01)

        // Negative or zero dimensions must fail
        assertFailsWith<IllegalArgumentException> {
            KromiumPaperSize(-1.0, 10.0)
        }
        assertFailsWith<IllegalArgumentException> {
            KromiumPaperSize(8.0, 0.0)
        }
    }

    @Test
    fun testPdfMarginsAndConversions() {
        assertEquals(KromiumPdfMargins.Default, KromiumPdfMargins.Default)
        assertEquals(KromiumPdfMargins.None, KromiumPdfMargins.None)
        assertEquals(KromiumPdfMargins.Minimum, KromiumPdfMargins.Minimum)

        val custom = KromiumPdfMargins.fromMillimeters(10.0, 15.0, 10.0, 15.0)
        assertTrue(abs(custom.topInches - (10.0 / 25.4)) < 0.001)
        assertTrue(abs(custom.rightInches - (15.0 / 25.4)) < 0.001)

        assertFailsWith<IllegalArgumentException> {
            KromiumPdfMargins.Custom(-0.1, 0.5, 0.5, 0.5)
        }
    }

    @Test
    fun testPdfSettingsDefaultsAndDecisions() {
        val settings = KromiumPdfSettings.Default

        assertFalse(settings.landscape)
        assertFalse(settings.printBackground)
        assertFalse(settings.createDirectories)
        assertEquals(1.0, settings.scale)
        assertEquals(KromiumPaperSize.A4, settings.paperSize)
        assertEquals(KromiumPdfMargins.Default, settings.margins)
        assertEquals("", settings.pageRanges)
        assertFalse(settings.displayHeaderFooter)
        assertFalse(settings.preferCssPageSize)

        assertFailsWith<IllegalArgumentException> {
            KromiumPdfSettings(scale = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            KromiumPdfSettings(scale = -1.5)
        }
    }

    @Test
    fun testJavaBuilderErgonomics() {
        val settings = KromiumPdfSettings.builder()
            .landscape(true)
            .printBackground(true)
            .scale(0.9)
            .paperSize(KromiumPaperSize.Legal)
            .preferCssPageSize(true)
            .margins(KromiumPdfMargins.None)
            .pageRanges("1-10")
            .displayHeaderFooter(true)
            .headerTemplate("<div>Header</div>")
            .footerTemplate("<div>Footer</div>")
            .createDirectories(true)
            .generateTaggedPdf(true)
            .generateDocumentOutline(true)
            .build()

        assertTrue(settings.landscape)
        assertTrue(settings.printBackground)
        assertEquals(0.9, settings.scale)
        assertEquals(KromiumPaperSize.Legal, settings.paperSize)
        assertTrue(settings.preferCssPageSize)
        assertEquals(KromiumPdfMargins.None, settings.margins)
        assertEquals("1-10", settings.pageRanges)
        assertTrue(settings.displayHeaderFooter)
        assertEquals("<div>Header</div>", settings.headerTemplate)
        assertEquals("<div>Footer</div>", settings.footerTemplate)
        assertTrue(settings.createDirectories)
        assertTrue(settings.generateTaggedPdf)
        assertTrue(settings.generateDocumentOutline)
    }

    @Test
    fun testExceptionHierarchy() {
        val ex = KromiumException.PdfPrintFailed("/tmp/test.pdf", RuntimeException("Disk full"))
        assertEquals("/tmp/test.pdf", ex.path)
        assertEquals("Failed to print web page to PDF: /tmp/test.pdf", ex.message)
        assertEquals("Disk full", ex.cause?.message)
    }
}
