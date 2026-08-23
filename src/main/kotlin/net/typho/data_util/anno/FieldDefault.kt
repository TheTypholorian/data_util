package net.typho.data_util.anno

import kotlin.reflect.KClass

/**
 * Specify a default value to use for reflected Codecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldDefault(
    val owner: KClass<*> = Any::class,
    val value: String
)
