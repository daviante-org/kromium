plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":kromium-api"))
    implementation(project(":kromium-swing"))
    implementation(project(":kromium-provider-jcef"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

val osName = System.getProperty("os.name")?.lowercase() ?: ""

application {
    mainClass.set("org.daviante.kromium.sample.swing.MainKt")
    applicationDefaultJvmArgs = if (osName.contains("mac")) {
        listOf(
            "-Xdock:name=Kromium Swing",
            "-Dapple.awt.application.name=Kromium Swing"
        )
    } else {
        emptyList()
    }
}
