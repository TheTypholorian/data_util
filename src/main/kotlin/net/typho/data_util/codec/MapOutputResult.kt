package net.typho.data_util.codec

interface MapOutputResult<P> : MapOutput {
    fun finish(): P
}