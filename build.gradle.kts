import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import java.net.URI

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.2.10"
    `maven-publish`
}

dependencies {
    compile(kotlin("stdlib", "1.2.10"))
    compile(gradleApi())
    compile("org.eclipse.jgit:org.eclipse.jgit:4.9.0.201710071750-r")
    compile("com.github.zafarkhaja:java-semver:0.9.0")

    compile("com.jcraft:jsch.agentproxy.jsch:0.0.9")
    compile("com.jcraft:jsch.agentproxy.usocket-jna:0.0.9")
    compile("com.jcraft:jsch.agentproxy.sshagent:0.0.9")
}

val repositoryUser by project
val repositoryPassword by project
val repositoryUrl by project

publishing {
    (publications) {
        if (components.names.contains("java")) {
            logger.info("Register java artifact for project: ${project.name}")

            val sourcesJar by tasks.creating(Jar::class) {
                classifier = "sources"
                from("src/main/java")
                from("src/main/kotlin")
            }

            "${project.name}-mvnPublication"(MavenPublication::class) {

                from(components["java"])
                groupId = "ru.fix"
                artifactId = "gradle-release-plugin"
                artifact(sourcesJar)

                pom.withXml {
                    asNode().apply {
                        appendNode("description", "Gradle release plugin.")
                        appendNode("licenses").appendNode("license").apply {
                            appendNode("name", "The Apache License, Version 2.0")
                            appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }

    }

    repositories {
        maven {
            credentials {
                username = "$repositoryUser"
                password = "$repositoryPassword"
            }
            name = "remoteRepository"
            url = URI("$repositoryUrl")
        }

    }
}
