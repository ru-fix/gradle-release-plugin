#!/usr/bin/python3
# Script generates travis.yml configuration for github project for Travis
# - encrypts gradle properties from ~/.gradle.properties
# - encrypts ~/gnupg/secring.gpg key store
# - generates `.travis.yml` template
from argparse import ArgumentParser
import subprocess
import os
import re
import jprops
from jinja2 import Template

parser = ArgumentParser()
parser.add_argument("-p", "--project", type=str, help="Project name and local git repository location", required=True)
parser.add_argument("-d", "--description", type=str, help="Project description", required=True)
parser.add_argument("-t", "--travis", help="Create travis file or not", action='store_true')

args = parser.parse_args()
projectLocation = args.project
projectDescription = args.description
travisRequired = args.travis

print(f"Project location: {projectLocation}")
print(f"Project description: {projectDescription}")
print(f"Travis required: {travisRequired}")

if travisRequired:

    print("Use `travis login` if you are running travis client first time")


    def run(popenargs) -> str:
        stdout = subprocess.run(popenargs, stdout=subprocess.PIPE, cwd=projectLocation).stdout.decode('utf-8')
        print(f"RUN>>{stdout}")
        return stdout


    print(f"Travis version: {run(['travis', 'version'])}")

    gradlePropertiesFile = f"{os.getenv('HOME')}/.gradle/gradle.properties"
    print(f"Looking for properties in: {gradlePropertiesFile}")

    properties = []
    with open(gradlePropertiesFile) as file:
        properties = jprops.load_properties(file)

    secureItems = ["repositoryUrl", "repositoryUser", "repositoryPassword", "signingKeyId", "signingPassword"]

    print(f"Found:")
    for item in secureItems:
        print(f"{item}={properties[item]}")

    print("encrypt properties")

    secure = []
    for item in secureItems:
        secure.append("" + run(['travis', 'encrypt', f"{item}={properties[item]}"]).strip())

    print("encrypt secring.gpg")

    secringFile = f"{os.getenv('HOME')}/.gnupg/secring.gpg"
    print(f"Looking for secring.gpg in: {secringFile}")

    secringFile = f"{os.getenv('HOME')}/.gnupg/secring.gpg"
    if not os.path.isfile(secringFile):
        print("secring.gpg not found")
        exit(1)

    secringFileEnc = f"{projectLocation}/secring.gpg.enc"
    if os.path.isfile(secringFileEnc):
        print(f"{secringFileEnc} already exist. Removing it")
        os.remove(secringFileEnc)

    fileEncryptionOutput = run(['travis', 'encrypt-file', secringFile])
    key = re.search('\$encrypted_([^_]+)_key', fileEncryptionOutput).group(1)

    travisTemplateString = """
    language: java
    jdk:
    - openjdk11
    cache:
      directories:
      - "$HOME/.gradle"
    jobs:
      include:
        - stage: build
          if: tag IS blank
          install: skip
          before_script: if [[ $encrypted_{{key}}_key ]]; then openssl aes-256-cbc -K $encrypted_{{key}}_key -iv $encrypted_{{key}}_iv -in secring.gpg.enc -out secring.gpg -d; fi
          script: ./gradlew clean build
    
        - stage: deploy
          if: tag =~ ^\d+\.\d+\.\d+$
          install: skip
          before_script: openssl aes-256-cbc -K $encrypted_{{key}}_key -iv $encrypted_{{key}}_iv -in secring.gpg.enc -out secring.gpg -d
          script: ./gradlew --info clean build publishToSonatype closeAndReleaseRepository
    env:
      global:
      - signingSecretKeyRingFile="`pwd`/secring.gpg"
      {% for item in secure %}
      - secure: {{item}}
      {% endfor %}
    """

    template = Template(travisTemplateString)
    print("* * * .travis.yml * * *")

    renderedTravis = template.render(key=key, secure=secure)
    print(renderedTravis)
    renderedTravisFile = f"{projectLocation}/.travis.yml"

    if not os.path.isfile(renderedTravisFile):
        with open(renderedTravisFile, "w") as out:
            out.write(renderedTravis)


# gradle tempalte
def write(path, tempalte):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.isfile(path):
        print(f"create {path}")
        with open(path, "w") as out:
            template = Template(tempalte)
            rendered = template.render(project=projectLocation, description=projectDescription)
            out.write(rendered)
    else:
        print(f"skip {path}: already exist.")

