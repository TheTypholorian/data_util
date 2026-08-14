package net.typho.tv_lib.io.impl

import net.typho.tv_lib.io.DataFileFormat
import net.typho.tv_lib.io.DataObjectSerializer
import net.typho.tv_lib.io.DataObjectSerializer.Companion.flatten
import net.typho.tv_lib.io.DataWithComment
import net.typho.tv_lib.io.FileFormatException

class PropertiesFileFormat(
    @JvmField
    val writeComments: Boolean = true,
    @JvmField
    /**
     * What delimiter to use ('=' or ':') when *writing* properties files
     */
    val delimiter: Char = '='
) : DataFileFormat {
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

    override fun write(data: Any): String {
        // Flatten all values
        val data = serializers.flatten(data) ?: return ""

        if (data !is Map<*, *>) {
            throw IllegalArgumentException("Property file format requires a Map input, got ${data.javaClass.name}")
        }

        val builder = StringBuilder()

        // Handle comments
        fun checkComment(value: Any?): Any? {
            return if (value is DataWithComment) {
                if (writeComments) {
                    builder.appendLine("#${value.comment}")
                }

                value.value
            } else value
        }

        data.forEach { (key, value) ->
            // Key comments should run before value comments
            val key = checkComment(key) ?: throw NullPointerException("Property file keys must not be null")
            val value = checkComment(value) ?: "null"

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