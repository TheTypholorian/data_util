package net.typho.data_util.impl

import net.typho.data_util.DataFormat
import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader
import net.typho.data_util.DataWriteException
import net.typho.data_util.DataWriter
import net.typho.data_util.SequentialOutput
import net.typho.data_util.StringDataFormat
import net.typho.data_util.SequentialInput
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueInput.Companion.fromObject
import net.typho.data_util.SingleValueOutput
import net.typho.data_util.SingleValueOutput.Companion.toConsumer
import java.util.function.Consumer
import kotlin.collections.set

class PropertiesFormat(
    @JvmField
    val delimiter: Char = '='
) : StringDataFormat<Map<String, String>> {
    init {
        if (delimiter != '=' && delimiter != ':') {
            throw IllegalArgumentException("Properties file delimiter must be '=' or ';', not '$delimiter'")
        }
    }

    override fun createInput(parsed: Map<String, String>): SingleValueInput {
        val readVersions = mutableSetOf<String>()

        return object : SingleValueInput {
            var used = false

            fun use() {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true
            }

            override fun readBoolean(): Boolean = throw DataReadException("Expected Boolean, got $parsed")

            override fun readByte(): Byte = throw DataReadException("Expected Byte, got $parsed")

            override fun readShort(): Short = throw DataReadException("Expected Short, got $parsed")

            override fun readInt(): Int = throw DataReadException("Expected Int, got $parsed")

            override fun readLong(): Long = throw DataReadException("Expected Long, got $parsed")

            override fun readFloat(): Float = throw DataReadException("Expected Float, got $parsed")

            override fun readDouble(): Double = throw DataReadException("Expected Double, got $parsed")

            override fun <E : Enum<E>> readEnum(cls: Class<E>): E = throw DataReadException("Expected Enum ${cls.name}, got $parsed")

            override fun readString(): String = throw DataReadException("Expected String, got $parsed")

            override fun readList() = throw DataReadException("Expected List, got $parsed")

            override fun readDynamicMap(): Iterator<Pair<String, SingleValueInput>> {
                use()
                return parsed.map { (key, value) -> key to SingleValueInput.fromString(value) }.iterator()
            }

            override fun readStaticMap(keys: List<String>): SequentialInput {
                use()
                return SequentialInput.fromStringMap(keys, parsed)
            }

            override fun readVersion(key: String): SingleValueInput? {
                if (!readVersions.add(key)) {
                    throw DataReadException("Already read version key '$key'")
                }

                return if (parsed.containsKey(key)) fromObject(parsed[key]) else null
            }

            override fun <T> readEither(options: List<DataReader<T>>): T {
                use()

                if (options.isEmpty()) {
                    throw DataReadException("Options list for readEither is empty")
                }

                val errors = mutableListOf<Throwable>()

                for (read in options) {
                    try {
                        used = false
                        return read.read(createInput(parsed))
                    } catch (t: Throwable) {
                        errors.add(t)
                    }
                }

                throw DataReadException("No options worked: ${errors.joinToString { it.message ?: "" }}")
            }

            override fun <T> readOptional(ifPresent: DataReader<T>): T? {
                return ifPresent.read(this)
            }
        }
    }

    override fun createOutput(out: Consumer<Map<String, String>>): SingleValueOutput {
        val map = mutableMapOf<String, String>()
        out.accept(map)
        return object : SingleValueOutput {
            var used = false

            override fun writeBoolean(v: Boolean) = throw DataWriteException("Properties format does not support root Boolean values")

            override fun writeByte(v: Byte) = throw DataWriteException("Properties format does not support root Byte values")

            override fun writeShort(v: Short) = throw DataWriteException("Properties format does not support root Short values")

            override fun writeInt(v: Int) = throw DataWriteException("Properties format does not support root Int values")

            override fun writeLong(v: Long) = throw DataWriteException("Properties format does not support root Long values")

            override fun writeFloat(v: Float) = throw DataWriteException("Properties format does not support root Float values")

            override fun writeDouble(v: Double) = throw DataWriteException("Properties format does not support root Double values")

            override fun writeString(v: String) = throw DataWriteException("Properties format does not support root String values")

            override fun <E : Enum<E>> writeEnum(v: E) = throw DataWriteException("Properties format does not support root Enum values")

            override fun writeList(size: Int): SequentialOutput = throw DataWriteException("Properties format does not support root List values")

            override fun writeDynamicMap(entries: List<Pair<String, Consumer<SingleValueOutput>>>) {
                if (used) {
                    throw DataWriteException("SingleValueOutput was already written")
                }

                used = true
                entries.forEach { (key, value) -> value.accept(toConsumer { map[key] = it.toString() }) }
            }

            override fun writeStaticMap(keys: List<String>): SequentialOutput {
                if (used) {
                    throw DataWriteException("SingleValueOutput was already written")
                }

                used = true
                return SequentialOutput.toStringMap(keys, map)
            }

            override fun writeVersion(key: String): SingleValueOutput {
                if (map.containsKey(key)) {
                    throw DataWriteException("Already wrote version key '$key'")
                }

                return toConsumer { map[key] = it.toString() }
            }

            override fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>) {
                throw DataWriteException("Properties format does not support root Optional values")
            }
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
            builder.appendLine("$key$delimiter${DataFormat.writeEscapeSequences(value)}")
        }

        return builder.toString()
    }
}