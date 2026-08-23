package net.typho.data_util.anno

import kotlin.reflect.KClass

/**
 * Specify a static codec field to use for reflected Codecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldCodec(
    val owner: KClass<*> = Any::class,
    val value: String
)
