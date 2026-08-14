package net.typho.tv_lib.io.impl.token

class LongToken(
    override val line: Int,
    override val value: Long
) : PrimitiveToken<Long> {
    override fun toString(): String {
        return "Long($value)"
    }
}