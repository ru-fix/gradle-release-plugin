package ru.fix.gradle.release.plugin

import mu.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.io.path.exists
import kotlin.io.path.readLines

private val log = KotlinLogging.logger {  }

class JournaledTestUserInteractor : UserInteractor {
    val messages = ConcurrentLinkedDeque<String>()
    val userAnswers = ConcurrentLinkedDeque<String>()

    fun clear() {
        messages.clear()
    }

    fun populateAnswersFromFile(file: Path){
        if(!file.exists()){
            log.warn { "User answers file not exist: $file" }
            return
        } else {
            log.info { "Read user answers from: $file" }
            for (line in file.readLines()) {
                addUserAnswer(line)
            }
        }
    }

    fun addUserAnswer(answer: String) {
        userAnswers.addLast(answer)
    }

    override fun promptQuestion(prompt: String, default: String?): String {
        log.info { "promptQuestion: $prompt" }
        messages.addLast(prompt)
        return userAnswers.removeFirst()
    }

    override fun promptPassword(prompt: String): CharArray {
        log.info { "promptPassword: $prompt" }
        messages.addLast(prompt)
        return userAnswers.removeFirst().toCharArray()
    }

    override fun info(msg: String) {
        messages.addLast(msg)
    }

    override fun error(msg: String) {
        messages.addLast(msg)
    }
}