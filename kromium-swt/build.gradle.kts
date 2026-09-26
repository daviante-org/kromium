plugins {
    alias(libs.plugins.kotlin.jvm)
}

val swtVersion = libs.versions.swt.get()
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val isArm = osArch == "aarch64" || osArch == "arm64"

val swtArtifact = when {
    osName.contains("win") -> "org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:$swtVersion"
    osName.contains("mac") -> if (isArm) "org.eclipse.platform:org.eclipse.swt.cocoa.macosx.aarch64:$swtVersion" else "org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:$swtVersion"
    osName.contains("linux") -> if (isArm) "org.eclipse.platform:org.eclipse.swt.gtk.linux.aarch64:$swtVersion" else "org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:$swtVersion"
    else -> "org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64:$swtVersion"
}

dependencies {
    api(project(":kromium-api"))
    api(project(":kromium-core"))
    compileOnly(project(":kromium-provider-jcef"))

    api(swtArtifact) {
        exclude(group = "org.eclipse.platform", module = "org.eclipse.swt")
    }

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(project(":kromium-provider-jcef"))
}

kotlin {
    jvmToolchain(17)
}
