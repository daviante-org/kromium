plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

val javafxVersion = libs.versions.javafx.get()
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val isArm = osArch == "aarch64" || osArch == "arm64"

val platformClassifier = when {
    osName.contains("win") -> if (isArm) "win-aarch64" else "win"
    osName.contains("mac") -> if (isArm) "mac-aarch64" else "mac"
    osName.contains("linux") -> if (isArm) "linux-aarch64" else "linux"
    else -> "linux"
}

dependencies {
    implementation(project(":kromium-api"))
    implementation(project(":kromium-javafx"))
    implementation(project(":kromium-provider-jcef"))

    listOf("base", "graphics", "controls").forEach { module ->
        implementation("org.openjfx:javafx-$module:$javafxVersion:$platformClassifier")
    }

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.javafx)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}

application {
    mainClass.set("org.daviante.kromium.sample.javafx.KromiumJavaFxLauncherKt")
    applicationDefaultJvmArgs = if (osName.contains("mac")) {
        listOf(
            "-Xdock:name=Kromium JavaFX",
            "-Dapple.awt.application.name=Kromium JavaFX"
        )
    } else {
        emptyList()
    }
}
