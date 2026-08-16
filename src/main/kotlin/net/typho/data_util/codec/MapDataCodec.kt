package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import net.typho.data_util.or
import org.jetbrains.annotations.Nullable
import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.jvm.java
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.kotlinProperty

interface MapDataCodec<T : Any> : DataCodec<T> {
    data class BuilderEntry<T : Any, V : Any> @JvmOverloads constructor(
        @JvmField
        val key: String,
        @JvmField
        val codec: DataCodec<V>,
        @JvmField
        val getter: Function<T, V>,
        @JvmField
        val setter: BiConsumer<T, V>,
        @JvmField
        val fallback: Optional<V> = Optional.empty<V>()
    ) {
        fun read(value: T, input: MapInput) {
            val result = input.readEntryOptional(key).map { codec.read(it) }
            setter.accept(value, result.or(fallback).orElseThrow { throw DataReadException("Missing value") }) // TODO add useful info
        }

        fun write(value: T, output: MapOutput) {
            codec.write(output.writeEntry(key), getter.apply(value))
        }
    }

    fun read(input: MapInput): T

    override fun read(input: SingleValueInput): T {
        return read(input.readMap())
    }

    fun write(output: MapOutput, value: T)

    override fun write(output: SingleValueOutput, value: T) {
        write(output.writeMap(), value)
    }

    class Builder<T : Any>(
        @JvmField
        val constructor: Supplier<T>
    ) {
        @JvmField
        val entries = mutableListOf<BuilderEntry<T, *>>()

        fun build() = of(constructor, entries)

        @JvmOverloads
        fun <V : Any> add(key: String, codec: DataCodec<V>, getter: Function<T, V>, setter: BiConsumer<T, V>, fallback: Optional<V> = Optional.empty()): Builder<T> {
            entries.add(BuilderEntry(key, codec, getter, setter, fallback))
            return this
        }

        @JvmOverloads
        fun <V : Any> add(property: KMutableProperty<V>, codec: DataCodec<V>, fallback: Optional<V> = Optional.empty()): Builder<T> {
            return add(property.name, codec, { parent -> property.getter.call(parent) }, { parent, value -> property.setter.call(parent, value) }, fallback)
        }

        @JvmOverloads
        fun addBool(key: String, getter: Function<T, Boolean>, setter: BiConsumer<T, Boolean>, fallback: Optional<Boolean> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.BOOL, getter, setter, fallback)
        }

