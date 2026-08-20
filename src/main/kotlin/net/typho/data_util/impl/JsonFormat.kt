package net.typho.data_util.impl

import net.typho.data_util.DataFormat
import net.typho.data_util.DataReadException
import net.typho.data_util.DataWriteException
import net.typho.data_util.StringDataFormat
import net.typho.data_util.MapInput
import net.typho.data_util.MapOutput
import net.typho.data_util.MapOutputResult

class JsonFormat(
    @JvmField
    val allowComments: Boolean = false,
    @JvmField
    val prettyPrint: Boolean = false
) : StringDataFormat<Any> {
    @Suppress("UNCHECKED_CAST")
    override fun createInput(parsed: Any): MapInput {
        return MapInput.fromMap((parsed as? Map<String, Any?>) ?: throw IllegalArgumentException("Can only create a map input to a map object, got a $parsed"))
    }

    override fun createOutput(): MapOutputResult<out Any> {
        return MapOutput.toMap(mutableMapOf())
    }

    override fun read(input: String): Any {
        val tokens = TokenList()
        var index = 0

        // used for single line comments
        fun jumpToAfterNext(char: Char): Boolean {
            var line1 = tokens.lastLineNumber()
            var index1 = index

            while (index1 < input.length) {
                val c = input[index1++]

                if (c == '\n') {
                    line1++
                }

                if (c == char) {
                    tokens.newLine(line1)
                    index = index1
                    return true
                }
            }

            return false
        }

        // used for multiline comments
        fun jumpToAfterNext(s: String, extraNewlines: Int = 0): Boolean {
            var line1 = tokens.lastLineNumber()
            var index1 = index

            while (index1 < input.length) {
                if (input[index1] == '\n') {
                    line1++
                }

                if (input.startsWith(s, index1)) {
                    tokens.newLine(line1 + extraNewlines)
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

            //println("$index '$c'")

            if (c == '\n') {
                tokens.newLine()
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
                                throw DataReadException("Multiline comment at line ${tokens.lastLineNumber()} never closes")
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
                            tokens.add(input.substring(numberStart, index).toFloat())
                            continue@mainLoop
                        }

                        exponent = true
                    } else if (c1 == '.') { // decimals
                        if (float) {
                            tokens.add(input.substring(numberStart, index).toFloat())
                            continue@mainLoop
                        }

                        float = true
                    } else if (!(c1.isDigit() || (exponent && (c1 == '-' || c1 == '+')))) { // number ends
                        if (float || exponent) {
                            tokens.add(input.substring(numberStart, index).toFloat())
                        } else {
                            val value = input.substring(numberStart, index).toLong()

                            if (value shr 32 == 0L) {
                                tokens.add(value.toInt())
                            } else {
                                tokens.add(value)
                            }
                        }

                        continue@mainLoop
                    }

                    index++
                }

                throw DataReadException("Unterminated number at line ${tokens.lastLineNumber()}")
            }

            when (c) {
                'n' -> { // null values
                    if (input.startsWith("null", index)) {
                        tokens.add(null)
                        index += "null".length
                        continue
                    }
                }
                't' -> { // true values
                    if (input.startsWith("true", index)) {
                        tokens.add(true)
                        index += "true".length
                        continue
                    }
                }
                'f' -> { // false values
                    if (input.startsWith("false", index)) {
                        tokens.add(false)
                        index += "false".length
                        continue
                    }
                }
            }

            when (c) {
                // single character tokens
                '{' -> tokens.add(TokenConstants.OBJECT_OPEN)
                '}' -> tokens.add(TokenConstants.OBJECT_CLOSE)
                '[' -> tokens.add(TokenConstants.ARRAY_OPEN)
                ']' -> tokens.add(TokenConstants.ARRAY_CLOSE)
                ':' -> tokens.add(TokenConstants.COLON)
                ',' -> tokens.add(TokenConstants.COMMA)

                // strings
                '"' -> {
                    index++
                    val stringStart = index

                    while (index < input.length) {
                        val c1 = input[index]

                        when (c1) {
                            '\n' -> throw DataReadException("Unterminated string at line ${tokens.lastLineNumber()}")
                            '\\' -> {
                                index++

                                if (index >= input.length) {
                                    throw DataReadException("Unterminated escape sequence at line ${tokens.lastLineNumber()}")
                                }
                            }
                            '"' -> {
                                tokens.add(DataFormat.readEscapeSequences({ tokens.lastLineNumber() }, input.substring(stringStart, index)))
                                index++
                                continue@mainLoop
                            }
                        }

                        index++
                    }
                }

                else -> throw DataReadException("Illegal character '$c' at line ${tokens.lastLineNumber()}")
            }

            index++
        }

        // parse the tokens
        val iterator = tokens.listIterator()

        if (!iterator.hasNext()) {
            throw DataReadException("Expected an array or object but got nothing at line 0")
        }

        val value = when (val token = iterator.next()) {
            TokenConstants.ARRAY_OPEN -> readList({ tokens.lineNumber(0) }, tokens, iterator)
            TokenConstants.OBJECT_OPEN -> readObject({ tokens.lineNumber(0) }, tokens, iterator)
            else -> throw DataReadException("Expected an array or object but got $token at line ${tokens.lineNumber(0)}")
        }

        if (iterator.hasNext()) {
            throw DataReadException("Extra tokens after content at line ${tokens.lineNumber(iterator.nextIndex())}")
        }

        return value
    }

    // reads an array/list from the iterator until a matching ArrayCloseToken
    private fun readList(line: () -> Int, tokens: TokenList, iterator: ListIterator<Any?>): List<Any?> {
        val list = mutableListOf<Any?>()

        while (iterator.hasNext()) {
            val index = iterator.nextIndex()

            when (val token = iterator.next()) {
                TokenConstants.ARRAY_OPEN -> list.add(readList({ tokens.lineNumber(index) }, tokens, iterator))
                TokenConstants.ARRAY_CLOSE -> return list
                TokenConstants.OBJECT_OPEN -> list.add(readObject({ tokens.lineNumber(index) }, tokens, iterator))
                is TokenConstants -> throw DataReadException("Expected an array, object, or primitive but got $token in array at line ${tokens.lineNumber(index)}")
                else -> list.add(token)
            }

            if (!iterator.hasNext()) {
                throw DataReadException("Expected comma or array end but got nothing in array at line ${tokens.lineNumber(index)}")
            }

            when (val commaToken = iterator.next()) {
                TokenConstants.COMMA -> {}
                TokenConstants.ARRAY_CLOSE -> return list
                else -> throw DataReadException("Expected comma or array end but got $commaToken in array at line ${tokens.lineNumber(index)}")
            }
        }

        throw DataReadException("Unterminated array at line ${line()}")
    }

    // reads an object from the iterator until a matching ObjectCloseToken
    private fun readObject(line: () -> Int, tokens: TokenList, iterator: ListIterator<Any?>): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        while (iterator.hasNext()) {
            val keyToken = iterator.next()

            if (keyToken === TokenConstants.OBJECT_CLOSE) {
                return map
            }

            val key = (keyToken as? String) ?: throw DataReadException("Expected string but got $keyToken in object at line ${tokens.lineNumber(iterator.previousIndex())}")

            if (map.containsKey(key)) {
                throw DataReadException("Duplicate object entry '$key' in object at line ${tokens.lineNumber(iterator.previousIndex())}")
            }

            val colonToken = iterator.next()

            if (colonToken !== TokenConstants.COLON) {
                throw DataReadException("Expected colon but got $colonToken in object at line ${tokens.lineNumber(iterator.previousIndex())}")
            }

            val valueIndex = iterator.nextIndex()
            val value = when (val valueToken = iterator.next()) {
                TokenConstants.ARRAY_OPEN -> readList({ tokens.lineNumber(valueIndex) }, tokens, iterator)
                TokenConstants.OBJECT_OPEN -> readObject({ tokens.lineNumber(valueIndex) }, tokens, iterator)
                is TokenConstants -> throw DataReadException("Expected an array, object, or primitive but got $valueToken in object at line ${tokens.lineNumber(iterator.previousIndex())}")
                else -> valueToken
            }

            map[key] = value

            when (val commaToken = iterator.next()) {
                TokenConstants.COMMA -> {}
                TokenConstants.OBJECT_CLOSE -> return map
                else -> throw DataReadException("Expected comma or object end but got $commaToken in object at line ${tokens.lineNumber(iterator.previousIndex())}")
            }
        }

        throw DataReadException("Unterminated object at line ${line()}")
    }

    override fun write(data: Any): String {
        if (data !is List<*> && data !is Map<*, *>) {
            throw DataWriteException("Jsons must be either a list or a map, got $data")
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
            is String -> builder.append('"').append(DataFormat.writeEscapeSequences(value)).append('"')
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
                        val key = entry.key

                        if (key !is String) {
                            throw DataWriteException("Object keys must be strings")
                        }

                        writeValue(key, depth + 1, builder)

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
            else -> throw DataWriteException("Unsupported json value $value")
        }
    }
}