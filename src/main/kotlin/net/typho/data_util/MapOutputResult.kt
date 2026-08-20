package net.typho.data_util

interface MapOutputResult<P> : MapOutput {
    fun finish(): P
}