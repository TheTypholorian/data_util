package net.typho.data_util

class DataReadException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)

    constructor(message: String, input: String, index: Int) : this("$message on ${lineAndColumn(input, index)}")

    companion object {
        @JvmStatic
        fun lineAndColumn(input: String, index: Int): String {
            var line = 0
            var column = 0

            repeat(index) { i ->
                val char = input[i]

                if (char == '\n') {
                    line++
                    column = 0
                } else {
                    column++
                }
            }

            return "line $line, column $column (character '${input[index]}')"
        }
    }
}