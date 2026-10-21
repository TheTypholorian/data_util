package net.typho.data_util.impl.xml

import net.typho.data_util.DataReadException

open class XMLElement(
    name: String,
    attributes: MutableMap<String, String> = mutableMapOf(),
    @JvmField
    var children: MutableList<XMLNode> = mutableListOf()
) : XMLStubElement(name, attributes) {
    fun getChildren(name: String): List<XMLElement> {
        return children.filterIsInstance<XMLElement>().filter { it.name == name }
    }

    fun getChild(name: String): XMLElement {
        val results = getChildren(name)
        when (results.size) {
            0 -> throw DataReadException("Element ${this.name} does not have child with name $name")
            1 -> return results.single()
            else -> throw DataReadException("Element ${this.name} has multiple children with name $name")
        }
    }

    fun contentsAsString() = children.joinToString { it.toXMLString() }

    override fun toXMLString() = buildString {
        append('<')
        append(name)

        for ((key, value) in attributes) {
            append(' ')
            append(key)
            append("=\"")
            append(XMLFormat.writeEntities(value))
            append('"')
        }

        if (children.isEmpty()) {
            if (name.startsWith('?')) {
                append("?>")
            } else {
                append("/>")
            }
        } else {
            append('>')
            append(contentsAsString())
            append("</")
            append(name)
            append('>')
        }
    }

    override fun toString() = buildString {
        append("XMLElement(name=").append(name)

        attributes.entries.forEach { (key, value) ->
            append(' ')
            append(key)
            append("=\"")
            append(value)
            append('"')
        }

        if (children.isNotEmpty()) {
            append(" children=${children}")
        }

        append(')')
    }
}