plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":kromium-api"))
    implementation(project(":kromium-provider-jcef"))
    implementation(project(":kromium-compose"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

val osName = System.getProperty("os.name")?.lowercase() ?: ""

compose.desktop {
    application {
        mainClass = "org.daviante.kromium.sample.MainKt"
        if (osName.contains("mac")) {
            jvmArgs += listOf(
                "-Xdock:name=Kromium Compose",
                "-Dapple.awt.application.name=Kromium Compose"
            )
        }
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "kromium-sample-compose"
            packageVersion = "2.0.0"
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
