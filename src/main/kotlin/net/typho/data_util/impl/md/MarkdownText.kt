package net.typho.data_util.impl.md

data class MarkdownText(
    override val text: String,
    @JvmField
    val header: MarkdownHeaderInfo?,
    @JvmField
    val bold: Boolean,
    @JvmField
    val italic: Boolean,
    @JvmField
    val strikethrough: Boolean,
    @JvmField
    val highlight: Boolean,
    @JvmField
    val level: TextLevel,
    /**
     * Depth of the block quote (0 for not a block quote, never negative)
     */
    @JvmField
    val blockQuote: Int,
    @JvmField
    val listElement: MarkdownListElement?
) : MarkdownTextNode