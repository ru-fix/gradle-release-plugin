package ru.fix.platform.plugin.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CreateReleaseTask : DefaultTask() {

    @TaskAction
    fun createRelease() {

        //Проверить что мы в правильном бранче





        println("createRelease!!111")
    }

}