        @JvmOverloads
        fun addBool(property: KMutableProperty<Boolean>, fallback: Optional<Boolean> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.BOOL, fallback)
        }

        @JvmOverloads
        fun addByte(key: String, getter: Function<T, Byte>, setter: BiConsumer<T, Byte>, fallback: Optional<Byte> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.BYTE, getter, setter, fallback)
        }

        @JvmOverloads
        fun addByte(property: KMutableProperty<Byte>, fallback: Optional<Byte> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.BYTE, fallback)
        }

        @JvmOverloads
        fun addShort(key: String, getter: Function<T, Short>, setter: BiConsumer<T, Short>, fallback: Optional<Short> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.SHORT, getter, setter, fallback)
        }

        @JvmOverloads
        fun addShort(property: KMutableProperty<Short>, fallback: Optional<Short> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.SHORT, fallback)
        }

        @JvmOverloads
        fun addInt(key: String, getter: Function<T, Int>, setter: BiConsumer<T, Int>, fallback: Optional<Int> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.INT, getter, setter, fallback)
        }

        @JvmOverloads
        fun addInt(property: KMutableProperty<Int>, fallback: Optional<Int> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.INT, fallback)
        }

        @JvmOverloads
        fun addLong(key: String, getter: Function<T, Long>, setter: BiConsumer<T, Long>, fallback: Optional<Long> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.LONG, getter, setter, fallback)
        }

        @JvmOverloads
        fun addLong(property: KMutableProperty<Long>, fallback: Optional<Long> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.LONG, fallback)
        }

        @JvmOverloads
        fun addFloat(key: String, getter: Function<T, Float>, setter: BiConsumer<T, Float>, fallback: Optional<Float> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.FLOAT, getter, setter, fallback)
        }

        @JvmOverloads
        fun addFloat(property: KMutableProperty<Float>, fallback: Optional<Float> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.FLOAT, fallback)
        }

        @JvmOverloads
        fun addDouble(key: String, getter: Function<T, Double>, setter: BiConsumer<T, Double>, fallback: Optional<Double> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.DOUBLE, getter, setter, fallback)
        }

        @JvmOverloads
        fun addDouble(property: KMutableProperty<Double>, fallback: Optional<Double> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.DOUBLE, fallback)
        }

        @JvmOverloads
        fun addString(key: String, getter: Function<T, String>, setter: BiConsumer<T, String>, fallback: Optional<String> = Optional.empty()): Builder<T> {
            return add(key, DataCodec.STRING, getter, setter, fallback)
        }

        @JvmOverloads
        fun addString(property: KMutableProperty<String>, fallback: Optional<String> = Optional.empty()): Builder<T> {
            return add(property, DataCodec.STRING, fallback)
        }
    }

    companion object {
        private val UNSAFE: Unsafe

        init {
            val fields = Unsafe::class.java.declaredFields

            UNSAFE = fields.firstNotNullOf {
                if (it.type == Unsafe::class.java && Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers)) {
                    it.isAccessible = true
                    it[null] as Unsafe
                } else {
                    null
                }
            }
        }

        @JvmStatic
        fun <T : Any> build(
            constructor: Supplier<T>,
            builder: Builder<T>.() -> Unit
        ) = Builder(constructor).apply(builder).build()

        @JvmStatic
        fun <T : Any> of(
            constructor: Supplier<T>,
            entries: List<BuilderEntry<T, *>>
        ) = object : MapDataCodec<T> {
            override fun read(input: MapInput): T {
                val value = constructor.get()

                for (entry in entries) {
                    entry.read(value, input)
                }

                return value
            }

            override fun write(output: MapOutput, value: T) {
                for (entry in entries) {
                    entry.write(value, output)
                }
            }
        }

        /**
         * @param reverseInheritanceOrder defines where superclass fields are in the constructor, True = at the end and False = at the start (defaults to false because it's the IntelliJ default)
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic
        fun <T : Any> reflect(cls: Class<T>, reverseInheritanceOrder: Boolean = false): MapDataCodec<T> {
            data class Entry(
                @JvmField
                val field: Field,
                @JvmField
                val codec: DataCodec<*>,
                @JvmField
                val optional: Boolean,
                @JvmField
                val default: Any?
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

            val entries = fields.map { field ->
                val fallback = Optional.ofNullable(field.annotations.filterIsInstance<FieldDefault>().firstOrNull())
                    .map { anno ->
                        var target = anno.owner.java

                        if (target == Any::class.java) {
                            when (field.type) {
                                Boolean::class.java, java.lang.Boolean::class.java -> return@map anno.value.toBoolean()
                                Byte::class.java, java.lang.Byte::class.java -> return@map anno.value.toByte()
                                Short::class.java, java.lang.Short::class.java -> return@map anno.value.toShort()
                                Int::class.java, Integer::class.java -> return@map anno.value.toInt()
                                Long::class.java, java.lang.Long::class.java -> return@map anno.value.toLong()
                                Float::class.java, java.lang.Float::class.java -> return@map anno.value.toFloat()
                                Double::class.java, java.lang.Double::class.java -> return@map anno.value.toDouble()
                                String::class.java -> return@map anno.value
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

                            return@map defaultField.get(null)
                        } catch (e: NoSuchFieldException) {
                            throw IllegalStateException("Field ${target.name} ${field.type.name} ${field.name} has @FieldDefault pointing to a nonexistent field")
                        }
                    }

                Entry(
                    field,
                    DataCodec.getFieldCodec(cls, field),
                    fallback.isPresent || field.isAnnotationPresent(Nullable::class.java) || field.kotlinProperty?.returnType?.isMarkedNullable == true,
                    fallback.getOrNull()
                )
            }

            return object : MapDataCodec<T> {
                override fun read(input: MapInput): T {
                    val args = entries.map { (field, codec, optional, fallback) ->
                        val value = input.readEntryOptional(field.name).map { codec.read(it) }

                        if (value.isPresent) {
                            return@map value.get()
                        } else if (optional) {
                            return@map fallback
                        } else {
                            throw DataReadException("Missing field ${field.name} and no default") // TODO better errors
                        }
                    }
                    return constructor.newInstance(*args.toTypedArray()) as T
                }

                fun <V : Any> write(field: Field, codec: DataCodec<V>, value: T, output: MapOutput) {
                    codec.write(output.writeEntry(field.name), field.get(value) as V)
                }

                override fun write(output: MapOutput, value: T) {
                    for (entry in entries) {
                        write(entry.field, entry.codec, value, output)
                    }
                }
            }
        }
    }
}