package net.typho.data_util.codec

import net.typho.data_util.DataReader
import net.typho.data_util.DataWriter
import org.jetbrains.annotations.Nullable
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URI
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import kotlin.io.path.Path
import kotlin.reflect.jvm.kotlinProperty

fun <T : Comparable<T>> Codec<T>.withRange(range: ClosedRange<T>) = withCondition { it in range }

fun <T : Comparable<T>> Codec<T>.withRange(min: T, max: T) = withRange(min..max)

fun <T : Comparable<T>> Codec<T>.withMin(min: T) = withCondition { it >= min }

fun <T : Comparable<T>> Codec<T>.withMax(max: T) = withCondition { it <= max }

/**
 * [Documentation](https://github.com/TheTypholorian/data_util/wiki#codec)
 */
interface Codec<T> : DataReader<T>, DataWriter<T> {
    companion object {
        private fun <T> simple(name: String, read: DataReader<T>, write: DataWriter<T>) = object : Codec<T> {
            override fun read(input: ValueInput): T {
                return read.read(input)
            }

            override fun write(output: ValueOutput, value: T) {
                write.write(output, value)
            }

            override fun toString(): String {
                return name
            }
        }

        @JvmField
        val BOOL = simple("Boolean Codec", ValueInput::readBoolean, ValueOutput::writeBoolean)
        @JvmField
        val BYTE = simple("Byte Codec", ValueInput::readByte, ValueOutput::writeByte)
        @JvmField
        val SHORT = simple("Short Codec", ValueInput::readShort, ValueOutput::writeShort)
        @JvmField
        val INT = simple("Int Codec", ValueInput::readInt, ValueOutput::writeInt)
        @JvmField
        val LONG = simple("Long Codec", ValueInput::readLong, ValueOutput::writeLong)
        @JvmField
        val FLOAT = simple("Float Codec", ValueInput::readFloat, ValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple("Double Codec", ValueInput::readDouble, ValueOutput::writeDouble)
        @JvmField
        val STRING = simple("String Codec", ValueInput::readString, ValueOutput::writeString)

        @JvmField
        val FILE = STRING.map({ File(it) }, { it.toString() })
        @JvmField
        val PATH = STRING.map({ Path(it) }, { it.toString() })
        @JvmField
        val URI = STRING.map({ URI(it) }, { it.toString() })

        @JvmOverloads
        @JvmStatic
        fun <V> dynamicMap(valueCodec: Codec<V>, errorOnDuplicateKeys: Boolean = true) = dynamicMap({ it }, { it }, valueCodec, errorOnDuplicateKeys)

        @JvmOverloads
        @JvmStatic
        fun <K, V> dynamicMap(
            readKey: Function<String, K>,
            writeKey: Function<K, String>,
            valueCodec: Codec<V>,
            errorOnDuplicateKeys: Boolean = true
        ): Codec<Map<K, V>> = simple(
            "Dynamic Map Codec with value codec $valueCodec",
            { input ->
                val map = mutableMapOf<K, V>()
                input.readDynamicMap { key, value ->
                    val key = readKey.apply(key)

                    if (errorOnDuplicateKeys && map.containsKey(key)) {
                        input.error("Duplicate key in dynamic map '$key'")
                    }

                    map[key] = valueCodec.read(value)
                }
                map
            },
            { output, value ->
                output.writeDynamicMap(
                    value.map { (key, value) ->
                        writeKey.apply(key) to Consumer<ValueOutput> { valueCodec.write(it, value) }
                    }.iterator()
                )
            }
        )

        @JvmOverloads
        @JvmStatic
        fun <T> multi(
            primary: Codec<T>,
            read: Function<ValueType, DataReader<T>>,
            write: Function<T, Codec<T>> = { primary }
        ) = simple(
            "Multi Codec with primary codec $primary",
            { input ->
                val type = input.type

                if (type == null) {
                    primary.read(input)
                } else {
                    read.apply(type).read(input)
                }
            },
            { output, value ->
                if (output.areTypesWritten) {
                    write.apply(value)
                } else {
                    primary
                }.write(output, value)
            }
        )

        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <V, T> versioned(
            versionCodec: Codec<V>,
            versionGetter: Function<T, V>,
            dataCodec: Function<V, Codec<T>>,
            versionKey: String = "version"
        ) = simple(
            "Versioned Codec with version codec $versionCodec, version getter $versionGetter, version key $versionKey",
            { input ->
                var value: T? = null

                input.readStaticMap(listOf(versionKey)) {
                    value = dataCodec.apply(versionCodec.read(it)).read(input)
                }

                value as T
            },
            { output, value ->
                val version = versionGetter.apply(value)
                output.writeStaticMap(listOf(versionKey to Consumer<ValueOutput> { versionCodec.write(it, version) }).iterator())
                dataCodec.apply(version).write(output, value)
            }
        )

        @JvmOverloads
        @JvmStatic
        fun <T> versioned(
            versionGetter: Function<T, String>,
            dataCodec: Function<String, Codec<T>>,
            versionKey: String = "version"
        ) = versioned(STRING, versionGetter, dataCodec, versionKey)

        @JvmOverloads
        @JvmStatic
        fun <E : Enum<E>> enumCodec(cls: Class<E>, caseSensitive: Boolean = false): Codec<E> {
            return object : Codec<E> {
                override fun read(input: ValueInput): E {
                    return input.readEnum(cls, caseSensitive)
                }

                override fun write(output: ValueOutput, value: E) {
                    output.writeEnum(value)
                }

                override fun toString(): String {
                    return "Enum Codec for $cls"
                }
            }
        }

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun getDefaultCodec(type: Type): Codec<*>? {
            fun <T> listToSet(codec: Codec<List<T>>): Codec<Set<T>> {
                return codec.map({ it.toSet() }, { it.toList() })
            }

            return when (type) {
                is ParameterizedType -> when (type.rawType) {
                    List::class.java -> getDefaultCodec(type.actualTypeArguments[0])?.listOf()
                    Set::class.java -> getDefaultCodec(type.actualTypeArguments[0])?.let { listToSet(it.listOf()) }
                    Map::class.java -> if (type.actualTypeArguments[0] == String::class.java) {
                        getDefaultCodec(type.actualTypeArguments[1])?.let { value -> dynamicMap(value) }
                    } else null
                    else -> null
                }
                is Class<*> -> when (type) {
                    Boolean::class.java, Boolean::class.javaObjectType -> BOOL
                    Byte::class.java, Byte::class.javaObjectType -> BYTE
                    Short::class.java, Short::class.javaObjectType -> SHORT
                    Int::class.java, Int::class.javaObjectType -> INT
                    Long::class.java, Long::class.javaObjectType -> LONG
                    Float::class.java, Float::class.javaObjectType -> FLOAT
                    Double::class.java, Double::class.javaObjectType -> DOUBLE
                    String::class.java -> STRING
                    File::class.java -> FILE
                    @Suppress("RemoveRedundantQualifierName")
                    java.net.URI::class.java -> URI
                    Path::class.java -> PATH
                    else -> if (type.isEnum) enumCodec(type as Class<out Enum<*>>) else null
                }
                else -> null
            }
        }

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun getDefaultCodec(field: Field): Codec<*>? {
            var codec = getDefaultCodec(field.genericType) ?: return null

            if (field.isAnnotationPresent(Nullable::class.java) || field.kotlinProperty?.returnType?.isMarkedNullable == true) {
                codec = codec.optional()
            }

            return codec
        }

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @JvmOverloads
        @JvmStatic
        inline fun <reified T : Any> reflect(noinline config: CodecReflectionConfig<T>.() -> Unit = { }) = reflect(T::class.java, config)

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <T : Any> reflect(cls: Class<T>, config: Consumer<CodecReflectionConfig<T>> = { }) = reflect(CodecReflectionConfig(cls).apply { config.accept(this) })

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <T : Any> reflect(cls: Class<T>, config: CodecReflectionConfig<T>.() -> Unit = { }) = reflect(CodecReflectionConfig(cls).apply(config))

        /**
         * [Documentation](https://github.com/TheTypholorian/data_util/wiki#reflection)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : Any> reflect(config: CodecReflectionConfig<T>): MapCodec<T> {
            data class Entry(
                @JvmField
                val field: Field,
                @JvmField
                val codec: Codec<*>
            )

            val fields = mutableListOf<Field>()
            var temp: Class<*>? = config.cls

            while (temp != null) {
                val clsFields = temp.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.modifiers) }
                fields.addAll(0, clsFields)
                temp = temp.superclass
            }

            fields.forEach { it.isAccessible = true }

            val types = fields.map { it.type }.toTypedArray()
            val constructor = config.cls.getConstructor(*types)
            val entries = fields.map { field -> Entry(field, config.fields[field.name] ?: getDefaultCodec(field) ?: throw IllegalArgumentException("No codec found for field ${field.name}, use CodecReflectionConfig.field to set one")) }

            return object : MapCodec<T> {
                override val keys = entries.map { it.field.name }

                override fun read(input: ValueInput): T {
                    val args = mutableListOf<Any?>()
                    val entries = entries.iterator()
                    input.readStaticMap(keys) { input ->
                        args.add(entries.next().codec.read(input))
                    }
                    return constructor.newInstance(*args.toTypedArray()) as T
                }

                fun <V> write(field: Field, codec: Codec<V>, value: T): Consumer<ValueOutput> {
                    return { codec.write(it, field.get(value) as V) }
                }

                override fun write(output: ValueOutput, value: T) {
                    output.writeStaticMap(entries.map {
                        it.field.name to write(it.field, it.codec, value)
                    }.iterator())
                }

                override fun toString(): String {
                    return "Reflected MapCodec of ${config.cls}, fields: {${entries.joinToString(separator = "\n", prefix = "\n", transform = { "'${it.field.name}' with codec ${it.codec}" }).replace("\n", "\n\t")}\n}"
                }
            }
        }
    }

    fun withCondition(condition: Predicate<T>) = withCondition(condition) { "Value $it does not match condition $condition" }

    fun withCondition(condition: Predicate<T>, error: Function<T, String>): Codec<T> {
        val parent = this
        return object : Codec<T> {
            override fun read(input: ValueInput): T {
                val value = parent.read(input)

                if (!condition.test(value)) {
                    input.error(error.apply(value))
                }

                return value
            }

            override fun write(output: ValueOutput, value: T) {
                if (!condition.test(value)) {
                    output.error(error.apply(value))
                }

                parent.write(output, value)
            }

            override fun toString(): String {
                return "$parent, with condition $condition"
            }
        }
    }

    fun optional(): Codec<T?> {
        val parent = this
        return object : Codec<T?> {
            override fun read(input: ValueInput): T? {
                return input.readOptional(parent)
            }

            override fun write(output: ValueOutput, value: T?) {
                output.writeOptional(value, parent)
            }

            override fun toString(): String {
                return "$parent, optional"
            }
        }
    }

    fun optional(default: T): Codec<T> {
        val parent = this
        return object : Codec<T> {
            override fun read(input: ValueInput): T {
                return input.readOptional(parent) ?: default
            }

            override fun write(output: ValueOutput, value: T) {
                output.writeOptional(value, parent)
            }

            override fun toString(): String {
                return "$parent, optional with default $default"
            }
        }
    }

    fun listOf(): Codec<List<T>> {
        val parent = this
        return object : Codec<List<T>> {
            override fun read(input: ValueInput): List<T> {
                val list = mutableListOf<T>()
                input.readDynamicList { list.add(parent.read(it)) }
                return list
            }

            override fun write(output: ValueOutput, value: List<T>) {
                output.writeDynamicList(value.map { value -> Consumer<ValueOutput> { output -> parent.write(output, value) } })
            }

            override fun toString(): String {
                return "$parent as list"
            }
        }
    }

    /**
     * More optimized for packets than doing a normal [listOf] then checking the size.
     */
    fun listOf(size: Int): Codec<List<T>> {
        val parent = this
        return object : Codec<List<T>> {
            override fun read(input: ValueInput): List<T> {
                val list = mutableListOf<T>()
                input.readStaticList(size) { list.add(parent.read(it)) }
                return list
            }

            override fun write(output: ValueOutput, value: List<T>) {
                if (value.size != size) {
                    output.error("List must have $size elements, instead has ${value.size}")
                }

                output.writeStaticList(value.map { value -> Consumer<ValueOutput> { output -> parent.write(output, value) } }.iterator())
            }

            override fun toString(): String {
                return "$parent as list"
            }
        }
    }

    fun <N> map(
        read: Function<T, N>,
        write: Function<N, T>
    ): Codec<N> {
        val parent = this
        return object : Codec<N> {
            override fun read(input: ValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun write(output: ValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped with read $read and write $write"
            }
        }
    }
}