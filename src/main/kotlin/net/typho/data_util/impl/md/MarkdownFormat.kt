package net.typho.data_util.impl.md

import net.typho.data_util.DataFormat

open class MarkdownFormat(
) : DataFormat<String, > {
    companion object Default : MarkdownFormat()
}