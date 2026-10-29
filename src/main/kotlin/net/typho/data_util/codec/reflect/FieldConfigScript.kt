package net.typho.data_util.codec.reflect

import net.typho.data_util.anno.FieldConfig
import net.typho.data_util.codec.Codec
import net.typho.data_util.codec.Codec.Companion.withMax
import net.typho.data_util.codec.Codec.Companion.withMin
import org.jetbrains.annotations.Nullable
import java.lang.reflect.Field
import kotlin.reflect.jvm.kotlinProperty
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@Suppress("unused")
open class FieldConfigScript<V>(
    @JvmField
    val owner: Class<*>,
    @JvmField
    val field: Field,
    open var codec: Codec<V>?
) {
    private var hasDefault = field.isAnnotationPresent(Nullable::class.java) || (field.kotlinProperty?.returnType?.isMarkedNullable == true)
    var default: V? = null
        set(value) {
            field = value
            hasDefault = true
        }

    @Suppress("UNCHECKED_CAST")
    open fun finishCodec() = codec?.let {
        var codec = it

        if (hasDefault) {
            codec = codec.optional(default as V)
        }

        codec
    }

    open class Comparable<V : kotlin.Comparable<V>>(
        owner: Class<*>,
        field: Field,
        codec: Codec<V>?
    ) : FieldConfigScript<V>(owner, field, codec) {
        @JvmField
        var min: V? = null
        @JvmField
        var max: V? = null
        var range: ClosedRange<V>?
            get() {
                val min = min
                val max = max

                return if (min != null && max != null) {
                    min..max
                } else {
                    null
                }
            }
            set(value) {
                if (value == null) {
                    min = null
                    max = null
                } else {
                    min = value.start
                    max = value.endInclusive
                }
            }

        override fun finishCodec() = super.finishCodec()?.let {
            var codec = it

            min?.let {
                codec = codec.withMin(it)
            }
            max?.let {
                codec = codec.withMax(it)
            }

            codec
        }
    }

    companion object {
        @JvmStatic
        fun createCompilationConfig(owner: Class<*>, field: Field) = ScriptCompilationConfiguration {
            val fieldType = field.genericType.let {
                if (it is Class<*> && it.isPrimitive) {
                    it.kotlin.javaObjectType.typeName
                } else {
                    it.typeName
                }
            }

            displayName("Codec Field Config")

            defaultImports.append(owner.name.replace('$', '.'))
            owner.`package`?.let { defaultImports.append("${it.name}.*") }
            defaultImports.append(field.type.name.replace('$', '.'))
            field.type.`package`?.let { defaultImports.append("${it.name}.*") }
            defaultImports.append("net.typho.data_util.codec.*")

            val baseClass = if (kotlin.Comparable::class.java.isAssignableFrom(field.type)) {
                Comparable::class
            } else {
                FieldConfigScript::class
            }
            baseClass(KotlinType(baseClass)) // TODO type param

            jvm {
                jvmTarget("8")
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <V> compile(owner: Class<*>, field: Field, codec: Codec<V>?): Codec<V>? {
            return field.getAnnotation(FieldConfig::class.java)?.let { config ->
                val host = BasicJvmScriptingHost()
                val result = host.eval(
                    config.value.toScriptSource(name = "${owner.name}.${field.name}"),
                    createCompilationConfig(owner, field),
                    ScriptEvaluationConfiguration {
                        constructorArgs(owner, field, codec)
                    }
                )

                result.reports.forEach {
                    if (it.severity >= ScriptDiagnostic.Severity.ERROR) {
                        System.err.println(it.render(withSeverity = false))
                    }
                }

                val evaluation = result.valueOrNull() ?: return null

                return when (val result = evaluation.returnValue) {
                    is ResultValue.Value -> {
                        val script = result.scriptInstance as FieldConfigScript<V>
                        (result.value as? Codec<V>)?.let { script.codec = codec }
                        script.finishCodec()
                    }
                    is ResultValue.Unit -> (result.scriptInstance as FieldConfigScript<V>).finishCodec()
                    is ResultValue.Error -> throw result.error
                    is ResultValue.NotEvaluated -> throw AssertionError()
                }
            }
        }
    }
}