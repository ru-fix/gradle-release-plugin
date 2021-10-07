object Vers {
    val kotlin = "1.5.21"
    val dokka = "0.9.18"
    val junit = "5.6.0"
    val sl4j = "1.7.30"
    val log4j = "2.13.1"
    val jgit = "5.13.0.202109080827-r"
}

object Libs {
    val dokka_gradle_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokka}"
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"

    const val nexus_staging_plugin = "io.codearte.nexus-staging"
    const val nexus_publish_plugin = "de.marcphilipp.nexus-publish"

    val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"

    val kotlin_test = "io.kotlintest:kotlintest-runner-junit5:3.4.2"

    val mockk = "io.mockk:mockk:1.9.3"

    val jgit = "org.eclipse.jgit:org.eclipse.jgit:${Vers.jgit}"
    val jgit_apache_mina_sshd = "org.eclipse.jgit:org.eclipse.jgit.ssh.apache:${Vers.jgit}"

    val semver = "com.github.zafarkhaja:java-semver:0.9.0"


//    val jsch = "com.jcraft:jsch.agentproxy.jsch:0.0.9"
//    val jsch_proxy_jna = "com.jcraft:jsch.agentproxy.usocket-jna:0.0.9"
//    val jsch_proxy_sshagent = "com.jcraft:jsch.agentproxy.sshagent:0.0.9"

    val kotlin_logging = "io.github.microutils:kotlin-logging:1.7.8"
    val slf4j_api = "org.slf4j:slf4j-api:${Vers.sl4j}"
    val log4j_core = "org.apache.logging.log4j:log4j-core:${Vers.log4j}"
    val slf4j_over_log4j = "org.apache.logging.log4j:log4j-slf4j-impl:${Vers.log4j}"
}


