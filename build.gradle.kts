import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import java.net.URI

buildscript {

    repositories {
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}")
    }
}

repositories {
    jcenter()
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "${Vers.kotlin}"
    `maven-publish`

    id("org.jetbrains.dokka") version "${Vers.dokkav}"

}

dependencies {
    compile(Libs.kotlin_stdlib)
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

//            task dokkaJavadoc(type: org.jetbrains.dokka.gradle.DokkaTask) {
//                outputFormat = 'javadoc'
//                outputDirectory = javadoc.destinationDir
//                inputs.dir 'src/main/kotlin'
//            }
//
//            task javadocJar(type: Jar, dependsOn: dokkaJavadoc) {
//                classifier = 'javadoc'
//                from javadoc.destinationDir
//            }

            val dokkaJavadoc by tasks.creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
                outputFormat = "javadoc"
            }

            val javadocJar by tasks.creating(Jar::class) {
                classifier = "javadoc"
                from(dokkaJavadoc.outputDirectory)
                dependsOn(dokkaJavadoc)
            }

            "${project.name}-mvnPublication"(MavenPublication::class) {

                from(components["java"])
                groupId = "ru.fix"
                artifactId = "gradle-release-plugin"

                artifact(sourcesJar)
                artifact(javadocJar)

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
