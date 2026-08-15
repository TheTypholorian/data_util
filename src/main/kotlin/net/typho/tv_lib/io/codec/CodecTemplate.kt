package net.typho.tv_lib.io.codec

import net.typho.tv_lib.io.DataReadException
import net.typho.tv_lib.io.DataWriteException
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Function
import kotlin.collections.set

interface CodecTemplate<T : Any> {
    companion object {
        private fun <T : Any> simple(read: Function<ValueInput, T>, write: BiConsumer<ValueOutput, T>) = object : CodecTemplate<T> {
            override fun read(input: ValueInput): T {
                return read.apply(input)
            }

            override fun write(value: T, output: ValueOutput) {
                write.accept(output, value)
            }
        }

        @JvmField
        val BOOL = simple(ValueInput::readBoolean, ValueOutput::writeBoolean)
        @JvmField
        val BYTE = simple(ValueInput::readByte, ValueOutput::writeByte)
        @JvmField
        val SHORT = simple(ValueInput::readShort, ValueOutput::writeShort)
        @JvmField
        val INT = simple(ValueInput::readInt, ValueOutput::writeInt)
        @JvmField
        val LONG = simple(ValueInput::readLong, ValueOutput::writeLong)
        @JvmField
        val FLOAT = simple(ValueInput::readFloat, ValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple(ValueInput::readDouble, ValueOutput::writeDouble)
        @JvmField
        val STRING = simple(ValueInput::readString, ValueOutput::writeString)
    }

    fun read(input: ValueInput): T

    fun write(value: T, output: ValueOutput)

    interface ValueInput {
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

        fun <T : Any> readOptional(ifPresent: Function<ValueInput, T>): Optional<T>

        companion object {
            @JvmStatic
            fun fromObject(value: Any?): ValueInput = object : ValueInput {
                inline fun <reified T : Any> cast(): T {
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

                override fun <T : Any> readOptional(ifPresent: Function<ValueInput, T>): Optional<T> {
                    return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(this))
                }
            }

            @JvmStatic
            fun fromString(value: String?): ValueInput = object : ValueInput {
                inline fun <reified T : Any> cast(converter: Function<String, T?>): T {
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

                override fun <T : Any> readOptional(ifPresent: Function<ValueInput, T>): Optional<T> {
                    return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(this))
                }
            }
        }
    }

    interface ListInput : ValueInput {
        val left: Int

        fun <T> toList(read: Function<ListInput, T>): List<T> {
            val list = ArrayList<T>(left)

            repeat(left) {
                list.add(read.apply(this@ListInput))
            }

            return list
        }

        fun <T> iterator(read: Function<ListInput, T>) = object : Iterator<T> {
            override fun next() = read.apply(this@ListInput)

            override fun hasNext() = left > 0
        }

        companion object {
            @JvmStatic
            fun fromList(list: List<Any?>): ListInput = object : ListInput {
                var index = 0
                override val left: Int
                    get() = list.size - index

                inline fun <reified T> read(): T {
                    if (left == 0) {
                        throw DataReadException("Read too many entries from list of size ${list.size}")
                    }

                    val value = list[index]

                    if (value is T) {
                        index++
                        return value
                    } else {
                        throw DataReadException("Expected ${T::class.java.name} at index $index, got $value")
                    }
                }

                override fun readBoolean(): Boolean = read()

                override fun readByte(): Byte = read()

                override fun readShort(): Short = read()

                override fun readInt(): Int = read()

                override fun readLong(): Long = read()

                override fun readFloat(): Float = read()

                override fun readDouble(): Double = read()

                override fun readString(): String = read()

                override fun readList() = fromList(read())

                override fun readMap() = MapInput.fromMap(read())

                override fun <T : Any> readOptional(ifPresent: Function<ValueInput, T>): Optional<T> {
                    val value = read<Any?>()
                    return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(ValueInput.fromObject(value)))
                }
            }
        }
    }

    interface MapInput {
        fun readEntry(key: String): Optional<ValueInput>

        companion object {
            @JvmStatic
            fun fromMap(map: Map<String, Any?>): MapInput = object : MapInput {
                val read = mutableSetOf<String>()

                override fun readEntry(key: String): Optional<ValueInput> {
                    if (!read.add(key)) {
                        throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                    }

                    return if (map.containsKey(key)) Optional.of(ValueInput.fromObject(map[key]!!)) else Optional.empty()
                }
            }

            @JvmStatic
            fun fromStringMap(map: Map<String, String>): MapInput = object : MapInput {
                val read = mutableSetOf<String>()

                override fun readEntry(key: String): Optional<ValueInput> {
                    if (!read.add(key)) {
                        throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                    }

                    return if (map.containsKey(key)) Optional.of(ValueInput.fromString(map[key]!!)) else Optional.empty()
                }
            }
        }
    }

    interface ValueOutput {
        fun writeBoolean(v: Boolean)

        fun writeByte(v: Byte)

