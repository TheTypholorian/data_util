package net.typho.data_util.impl.md

data class MarkdownCodeBlock(
    override val text: String,
    /**
     * Render as single line or multi line
     */
    @JvmField
    val multiLine: Boolean,
    @JvmField
    val syntax: String?
) : MarkdownTextNode
