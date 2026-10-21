package net.typho.data_util.impl.md

data class MarkdownTable(
    @JvmField
    val columns: List<List<MarkdownMultiNode<MarkdownNode>>>
) : MarkdownNode
