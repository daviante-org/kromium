package org.daviante.kromium.api.scheme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KromiumSchemeHandlerTest {

    @Test
    fun testPathSanitizationPreventsTraversal() {
        assertEquals("/index.html", KromiumSchemeHandler.sanitizePath("/index.html"))
        assertEquals("/css/style.css", KromiumSchemeHandler.sanitizePath("/css/style.css"))
        assertEquals("/assets/data.json", KromiumSchemeHandler.sanitizePath("\\assets\\data.json"))

        // Path traversal attempts must return null
        assertNull(KromiumSchemeHandler.sanitizePath("/../secret.txt"))
        assertNull(KromiumSchemeHandler.sanitizePath("/assets/../../etc/passwd"))
        assertNull(KromiumSchemeHandler.sanitizePath("/%2e%2e/secret"))
        assertNull(KromiumSchemeHandler.sanitizePath("/index.html\u0000.png"))
    }

    @Test
    fun testKromiumAssetRequestParsing() {
        val request = KromiumAssetRequest(
            url = "app://myapp/dashboard?user=kiran&tab=settings",
            method = "GET",
            scheme = "app",
            domain = "myapp",
            path = "/dashboard",
            queryString = "user=kiran&tab=settings",
            queryParameters = mapOf("user" to "kiran", "tab" to "settings"),
            headers = mapOf("Authorization" to "Bearer token123", "User-Agent" to "KromiumTest")
        )

        assertEquals("app", request.scheme)
        assertEquals("myapp", request.domain)
        assertEquals("/dashboard", request.path)
        assertEquals("kiran", request.getQueryParam("user"))
        assertEquals("settings", request.getQueryParam("tab"))
        assertEquals("Bearer token123", request.getHeader("authorization"))
        assertEquals("KromiumTest", request.getHeader("USER-AGENT"))
        assertNull(request.getHeader("Non-Existent"))
    }

    @Test
    fun testKromiumAssetResponseFactoriesAndHeaders() {
        val jsonRes = KromiumAssetResponse.json("""{"status":"ok"}""")
            .withCors("https://example.com")
            .withCacheControl(3600)

        assertEquals(200, jsonRes.statusCode)
        assertEquals("application/json; charset=utf-8", jsonRes.mimeType)
        assertEquals("https://example.com", jsonRes.headers["Access-Control-Allow-Origin"])
        assertEquals("public, max-age=3600", jsonRes.headers["Cache-Control"])

        val stream = jsonRes.openStream()
        val text = stream.bufferedReader().readText()
        assertEquals("""{"status":"ok"}""", text)

        val notFoundRes = KromiumAssetResponse.notFound()
        assertEquals(404, notFoundRes.statusCode)
        assertEquals("Not Found", notFoundRes.statusText)

        val forbiddenRes = KromiumAssetResponse.forbidden()
        assertEquals(403, forbiddenRes.statusCode)
        assertEquals("Forbidden", forbiddenRes.statusText)

        val errorRes = KromiumAssetResponse.error(500, "Internal Server Error")
        assertEquals(500, errorRes.statusCode)
        assertEquals("Internal Server Error", errorRes.statusText)
    }

    @Test
    fun testFromDirectoryAndSpaFallback() {
        val tempDir = File.createTempFile("kromium-test-assets", "")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val indexHtml = File(tempDir, "index.html")
            indexHtml.writeText("<h1>Home</h1>")

            val appJs = File(tempDir, "app.js")
            appJs.writeText("console.log('app');")

            val subDir = File(tempDir, "sub")
            subDir.mkdirs()
            val subIndex = File(subDir, "index.html")
            subIndex.writeText("<h2>Sub Home</h2>")

            val handler = KromiumSchemeHandler.fromDirectory(
                directory = tempDir,
                spaFallback = "index.html",
                defaultHeaders = mapOf("X-App" to "Kromium")
            )

            // Test normal asset fetch
            val res1 = handler.handle(KromiumAssetRequest(url = "app://local/app.js", path = "/app.js"))
            assertNotNull(res1)
            assertEquals(200, res1.statusCode)
            assertEquals("application/javascript; charset=utf-8", res1.mimeType)
            assertEquals("Kromium", res1.headers["X-App"])
            assertEquals("console.log('app');", res1.openStream().bufferedReader().readText())

            // Test root index fetch
            val resRoot = handler.handle(KromiumAssetRequest(url = "app://local/", path = "/"))
            assertNotNull(resRoot)
            assertEquals("<h1>Home</h1>", resRoot.openStream().bufferedReader().readText())

            // Test subdirectory index fetch
            val resSub = handler.handle(KromiumAssetRequest(url = "app://local/sub", path = "/sub"))
            assertNotNull(resSub)
            assertEquals("<h2>Sub Home</h2>", resSub.openStream().bufferedReader().readText())

            // Test SPA fallback on extensionless route
            val resSpa = handler.handle(KromiumAssetRequest(url = "app://local/users/42", path = "/users/42"))
            assertNotNull(resSpa)
            assertEquals(200, resSpa.statusCode)
            assertEquals("<h1>Home</h1>", resSpa.openStream().bufferedReader().readText())

            // Test missing static asset does NOT trigger SPA fallback
            val resMissing = handler.handle(KromiumAssetRequest(url = "app://local/missing.png", path = "/missing.png"))
            assertNotNull(resMissing)
            assertEquals(404, resMissing.statusCode)

            // Test directory traversal blocked
            val resBlocked = handler.handle(KromiumAssetRequest(url = "app://local/../secret.txt", path = "/../secret.txt"))
            assertNotNull(resBlocked)
            assertEquals(403, resBlocked.statusCode)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
