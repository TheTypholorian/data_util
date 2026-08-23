package net.typho.data_util.anno

/**
 * Specify an inclusive number range for reflected Codecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldRange(
    val min: Double,
    val max: Double
)
