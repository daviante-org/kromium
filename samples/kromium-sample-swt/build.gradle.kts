plugins {
    alias(libs.plugins.kotlin.jvm)
    application
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
    implementation(project(":kromium-api"))
    implementation(project(":kromium-swt"))
    implementation(project(":kromium-provider-jcef"))

    implementation(swtArtifact) {
        exclude(group = "org.eclipse.platform", module = "org.eclipse.swt")
    }

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

application {
    mainClass.set("org.daviante.kromium.sample.swt.KromiumSwtAppKt")
    applicationDefaultJvmArgs = if (osName.contains("mac")) {
        listOf(
            "-XstartOnFirstThread",
            "-Dapple.awt.application.name=Kromium SWT"
        )
    } else {
        emptyList()
    }
}
