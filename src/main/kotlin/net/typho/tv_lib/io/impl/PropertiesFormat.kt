package net.typho.tv_lib.io.impl

import net.typho.tv_lib.io.DataFormat
import net.typho.tv_lib.io.DataReadException
import net.typho.tv_lib.io.StringDataFormat
import net.typho.tv_lib.io.codec.CodecTemplate

class PropertiesFormat(
    @JvmField
    val delimiter: Char = '='
) : StringDataFormat<Map<String, String>> {
    init {
        if (delimiter != '=' && delimiter != ':') {
            throw IllegalArgumentException("Properties file delimiter must be '=' or ';', not '$delimiter'")
        }
    }

    override fun createInput(parsed: Map<String, String>): CodecTemplate.MapInput {
        return CodecTemplate.MapInput.fromStringMap(parsed)
    }

    override fun createOutput(): CodecTemplate.MapOutputTo<out Map<String, String>> {
        return CodecTemplate.MapOutput.toStringMap(mutableMapOf())
    }

    override fun read(input: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var lineIndex = 0

        val lines = input.split('\n').iterator()

        while (lines.hasNext()) {
            val currentLine = lineIndex++
            var line = lines.next().trimStart()

            // If the line is empty or a comment, skip
            if (line.isBlank() || line.startsWith('!') || line.startsWith('#')) {
                continue
            }

            // Multiline values
            while (line.endsWith('\\') && !line.endsWith("\\\\")) {
                lineIndex++
                line = line.substring(0, line.length - 1) + lines.next().trimStart()
            }

            // Apply escape sequences
            line = DataFormat.readEscapeSequences({ currentLine }, line)

            // Parse the line
            val tokens = line.split('=', ':', limit = 2)

            if (tokens.size < 2) {
                throw DataReadException("Line $currentLine is missing a '=' or ':' delimiter: '$line'")
            }

            map[tokens.first().trimEnd()] = tokens.last().trimStart()
        }

        return map
    }

    override fun write(data: Map<String, String>): String {
        val builder = StringBuilder()

        data.forEach { (key, value) ->
            if (key.any { it.isWhitespace() }) {
                throw IllegalArgumentException("Property keys must not have whitespaces")
            }

            // Write line
            builder.appendLine("$key$delimiter${DataFormat.writeEscapeSequences(value)}")
        }

        return builder.toString()
    }
}