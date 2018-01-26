import org.gradle.api.publication.maven.internal.action.MavenInstallAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
    maven
    id("org.jetbrains.dokka") version "${Vers.dokkav}"
    signing

}


val groupId = "ru.fix"
val artifactId = "gradle-release-plugin"

project.group = groupId

dependencies {
    compile(Libs.kotlin_stdlib)
    compile(gradleApi())
    compile("org.eclipse.jgit:org.eclipse.jgit:4.9.0.201710071750-r")
    compile("com.github.zafarkhaja:java-semver:0.9.0")

    compile("com.jcraft:jsch.agentproxy.jsch:0.0.9")
    compile("com.jcraft:jsch.agentproxy.usocket-jna:0.0.9")
    compile("com.jcraft:jsch.agentproxy.sshagent:0.0.9")
}

signing {
    sign(configurations.archives)
}


val repositoryUser by project
val repositoryPassword by project
val repositoryUrl by project

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from("src/main/java")
    from("src/main/kotlin")
}

val dokkaJavadoc by tasks.creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/dokka"
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(dokkaJavadoc)
    classifier = "javadoc"
    from(dokkaJavadoc.outputDirectory)
}

artifacts {
    add("archives", sourcesJar)
    add("archives", javadocJar)
}


tasks {

    "uploadArchives"(Upload::class) {
        dependsOn(javadocJar, sourcesJar)

        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"(
                                "url" to URI("$repositoryUrl")) {
                            "authentication"(
                                    "userName" to "$repositoryUser",
                                    "password" to "$repositoryPassword"
                            )
                        }
                    }

                    pom.project {
                        withGroovyBuilder {
                            "artifactId"("$artifactId")
                            "groupId"("$groupId")
                            "version"("$version")

                            "name"("${groupId}:${artifactId}")
                            "description"("Plugin automatically creates branches and tags" +
                                    " and changes version in project gradle.properties file.")

                            "url"("https://github.com/ru-fix/gradle-release-plugin")

                            "licenses" {
                                "license" {
                                    "name"("The Apache License, Version 2.0")
                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }


                            "developers" {
                                "developer"{
                                    "id"("elukianov")
                                    "name"("Evgeniy Lukianov")
                                    "url"("https://github.com/elukianov")
                                }
                                "developer"{
                                    "id"("swarmshine")
                                    "name"("Kamil Asfandiyarov")
                                    "url"("https://github.com/swarmshine")
                                }
                            }
                            "scm" {
                                "url"("https://github.com/ru-fix/gradle-release-plugin")
                                "connection"("https://github.com/ru-fix/gradle-release-plugin.git")
                                "developerConnection"("https://github.com/ru-fix/gradle-release-plugin.git")
                            }
                        }
                    }
                }
            }
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
