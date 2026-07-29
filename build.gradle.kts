plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

fun registerVersionBumpTask(
    taskName: String,
    taskDescription: String,
    bump: (major: Int, minor: Int, patch: Int) -> Triple<Int, Int, Int>,
) {
    tasks.register(taskName) {
        group = "versioning"
        description = taskDescription

        doLast {
            val versionFile = rootProject.file("gradle.properties")
            val lines = versionFile.readLines()

            var currentCode: Int? = null
            var currentName: String? = null

            lines.forEach { line ->
                when {
                    line.startsWith("VERSION_CODE=") -> currentCode = line.substringAfter("=").toIntOrNull()
                    line.startsWith("VERSION_NAME=") -> currentName = line.substringAfter("=")
                }
            }

            val versionCode = currentCode ?: error("VERSION_CODE is missing or invalid in gradle.properties")
            val versionName = currentName ?: error("VERSION_NAME is missing in gradle.properties")
            val parts = versionName.split(".").map { it.toIntOrNull() }

            if (parts.size != 3 || parts.any { it == null }) {
                error("VERSION_NAME must use MAJOR.MINOR.PATCH format, for example 1.3.0")
            }

            val (major, minor, patch) = bump(parts[0]!!, parts[1]!!, parts[2]!!)
            val nextCode = versionCode + 1
            val nextName = "$major.$minor.$patch"

            val updated = lines.map { line ->
                when {
                    line.startsWith("VERSION_CODE=") -> "VERSION_CODE=$nextCode"
                    line.startsWith("VERSION_NAME=") -> "VERSION_NAME=$nextName"
                    else -> line
                }
            }

            versionFile.writeText(updated.joinToString(System.lineSeparator()) + System.lineSeparator())
            println("Version bumped: $versionName ($versionCode) -> $nextName ($nextCode)")
        }
    }
}

registerVersionBumpTask(
    taskName = "bumpPatch",
    taskDescription = "Increment VERSION_NAME patch and VERSION_CODE.",
) { major, minor, patch -> Triple(major, minor, patch + 1) }

registerVersionBumpTask(
    taskName = "bumpMinor",
    taskDescription = "Increment VERSION_NAME minor, reset patch, and increment VERSION_CODE.",
) { major, minor, _ -> Triple(major, minor + 1, 0) }

registerVersionBumpTask(
    taskName = "bumpMajor",
    taskDescription = "Increment VERSION_NAME major, reset minor and patch, and increment VERSION_CODE.",
) { major, _, _ -> Triple(major + 1, 0, 0) }
