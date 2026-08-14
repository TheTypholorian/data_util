package net.typho.tv_lib.io.impl

import net.typho.tv_lib.io.DataFileFormat
import net.typho.tv_lib.io.DataObjectSerializer
import net.typho.tv_lib.io.DataFileReadingException
import net.typho.tv_lib.io.DataFileWritingException
import net.typho.tv_lib.io.StringDataFileFormat
import net.typho.tv_lib.io.impl.token.ArrayCloseToken
import net.typho.tv_lib.io.impl.token.ArrayOpenToken
import net.typho.tv_lib.io.impl.token.BoolToken
import net.typho.tv_lib.io.impl.token.ColonToken
import net.typho.tv_lib.io.impl.token.CommaToken
import net.typho.tv_lib.io.impl.token.FloatToken
import net.typho.tv_lib.io.impl.token.IntToken
import net.typho.tv_lib.io.impl.token.LongToken
import net.typho.tv_lib.io.impl.token.NullToken
import net.typho.tv_lib.io.impl.token.ObjectCloseToken
import net.typho.tv_lib.io.impl.token.ObjectOpenToken
import net.typho.tv_lib.io.impl.token.PrimitiveToken
import net.typho.tv_lib.io.impl.token.StringToken
import net.typho.tv_lib.io.impl.token.Token

