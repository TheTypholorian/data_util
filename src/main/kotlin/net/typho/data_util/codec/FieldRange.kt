package net.typho.data_util.codec

/**
 * Specify an inclusive number range for reflected DataCodecs
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldRange(
    val min: Double,
    val max: Double
)
