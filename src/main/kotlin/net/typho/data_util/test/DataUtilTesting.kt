package net.typho.data_util.test

import net.typho.data_util.SerializedData
import net.typho.data_util.anno.FieldCodec
import net.typho.data_util.codec.Codec
import net.typho.data_util.json.JsonFormat
import net.typho.data_util.xml.XMLFormat

data class ExampleObject(
    @FieldCodec("Codec.INT.listOf()")
    val exampleArray: List<Int>,
    val otherField: String
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect(ExampleObject::class.java)
    }
}

data class Example(
    val abc: Int,
    @FieldCodec("ExampleObject.CODEC")
    val obj: ExampleObject
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect(Example::class.java)
    }
}

fun main(args: Array<String>) {
    val format = JsonFormat(
        allowComments = true,
        prettyPrint = true
    )

    val example = Example(
        123,
        ExampleObject(
            listOf(1, 2, 3, 4, 5),
            "test"
        )
    )

    println(format.write(Example.CODEC, example).asString())
    println(format.read(Example.CODEC, SerializedData("""
        {
            "abc": 123,
            "obj": {
                "exampleArray": [
                    1,
                    2,
                    3,
                    4,
                    5
                ],
                "otherField": "test"
            }
        }
    """.trimIndent())))

    val xml = XMLFormat(prettyPrint = true)
    val written = xml.write(Example.CODEC, example)
    println(written.asString())
    println(xml.read(Example.CODEC, written))
}