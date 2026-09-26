plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    api(project(":kromium-api"))
    api(project(":kromium-core"))
    compileOnly(project(":kromium-provider-jcef"))

    testImplementation(libs.kotlin.test)
    testImplementation(project(":kromium-provider-jcef"))
}

kotlin {
    jvmToolchain(17)
}
