package net.typho.data_util.codec

import kotlin.reflect.KClass

/**
 * Specify a static codec field to use for reflected DataCodecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldCodec(
    val owner: KClass<*> = Any::class,
    val value: String
)
