package ru.fix.gradle.release.plugin

open class ReleaseExtension {
    var mainBranch: String = "master"
    var releaseBranchPrefix: String = "releases/"
}