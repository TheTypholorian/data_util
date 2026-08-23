package net.typho.data_util.anno

/**
 * Specify that the field should be written as a variable length integer for byte outputs in reflected Codecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class VarInt