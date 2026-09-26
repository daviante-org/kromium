plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":kromium-core"))
    
    implementation(libs.kotlinx.coroutines.core)
    
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(17)
}
