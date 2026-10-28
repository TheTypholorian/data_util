package net.typho.data_util

import net.typho.data_util.codec.Codec
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueOutput

interface DataFormat<V> {
    fun read(input: SerializedData): V

    fun createInput(data: V): ValueInput

    fun <T> read(codec: Codec<T>, input: SerializedData): T = codec.read(createInput(read(input)))

    fun write(data: V): SerializedData

    fun createOutput(): ValueOutput.WithResult<V>

    fun <T> write(codec: Codec<T>, value: T): SerializedData = write(createOutput().also { codec.write(it, value) }.finish())

    companion object {
        /**
         * Checks if a map key is valid (according to .xml and .properties, plus no periods because of .properties object serialization).
         *
         * Valid keys can contain characters `A-Z`, `a-z`, and `_`, and can contain `0-9` and `-` after the first character.
         * Valid keys cannot be empty nor contain whitespaces.
         */
        @JvmStatic
        fun isValidKey(key: String): Boolean {
            if (key.isEmpty()) {
                return false
            }

            key.forEachIndexed { index, char ->
                if (!(char in 'A'..'Z' || char in 'a'..'z' || char == '_')) {
                    return false
                }

                if (index > 0 && !(char in '0'..'9' || char == '-')) {
                    return false
                }
            }

            return true
        }

        // TODO modernize
        @JvmStatic
        fun readEscapeSequences(line: (() -> Int)?, text: String): String {
            var text = text
            var i = 0

            while (i < text.length) {
                val c = text[i++]

                if (c == '\\') {
                    if (i == text.length) {
                        throw DataReadException("Unterminated escape sequence in string at line ${line?.invoke()}")
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
                        else -> throw DataReadException("Invalid escape sequence '\\$c1' at line ${line?.invoke()}")
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
    }
}