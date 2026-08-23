package net.typho.data_util

import java.io.DataInput
import java.util.function.Function

interface SequentialInput {
    val left: Int

    fun readNextEntry(): SingleValueInput

    fun <T> toList(read: Function<SequentialInput, T>): List<T> {
        val list = ArrayList<T>(left)

        repeat(left) {
            list.add(read.apply(this@SequentialInput))
        }

        return list
    }

    fun <T> iterator(read: Function<SequentialInput, T>) = object : Iterator<T> {
        override fun next() = read.apply(this@SequentialInput)

        override fun hasNext() = left > 0
    }

    companion object {
        @JvmStatic
        fun fromList(list: List<Any?>): SequentialInput = object : SequentialInput {
            var index = 0
            override val left: Int
                get() = list.size - index

            override fun readNextEntry(): SingleValueInput {
                if (left == 0) {
                    throw DataReadException("Read too many entries from list of size ${list.size}")
                }

                return SingleValueInput.fromObject(list[index++])
            }
        }

        @JvmStatic
        fun fromMap(keys: List<String>, map: Map<String, Any?>): SequentialInput = object : SequentialInput {
            var index = 0
            override val left: Int
                get() = keys.size - index

            override fun readNextEntry(): SingleValueInput {
                if (index == keys.size) {
                    throw DataReadException("Already read all entries from map")
                }

                return SingleValueInput.fromObject(map[keys[index++]])
            }
        }

        @JvmStatic
        fun fromStringMap(keys: List<String>, map: Map<String, String>): SequentialInput = object : SequentialInput {
            var index = 0
            override val left: Int
                get() = keys.size - index

            override fun readNextEntry(): SingleValueInput {
                if (index == keys.size) {
                    throw DataReadException("Already read all entries from map")
                }

                return SingleValueInput.fromObject(map[keys[index++]])
            }
        }

        @JvmOverloads
        @JvmStatic
        fun fromData(input: DataInput, size: Int = input.readInt()): SequentialInput = object : SequentialInput {
            var index = 0
            override val left: Int
                get() = size - index

            override fun readNextEntry(): SingleValueInput {
                if (left == 0) {
                    throw DataReadException("Read too many entries from list of size ${size}")
                }

                index--

                return SingleValueInput.fromData(input)
            }
        }
    }
}