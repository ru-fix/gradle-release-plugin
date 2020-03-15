package ru.fix.gradle.release.plugin

import com.github.zafarkhaja.semver.Version
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*


class VersionManager(
        private val git: GitRepository,
        private val userInteractor: UserInteractor) {

    fun isValidBranchVersion(version: String): Boolean {
        val pattern = "(\\d+)\\.(\\d+)"
        userInteractor.info("Checking that version '$version' matches pattern '$pattern'")
        return Regex(pattern).matches(version)
    }


    fun supposeBranchVersion(): String {
        val versions = getExistingVersionsInDescendingOrder()
        return if (versions.isEmpty()) {
            "1.0"
        } else {
            var version = Version.valueOf(versions.first())
            version = version.incrementMinorVersion()
            "${version.majorVersion}.${version.minorVersion}"
        }
    }

    fun supposeReleaseVersion(majorVersion: String): String {
        val minorVersions = getExistingVersionsInDescendingOrder()
                .filter { it.startsWith("$majorVersion.") }

        return if (minorVersions.isEmpty()) {
            "$majorVersion.0"
        } else {
            var lastMinor = Version.valueOf(minorVersions[0])
            lastMinor = lastMinor.incrementPatchVersion()
            lastMinor.toString()
        }
    }

    fun branchVersionExists(majorVersion: String): Boolean =
            getExistingVersionsInDescendingOrder().find { it.startsWith("$majorVersion.") } != null


    fun updateVersionInFile(filename: Path, version: String) {
        userInteractor.info("Updating file $filename to version $version")
        val props = Properties()
        props.load(FileInputStream(filename.toFile()))
        if (props.getProperty("version") != null) {
            props.setProperty("version", version)
        } else {
            userInteractor.info("There is no 'version' property in '$filename', skipping")
        }

        props.store(FileOutputStream(filename.toFile()), "")
    }

    fun extractVersionFromBranch(name: String): String = Regex(".*((\\d+)\\.(\\d+))$")
            .matchEntire(name)!!.groups[1]!!.value


    private fun getExistingVersionsInDescendingOrder(): List<String> {
        return git.listTags()
                .filter { Regex("(\\d+)\\.(\\d+).(\\d+)").matches(it) }
                .sortedByDescending { Version.valueOf(it) }
    }


}