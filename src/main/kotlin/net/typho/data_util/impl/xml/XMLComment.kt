package net.typho.data_util.impl.xml

open class XMLComment(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString(prettyPrint: Boolean) = "<!--$text-->"

    override fun isBlank() = text.isBlank()

    override fun toString() = "XMLComment($text)"
}