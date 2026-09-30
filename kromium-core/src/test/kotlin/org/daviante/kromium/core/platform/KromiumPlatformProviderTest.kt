package org.daviante.kromium.core.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KromiumPlatformProviderTest {

    @Test
    fun testCurrentPlatformDetection() {
        val platform: KromiumPlatformInfo = KromiumPlatformProvider.instance.current()
        assertNotNull(platform, "Platform provider should resolve a non-null platform")

        assertTrue(
            platform.os in listOf(KromiumOperatingSystem.Windows, KromiumOperatingSystem.MacOS, KromiumOperatingSystem.Linux),
            "Resolved OS must be one of the known supported operating systems"
        )
        assertTrue(
            platform.arch in listOf(KromiumArchitecture.X64, KromiumArchitecture.Arm64),
            "Resolved Architecture must be one of the known architectures"
        )
        assertTrue(
            platform.os.dynamicLibraryExtension.isNotBlank(),
            "Dynamic library extension should be populated for the current OS"
        )
    }

    @Test
    fun testOsSpecificBehaviors() {
        assertEquals(".dll", KromiumOperatingSystem.Windows.dynamicLibraryExtension)
        assertEquals(".dylib", KromiumOperatingSystem.MacOS.dynamicLibraryExtension)
        assertEquals(".so", KromiumOperatingSystem.Linux.dynamicLibraryExtension)

        assertTrue(KromiumOperatingSystem.MacOS.isMacOS)
        assertTrue(KromiumOperatingSystem.Windows.isWindows)
        assertTrue(KromiumOperatingSystem.Linux.isLinux)
    }

    @Test
    fun testMacOsDynamicFrameworkPaths() {
        val tempDir: File = Files.createTempDirectory("macos_framework_test").toFile()
        try {
            val fallbackFramework = KromiumOperatingSystem.MacOS.getFrameworkPath(tempDir, inFrameworks = true)
            assertTrue(fallbackFramework.endsWith("Chromium Embedded Framework.framework"))
            assertTrue(fallbackFramework.replace('\\', '/').contains("Frameworks"))

            val cefServerFrameworksDir = File(tempDir, "Frameworks/cef_server.app/Contents/Frameworks")
            cefServerFrameworksDir.mkdirs()

            val resolvedFramework = KromiumOperatingSystem.MacOS.getFrameworkPath(tempDir, inFrameworks = true).replace('\\', '/')
            assertTrue(resolvedFramework.contains("cef_server.app/Contents/Frameworks"))

            val resolvedBundle = KromiumOperatingSystem.MacOS.getMainBundlePath(tempDir).replace('\\', '/')
            assertTrue(resolvedBundle.contains("cef_server.app/Contents/Frameworks/jcef Helper.app"))

            val resolvedBrowser = KromiumOperatingSystem.MacOS.getBrowserPath(tempDir).replace('\\', '/')
            assertTrue(resolvedBrowser.contains("cef_server.app/Contents/Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper"))

            val args = KromiumOperatingSystem.MacOS.getFixedArgs(tempDir, listOf("--custom-arg=1"))
            assertEquals(4, args.size)
            val argList = args.toList()
            assertTrue(argList[0].startsWith("--framework-dir-path="))
            assertTrue(argList[1].startsWith("--main-bundle-path="))
            assertTrue(argList[2].startsWith("--browser-subprocess-path="))
            assertEquals("--custom-arg=1", argList[3])
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLinuxPathResolution() {
        val tempDir: File = Files.createTempDirectory("linux_path_test").toFile()
        try {
            val defaultHelper = KromiumOperatingSystem.Linux.getBrowserPath(tempDir)
            assertTrue(defaultHelper.replace('\\', '/').endsWith("lib/jcef_helper"))

            val binHelper = File(tempDir, "bin/jcef_helper")
            binHelper.parentFile?.mkdirs()
            binHelper.createNewFile()
            val resolvedHelper = KromiumOperatingSystem.Linux.getBrowserPath(tempDir)
            assertEquals(binHelper.canonicalPath, resolvedHelper)

            val libDir = File(tempDir, "lib")
            libDir.mkdirs()
            val pak = File(libDir, "resources.pak")
            pak.createNewFile()
            val resolvedResources = KromiumOperatingSystem.Linux.getResourcesPath(tempDir)
            assertEquals(libDir.canonicalPath, resolvedResources)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testWindowsPathResolution() {
        val tempDir: File = Files.createTempDirectory("windows_path_test").toFile()
        try {
            val defaultHelper = KromiumOperatingSystem.Windows.getBrowserPath(tempDir)
            assertTrue(defaultHelper.replace('\\', '/').endsWith("bin/jcef_helper.exe"))

            val rootHelper = File(tempDir, "jcef_helper.exe")
            rootHelper.createNewFile()
            val resolvedHelper = KromiumOperatingSystem.Windows.getBrowserPath(tempDir)
            assertEquals(rootHelper.canonicalPath, resolvedHelper)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testServerPathResolution() {
        val tempDir: File = Files.createTempDirectory("server_path_test").toFile()
        try {
            val winServer = File(tempDir, "bin/cef_server.exe").apply {
                parentFile?.mkdirs()
                createNewFile()
            }
            assertEquals(winServer.canonicalPath, KromiumOperatingSystem.Windows.getServerPath(tempDir))

            val linuxServer = File(tempDir, "bin/cef_server").apply {
                parentFile?.mkdirs()
                createNewFile()
            }
            assertEquals(linuxServer.canonicalPath, KromiumOperatingSystem.Linux.getServerPath(tempDir))

            val macServer = File(tempDir, "Frameworks/cef_server.app/Contents/MacOS/cef_server").apply {
                parentFile?.mkdirs()
                createNewFile()
            }
            assertEquals(macServer.canonicalPath, KromiumOperatingSystem.MacOS.getServerPath(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testFixedArgsInclusionOnWindowsAndLinux() {
        val tempDir: File = Files.createTempDirectory("args_test").toFile()
        try {
            val initialArgs = listOf("--enable-logging", "--v=1")

            val winArgs = KromiumOperatingSystem.Windows.getFixedArgs(tempDir, initialArgs)
            assertTrue(winArgs.any { it.startsWith("--browser-subprocess-path=") })
            assertTrue(winArgs.contains("--enable-logging"))

            val linuxArgs = KromiumOperatingSystem.Linux.getFixedArgs(tempDir, initialArgs)
            assertTrue(linuxArgs.any { it.startsWith("--browser-subprocess-path=") })
            assertTrue(linuxArgs.contains("--enable-logging"))

            val alreadyPresent = listOf("--browser-subprocess-path=/custom/path", "--v=1")
            val winArgsIdempotent = KromiumOperatingSystem.Windows.getFixedArgs(tempDir, alreadyPresent)
            assertEquals(1, winArgsIdempotent.count { it.startsWith("--browser-subprocess-path=") })
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

