package net.typho.tv_lib.io

import java.io.InputStream
import java.io.OutputStream

interface DataFileFormat<D, P> {
    val extension: String
    val serializers: MutableList<DataObjectSerializer<Any>>

    fun read(input: D): P

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun read(input: InputStream): P

    fun write(data: P): D

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun write(data: P, output: OutputStream)

    companion object {
        @JvmStatic
        fun readEscapeSequences(line: Int?, text: String): String {
            var text = text
            var i = 0

            while (i < text.length) {
                val c = text[i++]

                if (c == '\\') {
                    if (i == text.length) {
                        throw DataFileReadingException("Unterminated escape sequence in string at line $line")
                    }

                    var endIndex = i + 1
                    val escaped = when (val c1 = text[i]) {
                        '"', '\\', '/' -> c1
                        'b' -> '\b'
                        'f' -> '\u000c'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'u' -> {
                            endIndex += 4
                            text.substring(i + 1, endIndex).toInt(16).toChar()
                        }
                        else -> throw DataFileReadingException("Invalid escape sequence '\\$c1' at line $line")
                    }

                    text = text.substring(0, i - 1) + escaped + text.substring(endIndex)
                }
            }

            return text
        }

        @JvmStatic
        fun writeEscapeSequences(text: String): String {
            var text = text
            var i = 0

            while (i < text.length) {
                val s = when (val c = text[i]) {
                    '"', '\\', '/' -> "\\$c"
                    '\b' -> "\\b"
                    '\u000c' -> "\\f"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> if (c.isISOControl() || !c.isDefined() || c.category == CharCategory.PRIVATE_USE || c.category == CharCategory.FORMAT) {
                        "\\u" + c.code.toString(16).padStart(4, '0')
                    } else null
                }

                if (s == null) {
                    i++
                } else {
                    text = text.substring(0, i) + s + text.substring(i + 1)
                    i += s.length
                }
            }

            return text
        }

        @JvmStatic
        fun trimQuotes(text: String): String {
            return if (text.startsWith('"') && text.endsWith('"')) {
                text.substring(1, text.length - 1)
            } else if (text.startsWith('\'') && text.endsWith('\'')) {
                text.substring(1, text.length - 1)
            } else {
                text
            }
        }
    }
}