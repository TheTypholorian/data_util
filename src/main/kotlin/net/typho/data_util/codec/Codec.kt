package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader
import net.typho.data_util.DataWriteException
import net.typho.data_util.DataWriter
import net.typho.data_util.SequentialInput
import net.typho.data_util.SequentialOutput
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import net.typho.data_util.anno.FieldCodec
import net.typho.data_util.anno.FieldDefault
import net.typho.data_util.anno.FieldRange
import net.typho.data_util.anno.InlineCodec
import org.jetbrains.annotations.Nullable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Function
import java.util.function.Predicate
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.jvm.java
import kotlin.reflect.jvm.kotlinProperty

interface Codec<T> : DataReader<T>, DataWriter<T> {
    companion object {
        private fun <T> simple(name: String, read: DataReader<T>, write: DataWriter<T>) = object : Codec<T> {
            override fun read(input: SingleValueInput): T {
                return read.read(input)
            }

            override fun write(output: SingleValueOutput, value: T) {
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
        val BOOL = simple("Boolean Codec", SingleValueInput::readBoolean, SingleValueOutput::writeBoolean)
        @JvmField
        val BYTE = simple("Byte Codec", SingleValueInput::readByte, SingleValueOutput::writeByte)
        @JvmField
        val SHORT = simple("Short Codec", SingleValueInput::readShort, SingleValueOutput::writeShort)
        @JvmField
        val INT = simple("Int Codec", SingleValueInput::readInt, SingleValueOutput::writeInt)
        @JvmField
        val VAR_INT = simple("Var Int Codec", SingleValueInput::readVarInt, SingleValueOutput::writeVarInt)
        @JvmField
        val LONG = simple("Long Codec", SingleValueInput::readLong, SingleValueOutput::writeLong)
        @JvmField
        val FLOAT = simple("Float Codec", SingleValueInput::readFloat, SingleValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple("Double Codec", SingleValueInput::readDouble, SingleValueOutput::writeDouble)
        @JvmField
        val STRING = simple("String Codec", SingleValueInput::readString, SingleValueOutput::writeString)

        @JvmStatic
        fun <E : Enum<E>> enumCodec(cls: Class<E>): Codec<E> {
            return object : Codec<E> {
                override fun read(input: SingleValueInput): E {
                    try {
                        return input.readEnum(cls)
                    } catch (e: RuntimeException) {
                        throw DataReadException("Error while reading enum entry of class $cls", e, true, false)
                    }
                }

                override fun write(output: SingleValueOutput, value: E) {
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

        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <T> getPrimitiveCodec(cls: Class<T>, isVarInt: Boolean = false): Codec<T>? {
            return when (cls) {
                Boolean::class.java, java.lang.Boolean::class.java -> BOOL as Codec<T>
                Byte::class.java, java.lang.Byte::class.java -> BYTE as Codec<T>
                Short::class.java, java.lang.Short::class.java -> SHORT as Codec<T>
                Int::class.java, Integer::class.java -> (if (isVarInt) VAR_INT else INT) as Codec<T>
                Long::class.java, java.lang.Long::class.java -> LONG as Codec<T>
                Float::class.java, java.lang.Float::class.java -> FLOAT as Codec<T>
                Double::class.java, java.lang.Double::class.java -> DOUBLE as Codec<T>
                String::class.java -> STRING as Codec<T>
                else -> if (cls.isEnum) enumCodec(cls as Class<out Enum<*>>) as Codec<T> else null
            }
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

            codec = codec ?: getPrimitiveCodec(field.type)

            if (codec == null) {
                try {
                    val codecField = field.type.getDeclaredField("CODEC")
                    codecField.isAccessible = true
                    codec = codecField.get(null) as Codec<*>
                } catch (_: NoSuchFieldException) {
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
                        Boolean::class.java, java.lang.Boolean::class.java -> return@let anno.value.toBoolean()
                        Byte::class.java, java.lang.Byte::class.java -> return@let anno.value.toByte()
                        Short::class.java, java.lang.Short::class.java -> return@let anno.value.toShort()
                        Int::class.java, Integer::class.java -> return@let anno.value.toInt()
                        Long::class.java, java.lang.Long::class.java -> return@let anno.value.toLong()
                        Float::class.java, java.lang.Float::class.java -> return@let anno.value.toFloat()
                        Double::class.java, java.lang.Double::class.java -> return@let anno.value.toDouble()
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

        @JvmStatic
        fun <T> either(primary: Codec<T>, options: List<DataReader<T>>): Codec<T> {
            if (options.isEmpty()) {
                throw IllegalArgumentException("Must specify at least one option")
            }

            val all = mutableListOf<DataReader<T>>(primary)
            all.addAll(options)

            return object : Codec<T> {
                override fun read(input: SingleValueInput): T {
                    return input.readEither(all)
                }

                override fun write(output: SingleValueOutput, value: T) {
                    primary.write(output, value)
                }

                override fun toString(): String {
                    return "Either codec with primary $primary and options [${options.joinToString(separator = "\n", prefix = "\n").replace("\n", "\n\t")}\n]"
                }
            }
        }

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

            val codec = object : MapCodec<T> {
                override val keys = entries.map { it.field.name }

                override fun read(input: SequentialInput): T {
                    val args = entries.map {
                        try {
                            it.codec.read(input.readNextEntry())
                        } catch (e: RuntimeException) {
                            throw DataReadException("Error while reading map entry ${it.field.name}", e, true, false)
                        }
                    }
                    return constructor.newInstance(*args.toTypedArray()) as T
                }

                fun <V> write(field: Field, codec: Codec<V>, value: T, output: SequentialOutput) {
                    try {
                        codec.write(output.writeNextEntry(), field.get(value) as V)
                    } catch (e: RuntimeException) {
                        throw DataWriteException("Error while writing map entry ${field.name}", e, true, false)
                    }
                }

                override fun write(output: SequentialOutput, value: T) {
                    for (entry in entries) {
                        write(entry.field, entry.codec, value, output)
                    }
                }

                override fun toString(): String {
                    return "Reflected MapCodec of $cls, fields: {${entries.joinToString(separator = "\n", prefix = "\n", transform = { "'${it.field.name}' with codec ${it.codec}" }).replace("\n", "\n\t")}\n}"
                }
            }

            return if (cls.isAnnotationPresent(InlineCodec::class.java)) {
                val candidates = entries.withIndex().filter { it.value.codec !is OptionalCodec }

                if (candidates.size != 1) {
                    throw IllegalArgumentException("$cls has @InlineCodec but has ${candidates.size} non-optional fields, must be exactly one.")
                }

                val primary = candidates.first()

                either(codec, listOf(object : DataReader<T> {
                    override fun read(input: SingleValueInput): T {
                        val args = entries.mapIndexed { index, entry -> if (index == primary.index) {
                            try {
                                primary.value.codec.read(input)
                            } catch (e: RuntimeException) {
                                throw DataReadException("Error while reading inlined map entry ${entry.field.name}", e, true, false)
                            }
                        } else (entry.codec as OptionalCodec).default }
                        return constructor.newInstance(*args.toTypedArray()) as T
                    }

                    override fun toString(): String {
                        return "Reflected inline reader of $cls"
                    }
                }))
            } else codec
        }
    }

    fun withCondition(condition: Predicate<T>, error: Function<T, String>, name: String): Codec<T> {
        val parent = this
        return object : Codec<T> {
            override fun read(input: SingleValueInput): T {
                val value = parent.read(input)

                if (!condition.test(value)) {
                    throw DataReadException(error.apply(value))
                }

                return value
            }

            override fun write(output: SingleValueOutput, value: T) {
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

    fun optional(): OptionalCodec<T?> {
        val parent = this
        return object : OptionalCodec<T?> {
            override val default: T?
                get() = null

            override fun read(input: SingleValueInput): T? {
                return input.readOptional(parent)
            }

            override fun write(output: SingleValueOutput, value: T?) {
                output.writeOptional(value, parent)
            }

            override fun toString(): String {
                return "$parent, optional"
            }
        }
    }

    fun optional(default: T): OptionalCodec<T> {
        val parent = this
        return object : OptionalCodec<T> {
            override val default: T
                get() = default

            override fun read(input: SingleValueInput): T {
                return input.readOptional(parent) ?: default
            }

            override fun write(output: SingleValueOutput, value: T) {
                output.writeOptional(value, parent)
            }

            override fun toString(): String {
                return "$parent, optional with default $default"
            }
        }
    }

    fun <N> map(
        read: Function<T, N>,
        write: Function<N, T>
    ): Codec<N> {
        val parent = this
        return object : Codec<N> {
            override fun read(input: SingleValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun write(output: SingleValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped with read $read and write $write"
            }
        }
    }

    fun <N> map(
        cls: Class<N>,
        read: Function<T, N>,
        write: Function<N, T>
    ): Codec<N> {
        val parent = this
        return object : Codec<N> {
            override fun read(input: SingleValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun write(output: SingleValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped to ${cls.name}"
            }
        }
    }
}