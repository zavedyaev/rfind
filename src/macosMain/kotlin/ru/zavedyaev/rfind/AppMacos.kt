package ru.zavedyaev.rfind

/**
 * rfind name
 * rfind /asd "name.*"
 * rfind "/asd/asd asd" "name.*"
 */
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val argsCount = args.size
    if (argsCount == 0 || argsCount > 3) {
        printHelp()
        return
    }

    val firstArg = args.first()
    if (argsCount == 1 && firstArg == HELP_ARG) {
        printHelp()
        return
    }

    val flags = ArgFlag.parseFromArg(firstArg)

    val regex: String
    val path: String
    if (flags.isEmpty()) {
        regex = if (argsCount == 1) firstArg else args.last()
        path = if (argsCount == 1) DirectoryUtils.getCurrentDirectory() else firstArg
    } else {
        regex = if (argsCount == 2) args[1] else args.last()
        path = if (argsCount == 2) DirectoryUtils.getCurrentDirectory() else args[1]
    }

    DirectoryUtils.recursiveSearch(
        path,
        flags.contains(ArgFlag.D),
        !flags.contains(ArgFlag.F),
        flags.contains(ArgFlag.P),
        regex.toRegex()
    )
}

const val HELP_ARG = "--help"

fun printHelp() {
    println(
        """
        NAME
            rfind -- recursive regex find

        SYNOPSIS
            rfind [-dfp] [directory_path] file_name_or_regex

        DESCRIPTION
            Will find all files and directories which are matching to a specified regex expression.
            By default it will search for matches only in file names (without path) recursive in current directory

            The following options are available:

            -d      additionally search for matches in directories

            -f      do not search for matches in file names

            -p      use full relative file/directory path for matches

            --help  print this manual
    """.trimIndent()
    )
}

enum class ArgFlag {
    D,
    F,
    P;

    companion object {
        fun parseFromArg(arg: String): Collection<ArgFlag> {
            if (!arg.startsWith("-")) {
                return emptyList()
            }

            val argWithoutMinus = arg.substring(1).toUpperCase()

            val result = ArrayList<ArgFlag>(3)

            var remainedArg = argWithoutMinus
            ArgFlag.values().forEach { flagEnum ->
                if (remainedArg.contains(flagEnum.name)) {
                    result.add(flagEnum)
                    remainedArg = remainedArg.replaceFirst(flagEnum.name, "")
                }
            }
            return if (remainedArg == "") result
            else emptyList()
        }
    }
}

