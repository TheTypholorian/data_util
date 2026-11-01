package net.typho.data_util.codec

import net.typho.data_util.DataReader
import net.typho.data_util.error.DataReadException
import net.typho.data_util.error.ErrorStack
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * All methods can only be called once per instance (except [readStaticMap])
 * [Documentation](https://github.com/TheTypholorian/data_util/wiki#valueinput-and-valueoutput)
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
     *
     * [Documentation](https://github.com/TheTypholorian/data_util/wiki#value-types)
     */
    val type: ValueType?
    val errorStack: ErrorStack?
        get() = null

    fun error(message: String): Nothing = throw DataReadException(message, errorStack)

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
        return cls.enumConstants.firstOrNull { it.name.equals(name, ignoreCase = ignoreCase) } ?: error("Enum ${cls.name} does not have constant '$name', options: ${cls.enumConstants}")
    }

    /**
     * See [ValueOutput.writeDynamicList]
     *
     * [Documentation](https://github.com/TheTypholorian/data_util/wiki#collections)
     */
    fun readDynamicList(out: Consumer<ValueInput>)

    /**
     * See [ValueOutput.writeStaticList]
     *
     * [Documentation](https://github.com/TheTypholorian/data_util/wiki#collections)
     */
    fun readStaticList(size: Int, out: Consumer<ValueInput>)

    /**
     * See [ValueOutput.writeDynamicMap]
     *
     * [Documentation](https://github.com/TheTypholorian/data_util/wiki#collections)
     */
    fun readDynamicMap(out: BiConsumer<String, ValueInput>)

    /**
     * See [ValueOutput.writeStaticMap]
     *
     * Can be called multiple times (ex. for reading a version number, though that should be done with [Codec.versioned]).
     *
     * The consumer will be called in the same order as [keys].
     *
     * [Documentation](https://github.com/TheTypholorian/data_util/wiki#collections)
     */
    fun readStaticMap(keys: List<String>, out: Consumer<ValueInput>)

    fun <V> readOptional(ifPresent: DataReader<V>): V?
}