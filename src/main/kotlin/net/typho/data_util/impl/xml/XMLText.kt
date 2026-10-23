package net.typho.data_util.impl.xml

open class XMLText(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString(prettyPrint: Boolean) = XMLFormat.writeEntities(text)

    override fun isBlank() = text.isBlank()

    override fun toString() = "XMLText($text)"
}