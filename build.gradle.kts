import org.gradle.api.publication.maven.internal.action.MavenInstallAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


buildscript {

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath(Libs.dokkaGradlePlugin)
        classpath(Libs.kotlin_stdlib)
        classpath(Libs.kotlin_jdk8)
        classpath(Libs.kotlin_reflect)
    }
}

/**
 * Project configuration by properties and environment
 */
fun envConfig() = object : ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? =
            if (ext.has(property.name)) {
                ext[property.name] as? String
            } else {
                System.getenv(property.name)
            }
}

val repositoryUser by envConfig()
val repositoryPassword by envConfig()
val repositoryUrl by envConfig()
val signingKeyId by envConfig()
val signingPassword by envConfig()
val signingSecretKeyRingFile by envConfig()


repositories {
    jcenter()
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "${Vers.kotlin}"
    `maven-publish`
    signing

    id("java")
}
apply {
    plugin("org.jetbrains.dokka")
}

group = "ru.fix"

dependencies {
    compile(Libs.kotlin_stdlib)
    compile(gradleApi())
    compile("org.eclipse.jgit:org.eclipse.jgit:4.9.0.201710071750-r")
    compile("com.github.zafarkhaja:java-semver:0.9.0")

    compile("com.jcraft:jsch.agentproxy.jsch:0.0.9")
    compile("com.jcraft:jsch.agentproxy.usocket-jna:0.0.9")
    compile("com.jcraft:jsch.agentproxy.sshagent:0.0.9")
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from("src/main/java")
    from("src/main/kotlin")
}

val dokkaTask by tasks.creating(DokkaTask::class) {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/dokka"
}

val dokkaJar by tasks.creating(Jar::class) {
    classifier = "javadoc"

    from(dokkaTask.outputDirectory)
    dependsOn(dokkaTask)
}


publishing {
    repositories {
        maven {
            url = uri("$repositoryUrl")
            if (url.scheme.startsWith("http", true)) {
                credentials {
                    username = "$repositoryUser"
                    password = "$repositoryPassword"
                }
            }
        }
    }
    publications {

        register("maven", MavenPublication::class) {
            from(components["java"])

            artifact(sourcesJar)
            artifact(dokkaJar)


            pom {
                name.set("${project.group}:${project.name}")
                description.set("Plugin automatically creates branches and tags" +
                        " and changes version in project gradle.properties file.")
                url.set("https://github.com/ru-fix/gradle-release-plugin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("swarmshine")
                        name.set("Evgeniy Lukianov")
                        url.set("https://github.com/elukianov")
                    }
                    developer {
                        id.set("swarmshine")
                        name.set("Kamil Asfandiyarov")
                        url.set("https://github.com/swarmshine")
                    }
                }
                scm {
                    url.set("https://github.com/ru-fix/gradle-release-plugin")
                    connection.set("https://github.com/ru-fix/gradle-release-plugin.git")
                    developerConnection.set("https://github.com/ru-fix/gradle-release-plugin.git")
                }
            }
        }
    }
}

signing {

    if (!signingKeyId.isNullOrEmpty()) {
        project.ext["signing.keyId"] = signingKeyId
        project.ext["signing.password"] = signingPassword
        project.ext["signing.secretKeyRingFile"] = signingSecretKeyRingFile

        logger.info("Signing key id provided. Sign artifacts for $project.")

        isRequired = true
    } else {
        logger.warn("${project.name}: Signing key not provided. Disable signing for  $project.")
        isRequired = false
    }

    sign(publishing.publications)
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        useJUnitPlatform()

        maxParallelForks = 10

        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

}
