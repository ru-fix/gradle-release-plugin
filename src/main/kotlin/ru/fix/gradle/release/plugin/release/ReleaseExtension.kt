package ru.fix.gradle.release.plugin.release

open class ReleaseExtension {
    var mainBranch: String = "develop"
    var releaseBranchPrefix: String = "releases/release-"
}