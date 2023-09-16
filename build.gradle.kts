plugins {
    kotlin("multiplatform") version "1.9.0"
    id("org.jetbrains.dokka") version "1.8.20"
    `maven-publish`
    signing
}

group = "io.github.devngho"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

kotlin {
    publishing {
        signing {
            sign(publishing.publications)
        }

        repositories {
            if (version.toString().endsWith("SNAPSHOT")) {
                maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
                    name = "sonatypeSnapshotRepository"
                    credentials(PasswordCredentials::class)
                }
            } else {
                maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                    name = "sonatypeReleaseRepository"
                    credentials(PasswordCredentials::class)
                }
            }
        }

        publications.withType(MavenPublication::class) {
            groupId = project.group as String?
            artifactId = "kt_kisopenapi"
            version = project.version as String?

            artifact(tasks["javadocJar"])

            pom {
                name.set(artifactId)
                description.set("A Kotlin library for korea stock trading.")
                url.set("https://github.com/devngho/kt_kisopenapi")


                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/devngho/kirok/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("devngho")
                        name.set("devngho")
                        email.set("yjh135908@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/devngho/kirok.git")
                    developerConnection.set("https://github.com/devngho/kirok.git")
                    url.set("https://github.com/devngho/kirok")
                }
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useTestNG()
        }
    }
}

kotlin {
    jvm {
        jvmToolchain(8)
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}
