package net.typho.tv_lib.io.codec

import net.typho.tv_lib.io.DataWriteException
import java.util.Optional
import java.util.function.BiConsumer
import kotlin.collections.set

interface MapOutput {
    fun writeEntry(key: String): SingleValueOutput

    companion object {
        @JvmStatic
        fun toMap(map: MutableMap<String, Any?>): MapOutputResult<Map<String, Any?>> = object : MapOutputResult<Map<String, Any?>> {
            override fun finish(): Map<String, Any?> {
                return map
            }

            override fun writeEntry(key: String): SingleValueOutput {
                if (map.containsKey(key)) {
                    throw DataWriteException("Duplicate key $key")
                }

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

                    override fun writeList(size: Int): SingleValueOutput {
                        val list = ArrayList<Any?>(size)
                        map[key] = list
                        return SingleValueOutput.toList(size, list)
                    }

                    override fun writeMap(): MapOutput {
                        val map1 = mutableMapOf<String, Any?>()
                        map[key] = map1
                        return toMap(map1)
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
        fun toStringMap(map: MutableMap<String, String>): MapOutputResult<Map<String, String>> = object :
            MapOutputResult<Map<String, String>> {
            val alreadyWrote = map.keys.toMutableSet()

            override fun finish(): Map<String, String> {
                return map
            }

            override fun writeEntry(key: String): SingleValueOutput {
                if (!alreadyWrote.add(key)) {
                    throw DataWriteException("Duplicate key $key")
                }

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

                    override fun writeList(size: Int): SingleValueOutput {
                        throw DataWriteException("List values are unsupported")
                    }

                    override fun writeMap(): MapOutput {
                        throw DataWriteException("Map values are unsupported")
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