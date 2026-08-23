package net.typho.data_util

import java.io.DataInput
import java.util.function.Function
import kotlin.jvm.java

interface SingleValueInput {
    fun readBoolean(): Boolean

    fun readByte(): Byte

    fun readShort(): Short

    fun readInt(): Int

    fun readLong(): Long

    fun readFloat(): Float

    fun readDouble(): Double

    fun readString(): String

    fun readList(): SequentialInput

    fun readStaticMap(keys: List<String>): SequentialInput

    fun <T> readOptional(ifPresent: Function<SingleValueInput, T>): T?

    companion object {
        @JvmStatic
        fun fromObject(value: Any?): SingleValueInput = object : SingleValueInput {
            var used = false

            fun use() {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true
            }

            inline fun <reified T> cast(): T {
                use()

                if (value is T) {
                    return value
                } else {
                    throw DataReadException("Expected ${T::class.java.name}, got $value")
                }
            }

            override fun readBoolean(): Boolean = cast()

            override fun readByte(): Byte = cast()

            override fun readShort(): Short = cast()

            override fun readInt(): Int = cast()

            override fun readLong(): Long = cast()

            override fun readFloat(): Float = cast()

            override fun readDouble(): Double = cast()

            override fun readString(): String = cast()

            override fun readList() = SequentialInput.fromList(cast())

            override fun readStaticMap(keys: List<String>) = SequentialInput.fromMap(keys, cast())

            override fun <T> readOptional(ifPresent: Function<SingleValueInput, T>): T? {
                use()
                return if (value == null) null else ifPresent.apply(fromObject(value))
            }
        }

        @JvmStatic
        fun fromString(value: String?): SingleValueInput = object : SingleValueInput {
            var used = false

            fun use() {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true
            }

            inline fun <reified T> cast(converter: Function<String, T?>): T {
                use()

                value?.let { converter.apply(it)?.let { return it } }

                throw DataReadException("Expected ${T::class.java.name}, got '$value'")
            }

            override fun readBoolean(): Boolean = cast(String::toBooleanStrictOrNull)

            override fun readByte(): Byte = cast(String::toByteOrNull)

            override fun readShort(): Short = cast(String::toShortOrNull)

            override fun readInt(): Int = cast(String::toIntOrNull)

            override fun readLong(): Long = cast(String::toLongOrNull)

            override fun readFloat(): Float = cast(String::toFloatOrNull)

            override fun readDouble(): Double = cast(String::toDoubleOrNull)

            override fun readString(): String = cast { it }

            override fun readList(): SequentialInput = throw DataReadException("List values are unsupported when reading from a string")

            override fun readStaticMap(keys: List<String>): SequentialInput = throw DataReadException("Map values are unsupported when reading from a string")

            override fun <T> readOptional(ifPresent: Function<SingleValueInput, T>): T? {
                use()
                return if (value == null) null else ifPresent.apply(fromString(value))
            }
        }

        @JvmStatic
        fun fromData(input: DataInput): SingleValueInput = object : SingleValueInput {
            var used = false

            fun <T> read(read: Function<DataInput, T>): T {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true

                return read.apply(input)
            }

            override fun readBoolean(): Boolean = read(DataInput::readBoolean)

            override fun readByte(): Byte = read(DataInput::readByte)

            override fun readShort(): Short = read(DataInput::readShort)

            override fun readInt(): Int = read(DataInput::readInt)

            override fun readLong(): Long = read(DataInput::readLong)

            override fun readFloat(): Float = read(DataInput::readFloat)

            override fun readDouble(): Double = read(DataInput::readDouble)

            override fun readString(): String = read(DataInput::readUTF)

            override fun readList(): SequentialInput = read(SequentialInput::fromData)

            override fun readStaticMap(keys: List<String>): SequentialInput = read { SequentialInput.fromData(it, keys.size) }

            override fun <T> readOptional(ifPresent: Function<SingleValueInput, T>): T? {
                return read {
                    val present = it.readBoolean()
                    if (present) ifPresent.apply(fromData(input)) else null
                }
            }
        }
    }
}