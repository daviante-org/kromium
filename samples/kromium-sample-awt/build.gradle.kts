plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

val osName = System.getProperty("os.name")?.lowercase() ?: ""

dependencies {
    implementation(project(":kromium-api"))
    implementation(project(":kromium-awt"))
    implementation(project(":kromium-provider-jcef"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

application {
    mainClass.set("org.daviante.kromium.sample.awt.KromiumAwtAppKt")
    applicationDefaultJvmArgs = if (osName.contains("mac")) {
        listOf(
            "-Xdock:name=Kromium AWT",
            "-Dapple.awt.application.name=Kromium AWT"
        )
    } else {
        emptyList()
    }
}
