package ru.fix.gradle.release.plugin

open class ReleaseExtension {
    var mainBranch: String = "develop"
    var releaseBranchPrefix: String = "releases/release-"
}