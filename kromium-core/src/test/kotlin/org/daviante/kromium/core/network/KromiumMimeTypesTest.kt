package org.daviante.kromium.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KromiumMimeTypesTest {

    @Test
    fun testMimeTypeResolution() {
        assertEquals("text/html; charset=utf-8", KromiumMimeTypes.lookup("index.html"))
        assertEquals("text/css; charset=utf-8", KromiumMimeTypes.lookup("style.css"))
        assertEquals("application/javascript; charset=utf-8", KromiumMimeTypes.lookup("bundle.js"))
        assertEquals("application/javascript; charset=utf-8", KromiumMimeTypes.lookup("module.mjs"))
        assertEquals("application/wasm", KromiumMimeTypes.lookup("engine.wasm"))
        assertEquals("application/json; charset=utf-8", KromiumMimeTypes.lookup("data.json"))
        assertEquals("image/svg+xml", KromiumMimeTypes.lookup("logo.svg"))
        assertEquals("image/png", KromiumMimeTypes.lookup("icon.png"))
        assertEquals("image/webp", KromiumMimeTypes.lookup("photo.webp"))
        assertEquals("font/woff2", KromiumMimeTypes.lookup("font.woff2"))
        assertEquals("video/mp4", KromiumMimeTypes.lookup("video.mp4"))
        assertEquals("application/pdf", KromiumMimeTypes.lookup("document.pdf"))

        assertTrue(KromiumMimeTypes.hasExtension("file.html"))
        assertTrue(KromiumMimeTypes.hasExtension("/path/to/script.js"))
        assertFalse(KromiumMimeTypes.hasExtension("/path/to/dashboard"))
        assertFalse(KromiumMimeTypes.hasExtension("/"))
    }
}

