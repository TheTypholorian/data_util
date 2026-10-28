package net.typho.data_util.json

class JsonNumber(
    @JvmField
    val text: String
): Number() {
    override fun toByte() = text.toByte()

    override fun toDouble() = text.toDouble()

    override fun toFloat() = text.toFloat()

    override fun toInt() = text.toInt()

    override fun toLong() = text.toLong()

    override fun toShort() = text.toShort()
}