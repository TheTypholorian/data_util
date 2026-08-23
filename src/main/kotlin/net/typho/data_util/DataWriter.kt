package net.typho.data_util

fun interface DataWriter<T> {
    fun write(output: SingleValueOutput, value: T)
}