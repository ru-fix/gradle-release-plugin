package ru.fix.platform.plugin.release

import com.github.zafarkhaja.semver.Version
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


class VersionUtils {
    companion object {

        fun isValidVersion(version: String): Boolean = Regex("(\\d+)\\.(\\d+)").matches(version)

        fun supposeMajorVersion(): String {
            val versions = getExistingVersions()
            return if (versions.isEmpty()) {
                "1.0"
            } else {
                var version = Version.valueOf(versions[0])
                version = version.incrementMajorVersion()
                "${version.majorVersion}.${version.minorVersion}"
            }
        }

        fun supposePatchVersion(majorVersion: String): String {
            val minorVersions = getExistingVersions()
                    .filter { it.startsWith("$majorVersion.") }

            return if (minorVersions.isEmpty()) {
                "$majorVersion.1"
            } else {
                val lastMinor = Version.valueOf(minorVersions[0])
                lastMinor.incrementPatchVersion()
                lastMinor.toString()
            }
        }

        fun majorVersionExists(majorVersion: String): Boolean =
                getExistingVersions().find { it.startsWith("$majorVersion.") } != null


        fun updateVersionInFile(filename: String, version: String) {
            println("Updating file $filename to version $version")
            val props = Properties()
            props.load(FileInputStream(filename))
            if (props.getProperty("version") != null) {
                props.setProperty("version", version)
            } else {
                println("There isn't version property, skipping")
            }

            props.store(FileOutputStream(filename),"")
        }


        private fun getExistingVersions(): List<String> {
            return GitHolder.git
                    .tagList()
                    .call()
                    .map { checkNotNull(it.name) }
                    .filter { Regex("(\\d+)\\.(\\d+).(\\d+)").matches(it) }
                    .sortedByDescending { Version.valueOf(it) }
        }


    }
}