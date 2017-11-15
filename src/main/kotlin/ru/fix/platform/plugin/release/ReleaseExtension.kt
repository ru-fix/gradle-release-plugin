package ru.fix.platform.plugin.release

open class ReleaseExtension {
    var mainBranch: String = "develop"
    var releaseBranchPrefix: String = "releases/release-"
    var tagPrefix: String = "release-"
    var baseVersion: String? = null
}