package net.typho.data_util.xml

open class XMLStubElement(
    @JvmField
    var name: String,
    @JvmField
    var attributes: MutableMap<String, String> = mutableMapOf(),
) : XMLNode {
    override fun toXMLString(prettyPrint: Boolean) = buildString {
        append('<')
        append(name)

        for ((key, value) in attributes) {
            append(' ')
            append(key)
            append("=\"")
            append(XMLFormat.writeEntities(value))
            append('"')
        }

        if (name.startsWith('?')) {
            append("?>")
        } else {
            append("/>")
        }
    }

    override fun toString() = buildString {
        append("XMLStubElement(name=").append(name)

        attributes.entries.forEach { (key, value) ->
            append(' ')
            append(key)
            append("=\"")
            append(value)
            append('"')
        }

        append(')')
    }
}
