package net.typho.data_util

class DataResult<V>(
    @JvmField
    val value: V,
    private val input: () -> SingleValueInput
) {
    fun createInput() = input()
}
