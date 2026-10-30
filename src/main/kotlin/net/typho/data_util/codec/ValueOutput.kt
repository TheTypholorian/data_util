package net.typho.data_util.codec

import net.typho.data_util.DataWriter
import net.typho.data_util.error.DataWriteException
import net.typho.data_util.error.ErrorStack
import java.util.function.Consumer

/**
 * All methods can only be called once per instance (except [writeStaticList] and [writeStaticMap])
 */
interface ValueOutput {
    /**
     * If the corresponding [ValueInput.type] will equal `null`, this value is false, otherwise true.
     *
     * Signifies if the types will be known when read, which is true for data formats like json but false for packet serialization and XML.
     *
     * Used by [Codec.multi].
     */
    val areTypesWritten: Boolean
    val errorStack: ErrorStack?
        get() = null

    fun error(message: String): Nothing = throw DataWriteException(message, errorStack)

    fun writeBoolean(v: Boolean)

    fun writeByte(v: Byte)

    fun writeShort(v: Short)

    fun writeInt(v: Int)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeString(v: String)

    fun <E : Enum<E>> writeEnum(v: E) {
        writeString(v.name)
    }

    /**
     * Writes a list of dynamic size.
     *
     * See [writeStaticList]
     */
    fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>)

    /**
     * Writes a list of static size.
     *
     * The size should be known separately by both writer and reader, so it does not need to be written.
     * This only matters for packet serialization, for data formats such as json and XML this method will be equivalent to [writeDynamicList].
     */
    fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>)

    /**
     * Writes a map with dynamic keys.
     *
     * See [writeStaticMap]
     */
    fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>)

    /**
     * Writes a map with static keys.
     *
     * The keys should be known separately by both writer and reader, so they do not need to be written.
     * This only matters for packet serialization, for data formats such as json and XML this method will be equivalent to [writeDynamicMap].
     *
     * Can be called multiple times (ex. for writing a version number, though that should be done with [Codec.versioned]).
     */
    fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>)

    fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>)

    interface WithResult<R> : ValueOutput {
        fun finish(): R
    }
}