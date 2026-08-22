package net.typho.data_util

import java.util.Optional
import java.util.function.BiConsumer
import kotlin.collections.set

interface MapOutput {
    fun writeNextEntry(): SingleValueOutput

    companion object {
        @JvmStatic
        fun toMap(keys: List<String>, map: MutableMap<String, Any?>): MapOutputResult<Map<String, Any?>> = object : MapOutputResult<Map<String, Any?>> {
            var index = 0

            override fun finish(): Map<String, Any?> {
                return map
            }

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

                    override fun writeList(size: Int): ListOutput {
                        val list = ArrayList<Any?>(size)
                        map[key] = list
                        return SingleValueOutput.toList(size, list)
                    }

                    override fun writeMap(keys: List<String>): MapOutput {
                        val map1 = mutableMapOf<String, Any?>()
                        map[key] = map1
                        return toMap(keys, map1)
                    }

                    override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>) {
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
        fun toStringMap(keys: List<String>, map: MutableMap<String, String>): MapOutputResult<Map<String, String>> = object : MapOutputResult<Map<String, String>> {
            var index = 0

            override fun finish(): Map<String, String> {
                return map
            }

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

                    override fun writeList(size: Int): ListOutput {
                        throw DataWriteException("List values are unsupported in string maps")
                    }

                    override fun writeMap(keys: List<String>): MapOutput {
                        throw DataWriteException("Map values are unsupported in string maps")
                    }

                    override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>) {
                        if (optional.isPresent) {
                            ifPresent.accept(this, optional.get())
                        }
                    }
                }
            }
        }
    }
}