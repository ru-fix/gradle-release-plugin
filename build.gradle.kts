import org.gradle.api.publication.maven.internal.action.MavenInstallAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val groupId = "ru.fix"
val artifactId = "gradle-release-plugin"

buildscript {

    repositories {
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}")
        classpath(Libs.kotlin_stdlib)
        classpath(Libs.kotlin_jre8)
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
    maven
    id("org.jetbrains.dokka") version "${Vers.dokkav}"
    signing

}

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
    if (!signingKeyId.isNullOrEmpty()) {
        ext["signing.keyId"] = signingKeyId
        ext["signing.password"] = signingPassword
        ext["signing.secretKeyRingFile"] = signingSecretKeyRingFile
        isRequired = true
    } else {
        logger.warn("${project.name}: Signing key not provided. Disable signing.")
        isRequired = false
    }

    sign(configurations.archives)
}


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
    dependsOn(tasks.getByName("javadoc"))

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
                        //Sign pom.xml file
                        "beforeDeployment" {
                            signing.signPom(delegate as MavenDeployment)
                        }

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
