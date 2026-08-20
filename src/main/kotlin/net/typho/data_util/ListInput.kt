package net.typho.data_util

import java.io.DataInput
import java.util.Optional
import java.util.function.Function

interface ListInput : SingleValueInput {
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

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                val value = read<Any?>()
                return if (value == null) Optional.empty() else Optional.of(ifPresent.apply(SingleValueInput.fromObject(value)))
            }
        }

        @JvmOverloads
        @JvmStatic
        fun fromData(input: DataInput, size: Int = input.readInt()): ListInput = object : ListInput {
            override var left: Int = size

            fun decrement() {
                if (left == 0) {
                    throw DataReadException("Read too many entries from list of size $size")
                }

                left--
            }

            override fun readBoolean(): Boolean {
                decrement()
                return input.readBoolean()
            }

            override fun readByte(): Byte {
                decrement()
                return input.readByte()
            }

            override fun readShort(): Short {
                decrement()
                return input.readShort()
            }

            override fun readInt(): Int {
                decrement()
                return input.readInt()
            }

            override fun readLong(): Long {
                decrement()
                return input.readLong()
            }

            override fun readFloat(): Float {
                decrement()
                return input.readFloat()
            }

            override fun readDouble(): Double {
                decrement()
                return input.readDouble()
            }

            override fun readString(): String {
                decrement()
                return input.readUTF()
            }

            override fun readList(): ListInput {
                decrement()
                return fromData(input)
            }

            override fun readMap(): MapInput {
                decrement()
                return MapInput.fromData(input)
            }

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                decrement()
                val present = input.readBoolean()
                return if (present) Optional.of(ifPresent.apply(SingleValueInput.fromData(input))) else Optional.empty()
            }
        }
    }
}