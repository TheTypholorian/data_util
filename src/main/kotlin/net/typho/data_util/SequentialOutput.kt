package net.typho.data_util

import net.typho.data_util.SingleValueOutput.Companion.writeVarInt
import java.io.DataOutput
import java.util.function.BiConsumer

interface SequentialOutput {
    val left: Int

    fun writeNextEntry(): SingleValueOutput

    companion object {
        @JvmStatic
        fun toList(size: Int, list: MutableList<Any?>): SequentialOutput = object : SequentialOutput {
            override var left: Int = size

            override fun writeNextEntry(): SingleValueOutput {
                if (left == 0) {
                    throw DataReadException("Wrote too many entries to list of size $size")
                }

                left--
                return SingleValueOutput.toConsumer(list::add)
            }
        }

        @JvmStatic
        fun toMap(keys: List<String>, map: MutableMap<String, Any?>): SequentialOutput = object : SequentialOutput {
            var index = 0
            override val left: Int
                get() = keys.size - index

            override fun writeNextEntry(): SingleValueOutput {
                if (index == keys.size) {
                    throw DataReadException("Already wrote all entries to map")
                }

                val key = keys[index++]

                return SingleValueOutput.toConsumer { map[key] = it }
            }
        }

        @JvmStatic
        fun toStringMap(keys: List<String>, map: MutableMap<String, String>): SequentialOutput = object : SequentialOutput {
            var index = 0
            override val left: Int
                get() = keys.size - index

            override fun writeNextEntry(): SingleValueOutput {
                if (index == keys.size) {
                    throw DataReadException("Already wrote all entries to map")
                }

                val key = keys[index++]

                return SingleValueOutput.toConsumer {
                    if (it is List<*>) {
                        throw DataWriteException("List values are unsupported in string maps")
                    }

                    if (it is Map<*, *>) {
                        throw DataWriteException("Map values are unsupported in string maps")
                    }

                    map[key] = it.toString()
                }
            }
        }

        @JvmOverloads
        @JvmStatic
        fun toData(size: Int, output: DataOutput, writeSize: Boolean = true): SequentialOutput {
            if (writeSize) {
                output.writeVarInt(size)
            }

            return object : SequentialOutput {
                override var left: Int = size

                override fun writeNextEntry(): SingleValueOutput {
                    if (left == 0) {
                        throw DataReadException("Wrote too many entries to list of size $size")
                    }

                    left--
                    return SingleValueOutput.toData(output)
                }
            }
        }
    }
}