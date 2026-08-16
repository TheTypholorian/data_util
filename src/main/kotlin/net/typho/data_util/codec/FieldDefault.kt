package net.typho.data_util.codec

import kotlin.reflect.KClass

/**
 * Specify a default value to use for reflected DataCodecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldDefault(
    val owner: KClass<*> = Any::class,
    val value: String
)
