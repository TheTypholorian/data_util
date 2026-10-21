package net.typho.data_util.impl.md

sealed interface MarkdownListElement {
    val indent: Int

    fun asString(): String

    data class Ordered(
        @JvmField
        val index: Int,
        override val indent: Int
    ) : MarkdownListElement {
        override fun asString() = "$index."
    }

    data class Unordered(
        override val indent: Int
    ) : MarkdownListElement {
        override fun asString() = "-"
    }

    data class Checklist(
        @JvmField
        val checked: Boolean,
        override val indent: Int
    ) : MarkdownListElement {
        override fun asString() = if (checked) "[X]" else "[ ]"
    }
}