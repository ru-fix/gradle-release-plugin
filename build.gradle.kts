import org.gradle.api.tasks.bundling.Jar


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


publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
        }
    }
}

