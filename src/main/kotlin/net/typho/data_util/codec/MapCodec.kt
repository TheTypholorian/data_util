package net.typho.data_util.codec

import net.typho.data_util.SequentialInput
import net.typho.data_util.SequentialOutput
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import sun.misc.Unsafe
import java.lang.reflect.Modifier
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.jvm.java
import kotlin.reflect.KMutableProperty

interface MapCodec<T> : Codec<T> {
    data class BuilderEntry<T, V>(
        @JvmField
        val key: String,
        @JvmField
        val codec: Codec<V>,
        @JvmField
        val getter: Function<T, V>,
        @JvmField
        val setter: BiConsumer<T, V>
    ) {
        fun read(value: T, input: SequentialInput) {
            setter.accept(value, codec.read(input.readNextEntry()))
        }

        fun write(value: T, output: SequentialOutput) {
            codec.write(output.writeNextEntry(), getter.apply(value))
        }
    }

    val keys: List<String>

    fun read(input: SequentialInput): T

    override fun read(input: SingleValueInput): T {
        return read(input.readStaticMap(keys))
    }

    fun write(output: SequentialOutput, value: T)

    override fun write(output: SingleValueOutput, value: T) {
        write(output.writeStaticMap(keys), value)
    }

    class Builder<T>(
        @JvmField
        val constructor: Supplier<T>
    ) {
        @JvmField
        val entries = mutableListOf<BuilderEntry<T, *>>()

        fun build() = of(constructor, entries)

        fun <V> add(key: String, codec: Codec<V>, getter: Function<T, V>, setter: BiConsumer<T, V>): Builder<T> {
            entries.add(BuilderEntry(key, codec, getter, setter))
            return this
        }

        fun <V> add(property: KMutableProperty<V>, codec: Codec<V>): Builder<T> {
            return add(property.name, codec, { parent -> property.getter.call(parent) }, { parent, value -> property.setter.call(parent, value) })
        }

        fun addBool(key: String, getter: Function<T, Boolean>, setter: BiConsumer<T, Boolean>): Builder<T> {
            return add(key, Codec.BOOL, getter, setter)
        }

        fun addBool(property: KMutableProperty<Boolean>): Builder<T> {
            return add(property, Codec.BOOL)
        }

        fun addByte(key: String, getter: Function<T, Byte>, setter: BiConsumer<T, Byte>): Builder<T> {
            return add(key, Codec.BYTE, getter, setter)
        }

        fun addByte(property: KMutableProperty<Byte>): Builder<T> {
            return add(property, Codec.BYTE)
        }

        fun addShort(key: String, getter: Function<T, Short>, setter: BiConsumer<T, Short>): Builder<T> {
            return add(key, Codec.SHORT, getter, setter)
        }

        fun addShort(property: KMutableProperty<Short>): Builder<T> {
            return add(property, Codec.SHORT)
        }

        fun addInt(key: String, getter: Function<T, Int>, setter: BiConsumer<T, Int>): Builder<T> {
            return add(key, Codec.INT, getter, setter)
        }

        fun addInt(property: KMutableProperty<Int>): Builder<T> {
            return add(property, Codec.INT)
        }

        fun addLong(key: String, getter: Function<T, Long>, setter: BiConsumer<T, Long>): Builder<T> {
            return add(key, Codec.LONG, getter, setter)
        }

        fun addLong(property: KMutableProperty<Long>): Builder<T> {
            return add(property, Codec.LONG)
        }

        fun addFloat(key: String, getter: Function<T, Float>, setter: BiConsumer<T, Float>): Builder<T> {
            return add(key, Codec.FLOAT, getter, setter)
        }

        fun addFloat(property: KMutableProperty<Float>): Builder<T> {
            return add(property, Codec.FLOAT)
        }

        fun addDouble(key: String, getter: Function<T, Double>, setter: BiConsumer<T, Double>): Builder<T> {
            return add(key, Codec.DOUBLE, getter, setter)
        }

        fun addDouble(property: KMutableProperty<Double>): Builder<T> {
            return add(property, Codec.DOUBLE)
        }

        fun addString(key: String, getter: Function<T, String>, setter: BiConsumer<T, String>): Builder<T> {
            return add(key, Codec.STRING, getter, setter)
        }

        fun addString(property: KMutableProperty<String>): Builder<T> {
            return add(property, Codec.STRING)
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
        fun <T> build(
            constructor: Supplier<T>,
            builder: Builder<T>.() -> Unit
        ) = Builder(constructor).apply(builder).build()

        @JvmStatic
        fun <T> of(
            constructor: Supplier<T>,
            entries: List<BuilderEntry<T, *>>
        ) = object : MapCodec<T> {
            override val keys = entries.map { it.key }

            override fun read(input: SequentialInput): T {
                val value = constructor.get()

                for (entry in entries) {
                    entry.read(value, input)
                }

                return value
            }

            override fun write(output: SequentialOutput, value: T) {
                for (entry in entries) {
                    entry.write(value, output)
                }
            }

            override fun toString(): String {
                return "Simple MapCodec, fields: {${entries.joinToString(separator = "\n", prefix = "\n", transform = { "'${it.key}' with codec ${it.codec}" }).replace("\n", "\n\t")}\n}"
            }
        }
    }
}