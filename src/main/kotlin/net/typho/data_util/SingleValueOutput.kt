package net.typho.data_util

import java.util.Optional
import java.util.function.BiConsumer

interface SingleValueOutput {
    fun writeBoolean(v: Boolean)

    fun writeByte(v: Byte)

    fun writeShort(v: Short)

    fun writeInt(v: Int)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeString(v: String)

    fun writeList(size: Int): SingleValueOutput

    fun writeMap(): MapOutput

    fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>)

    companion object {
        @JvmStatic
        fun toList(size: Int, list: MutableList<Any?>): SingleValueOutput = object : SingleValueOutput {
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

            override fun writeList(size: Int): SingleValueOutput {
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

            override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>) {
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