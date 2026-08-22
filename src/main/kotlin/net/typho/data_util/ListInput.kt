package net.typho.data_util

import java.io.DataInput
import java.util.Optional
import java.util.function.Function

interface ListInput {
    val left: Int

    fun readNextEntry(): SingleValueInput

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

            override fun readNextEntry(): SingleValueInput {
                if (left == 0) {
                    throw DataReadException("Read too many entries from list of size ${list.size}")
                }

                return SingleValueInput.fromObject(list[index++])
            }
        }

        @JvmOverloads
        @JvmStatic
        fun fromData(input: DataInput, size: Int = input.readInt()): ListInput = object : ListInput {
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