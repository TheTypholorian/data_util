package net.typho.data_util.xml

open class XMLComment(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString(prettyPrint: Boolean) = "<!--$text-->"

    override fun toString() = "XMLComment($text)"
}