package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.DataWriteException
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
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

interface DataCodec<T : Any> {
    companion object {
        private fun <T : Any> simple(read: Function<SingleValueInput, T>, write: BiConsumer<SingleValueOutput, T>) = object : DataCodec<T> {
            override fun read(input: SingleValueInput): T {
                return read.apply(input)
            }

            override fun write(output: SingleValueOutput, value: T) {
                write.accept(output, value)
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
        val BOOL = simple(SingleValueInput::readBoolean, SingleValueOutput::writeBoolean)
        @JvmField
        val BYTE = simple(SingleValueInput::readByte, SingleValueOutput::writeByte)
        @JvmField
        val SHORT = simple(SingleValueInput::readShort, SingleValueOutput::writeShort)
        @JvmField
        val INT = simple(SingleValueInput::readInt, SingleValueOutput::writeInt)
        @JvmField
        val LONG = simple(SingleValueInput::readLong, SingleValueOutput::writeLong)
        @JvmField
        val FLOAT = simple(SingleValueInput::readFloat, SingleValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple(SingleValueInput::readDouble, SingleValueOutput::writeDouble)
        @JvmField
        val STRING = simple(SingleValueInput::readString, SingleValueOutput::writeString)

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : Any> getPrimitiveCodec(cls: Class<T>): DataCodec<T>? {
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
                    } catch (e: NoSuchFieldException) {
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
                } catch (e: NoSuchFieldException) {
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
                        { "$it must be between ${anno.min} and ${anno.max}" }
                    )
                    break
                }
            }

            return codec1
        }
    }

    fun read(input: SingleValueInput): T

    fun write(output: SingleValueOutput, value: T)

    fun withCondition(condition: Predicate<T>, error: Function<T, String>): DataCodec<T> {
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
        }
    }
}