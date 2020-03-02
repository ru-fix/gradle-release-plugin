object Vers {
    val kotlin = "1.3.61"
    val dokka = "0.9.18"
    val junit = "5.6.0"
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

    //1.9.3 has a bug
    //https://github.com/mockk/mockk/issues/280
    val mockk = "io.mockk:mockk:1.9.2"

    val jgit = "org.eclipse.jgit:org.eclipse.jgit:5.6.1.202002131546-r"

    val semver = "com.github.zafarkhaja:java-semver:0.9.0"
    val jsch = "com.jcraft:jsch.agentproxy.jsch:0.0.9"
    val jsch_proxy_jna = "com.jcraft:jsch.agentproxy.usocket-jna:0.0.9"
    val jsch_proxy_sshagent = "com.jcraft:jsch.agentproxy.sshagent:0.0.9"

}