write(f"{projectLocation}/buildSrc/src/main/kotlin/Dependencies.kt","""
object Vers {
    //Plugins
    const val gradle_release_plugin = "1.3.9"
    const val dokkav = "0.9.18"
    const val asciidoctor = "1.5.9.2"
    
    //Dependencies
    const val kotlin = "1.3.61"
    const val kotlin_coroutines = "1.3.3"
    const val junit = "5.5.2"
    const val log4j =  "2.12.0"
}

object Libs {
    //Plugins
    const val gradle_release_plugin = "ru.fix:gradle-release-plugin:${Vers.gradle_release_plugin}"
    const val dokka_gradle_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"
    const val asciidoctor = "org.asciidoctor:asciidoctor-gradle-plugin:${Vers.asciidoctor}"
    
    //Dependencies
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    const val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"
    const val kotlinx_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Vers.kotlin_coroutines}"

    const val mu_kotlin_logging = "io.github.microutils:kotlin-logging:1.7.6"
    const val log4j_core = "org.apache.logging.log4j:log4j-core:${Vers.log4j}"
    const val slf4j_over_log4j = "org.apache.logging.log4j:log4j-slf4j-impl:${Vers.log4j}"
    
    const val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    const val junit_params = "org.junit.jupiter:junit-jupiter-params:${Vers.junit}"
    const val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"
    //1.9.3 has a bug https://github.com/mockk/mockk/issues/280
    const val mockk = "io.mockk:mockk:1.9.2"
    const val kotlin_test = "io.kotlintest:kotlintest-runner-junit5:3.4.2"
}

enum class Projs{
    `project-name`,
    ;

    val asDependency get(): String = ":$name"
}
""")
write(f"{projectLocation}/buildSrc/build.gradle.kts","""
import org.gradle.kotlin.dsl.*

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    mavenCentral()
}
""")
write(f"{projectLocation}/.gitignore","""
.idea/
*.iml
target/
.gradle/
build/
out/
""")
write(f"{projectLocation}/build.gradle.kts","""
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath(Libs.kotlin_stdlib)
        classpath(Libs.kotlin_jdk8)
        classpath(Libs.kotlin_reflect)

        classpath(Libs.gradle_release_plugin)
        classpath(Libs.dokka_gradle_plugin)
        classpath(Libs.asciidoctor)

    }
}

plugins {
    kotlin("jvm") version "${Vers.kotlin}" apply false
    signing
    `maven-publish`
    id(Libs.nexus_publish_plugin) version "0.4.0" apply false
    id(Libs.nexus_staging_plugin) version "0.21.2"
    id("org.asciidoctor.convert") version Vers.asciidoctor
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
    stagingProfileId = "1f0730098fd259"
    username = "$repositoryUser"
    password = "$repositoryPassword"
    numberOfRetries = 50
    delayBetweenRetriesInMillis = 3_000
}

apply {
    plugin("ru.fix.gradle.release")
}

subprojects {
    group = "ru.fix"

    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("java")
        plugin("org.jetbrains.dokka")
        plugin(Libs.nexus_publish_plugin)
    }

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
        if(!repositoryUrl.isNullOrEmpty()){
            maven(url=repositoryUrl.toString())
        }
    }

    val sourcesJar by tasks.creating(Jar::class) {
        classifier = "sources"
        from("src/main/java")
        from("src/main/kotlin")
    }

    val dokkaTask by tasks.creating(DokkaTask::class){
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/dokka"

        //TODO: wait dokka support JDK11 - https://github.com/Kotlin/dokka/issues/428
        //TODO: wait dokka fix https://github.com/Kotlin/dokka/issues/464
        enabled = false
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
        clientTimeout.set(java.time.Duration.of(3, java.time.temporal.ChronoUnit.MINUTES))
    }

    project.afterEvaluate {
        publishing {
            publications {
                //Internal repository setup
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
    
                create<MavenPublication>("maven") {
                    from(components["java"])
    
                    artifact(sourcesJar)
                    artifact(dokkaJar)
    
                    pom {
                        name.set("${project.group}:${project.name}")
                        description.set("https://github.com/ru-fix/${rootProject.name}")
                        url.set("https://github.com/ru-fix/${rootProject.name}")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("JFix Team")
                                name.set("JFix Team")
                                url.set("https://github.com/ru-fix/")
                            }
                        }
                        scm {
                            url.set("https://github.com/ru-fix/${rootProject.name}")
                            connection.set("https://github.com/ru-fix/${rootProject.name}.git")
                            developerConnection.set("https://github.com/ru-fix/${rootProject.name}.git")
                        }
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
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
            kotlinOptions {
                jvmTarget = "1.8"
        }
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
}
        
tasks {
        withType<AsciidoctorTask> {
            sourceDir = project.file("asciidoc")
            resources(closureOf<CopySpec> {
                from("asciidoc")
                include("**/*.png")
            })
            doLast {
                copy {
                    from(outputDir.resolve("html5"))
                    into(project.file("docs"))
                    include("**/*.html", "**/*.png")
                }
            }
        }
    }
}
""")
write(f"{projectLocation}/settings.gradle.kts","""
rootProject.name = "{{project}}"
include("{{project}}")
""")
write(f"{projectLocation}/{projectLocation}/build.gradle.kts","""
import org.gradle.kotlin.dsl.*


plugins {
    java
    kotlin("jvm")
}

dependencies {
    compile(Libs.slf4j_api)

    testImplementation(Libs.junit_api)
    testRuntimeOnly(Libs.junit_engine)
    testRuntimeOnly(Libs.slf4j_simple)
}
""")
write(f"{projectLocation}/{projectLocation}/src/main/kotlin/Main.kt","""
class Main {
}
""")

write(f"{projectLocation}/{projectLocation}/src/test/kotlin/MainTest.kt","""
import org.junit.jupiter.api.Test

class MainTest {
    @Test
    fun test(){
    }
}
""")
write(f"{projectLocation}/gradle.properties","""
version=1.0-SNAPSHOT
""")