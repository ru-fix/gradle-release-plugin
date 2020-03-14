package ru.fix.gradle.release.plugin

open class ReleaseExtension {
    var releaseBranchPrefix = "release/"
    var commitMessageTemplate = "Release v{VERSION}"
    var tagNameTemplate = "{VERSION}"
    var templateVersionMarker = "{VERSION}"

    fun commitMessage(version: String) = commitMessageTemplate.replace(templateVersionMarker, version)
    fun tagName(version: String) = tagNameTemplate.replace(templateVersionMarker, version)
}