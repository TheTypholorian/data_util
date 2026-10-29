package net.typho.data_util.anno

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldCodec(
    val value: String
)
