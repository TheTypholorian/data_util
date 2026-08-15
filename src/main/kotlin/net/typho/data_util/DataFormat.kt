package net.typho.data_util

import net.typho.data_util.codec.MapDataCodec
import net.typho.data_util.codec.MapInput
import net.typho.data_util.codec.MapOutputResult
import java.io.DataInput
import java.io.DataOutput

interface DataFormat<D, R> : DataSerializer<D, R> {
    fun <C : Any> map(codec: MapDataCodec<C>): DataSerializer<D, C> {
        val parent = this

        return object : DataSerializer<D, C> {
            override fun read(input: D): C {
                return codec.read(parent.createInput(parent.read(input)))
            }

            override fun read(bytes: Int, input: DataInput): C {
                return codec.read(parent.createInput(parent.read(bytes, input)))
            }

            override fun write(data: C): D {
                val out = parent.createOutput()
                codec.write(data, out)
                return parent.write(out.finish())
            }

            override fun write(data: C, output: DataOutput) {
                val out = parent.createOutput()
                codec.write(data, out)
                parent.write(out.finish(), output)
            }
        }
    }

    fun createInput(parsed: R): MapInput

    fun createOutput(): MapOutputResult<out R>

    companion object {
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