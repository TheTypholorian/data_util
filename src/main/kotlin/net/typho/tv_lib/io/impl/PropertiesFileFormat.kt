package net.typho.tv_lib.io.impl

import net.typho.tv_lib.io.DataFileFormat
import net.typho.tv_lib.io.DataObjectSerializer
import net.typho.tv_lib.io.DataObjectSerializer.Companion.flatten
import net.typho.tv_lib.io.FileFormatException
import net.typho.tv_lib.io.StringDataFileFormat

class PropertiesFileFormat(
    @JvmField
    val writeComments: Boolean = true,
    @JvmField
    val delimiter: Char = '='
) : StringDataFileFormat<Map<String, String>> {
    override val extension: String
        get() = "properties"
    override val serializers = mutableListOf<DataObjectSerializer<Any>>()

    init {
        if (delimiter != '=' && delimiter != ':') {
            throw IllegalArgumentException("Properties file delimiter must be '=' or ';', not '$delimiter'")
        }
    }

    override fun read(input: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var index = 0

        val lines = input.split('\n').iterator()

        while (lines.hasNext()) {
            val currentIndex = index++
            var line = lines.next().trimStart()

            // If the line is empty or a comment, skip
            if (line.isBlank() || line.startsWith('!') || line.startsWith('#')) {
                continue
            }

            // Multiline values
            while (line.endsWith('\\') && !line.endsWith("\\\\")) {
                index++
                line = line.substring(0, line.length - 1) + lines.next().trimStart()
            }

            // Apply escape sequences
            line = DataFileFormat.applyEscapeSequences(line)

            // Parse the line
            val tokens = line.split('=', ':', limit = 2)

            if (tokens.size < 2) {
                throw FileFormatException("Line $currentIndex is missing a '=' or ':' delimiter: '$line'")
            }

            map[tokens.first().trimEnd()] = tokens.last().trimStart()
        }

        return map
    }

    override fun write(data: Map<String, String>): String {
        // Flatten all values
        val data = (serializers.flatten(data) ?: return "") as Map<*, *>

        val builder = StringBuilder()

        data.forEach { (key, value) ->
            key!!
            val value = value ?: "null"

            if (key !is CharSequence) {
                throw IllegalArgumentException("Expected a CharSequence key, got ${key.javaClass.name}")
            }

            if (key.any { it.isWhitespace() }) {
                throw IllegalArgumentException("Expected a CharSequence key, got ${key.javaClass.name}")
            }

            // Only primitives and strings are allowed
            if (!value.javaClass.isPrimitive && value !is CharSequence) {
                throw IllegalArgumentException("Expected a CharSequence or primitive value, got ${value.javaClass.name}")
            }

            // Replace special characters
            val newValue = value.toString().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

            // Write line
            builder.appendLine("$key $delimiter $newValue")
        }

        return builder.toString()
    }
}