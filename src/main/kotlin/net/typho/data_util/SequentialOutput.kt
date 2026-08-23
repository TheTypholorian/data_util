package net.typho.data_util

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

                return object : SingleValueOutput {
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

                    override fun <E : Enum<E>> writeEnum(v: E) {
                        writeString(v.name)
                    }

                    override fun writeList(size: Int): SequentialOutput {
                        val list = ArrayList<Any?>(size)
                        map[key] = list
                        return toList(size, list)
                    }

                    override fun writeStaticMap(keys: List<String>): SequentialOutput {
                        val map1 = mutableMapOf<String, Any?>()
                        map[key] = map1
                        return toMap(keys, map1)
                    }

                    override fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>) {
                        if (v == null) {
                            map[key] = null
                        } else {
                            ifPresent.write(this, v)
                        }
                    }
                }
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

                return object : SingleValueOutput {
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

                    override fun <E : Enum<E>> writeEnum(v: E) {
                        writeString(v.name)
                    }

                    override fun writeList(size: Int): SequentialOutput {
                        throw DataWriteException("List values are unsupported in string maps")
                    }

                    override fun writeStaticMap(keys: List<String>): SequentialOutput {
                        throw DataWriteException("Map values are unsupported in string maps")
                    }

                    override fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>) {
                        if (v != null) {
                            ifPresent.write(this, v)
                        }
                    }
                }
            }
        }

        @JvmOverloads
        @JvmStatic
        fun toData(size: Int, output: DataOutput, writeSize: Boolean = true): SequentialOutput {
            if (writeSize) {
                output.writeInt(size)
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