plugins {
    alias(libs.plugins.kotlin.jvm)
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
    api(project(":kromium-api"))
    api(project(":kromium-core"))
    compileOnly(project(":kromium-provider-jcef"))

    listOf("base", "graphics", "controls").forEach { module ->
        implementation("org.openjfx:javafx-$module:$javafxVersion:$platformClassifier")
    }

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(project(":kromium-provider-jcef"))
}

kotlin {
    jvmToolchain(17)
}
