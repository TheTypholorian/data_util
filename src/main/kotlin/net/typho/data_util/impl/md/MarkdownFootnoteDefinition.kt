package net.typho.data_util.impl.md

data class MarkdownFootnoteDefinition(
    @JvmField
    val id: String,
    @JvmField
    val definition: MarkdownMultiNode<MarkdownTextNode>
) : MarkdownNode
