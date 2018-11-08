package ru.zavedyaev.rfind

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.*

class DirectoryUtils {
    companion object {
        @ExperimentalUnsignedTypes
        fun getCurrentDirectory(): String {
            return getcwd(null, ULong.MAX_VALUE)?.toKString()
                ?: throw IllegalStateException("Cannot find current directory")
        }

        /**
         * will print all entries which were matched to regex
         * in case matchFolders == false, we will not print matched folders
         * in case matchFiles == false, we will not print matched files
         * in case matchFullPath == true we will use fullPath (relative) to match
         */
        @ExperimentalUnsignedTypes
        fun recursiveSearch(
            directoryPath: String, matchDirectories: Boolean, matchFiles: Boolean,
            matchFullPath: Boolean, regex: Regex
        ) {
            try {
                Directory(directoryPath).forEachEntry { entry ->
                    when (entry.type) {
                        EntryType.DIRECTORY -> {
                            val directoryName = entry.name
                            if (directoryName != "." && directoryName != "..") {
                                val path = "$directoryPath/$directoryName"

                                if (matchDirectories) {
                                    if (isMatches(directoryName, path, matchFullPath, regex)) {
                                        println(path)
                                    }
                                }

                                recursiveSearch(path, matchDirectories, matchFiles, matchFullPath, regex)
                            } //else - ignore
                        }
                        EntryType.FILE -> {
                            val fileName = entry.name
                            val filePath = "$directoryPath/$fileName"

                            if (matchFiles) {
                                if (isMatches(fileName, filePath, matchFullPath, regex)) {
                                    println(filePath)
                                }
                            }
                        }
                    }
                }
            } catch (ex: CannotOpenDirectoryException) {
                println("Cannot open directory: ${ex.message}")
            }
        }

        private fun isMatches(name: String, nameWithPath: String, matchFullPath: Boolean, regex: Regex): Boolean {
            return if (matchFullPath) {
                regex.containsMatchIn(nameWithPath)
            } else {
                regex.containsMatchIn(name)
            }
        }
    }
}

class Directory(
    private val path: String //relative
) {
    @ExperimentalUnsignedTypes
    fun withOpenedDirectory(code: (CPointer<DIR>) -> Unit) {
        val directory = opendir(path) ?: throw CannotOpenDirectoryException(path)
        try {
            code(directory)
        } finally {
            closedir(directory)
        }
    }

    @ExperimentalUnsignedTypes
    fun forEachEntry(code: (Entry) -> Unit) {
        withOpenedDirectory { dir ->
            EntryIterator(dir, path).forEach { entry ->
                code(entry)
            }
        }
    }
}

class CannotOpenDirectoryException(path: String) : Exception(path)

class EntryIterator(
    private val directoryPointer: CPointer<DIR>,
    private val path: String //relative
) : Iterator<Entry> {
    private var entry: CPointer<dirent>? = readdir(directoryPointer)

    override fun hasNext(): Boolean {
        return entry != null
    }

    @ExperimentalUnsignedTypes
    override fun next(): Entry {
        val notNullEntry = entry ?: throw IllegalStateException("entry pointer should not be null here")
        val entryName = notNullEntry.pointed.d_name.toKString()

        val result = Entry(
            entryName,
            "$path/$entryName",
            EntryType.parse(notNullEntry)
        )

        entry = readdir(directoryPointer)
        return result
    }

}

data class Entry(
    val name: String,
    val pathWithName: String, //relative
    val type: EntryType
)

enum class EntryType {
    DIRECTORY,
    FILE;

    companion object {
        @ExperimentalUnsignedTypes
        fun parse(entryPointer: CPointer<dirent>): EntryType {
            return if (entryPointer.pointed.d_type.toInt() == DT_DIR) {
                DIRECTORY
            } else {
                FILE
            }
        }
    }
}