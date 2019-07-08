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
    - oraclejdk8
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
          script: ./gradlew clean build publish
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
    val kotlin = "1.3.41"
    val sl4j = "1.7.25"
    val dokka = "0.9.18"
    val gradle_release_plugin = "1.3.8"
    val junit = "5.2.0"
    val hamkrest = "1.4.2.2"
}

object Libs {
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"

    val gradle_release_plugin = "ru.fix:gradle-release-plugin:${Vers.gradle_release_plugin}"
    val dokka_gradle_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokka}"

    val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    val slf4j_simple = "org.slf4j:slf4j-simple:${Vers.sl4j}"

    val mockito = "org.mockito:mockito-all:1.10.19"
    val mockito_kotiln = "com.nhaarman:mockito-kotlin-kt1.1:1.5.0"
    val kotlin_logging = "io.github.microutils:kotlin-logging:1.4.9"

    val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    val junit_parametri = "org.junit.jupiter:junit-jupiter-params:${Vers.junit}"
    val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"
    val hamkrest = "com.natpryce:hamkrest:${Vers.hamkrest}"
    val hamcrest = "org.hamcrest:hamcrest-all:1.3"
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
    }
    dependencies {
        classpath(Libs.gradle_release_plugin)
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


plugins {
    kotlin("jvm") version "${Vers.kotlin}" apply false
    signing
    `maven-publish`
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
    }

    repositories {
        jcenter()
        mavenCentral()
    }

    val sourcesJar by tasks.creating(Jar::class) {
        classifier = "sources"
        from("src/main/java")
        from("src/main/kotlin")
    }

    val dokkaTask by tasks.creating(DokkaTask::class){
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
                    description.set("{{project}} {{description}}")
                    url.set("https://github.com/ru-fix/{{project}}")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("swarmshine")
                            name.set("Kamil Asfandiyarov")
                            url.set("https://github.com/swarmshine")
                        }
                    }
                    scm {
                        url.set("https://github.com/ru-fix/{{project}}")
                        connection.set("https://github.com/ru-fix/{{project}}.git")
                        developerConnection.set("https://github.com/ru-fix/{{project}}.git")
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
write(f"{projectLocation}/{projectLocation}/src/main/kotlin/Main.kts","""
class Main {
}
""")

write(f"{projectLocation}/{projectLocation}/src/test/kotlin/MainTest.kts","""
class MainTest {
    @Test
    fun test(){
    }
}
""")
write(f"{projectLocation}/gradle.properties","""
version=1.0-SNAPSHOT
""")