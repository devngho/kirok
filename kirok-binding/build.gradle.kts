plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

group = "io.github.devngho"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

fun PublishingExtension.kirok() {
    signing {
        sign(publishing.publications)
    }

    repositories {
        mavenLocal()
        val id: String =
            if (project.hasProperty("repoUsername")) project.property("repoUsername") as String
            else System.getenv("repoUsername")
        val pw: String =
            if (project.hasProperty("repoPassword")) project.property("repoPassword") as String
            else System.getenv("repoPassword")
        if (!version.toString().endsWith("SNAPSHOT")) {
            maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                name = "sonatypeReleaseRepository"
                credentials {
                    username = id
                    password = pw
                }
            }
        }
    }

    fun MavenPublication.kirok() {
        pom {
            name.set(artifactId)
            description.set("Frontend Logic Library for Kotlin/Wasm")
            url.set("https://github.com/devngho/kirok")

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

    publications.create<MavenPublication>("maven")  {
        groupId = project.group as String?
        artifactId = "kirok-binding"
        version = project.version as String?

        from(components.getByName("java"))

        kirok()
    }
}

kotlin {
    publishing {
        kirok()
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}