class JsonFileFormat @JvmOverloads constructor(
    @JvmField
    val allowComments: Boolean = false,
    @JvmField
    val prettyPrint: Boolean = false
) : StringDataFileFormat<Any> {
    override val extension: String
        get() = "json"
    override val serializers = mutableListOf<DataObjectSerializer<Any>>()

    override fun read(input: String): Any {
        val tokens = arrayListOf<Token>()
        var line = 0
        var index = 0

        // used for single line comments
        fun jumpToAfterNext(char: Char): Boolean {
            var line1 = line
            var index1 = index

            while (index1 < input.length) {
                val c = input[index1++]

                if (c == '\n') {
                    line1++
                }

                if (c == char) {
                    line = line1
                    index = index1
                    return true
                }
            }

            return false
        }

        // used for multiline comments
        fun jumpToAfterNext(s: String, extraNewlines: Int = 0): Boolean {
            var line1 = line
            var index1 = index

            while (index1 < input.length) {
                if (input[index1] == '\n') {
                    line1++
                }

                if (input.startsWith(s, index1)) {
                    line = line1 + extraNewlines
                    index = index1 + s.length
                    return true
                }

                index1++
            }

            return false
        }

        mainLoop@
        while (index < input.length) {
            val c = input[index]

            if (c == '\n') {
                line++
                index++
                continue
            }

            if (c.isWhitespace()) {
                index++
                continue
            }

            // skip comments
            if (allowComments && c == '/') {
                val index1 = index + 1

                if (index1 < input.length) {
                    val c1 = input[index1]

                    when (c1) {
                        '*' -> {
                            if (jumpToAfterNext("*/")) {
                                continue
                            } else {
                                throw DataFileReadingException("Multiline comment at line $line never closes")
                            }
                        }
                        '/' -> {
                            if (jumpToAfterNext('\n')) {
                                continue
                            } else {
                                break
                            }
                        }
                    }
                }
            }

            // numbers
            if (c.isDigit() || c == '-' || c == '.') {
                val numberStart = index++
                var float = false
                var exponent = false

                while (index < input.length) {
                    val c1 = input[index]

                    if (c1 == 'E' || c1 == 'e') { // exponents
                        if (exponent) {
                            tokens.add(FloatToken(line, input.substring(numberStart, index).toFloat()))
                            continue@mainLoop
                        }

                        exponent = true
                    } else if (c1 == '.') { // decimals
                        if (float) {
                            tokens.add(FloatToken(line, input.substring(numberStart, index).toFloat()))
                            continue@mainLoop
                        }

                        float = true
                    } else if (!(c1.isDigit() || (exponent && (c1 == '-' || c1 == '+')))) { // number ends
                        if (float || exponent) {
                            tokens.add(FloatToken(line, input.substring(numberStart, index).toFloat()))
                        } else {
                            val value = input.substring(numberStart, index).toLong()

                            if (value shr 32 == 0L) {
                                tokens.add(IntToken(line, value.toInt()))
                            } else {
                                tokens.add(LongToken(line, value))
                            }
                        }

                        if (c1 == '\n') {
                            line++
                        }

                        continue@mainLoop
                    }

                    index++
                }

                throw DataFileReadingException("Unterminated number at line $line")
            }

            when (c) {
                'n' -> { // null values
                    if (input.startsWith("null", index)) {
                        tokens.add(NullToken(line))
                        index += "null".length
                        continue
                    }
                }
                't' -> { // true values
                    if (input.startsWith("true", index)) {
                        tokens.add(BoolToken(line, true))
                        index += "true".length
                        continue
                    }
                }
                'f' -> { // false values
                    if (input.startsWith("false", index)) {
                        tokens.add(BoolToken(line, false))
                        index += "false".length
                        continue
                    }
                }
            }

            when (c) {
                // single character tokens
                '{' -> tokens.add(ObjectOpenToken(line))
                '}' -> tokens.add(ObjectCloseToken(line))
                '[' -> tokens.add(ArrayOpenToken(line))
                ']' -> tokens.add(ArrayCloseToken(line))
                ':' -> tokens.add(ColonToken(line))
                ',' -> tokens.add(CommaToken(line))

                // strings
                '"' -> {
                    index++
                    val stringStart = index

                    while (index < input.length) {
                        val c1 = input[index]

                        when (c1) {
                            '\n' -> throw DataFileReadingException("Unterminated string at line $line")
                            '\\' -> {
                                index++

                                if (index >= input.length) {
                                    throw DataFileReadingException("Unterminated escape sequence at line $line")
                                }
                            }
                            '"' -> {
                                tokens.add(StringToken(line, DataFileFormat.readEscapeSequences(line, input.substring(stringStart, index))))
                                index++
                                continue@mainLoop
                            }
                        }

                        index++
                    }
                }

                else -> throw DataFileReadingException("Illegal character '$c' at line $line")
            }

            index++
        }

        // parse the tokens
        val iterator = tokens.iterator()

        if (!iterator.hasNext()) {
            throw DataFileReadingException("Expected an array or object but got nothing at line 0")
        }

        val value = when (val token = iterator.next()) {
            is ArrayOpenToken -> readList(token.line, iterator)
            is ObjectOpenToken -> readObject(token.line, iterator)
            else -> throw DataFileReadingException("Expected an array or object but got $token at line ${token.line}")
        }

        if (iterator.hasNext()) {
            throw DataFileReadingException("Extra tokens after content at line $line")
        }

        return value
    }

    // reads an array/list from the iterator until a matching ArrayCloseToken
    private fun readList(line: Int, tokens: Iterator<Token>): List<Any?> {
        val list = mutableListOf<Any?>()

        while (tokens.hasNext()) {
            val token = tokens.next()

            when (token) {
                is ArrayCloseToken -> return list
                is ArrayOpenToken -> list.add(readList(token.line, tokens))
                is ObjectOpenToken -> list.add(readObject(token.line, tokens))
                is PrimitiveToken<*> -> list.add(token.value)
                else -> throw DataFileReadingException("Expected an array, object, or primitive but got $token in array at line ${token.line}")
            }

            if (!tokens.hasNext()) {
                throw DataFileReadingException("Expected comma or array end but got nothing in array at line ${token.line}")
            }

            when (val commaToken = tokens.next()) {
                is CommaToken -> {}
                is ArrayCloseToken -> return list
                else -> throw DataFileReadingException("Expected comma or array end but got $commaToken in array at line ${commaToken.line}")
            }
        }

        throw DataFileReadingException("Unterminated array at line $line")
    }

    // reads an object from the iterator until a matching ObjectCloseToken
    private fun readObject(line: Int, tokens: Iterator<Token>): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        while (tokens.hasNext()) {
            val keyToken = tokens.next()

            if (keyToken is ObjectCloseToken) {
                return map
            }

            val key = (keyToken as? StringToken)?.value ?: throw DataFileReadingException("Expected string but got $keyToken in object at line ${keyToken.line}")

            if (map.containsKey(key)) {
                throw DataFileReadingException("Duplicate object entry '$key' in object at line ${keyToken.line}")
            }

            val colonToken = tokens.next()

            if (colonToken !is ColonToken) {
                throw DataFileReadingException("Expected colon but got $colonToken in object at line ${colonToken.line}")
            }

            val value = when (val valueToken = tokens.next()) {
                is ArrayOpenToken -> readList(valueToken.line, tokens)
                is ObjectOpenToken -> readObject(valueToken.line, tokens)
                is PrimitiveToken<*> -> valueToken.value
                else -> throw DataFileReadingException("Expected an array, object, or primitive but got $valueToken in object at line ${valueToken.line}")
            }

            map[key] = value

            when (val commaToken = tokens.next()) {
                is CommaToken -> {}
                is ObjectCloseToken -> return map
                else -> throw DataFileReadingException("Expected comma or object end but got $commaToken in object at line ${commaToken.line}")
            }
        }

        throw DataFileReadingException("Unterminated object at line $line")
    }

    override fun write(data: Any): String {
        if (data !is List<*> && data !is Map<*, *>) {
            throw DataFileWritingException("Jsons must be either a list or a map, got $data")
        }

        return buildString {
            writeValue(data, 0, this)
        }
    }

    private fun writeValue(value: Any?, depth: Int, builder: StringBuilder) {
        when (value) {
            null -> builder.append("null")
            is Boolean -> builder.append(value)
            is Double -> builder.append(value)
            is Float -> builder.append(value)
            is Long -> builder.append(value)
            is Int -> builder.append(value)
            is Short -> builder.append(value)
            is Byte -> builder.append(value)
            is String -> builder.append('"').append(DataFileFormat.writeEscapeSequences(value)).append('"')
            is List<*> -> {
                if (value.isEmpty()) {
                    builder.append("[]")
                } else {
                    val indent = if (prettyPrint) '\n' + "\t".repeat(depth + 1) else ""
                    builder.append('[')
                    builder.append(indent)

                    val iterator = value.iterator()

                    while (iterator.hasNext()) {
                        writeValue(iterator.next(), depth + 1, builder)

                        if (iterator.hasNext()) {
                            builder.append(',')
                            builder.append(indent)
                        }
                    }

                    if (prettyPrint) {
                        builder.append('\n').append("\t".repeat(depth))
                    }

                    builder.append(']')
                }
            }
            is Map<*, *> -> {
                if (value.isEmpty()) {
                    builder.append("{}")
                } else {
                    val indent = if (prettyPrint) '\n' + "\t".repeat(depth + 1) else ""
                    builder.append('{')
                    builder.append(indent)

                    val iterator = value.iterator()

                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        writeValue(entry.key, depth + 1, builder)

                        if (prettyPrint) {
                            builder.append(": ")
                        } else {
                            builder.append(':')
                        }

                        writeValue(entry.value, depth + 1, builder)

                        if (iterator.hasNext()) {
                            builder.append(',')
                            builder.append(indent)
                        }
                    }

                    if (prettyPrint) {
                        builder.append('\n').append("\t".repeat(depth))
                    }

                    builder.append('}')
                }
            }
            else -> throw DataFileWritingException("Unsupported json value $value")
        }
    }
}