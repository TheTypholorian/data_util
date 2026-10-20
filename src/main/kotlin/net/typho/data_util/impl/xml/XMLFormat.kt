package net.typho.data_util.impl.xml

import net.typho.data_util.DataReadException
import net.typho.data_util.StringDataFormat
import java.util.function.Predicate

open class XMLFormat(
    @JvmField
    val readComments: Boolean = false
) : StringDataFormat<XMLElement> {
    override fun read(input: String): XMLElement {
        val input = input.trim()
        var charIndex = 0

        fun isStartChar(char: Char) = char in 'A'..'Z' || char in 'a'..'z' || char == '_' || char == ':'

        fun isNextChar(char: Char) = isStartChar(char) || char in '0'..'9' || char == '-' || char == '.'

        fun skipWhitespaces() {
            while (true) {
                val char = input[charIndex]

                if (!char.isWhitespace()) {
                    break
                }

                charIndex++
            }
        }

        fun readWord(): String {
            skipWhitespaces()
            return buildString {
                var i = 0

                while (true) {
                    val char = input[charIndex]

                    if (i == 0) {
                        if (!isStartChar(char)) {
                            throw DataReadException("Invalid XML element name (starts with illegal character $char)")
                        }
                    } else {
                        if (!isNextChar(char)) {
                            break
                        }
                    }

                    append(char)
                    charIndex++

                    i++
                }
            }
        }

        fun skipWhitespacesUntil(vararg expected: Char, error: (Char) -> Nothing): Char {
            while (true) {
                val char = input[charIndex++]

                when {
                    char.isWhitespace() -> continue
                    char in expected -> return char
                    else -> error(char)
                }
            }
        }

        fun peekNextNonWhitespaceChar(): Pair<Int, Char> {
            var index = charIndex

            while (true) {
                val char = input[index]

                if (!char.isWhitespace()) {
                    return index to char
                }

                index++
            }
        }

        fun readText(end: Predicate<Char>): String {
            return readEntities(buildString {
                while (true) {
                    val char = input[charIndex++]

                    when {
                        end.test(char) -> break
                        else -> append(char)
                    }
                }
            }) // TODO custom entities
        }

        fun readQuoted(): String {
            val quote = skipWhitespacesUntil('"', '\'') { throw DataReadException("Illegal open quotes character: $it") }
            return readText { it == quote }
        }

        data class Tag(
            // name for elements, content of comments
            @JvmField
            val text: String,
            // TODO separate handling for comments
            @JvmField
            val isComment: Boolean,
            // integer cus we can't have a local enum class (ugh)
            // 0 for <tag>, 1 </tag>, 2 for <tag/>
            @JvmField
            val closure: Int,
            @JvmField
            val attributes: Map<String, String>
        )

        fun readTag(): Tag {
            var closure = 0

            val firstChar = peekNextNonWhitespaceChar()

            if (firstChar.second == '/') {
                closure = 1
                charIndex = firstChar.first + 1
            } else if (input.startsWith("!--", startIndex = firstChar.first)) {
                val commentStart = firstChar.first + "!--".length
                val end = input.indexOf("--", startIndex = commentStart)

                if (input[end + 2] != '>') {
                    throw DataReadException("Cannot have dangling '--' in a comment, must signify end of comment")
                }

                charIndex = end + "-->".length

                return Tag(input.substring(commentStart, end), true, 0, mapOf())
            }

            // read name
            val name = readWord()
            val attributes = mutableMapOf<String, String>()

            // read attributes
            while (true) {
                val char = input[charIndex++]

                when {
                    char.isWhitespace() -> continue
                    char == '/' -> {
                        skipWhitespacesUntil('>') { throw DataReadException("Invalid XML character after slash in element header: $it") }

                        if (closure == 1) {
                            throw DataReadException("XML tag both closes its opening tag and itself (ex. </name/>), you must pick one or the other.")
                        }

                        closure = 2
                        break
                    }
                    char == '>' -> break
                    else -> { // read attribute
                        charIndex--
                        val key = readWord()
                        skipWhitespacesUntil('=') { throw DataReadException("Invalid XML character after attribute name: $it") }
                        val value = readQuoted()

                        if (attributes.put(key, value) != null) {
                            throw DataReadException("Duplicate XML attribute '$key'")
                        }
                    }
                }
            }

            return Tag(name, false, closure, attributes)
        }

        fun readNode(start: Tag = readTag()): XMLNode? {
            if (start.isComment) {
                return if (readComments) XMLComment(start.text) else null
            }

            if (start.closure == 1) {
                throw DataReadException("XML tag closes a nonexistent opening tag")
            }

            val element = XMLElement(start.text, start.attributes.toMutableMap())

            // read tag contents
            if (start.closure == 0) {
                val text = StringBuilder()

                while (true) {
                    val char = input[charIndex++]

                    if (char == '<') {
                        if (text.isNotEmpty()) {
                            element.children.add(XMLText(readEntities(text.toString())))
                            text.clear()
                        }

                        val tag = readTag()

                        if (!tag.isComment && tag.text == start.text && tag.closure == 1) {
                            if (tag.attributes.isNotEmpty()) {
                                throw DataReadException("XML end tag contains attributes, only opening tags can have attributes.")
                            }

                            return element
                        } else {
                            readNode(tag)?.let { element.children.add(it) }
                        }
                    } else {
                        text.append(char)
                    }
                }
            }

            return element
        }

        skipWhitespacesUntil('<') { throw DataReadException("Illegal starting XML character: $it") }
        val element = readNode()// ?: throw DataReadException("XML does not have a root element")

        if (charIndex != input.length) {
            throw DataReadException("Trailing data after root XML element")
        }

        return (element as? XMLElement) ?: throw DataReadException("XML root value is not an element")
    }

    override fun write(data: XMLElement) = data.toXMLString()

    companion object Default : XMLFormat() {
        @JvmField
        val defaultEntities = mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'"
        )

        @JvmStatic
        fun readEntities(text: String, entities: Map<String, String> = defaultEntities): String {
            return buildString {
                var index = 0

                while (index < text.length) {
                    val char = text[index]

                    if (char == '&') {
                        val endIndex = text.indexOf(';', startIndex = index)
                        val key = text.substring(index + 1, endIndex)
                        append(entities[key] ?: throw DataReadException("Undefined entity &$key;"))
                        index = endIndex + 1
                    } else {
                        append(char)
                    }

                    index++
                }
            }
        }

        @JvmStatic
        fun writeEntities(text: String, entities: Map<String, String> = defaultEntities): String {
            return buildString {
                var index = 0

                while (index < text.length) {
                    val char = text[index]

                    val entity = entities.filter { (key, value) -> text.startsWith(value, startIndex = index) }
                        .maxByOrNull { (key, value) -> value.length }

                    if (entity == null) {
                        append(char)
                        index++
                    } else {
                        append("&${entity.key};")
                        index += entity.value.length
                    }
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val format = XMLFormat
            val xml = XMLElement("root", children = mutableListOf(
                XMLElement("abc"),
                XMLElement("def", attributes = mutableMapOf("attribute1" to "123", "attribute2" to "4\"5\"6")),
                XMLElement("ghi", children = mutableListOf(
                    XMLElement("abc1"),
                    XMLElement("def1", attributes = mutableMapOf("attribute1" to "123", "attribute2" to "4\"5\"6")),
                    XMLElement("ghi1")
                )),
            ))
            println(format.write(xml))
            println()
            val parsed = format.read("<abc attribute1='123' attribute2='456'>contents &amp; &lt;<inner a1=\"123472173\">abc123<!--this is a comment--><_123/></inner>other contents</abc>")
            println(parsed)
            println(format.write(parsed))
        }
    }
}