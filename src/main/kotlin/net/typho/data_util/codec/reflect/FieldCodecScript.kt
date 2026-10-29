package net.typho.data_util.codec.reflect

import net.typho.data_util.anno.FieldCodec
import net.typho.data_util.codec.Codec
import java.lang.reflect.Field
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@Suppress("unused")
open class FieldCodecScript(
    @JvmField
    val owner: Class<*>,
    @JvmField
    val field: Field
) {
    companion object {
        @JvmStatic
        fun createCompilationConfig(owner: Class<*>, field: Field) = ScriptCompilationConfiguration {
            displayName("Codec Field Config")

            defaultImports.append(owner.name.replace('$', '.'))
            owner.`package`?.let { defaultImports.append("${it.name}.*") }
            defaultImports.append(field.type.name.replace('$', '.'))
            field.type.`package`?.let { defaultImports.append("${it.name}.*") }
            defaultImports.append("net.typho.data_util.codec.*")

            baseClass(FieldCodecScript::class)

            jvm {
                jvmTarget("8")
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun compile(owner: Class<*>, field: Field): Codec<*>? {
            return field.getAnnotation(FieldCodec::class.java)?.let { config ->
                val host = BasicJvmScriptingHost()
                val result = host.eval(
                    config.value.toScriptSource(name = "${owner.name}.${field.name}"),
                    createCompilationConfig(owner, field),
                    ScriptEvaluationConfiguration {
                        constructorArgs(owner, field)
                    }
                )

                result.reports.forEach {
                    if (it.severity >= ScriptDiagnostic.Severity.ERROR) {
                        System.err.println(it.render(withSeverity = false))
                    }
                }

                val evaluation = result.valueOrNull() ?: return null

                return when (val result = evaluation.returnValue) {
                    is ResultValue.Value -> result.value as? Codec<*> ?: throw IllegalStateException("@FieldCodec for ${owner.name}.${field.name} must return a Codec<${field.genericType.typeName}> (returned ${result.value?.javaClass?.name})")
                    is ResultValue.Unit -> throw IllegalStateException("@FieldCodec for ${owner.name}.${field.name} must return a Codec<${field.genericType.typeName}>")
                    is ResultValue.Error -> throw result.error
                    is ResultValue.NotEvaluated -> throw AssertionError()
                }
            }
        }
    }
}