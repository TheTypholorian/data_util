package net.typho.tv_lib.io.impl.token

class NullToken(
    override val line: Int = -1
) : PrimitiveToken<Any?> {
    override val content: Any?
        get() = null

    override fun toString(): String {
        return "Null"
    }
}