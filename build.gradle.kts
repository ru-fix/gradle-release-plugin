import de.marcphilipp.gradle.nexus.NexusPublishExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(Libs.dokka_gradle_plugin)
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

nexusStaging {
    packageGroup = "ru.fix"
    username = "$repositoryUser"
    password = "$repositoryPassword"
    numberOfRetries = 50
    delayBetweenRetriesInMillis = 3_000
}

repositories {
    jcenter()
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "${Vers.kotlin}"
    signing
    `maven-publish`
    id(Libs.nexus_publish_plugin) version "0.4.0"
    id(Libs.nexus_staging_plugin) version "0.21.2"
}

apply {
    plugin("org.jetbrains.dokka")
    plugin("maven-publish")
    plugin("signing")
    plugin("java")
    plugin(Libs.nexus_publish_plugin)
}

group = "ru.fix"

dependencies {
    api(Libs.kotlin_stdlib)
    api(Libs.kotlin_jdk8)
    api(Libs.kotlin_reflect)

    api(gradleApi())

    api(Libs.jgit)
    api(Libs.jgit_apache_mina_sshd)
    api(Libs.semver)

    testApi(Libs.junit_api)
    testApi(Libs.mockk)
    testApi(Libs.kotlin_test)
    testApi(Libs.kotlin_logging)
    testImplementation(Libs.junit_engine)
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from("src/main/java")
    from("src/main/kotlin")
}

val dokkaTask = tasks.getByName<DokkaTask>("dokkaJavadoc") {
    outputDirectory.set(buildDir.resolve("dokka"))
}

val dokkaJar by tasks.creating(Jar::class) {
    classifier = "javadoc"

    from(dokkaTask.outputDirectory)
    dependsOn(dokkaTask)
}

configure<NexusPublishExtension> {
    repositories {
        sonatype {
            username.set("$repositoryUser")
            password.set("$repositoryPassword")
            useStaging.set(true)
        }
    }
    clientTimeout.set(Duration.of(3, ChronoUnit.MINUTES))
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

        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(sourcesJar)
            artifact(dokkaJar)


            pom {
                name.set("${project.group}:${project.name}")
                description.set(
                    "Plugin automatically creates branches and tags" +
                            " and changes version in project gradle.properties file."
                )
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
        kotlinOptions.jvmTarget = "11"
    }

    withType<Test> {
        useJUnitPlatform()

        maxParallelForks = 4

        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
