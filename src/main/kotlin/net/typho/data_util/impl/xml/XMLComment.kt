package net.typho.data_util.impl.xml

open class XMLComment(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString() = "<!--$text-->"

    override fun toString() = "XMLComment($text)"
}