import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.authentication.DefaultBasicAuthentication
import java.net.URI


plugins {
    kotlin("jvm") version "1.1.60"
    `maven-publish`

}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib", "1.1.60"))
    compile(gradleApi())
    compile("org.eclipse.jgit:org.eclipse.jgit:4.9.0.201710071750-r")
    compile("com.github.zafarkhaja:java-semver:0.9.0")
}

val repositoryUser by project
val repositoryPassword by project
val repositoryUrl by project

publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
            artifactId = "jfix-release-gradle-plugin"
        }

    }

    repositories{
        maven{
            credentials{
                username = "$repositoryUser"
                password = "$repositoryPassword"
            }

            name = "ru-fix-repo"
            url = URI("$repositoryUrl")
        }

    }
}


