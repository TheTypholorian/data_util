package net.typho.data_util

import java.util.function.Predicate

class ParsingIterator @JvmOverloads constructor(
    @JvmField
    val text: String,
    @JvmField
    val range: IntRange = text.indices,
    index: Int = range.first
) : ListIterator<Char> {
    var index: Int = index
        private set

    constructor(text: String, trim: Boolean) : this(
        text,
        IntRange(
            text.takeWhile { it.isWhitespace() }.length,
            text.length - text.takeLastWhile { it.isWhitespace() }.length - 1
        )
    )

    override fun hasNext() = index <= range.last

    override fun hasPrevious() = index >= range.first

    override fun next(): Char {
        if (!hasNext()) {
            throw NoSuchElementException("Read too many characters from parsing iterator")
        }

        return text[index++]
    }

    fun current(): Char {
        return text[index]
    }

    override fun nextIndex() = index

    override fun previous(): Char {
        if (!hasPrevious()) {
            throw NoSuchElementException("Read too many characters from parsing iterator")
        }

        return text[index--]
    }

    override fun previousIndex() = index - 1

    operator fun get(index: Int) = text[index]

    fun takeUntil(predicate: Predicate<Char>) = takeUntil(predicate, predicate)

    fun takeUntil(predicate: Predicate<Char>, afterFirst: Predicate<Char>): String {
        return buildString {
            var predicate = predicate

            while (index in range) {
                val char = current()

                if (predicate.test(char)) {
                    break
                }

                append(char)
                next()
                predicate = afterFirst
            }
        }
    }

    fun skipUntil(predicate: Predicate<Char>) = skipUntil(predicate, predicate)

    fun skipUntil(predicate: Predicate<Char>, afterFirst: Predicate<Char>): Int {
        var skipped = 0
        var predicate = predicate

        while (index in range) {
            val char = current()

            if (predicate.test(char)) {
                break
            }

            next()
            skipped++
            predicate = afterFirst
        }

        return skipped
    }

    @JvmOverloads
    fun fork(range: IntRange = this.range, index: Int = this.index) = ParsingIterator(text, range, index)

    fun error(message: String): Nothing {
        throw DataReadException(message, text, index)
    }
}