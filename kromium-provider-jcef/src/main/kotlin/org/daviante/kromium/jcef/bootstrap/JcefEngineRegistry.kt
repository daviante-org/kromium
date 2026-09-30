package org.daviante.kromium.jcef.bootstrap

import org.daviante.kromium.core.platform.KromiumOperatingSystem
import org.daviante.kromium.core.platform.KromiumPlatformProvider
import org.daviante.kromium.core.util.KromiumFileUtils
import java.io.File

object JcefEngineRegistry {

    private const val LOCK_FILE_NAME = "install.lock"
    const val DEFAULT_ENGINE_FOLDER = "jcef-150-b11"

    @JvmStatic
    fun defaultInstallDir(): File {
        val customProp = System.getProperty("kromium.install.dir")
        if (!customProp.isNullOrBlank() && !customProp.contains("..")) {
            val f = File(customProp).canonicalFile
            if (!f.path.contains("..")) {
                return f
            }
        }

        val platform = KromiumPlatformProvider.instance.current()
        val rawHome = System.getProperty("user.home") ?: "."
        if (rawHome.contains("..")) {
            return File(".kromium/$DEFAULT_ENGINE_FOLDER").canonicalFile
        }
        val homeDir = File(rawHome).canonicalFile
        if (homeDir.path.contains("..")) {
            return File(".kromium/$DEFAULT_ENGINE_FOLDER").canonicalFile
        }

        val dotKromium = KromiumFileUtils.resolveChild(homeDir, ".kromium") ?: homeDir
        val baseDir = when (platform.os) {
            KromiumOperatingSystem.Windows -> {
                val rawAppData = System.getenv("APPDATA")
                val appDataDir = if (!rawAppData.isNullOrBlank() && !rawAppData.contains("..")) {
                    val f = File(rawAppData).canonicalFile
                    if (!f.path.contains("..")) f else null
                } else null
                if (appDataDir != null) {
                    val kDir = File(appDataDir, "Kromium").canonicalFile
                    if (kDir.canonicalPath.startsWith(appDataDir.canonicalPath)) kDir else dotKromium
                } else {
                    dotKromium
                }
            }
            KromiumOperatingSystem.MacOS -> {
                val appSupport = File(homeDir, "Library/Application Support").canonicalFile
                if (appSupport.canonicalPath.startsWith(homeDir.canonicalPath)) {
                    val kDir = File(appSupport, "Kromium").canonicalFile
                    if (kDir.canonicalPath.startsWith(appSupport.canonicalPath)) kDir else homeDir
                } else {
                    dotKromium
                }
            }
            KromiumOperatingSystem.Linux -> {
                val rawXdg = System.getenv("XDG_DATA_HOME")
                val xdgDir = if (!rawXdg.isNullOrBlank() && !rawXdg.contains("..")) {
                    val f = File(rawXdg).canonicalFile
                    if (!f.path.contains("..")) f else null
                } else null
                val localShare = KromiumFileUtils.resolveChild(homeDir, ".local/share/kromium") ?: homeDir
                if (xdgDir != null) {
                    val kDir = File(xdgDir, "kromium").canonicalFile
                    if (kDir.canonicalPath.startsWith(xdgDir.canonicalPath)) kDir else localShare
                } else {
                    localShare
                }
            }
        }
        val target = File(baseDir, DEFAULT_ENGINE_FOLDER).canonicalFile
        return if (target.canonicalPath.startsWith(baseDir.canonicalPath)) {
            target
        } else {
            File(homeDir, ".kromium/$DEFAULT_ENGINE_FOLDER").canonicalFile
        }
    }

    @JvmStatic
    fun isInstalled(installDir: File): Boolean {
        val safeDir = KromiumFileUtils.sanitizeDirectory(installDir) ?: return false
        if (!safeDir.exists() || !safeDir.isDirectory) return false

        val platform = KromiumPlatformProvider.instance.current()
        fun checkFile(relative: String): Boolean {
            val file = KromiumFileUtils.resolveChild(safeDir, relative) ?: return false
            return file.exists()
        }

        val hasBinaries = when (platform.os) {
            KromiumOperatingSystem.Windows -> {
                (checkFile("jcef.dll") || checkFile("bin/jcef.dll")) &&
                    (checkFile("libcef.dll") || checkFile("bin/libcef.dll"))
            }
            KromiumOperatingSystem.MacOS -> {
                KromiumOperatingSystem.MacOS.ensureMacFrameworkLinks(safeDir)
                checkFile("Chromium Embedded Framework.framework") ||
                    checkFile("Frameworks/Chromium Embedded Framework.framework") ||
                    checkFile("Frameworks/cef_server.app/Contents/Frameworks/Chromium Embedded Framework.framework")
            }
            KromiumOperatingSystem.Linux -> {
                (checkFile("libcef.so") || checkFile("lib/libcef.so")) &&
                    (checkFile("libjcef.so") || checkFile("lib/libjcef.so"))
            }
        }
        if (!hasBinaries) return false

        val lock = KromiumFileUtils.resolveChild(safeDir, LOCK_FILE_NAME)
        if (lock == null || !lock.exists()) {
            // Framework binaries are present on disk; self-heal install.lock to prevent unnecessary re-download
            try {
                markInstalled(safeDir)
            } catch (_: Throwable) {}
        }

        return true
    }

    @JvmStatic
    fun markInstalled(installDir: File) {
        val safeDir = KromiumFileUtils.sanitizeDirectory(installDir)
            ?: throw IllegalArgumentException("Invalid or unsafe install directory: ${installDir.path}")
        val lock = KromiumFileUtils.resolveChild(safeDir, LOCK_FILE_NAME)
            ?: throw IllegalStateException("Cannot resolve lock file in: ${safeDir.path}")
        val metadata = """
            {
                "jcefVersion": "${JcefVersionConstants.JCEF_VERSION}",
                "cefVersion": "${JcefVersionConstants.CEF_VERSION}",
                "chromiumVersion": "${JcefVersionConstants.CHROMIUM_VERSION}",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        lock.writeText(metadata)

        val platform = KromiumPlatformProvider.instance.current()
        if (platform.os.isMacOS) {
            KromiumOperatingSystem.MacOS.ensureMacFrameworkLinks(safeDir)
            KromiumFileUtils.removeMacQuarantine(safeDir)
        }
    }

    @JvmStatic
    fun clearInstallation(installDir: File) {
        KromiumFileUtils.deleteDirectory(installDir)
    }
}
