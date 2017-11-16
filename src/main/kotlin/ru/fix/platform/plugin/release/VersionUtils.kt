package ru.fix.platform.plugin.release

import com.github.zafarkhaja.semver.Version
import org.gradle.api.logging.Logging
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


class VersionUtils {

    companion object {

        private val logger = Logging.getLogger("GitUtils")!!

        fun isValidBranchVersion(version: String): Boolean {
            println("Checking $version")
            return Regex("(\\d+)\\.(\\d+)").matches(version)
        }


        fun supposeBranchVersion(): String {
            val versions = getExistingVersions()
            return if (versions.isEmpty()) {
                "1.0"
            } else {
                var version = Version.valueOf(versions[0])
                version = version.incrementMinorVersion()
                "${version.majorVersion}.${version.minorVersion}"
            }
        }

        fun supposeReleaseVersion(majorVersion: String): String {
            val minorVersions = getExistingVersions()
                    .filter { it.startsWith("$majorVersion.") }

            return if (minorVersions.isEmpty()) {
                "$majorVersion.1"
            } else {
                var lastMinor = Version.valueOf(minorVersions[0])
                lastMinor = lastMinor.incrementPatchVersion()
                lastMinor.toString()
            }
        }

        fun branchVersionExists(majorVersion: String): Boolean =
                getExistingVersions().find { it.startsWith("$majorVersion.") } != null


        fun updateVersionInFile(filename: String, version: String) {
            logger.lifecycle("Updating file $filename to version $version")
            val props = Properties()
            props.load(FileInputStream(filename))
            if (props.getProperty("version") != null) {
                props.setProperty("version", version)
            } else {
                logger.lifecycle("There isn't version property, skipping")
            }

            props.store(FileOutputStream(filename), "")
        }

        fun extractVersionFromBranch(name: String): String = Regex(".*((\\d+)\\.(\\d+))$")
                .matchEntire(name)!!.groups[1]!!.value


        private fun getExistingVersions(): List<String> {

            return GitHolder.git

                    .tagList()
                    .call()
                    .map { checkNotNull(it.name) }
                    .map { it.replace("refs/tags/", "") }
                    .filter { Regex("(\\d+)\\.(\\d+).(\\d+)").matches(it) }
                    .sortedByDescending { Version.valueOf(it) }
        }


    }

}