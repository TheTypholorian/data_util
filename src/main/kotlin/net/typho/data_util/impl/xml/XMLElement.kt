package net.typho.data_util.impl.xml

data class XMLElement(
    @JvmField
    var name: String,
    @JvmField
    var attributes: MutableMap<String, String> = mutableMapOf(),
    @JvmField
    var children: MutableList<XMLNode> = mutableListOf()
) : XMLNode {
    override fun toXMLString() = buildString {
        append('<')
        append(name)

        for ((key, value) in attributes) {
            append(' ')
            append(key)
            append("=\"")
            append(XMLFormat.escape(value))
            append('"')
        }

        if (children.isEmpty()) {
            append("/>")
        } else {
            append('>')

            children.forEach { append(it.toXMLString()) }

            append("</")
            append(name)
            append('>')
        }
    }

    override fun toString() = buildString {
        append("XMLElement(name=").append(name)

        if (attributes.isNotEmpty()) {
            append(' ')
            append(attributes.entries.joinToString(separator = " ") { (key, value) -> "$key=\"$value\"" })
        }

        if (children.isNotEmpty()) {
            append(" children=${children}")
        }

        append(')')
    }
}