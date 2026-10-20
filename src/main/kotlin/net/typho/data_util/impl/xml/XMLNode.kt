package net.typho.data_util.impl.xml

import java.util.function.Consumer

sealed interface XMLNode {
    fun toXMLString(): String

    fun asElement() = this as XMLElement

    fun asText() = this as XMLText

    fun asComment() = this as XMLComment

    fun asIfElement() = this as? XMLElement

    fun asIfText() = this as? XMLText

    fun asIfComment() = this as? XMLComment

    fun ifElement(out: Consumer<XMLElement>) = asIfElement()?.let { out.accept(it) }

    fun ifText(out: Consumer<XMLText>) = asIfText()?.let { out.accept(it) }

    fun ifComment(out: Consumer<XMLComment>) = asIfComment()?.let { out.accept(it) }
}