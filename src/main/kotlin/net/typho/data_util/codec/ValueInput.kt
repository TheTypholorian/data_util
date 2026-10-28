package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader

/**
 * All methods can only be called once per instance (except [readStaticList] and [readStaticMap])
 */
interface ValueInput {
    /**
     * Only present for data formats such as json, will be null for packet serialization and XML.
     *
     * Note the difference between [ValueType.NULL] (the value is null) and the property being `null` (the type isn't known).
     *
     * Allows different reading logic based on the contents, for example:
     * ```json
     * {
     *     "entries": "abc"
     * }
     * ```
     * would have [ValueType.STRING], so you know to read a string then wrap it in a list.
     * ```json
     * {
     *     "entries": [ "abc" ]
     * }
     * ```
     * would have [ValueType.ARRAY], so you know to read it as a list.
     */
    val type: ValueType?

    fun readBoolean(): Boolean

    fun readByte(): Byte

    fun readShort(): Short

    fun readInt(): Int

    fun readLong(): Long

    fun readFloat(): Float

    fun readDouble(): Double

    fun readString(): String

    fun <E : Enum<E>> readEnum(cls: Class<E>, ignoreCase: Boolean = true): E {
        val name = readString()
        return cls.enumConstants.firstOrNull { it.name.equals(name, ignoreCase = ignoreCase) } ?: throw DataReadException("Enum ${cls.name} does not have constant '$name', options: ${cls.enumConstants}")
    }

    /**
     * See [ValueOutput.writeDynamicList]
     */
    fun readDynamicList(): Iterator<ValueInput>

    /**
     * See [ValueOutput.writeStaticList]
     */
    fun readStaticList(size: Int): Iterator<ValueInput>

    /**
     * See [ValueOutput.writeDynamicMap]
     */
    fun readDynamicMap(): Iterator<Pair<String, ValueInput>>

    /**
     * See [ValueOutput.writeStaticMap]
     *
     * Can be called multiple times (ex. for reading a version number, though that should be done with [Codec.versioned]).
     *
     * The returned iterator will have the same order as [keys].
     */
    fun readStaticMap(keys: List<String>): Iterator<ValueInput>

    fun <V> readOptional(ifPresent: DataReader<V>): V?
}