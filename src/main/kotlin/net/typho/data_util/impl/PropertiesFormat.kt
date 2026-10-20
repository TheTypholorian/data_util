package net.typho.data_util.impl

import net.typho.data_util.DataFormat
import net.typho.data_util.DataReadException
import net.typho.data_util.StringDataFormat
import kotlin.collections.set

open class PropertiesFormat(
    @JvmField
    val delimiterToWrite: Char = '='
) : StringDataFormat<Map<String, String>> {
    init {
        if (delimiterToWrite != '=' && delimiterToWrite != ':') {
            throw IllegalArgumentException("Properties file delimiter must be '=' or ';', not '$delimiterToWrite'")
        }
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
            builder.appendLine("$key$delimiterToWrite${DataFormat.writeEscapeSequences(value)}")
        }

        return builder.toString()
    }

    companion object Default : PropertiesFormat()
}