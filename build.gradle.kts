import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import java.net.URI

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.1.61"
    `maven-publish`
}

dependencies {
    compile(kotlin("stdlib", "1.1.61"))
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
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
            groupId = "ru.fix"
            artifactId = "gradle-release-plugin"
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
