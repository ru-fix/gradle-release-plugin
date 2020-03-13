package ru.fix.gradle.release.plugin

import java.util.concurrent.ConcurrentLinkedDeque

class JournaledTestUserInteractor : UserInteractor {
    private val messages = ConcurrentLinkedDeque<String>()
    private val userAnswers = ConcurrentLinkedDeque<String>()

    fun clear() {
        messages.clear()
    }

    fun addUserAnswer(answer: String) {
        userAnswers.addLast(answer)
    }

    val journal: ConcurrentLinkedDeque<String>
        get() = messages

    override fun promptQuestion(prompt: String, default: String?): String {
        messages.addLast(prompt)
        return userAnswers.removeFirst()
    }

    override fun promptPassword(prompt: String): CharArray {
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