package net.typho.data_util

fun interface DataReader<T> {
    fun read(input: SingleValueInput): T
}