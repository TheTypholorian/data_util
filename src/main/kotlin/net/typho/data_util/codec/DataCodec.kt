package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.DataWriteException
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import org.jetbrains.annotations.Nullable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.jvm.java
import kotlin.reflect.jvm.kotlinProperty

interface DataCodec<T> {
    companion object {
        private fun <T> simple(name: String, read: Function<SingleValueInput, T>, write: BiConsumer<SingleValueOutput, T>) = object : DataCodec<T> {
            override fun read(input: SingleValueInput): T {
                return read.apply(input)
            }

            override fun write(output: SingleValueOutput, value: T) {
                write.accept(output, value)
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
        val LONG = simple("Long Codec", SingleValueInput::readLong, SingleValueOutput::writeLong)
        @JvmField
        val FLOAT = simple("Float Codec", SingleValueInput::readFloat, SingleValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple("Double Codec", SingleValueInput::readDouble, SingleValueOutput::writeDouble)
        @JvmField
        val STRING = simple("String Codec", SingleValueInput::readString, SingleValueOutput::writeString)

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> getPrimitiveCodec(cls: Class<T>): DataCodec<T>? {
            return when (cls) {
                Boolean::class.java, java.lang.Boolean::class.java -> BOOL as DataCodec<T>
                Byte::class.java, java.lang.Byte::class.java -> BYTE as DataCodec<T>
                Short::class.java, java.lang.Short::class.java -> SHORT as DataCodec<T>
                Int::class.java, Integer::class.java -> INT as DataCodec<T>
                Long::class.java, java.lang.Long::class.java -> LONG as DataCodec<T>
                Float::class.java, java.lang.Float::class.java -> FLOAT as DataCodec<T>
                Double::class.java, java.lang.Double::class.java -> DOUBLE as DataCodec<T>
                String::class.java -> STRING as DataCodec<T>
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun getFieldCodec(owner: Class<*>, field: Field): DataCodec<*> {
            var codec: DataCodec<*>? = null

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

                        if (!DataCodec::class.java.isAssignableFrom(codecField.type)) {
                            throw IllegalStateException("Field ${owner.name} ${field.type.name} ${field.name} has @FieldCodec pointing to a field that isn't a codec")
                        }

                        codecField.isAccessible = true

                        codec = codecField.get(null) as DataCodec<*>
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
                    codec = codecField.get(null) as DataCodec<*>
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

            fun <T> generics(codec: DataCodec<T>): DataCodec<*> {
                return if (default != null || field.isAnnotationPresent(Nullable::class.java) || field.kotlinProperty?.returnType?.isMarkedNullable == true) {
                    if (default == null) codec.optional() else codec.optional(default as T)
                } else {
                    codec
                }
            }

            return generics(codec1)
        }
    }

    fun read(input: SingleValueInput): T

    fun write(output: SingleValueOutput, value: T)

    fun withCondition(condition: Predicate<T>, error: Function<T, String>, name: String): DataCodec<T> {
        val parent = this
        return object : DataCodec<T> {
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

    fun optional(): DataCodec<T?> {
        val parent = this
        return object : DataCodec<T?> {
            override fun read(input: SingleValueInput): T? {
                return input.readOptional(parent::read)
            }

            override fun write(output: SingleValueOutput, value: T?) {
                output.writeOptional(value, parent::write)
            }

            override fun toString(): String {
                return "$parent, optional"
            }
        }
    }

    fun optional(default: T): DataCodec<T> {
        val parent = this
        return object : DataCodec<T> {
            override fun read(input: SingleValueInput): T {
                return input.readOptional(parent::read) ?: default
            }

            override fun write(output: SingleValueOutput, value: T) {
                output.writeOptional(value, parent::write)
            }

            override fun toString(): String {
                return "$parent, optional with default $default"
            }
        }
    }
}