        fun writeShort(v: Short)

        fun writeInt(v: Int)

        fun writeLong(v: Long)

        fun writeFloat(v: Float)

        fun writeDouble(v: Double)

        fun writeString(v: String)

        fun writeList(size: Int): ValueOutput

        fun writeMap(): MapOutput

        fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<ValueOutput, T>)

        companion object {
            @JvmStatic
            fun toList(size: Int, list: MutableList<Any?>): ValueOutput = object : ValueOutput {
                var index = 0

                fun checkIndex() {
                    if (index++ >= size) {
                        throw DataWriteException("Wrote too many elements to list of size ${list.size}")
                    }
                }

                override fun writeBoolean(v: Boolean) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeByte(v: Byte) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeShort(v: Short) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeInt(v: Int) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeLong(v: Long) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeFloat(v: Float) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeDouble(v: Double) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeString(v: String) {
                    checkIndex()
                    list.add(v)
                }

                override fun writeList(size: Int): ValueOutput {
                    checkIndex()
                    val list1 = ArrayList<Any?>(size)
                    list.add(list1)
                    return toList(size, list1)
                }

                override fun writeMap(): MapOutput {
                    checkIndex()
                    val map = mutableMapOf<String, Any?>()
                    list.add(map)
                    return MapOutput.toMap(map)
                }

                override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<ValueOutput, T>) {
                    if (optional.isPresent) {
                        ifPresent.accept(this, optional.get())
                    } else {
                        checkIndex()
                        list.add(null)
                    }
                }
            }
        }
    }

    interface MapOutput {
        fun writeEntry(key: String): ValueOutput

        companion object {
            @JvmStatic
            fun toMap(map: MutableMap<String, Any?>): MapOutputTo<Map<String, Any?>> = object : MapOutputTo<Map<String, Any?>> {
                override fun finish(): Map<String, Any?> {
                    return map
                }

                override fun writeEntry(key: String): ValueOutput {
                    if (map.containsKey(key)) {
                        throw DataWriteException("Duplicate key $key")
                    }

                    return object : ValueOutput {
                        override fun writeBoolean(v: Boolean) {
                            map[key] = v
                        }

                        override fun writeByte(v: Byte) {
                            map[key] = v
                        }

                        override fun writeShort(v: Short) {
                            map[key] = v
                        }

                        override fun writeInt(v: Int) {
                            map[key] = v
                        }

                        override fun writeLong(v: Long) {
                            map[key] = v
                        }

                        override fun writeFloat(v: Float) {
                            map[key] = v
                        }

                        override fun writeDouble(v: Double) {
                            map[key] = v
                        }

                        override fun writeString(v: String) {
                            map[key] = v
                        }

                        override fun writeList(size: Int): ValueOutput {
                            val list = ArrayList<Any?>(size)
                            map[key] = list
                            return ValueOutput.toList(size, list)
                        }

                        override fun writeMap(): MapOutput {
                            val map1 = mutableMapOf<String, Any?>()
                            map[key] = map1
                            return toMap(map1)
                        }

                        override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<ValueOutput, T>) {
                            if (optional.isPresent) {
                                ifPresent.accept(this, optional.get())
                            } else {
                                map[key] = null
                            }
                        }
                    }
                }
            }

            @JvmStatic
            fun toStringMap(map: MutableMap<String, String>): MapOutputTo<Map<String, String>> = object : MapOutputTo<Map<String, String>> {
                val alreadyWrote = map.keys.toMutableSet()

                override fun finish(): Map<String, String> {
                    return map
                }

                override fun writeEntry(key: String): ValueOutput {
                    if (!alreadyWrote.add(key)) {
                        throw DataWriteException("Duplicate key $key")
                    }

                    return object : ValueOutput {
                        override fun writeBoolean(v: Boolean) {
                            map[key] = v.toString()
                        }

                        override fun writeByte(v: Byte) {
                            map[key] = v.toString()
                        }

                        override fun writeShort(v: Short) {
                            map[key] = v.toString()
                        }

                        override fun writeInt(v: Int) {
                            map[key] = v.toString()
                        }

                        override fun writeLong(v: Long) {
                            map[key] = v.toString()
                        }

                        override fun writeFloat(v: Float) {
                            map[key] = v.toString()
                        }

                        override fun writeDouble(v: Double) {
                            map[key] = v.toString()
                        }

                        override fun writeString(v: String) {
                            map[key] = v
                        }

                        override fun writeList(size: Int): ValueOutput {
                            throw DataWriteException("List values are unsupported")
                        }

                        override fun writeMap(): MapOutput {
                            throw DataWriteException("Map values are unsupported")
                        }

                        override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<ValueOutput, T>) {
                            if (optional.isPresent) {
                                ifPresent.accept(this, optional.get())
                            }
                        }
                    }
                }
            }
        }
    }

    interface MapOutputTo<P> : MapOutput {
        fun finish(): P
    }
}