package org.daviante.kromium.jcef.bootstrap

import org.daviante.kromium.core.platform.KromiumOperatingSystem
import org.daviante.kromium.core.platform.KromiumPlatformProvider
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JcefEngineRegistryTest {

    @Test
    fun testDefaultInstallDir() {
        val dir = JcefEngineRegistry.defaultInstallDir()
        assertNotNull(dir, "Default install dir must not be null")
        assertTrue(
            dir.path.contains("jcef", ignoreCase = true) || dir.path.contains("kromium", ignoreCase = true),
            "Install directory path should reflect its purpose"
        )
    }

    @Test
    fun testInstallationMarking() {
        val tempDir = Files.createTempDirectory("kromium-test-registry").toFile()

        try {
            assertFalse(JcefEngineRegistry.isInstalled(tempDir), "Should not be installed initially")

            JcefEngineRegistry.markInstalled(tempDir)

            // Single partial binary should NOT qualify as fully installed
            File(tempDir, "jcef.dll").createNewFile()
            File(tempDir, "libcef.so").createNewFile()
            val platform = KromiumPlatformProvider.instance.current()
            if (platform.os == KromiumOperatingSystem.Windows || platform.os == KromiumOperatingSystem.Linux) {
                assertFalse(JcefEngineRegistry.isInstalled(tempDir), "Should NOT be installed when only one required library is present")
            }

            // Create complete dummy files to simulate a real installation based on OS
            File(tempDir, "libcef.dll").createNewFile()
            File(tempDir, "libjcef.so").createNewFile()
            val macDir = File(tempDir, "Chromium Embedded Framework.framework")
            macDir.mkdirs()

            assertTrue(JcefEngineRegistry.isInstalled(tempDir), "Should be installed after marking and placing complete binaries")

        } finally {
            tempDir.deleteRecursively()
        }
    }
}
