package net.typho.data_util.impl

import net.typho.data_util.DataFormat
import net.typho.data_util.SerializedData
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.error.DataReadException

open class PropertiesFormat(
    @JvmField
    val delimiterToWrite: Char = '='
) : DataFormat<Map<String, String>> {
    init {
        if (delimiterToWrite != '=' && delimiterToWrite != ':') {
            throw IllegalArgumentException("Properties file delimiter must be '=' or ';', not '$delimiterToWrite'")
        }
    }

    override fun createInput(data: Map<String, String>): ValueInput {
        TODO("Not yet implemented")
    }

    override fun createOutput(): ValueOutput.WithResult<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override fun read(input: SerializedData): Map<String, String> {
        val input = input.asString()
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

    override fun write(data: Map<String, String>): SerializedData {
        val builder = StringBuilder()

        data.forEach { (key, value) ->
            if (key.any { it.isWhitespace() }) {
                throw IllegalArgumentException("Property keys must not have whitespaces")
            }

            // Write line
            builder.appendLine("$key$delimiterToWrite${DataFormat.writeEscapeSequences(value)}")
        }

        return SerializedData(builder.toString())
    }

    override fun toString(): String {
        return "PropertiesFormat(delimiterToWrite=$delimiterToWrite)"
    }

    companion object Default : PropertiesFormat()
}