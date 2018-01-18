rootProject.name = "gradle-release-plugin"

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.dokka")) {
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        // repository ordering matters because of broken POM mentioned in
        // https://github.com/Kotlin/dokka/issues/146#issuecomment-350272436
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }
}