package net.typho.data_util.impl

/*
class TomlFormat(
) : StringDataFormat<Map<String, Any?>> {
    @Suppress("UNCHECKED_CAST")
    override fun createInput(parsed: Map<String, Any?>): SequentialInput {
        return SequentialInput.fromMap(parsed)
    }

    override fun createOutput(): SequentialOutputResult<out Map<String, Any?>> {
        return SequentialOutput.toMap(mutableMapOf())
    }

    override fun read(input: String): Map<String, Any?> {
        var input = input

        // Remove comments, and replace multiline text with single line text
        var inMultilineDoubleQuotes = false
        var inMultilineSingleQuotes = false
        var inDoubleQuotes = false
        var inSingleQuotes = false
        var backslash = false
        var i = 0
        var line = 0

        while (i < input.length) {
            val c = input[i]

            if (c == '\\') {
                backslash = !backslash
            } else {
                if (!backslash) {
                    if (input.regionMatches(i, "\"\"\"", 0, 3, true)) {
                        if (inMultilineSingleQuotes) {
                            throw DataReadException("Cannot have triple quotes in a multiline single quote string (on line $line)")
                        }

                        inMultilineDoubleQuotes = !inMultilineDoubleQuotes
                        // Turn """ to " while we're here, and remove leading newline
                        input = input.substring(0, i) + '"' + if (inMultilineDoubleQuotes && input.getOrNull(i + 3) == '\n') {
                            line++
                            input.substring(i + 4)
                        } else {
                            input.substring(i + 3)
                        }.trimStart()
                    } else if (input.regionMatches(i, "'''", 0, 3, true)) {
                        if (inMultilineDoubleQuotes) {
                            throw DataReadException("Cannot have triple single quotes in a multiline double quote string (on line $line)")
                        }

                        inMultilineSingleQuotes = !inMultilineSingleQuotes
                        // Turn ''' to ' while we're here, and remove leading newline
                        input = input.substring(0, i) + '"' + if (inMultilineSingleQuotes && input.getOrNull(i + 3) == '\n') {
                            line++
                            input.substring(i + 4)
                        } else {
                            input.substring(i + 3)
                        }.trimStart()
                    } else if (c == '"') {
                        inDoubleQuotes = !inDoubleQuotes
                    } else if (c == '\'') {
                        inSingleQuotes = !inSingleQuotes
                        input = input.substring(0, i) + '"' + input.substring(i + 1)
                    } else if (c == '#') {
                        // Remove comments
                        if (!(inMultilineDoubleQuotes || inMultilineSingleQuotes || inDoubleQuotes || inSingleQuotes)) {
                            input = input.substring(0, i) + input.substring(input.indexOf('\n', startIndex = i))
                        }
                    } else if (c == '\n') {
                        // Turn multiline strings into single line
                        if (inMultilineDoubleQuotes || inMultilineSingleQuotes) {
                            input = input.substring(0, i) + "\\n" + input.substring(i + 1).trimStart()
                        }

                        line++
                    }
                }

                backslash = false
            }

            i++
        }

        println(input)

        val map = mutableMapOf<String, Any?>()
        val lines = input.split('\n').iterator()
        var index = 0

        while (lines.hasNext()) {
            val currentIndex = index++
            var line = lines.next().trimStart()

            // If the line is empty, skip
            if (line.isBlank()) {
                continue
            }

            // Apply escape sequences
            line = DataFormat.readEscapeSequences(null, line)

            // Parse the line
            val tokens = line.split('=', ':', limit = 2)

            if (tokens.size < 2) {
                throw DataReadException("Line $currentIndex is missing an '=' delimiter: '$line'")
            }

            map[DataFormat.trimQuotes(tokens.first().trim())] = DataFormat.trimQuotes(tokens.last().trim())
        }

        return map
    }

    override fun write(data: Map<String, Any?>): String {
        TODO("Not yet implemented")
    }
}
 */