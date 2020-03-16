package ru.fix.gradle.release.plugin

import com.github.zafarkhaja.semver.Version
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*

class InvalidMajorMinorVersion(givenVersion: String): Exception("Please specify valid version in x.y format, current is $givenVersion")

class VersionManager(
        private val git: GitRepository,
        private val userInteractor: UserInteractor) {

    fun assertValidMajorMinorVersion(version: String){
        val pattern = "(\\d+)\\.(\\d+)"
        userInteractor.info("Checking that version '$version' matches pattern '$pattern'")
        if(!Regex(pattern).matches(version)){
            throw InvalidMajorMinorVersion(version)
        }
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
        val versions = getExistingVersionsInDescendingOrder()
                .filter { it.startsWith("$majorVersion.") }

        if (versions.isEmpty()) {
            return "$majorVersion.0"
        }
        return Version.valueOf(versions.first()).incrementPatchVersion().toString()
    }

    fun supposeReleaseVersion(): String {
        val versions = getExistingVersionsInDescendingOrder()
        if (versions.isEmpty()) {
            return "1.0.0"
        }
        return Version.valueOf(versions.first()).incrementPatchVersion().toString()
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