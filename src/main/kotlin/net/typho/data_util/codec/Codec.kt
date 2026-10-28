package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader
import net.typho.data_util.DataWriteException
import net.typho.data_util.DataWriter
import net.typho.data_util.anno.FieldCodec
import net.typho.data_util.anno.FieldDefault
import net.typho.data_util.anno.FieldRange
import org.jetbrains.annotations.Nullable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import kotlin.reflect.jvm.kotlinProperty

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
        val NUMBER_CLASSES = listOf(
            Byte::class.java,
            Short::class.java,
            Int::class.java,
            Long::class.java,
            Float::class.java,
            Double::class.java
        )

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

        @JvmOverloads
        @JvmStatic
        fun <E : Enum<E>> enumCodec(cls: Class<E>, caseSensitive: Boolean = false): Codec<E> {
            return object : Codec<E> {
                override fun read(input: ValueInput): E {
                    try {
                        return input.readEnum(cls, caseSensitive)
                    } catch (e: RuntimeException) {
                        throw DataReadException("Error while reading enum entry of class $cls", e, true, false)
                    }
                }

                override fun write(output: ValueOutput, value: E) {
                    try {
                        output.writeEnum(value)
                    } catch (e: RuntimeException) {
                        throw DataWriteException("Error while writing enum entry of class $cls", e, true, false)
                    }
                }

                override fun toString(): String {
                    return "Enum Codec for $cls"
                }
            }
        }

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
                input.readDynamicMap().forEach { (key, value) ->
                    val key = readKey.apply(key)

                    if (errorOnDuplicateKeys && map.containsKey(key)) {
                        throw DataReadException("Duplicate key in dynamic map '$key'")
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

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> getPrimitiveCodec(cls: Class<T>): Codec<T>? {
            return when (cls) {
                Boolean::class.java, Boolean::class.javaObjectType -> BOOL as Codec<T>
                Byte::class.java, Byte::class.javaObjectType -> BYTE as Codec<T>
                Short::class.java, Short::class.javaObjectType -> SHORT as Codec<T>
                Int::class.java, Int::class.javaObjectType -> (INT) as Codec<T>
                Long::class.java, Long::class.javaObjectType -> LONG as Codec<T>
                Float::class.java, Float::class.javaObjectType -> FLOAT as Codec<T>
                Double::class.java, Double::class.javaObjectType -> DOUBLE as Codec<T>
                String::class.java -> STRING as Codec<T>
                else -> if (cls.isEnum) enumCodec(cls as Class<out Enum<*>>) as Codec<T> else null
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun getClassCodec(owner: Class<*>, type: Class<*>): Codec<*>? {
            var codec = getPrimitiveCodec(type)

            if (codec == null) {
                try {
                    val codecField = type.getDeclaredField("CODEC")
                    codecField.isAccessible = true
                    codec = codecField.get(null) as Codec<*>
                } catch (_: NoSuchFieldException) {
                }
            }

            return codec
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun getFieldCodec(owner: Class<*>, field: Field): Codec<*> {
            var codec: Codec<*>? = null

            for (anno in field.annotations) {
                if (anno is FieldCodec) {
                    try {
                        var target = anno.owner.java

                        if (target == Any::class.java) {
                            target = field.type
                        }

                        val codecField = target.getDeclaredField(anno.value)

                        if (!Modifier.isStatic(codecField.modifiers)) {
                            throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} has @FieldCodec pointing to a non-static codec field")
                        }

                        if (!Codec::class.java.isAssignableFrom(codecField.type)) {
                            throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} has @FieldCodec pointing to a field that isn't a codec")
                        }

                        codecField.isAccessible = true

                        codec = codecField.get(null) as Codec<*>
                        break
                    } catch (_: NoSuchFieldException) {
                        throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} has @FieldCodec pointing to a nonexistent field")
                    }
                }
            }

            codec = codec ?: getClassCodec(owner, field.type)

            if (codec == null) {
                if (List::class.java.isAssignableFrom(field.type) && field.type != List::class.java) {
                    throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} extends List but doesn't provide a codec, automatic list codecs are only for raw List types.")
                }

                var type = field.genericType as ParameterizedType
                var depth = 0

                while (type.rawType == List::class.java) {
                    depth++

                    val arg = type.actualTypeArguments[0]

                    if (arg is ParameterizedType) {
                        type = arg
                    } else if (arg is Class<*>) {
                        codec = getClassCodec(owner, arg) ?: throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} is a List and its deep type has no automatic codec")
                        break
                    }
                }

                if (depth > 0 && codec != null) {
                    repeat(depth) {
                        codec = codec!!.listOf()
                    }
                }
            }

            var codec1 = codec ?: throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} is missing a codec")

            for (anno in field.annotations) {
                if (anno is FieldRange) {
                    if (!(Number::class.java.isAssignableFrom(field.type) || NUMBER_CLASSES.contains(field.type))) {
                        throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} has @FieldRange but isn't a number field")
                    }

                    codec1 = codec1.withCondition(
                        {
                            val n = (it as Number).toDouble()
                            n >= anno.min && n <= anno.max
                        },
                        { "$it must be between ${anno.min} and ${anno.max}" },
                        "range [${anno.min}, ${anno.max}]"
                    )
                    break
                }
            }

            val default = field.annotations.filterIsInstance<FieldDefault>().firstOrNull()?.let { anno ->
                var target = anno.owner.java

                if (target == Any::class.java) {
                    when (field.type) {
                        Boolean::class.java, Boolean::class.javaObjectType -> return@let anno.value.toBoolean()
                        Byte::class.java, Byte::class.javaObjectType -> return@let anno.value.toByte()
                        Short::class.java, Short::class.javaObjectType -> return@let anno.value.toShort()
                        Int::class.java, Int::class.javaObjectType -> return@let anno.value.toInt()
                        Long::class.java, Long::class.javaObjectType -> return@let anno.value.toLong()
                        Float::class.java, Float::class.javaObjectType -> return@let anno.value.toFloat()
                        Double::class.java, Double::class.javaObjectType -> return@let anno.value.toDouble()
                        String::class.java -> return@let anno.value
                        else -> target = field.type
                    }
                }

                try {
                    val defaultField = target.getDeclaredField(anno.value)

                    if (!Modifier.isStatic(defaultField.modifiers)) {
                        throw IllegalStateException("Field ${target.name} ${field.type.name} ${field.name} has @FieldDefault pointing to a non-static field")
                    }

                    if (!field.type.isAssignableFrom(defaultField.type)) {
                        throw IllegalStateException("Field ${target.name} ${field.type.name} ${field.name} has @FieldDefault pointing to a field that isn't the same type as the field")
                    }

                    defaultField.isAccessible = true

                    return@let defaultField.get(null)
                } catch (e: NoSuchFieldException) {
                    throw IllegalStateException("Field ${target.name} ${field.type.name} ${field.name} has @FieldDefault pointing to a nonexistent field")
                }
            }

            fun <T> generics(codec: Codec<T>): Codec<*> {
                return if (default != null || field.isAnnotationPresent(Nullable::class.java) || field.kotlinProperty?.returnType?.isMarkedNullable == true) {
                    if (default == null) codec.optional() else codec.optional(default as T)
                } else {
                    codec
                }
            }

            return generics(codec1)
        }

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
                val version = versionCodec.read(input.readStaticMap(listOf(versionKey)).next())
                dataCodec.apply(version).read(input)
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

        /**
         * @param reverseInheritanceOrder defines where superclass fields are in the constructor, True = at the end and False = at the start (defaults to false because it's the IntelliJ default)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> reflect(cls: Class<T>, reverseInheritanceOrder: Boolean = false): Codec<T> {
            data class Entry(
                @JvmField
                val field: Field,
                @JvmField
                val codec: Codec<*>
            )

            val fields = mutableListOf<Field>()
            var temp: Class<*>? = cls

            while (temp != null) {
                val clsFields = temp.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.modifiers) }

                if (reverseInheritanceOrder) {
                    fields.addAll(clsFields)
                } else {
                    fields.addAll(0, clsFields)
                }

                temp = temp.superclass
            }

            fields.forEach { it.isAccessible = true }

            val types = fields.map { it.type }.toTypedArray()
            val constructor = cls.constructors.firstOrNull {
                it.parameterTypes.contentEquals(types)
            } ?: throw IllegalArgumentException("$cls is missing a constructor matching ${fields.map { "${it.type} ${it.name}" }}")

            val entries = fields.map { field -> Entry(field, getFieldCodec(cls, field)) }

            return object : MapCodec<T> {
                override val keys = entries.map { it.field.name }

                override fun read(input: Iterator<ValueInput>): T {
                    val args = entries.map {
                        try {
                            it.codec.read(input.next())
                        } catch (e: RuntimeException) {
                            throw DataReadException("Error while reading map entry ${it.field.name}", e, true, false)
                        }
                    }
                    return constructor.newInstance(*args.toTypedArray()) as T
                }

                fun <V> write(field: Field, codec: Codec<V>, value: T): Consumer<ValueOutput> {
                    try {
                        val value = field.get(value) as V
                        return { codec.write(it, value) }
                    } catch (e: RuntimeException) {
                        throw DataWriteException("Error while writing map entry ${field.name}", e, true, false)
                    }
                }

                override fun write(value: T): Iterator<Pair<String, Consumer<ValueOutput>>> {
                    return entries.map {
                        it.field.name to write(it.field, it.codec, value)
                    }.iterator()
                }

                override fun toString(): String {
                    return "Reflected MapCodec of $cls, fields: {${entries.joinToString(separator = "\n", prefix = "\n", transform = { "'${it.field.name}' with codec ${it.codec}" }).replace("\n", "\n\t")}\n}"
                }
            }
        }
    }

    fun withCondition(condition: Predicate<T>, error: Function<T, String>, name: String): Codec<T> {
        val parent = this
        return object : Codec<T> {
            override fun read(input: ValueInput): T {
                val value = parent.read(input)

                if (!condition.test(value)) {
                    throw DataReadException(error.apply(value))
                }

                return value
            }

            override fun write(output: ValueOutput, value: T) {
                if (!condition.test(value)) {
                    throw DataWriteException(error.apply(value))
                }

                parent.write(output, value)
            }

            override fun toString(): String {
                return "$parent, with condition $name"
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
                return input.readDynamicList().asSequence().map { parent.read(it) }.toList()
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
     * More optimized for packets than doing a normal [listOf] plus checking the size.
     */
    fun listOf(size: Int): Codec<List<T>> {
        val parent = this
        return object : Codec<List<T>> {
            override fun read(input: ValueInput): List<T> {
                return input.readStaticList(size).asSequence().map { parent.read(it) }.toList()
            }

            override fun write(output: ValueOutput, value: List<T>) {
                if (value.size != size) {
                    throw DataReadException("List must have $size elements, instead has ${value.size}")
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