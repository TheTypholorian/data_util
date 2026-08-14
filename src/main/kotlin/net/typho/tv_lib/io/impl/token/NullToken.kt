package net.typho.tv_lib.io.impl.token

class NullToken(
    override val line: Int? = null
) : PrimitiveToken<Any?> {
    override val value: Any?
        get() = null

    override fun toString(): String {
        return "Null"
    }
}