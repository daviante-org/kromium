package org.daviante.kromium.core.download

import java.io.File

/**
 * Utility to generate non-conflicting file names in a given directory.
 */
object KromiumUniqueFileNameGenerator {
    /**
     * Finds a non-conflicting file name by appending (1), (2), etc., if the file already exists.
     */
    fun generate(directory: File, baseName: String): File {
        var file = File(directory, baseName)
        var count = 1
        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }

        while (file.exists()) {
            file = File(directory, "$nameWithoutExt ($count)$ext")
            count++
        }
        return file
    }
}
