package net.typho.data_util.anno

// TODO
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldConfig(
    val value: String
)
