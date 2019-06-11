object Vers {
    val kotlin = "1.3.10"
    val dokka = "0.9.16"
    val junit = "5.2.0"
}

object Libs {
    val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokka}"
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"


    val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"

    val mockk = "io.mockk:mockk:1.8.13"


    val jgit = "org.eclipse.jgit:org.eclipse.jgit:5.3.1.201904271842-r"
    val semver = "com.github.zafarkhaja:java-semver:0.9.0"
    val jsch = "com.jcraft:jsch.agentproxy.jsch:0.0.9"
    val jsch_proxy_jna = "com.jcraft:jsch.agentproxy.usocket-jna:0.0.9"
    val jsch_proxy_sshagent = "com.jcraft:jsch.agentproxy.sshagent:0.0.9"
}


