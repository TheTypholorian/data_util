package net.typho.tv_lib.io.codec

interface MapOutputResult<P> : MapOutput {
    fun finish(): P
}