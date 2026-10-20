plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "daviante-org_kromium")
        property("sonar.organization", "daviante-org")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

allprojects {
    group = "org.daviante.kromium"
    version = (project.findProperty("version") as? String) ?: "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    val subproject = this
    if (subproject.path.startsWith(":samples")) {
        afterEvaluate {
            extensions.findByName("sonar")?.let { ext ->
                try {
                    ext::class.java.getMethod("setSkipProject", Boolean::class.javaPrimitiveType).invoke(ext, true)
                } catch (_: Throwable) {
                    try {
                        ext::class.java.getMethod("setIsSkipProject", Boolean::class.javaPrimitiveType).invoke(ext, true)
                    } catch (_: Throwable) {}
                }
            }
        }
    } else {
        apply(plugin = "jacoco")

        tasks.withType<Test> {
            finalizedBy("jacocoTestReport")
        }

        tasks.withType<JacocoReport> {
            reports {
                xml.required.set(true)
                html.required.set(false)
                csv.required.set(false)
            }
        }

        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "R2Local"
                    url = uri(rootProject.layout.buildDirectory.dir("repo"))
                }
            }
        }

        plugins.withId("org.jetbrains.kotlin.jvm") {
            configure<JavaPluginExtension> {
                withSourcesJar()
            }

            configure<PublishingExtension> {
                publications {
                    if (findByName("maven") == null) {
                        create<MavenPublication>("maven") {
                            from(components["java"])
                            pom {
                                name.set(subproject.name)
                                description.set("Kromium - Cross-platform Modern Chromium Engine & Desktop UI Framework for Java")
                                url.set("https://github.com/daviante-org/kromium")
                                licenses {
                                    license {
                                        name.set("Apache-2.0")
                                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    }
                                }
                                developers {
                                    developer {
                                        id.set("daviante")
                                        name.set("Daviante")
                                    }
                                }
                                scm {
                                    connection.set("scm:git:git://github.com/daviante-org/kromium.git")
                                    developerConnection.set("scm:git:ssh://github.com:daviante-org/kromium.git")
                                    url.set("https://github.com/daviante-org/kromium")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
