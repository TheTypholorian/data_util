package net.typho.data_util

import java.io.DataInput
import java.util.Optional
import java.util.function.Function

interface SingleValueInput {
    fun readBoolean(): Boolean

    fun readByte(): Byte

    fun readShort(): Short

    fun readInt(): Int

    fun readLong(): Long

    fun readFloat(): Float

    fun readDouble(): Double

    fun readString(): String

    fun readList(): ListInput

    fun readMap(): MapInput

    fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T>

    companion object {
        @JvmStatic
        fun fromObject(value: Any?): SingleValueInput = object : SingleValueInput {
            var used = false

            inline fun <reified T : Any> cast(): T {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true

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

            override fun readList() = ListInput.fromList(cast())

            override fun readMap() = MapInput.fromMap(cast())

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(this))
            }
        }

        @JvmStatic
        fun fromString(value: String?): SingleValueInput = object : SingleValueInput {
            var used = false

            inline fun <reified T : Any> cast(converter: Function<String, T?>): T {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true

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

            override fun readList(): ListInput = throw DataReadException("List values are unsupported")

            override fun readMap(): MapInput = throw DataReadException("Map values are unsupported")

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(this))
            }
        }

        @JvmStatic
        fun fromData(input: DataInput): SingleValueInput = object : SingleValueInput {
            var used = false

            fun <T> get(read: Function<DataInput, T>): T {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true

                return read.apply(input)
            }

            override fun readBoolean(): Boolean = get(DataInput::readBoolean)

            override fun readByte(): Byte = get(DataInput::readByte)

            override fun readShort(): Short = get(DataInput::readShort)

            override fun readInt(): Int = get(DataInput::readInt)

            override fun readLong(): Long = get(DataInput::readLong)

            override fun readFloat(): Float = get(DataInput::readFloat)

            override fun readDouble(): Double = get(DataInput::readDouble)

            override fun readString(): String = get(DataInput::readUTF)

            override fun readList(): ListInput = get(ListInput::fromData)

            override fun readMap(): MapInput = get(MapInput::fromData)

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                val present = input.readBoolean()
                return if (present) Optional.of(ifPresent.apply(this)) else Optional.empty()
            }
        }
    }
}