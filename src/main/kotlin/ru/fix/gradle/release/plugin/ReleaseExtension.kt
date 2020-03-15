package ru.fix.gradle.release.plugin

enum class ReleaseDetection{
    MAJOR_MINOR_FROM_BRANCH_NAME_PATCH_FROM_TAG,
    MAJOR_MINOR_PATCH_FROM_TAG
}

open class ReleaseExtension {
    
    var releaseBranchPrefix = "release/"
    var commitMessageTemplate = "Release v{VERSION}"
    var tagNameTemplate = "{VERSION}"
    var templateVersionMarker = "{VERSION}"
    var nextReleaseDeterminationSchema = ReleaseDetection.MAJOR_MINOR_FROM_BRANCH_NAME_PATCH_FROM_TAG

    fun commitMessage(version: String) = commitMessageTemplate.replace(templateVersionMarker, version)
    fun tagName(version: String) = tagNameTemplate.replace(templateVersionMarker, version)
}