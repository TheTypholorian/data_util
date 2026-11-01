package net.typho.data_util.test

import net.typho.data_util.SerializedData
import net.typho.data_util.codec.Codec
import net.typho.data_util.json.JsonFormat
import net.typho.data_util.properties.PropertiesFormat
import net.typho.data_util.xml.XMLFormat

data class ExampleObject(
    val exampleArray: List<Int>,
    val otherField: String
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect<ExampleObject> {
            field(ExampleObject::exampleArray, Codec.INT.listOf())
        }
    }
}

data class Example(
    val abc: Int,
    val obj: ExampleObject,
    val map: Map<String, String>
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect<Example> {
            field(Example::obj, ExampleObject.CODEC)
            field(Example::map, Codec.dynamicMap(Codec.STRING))
        }
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
            listOf(
                1,
                2,
                3,
                4,
                5
            ),
            "test"
        ),
        mapOf("abc" to "123", "def" to "456")
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
            },
            "map": {
                "abc": "123",
                "def": "456"
            }
        }
    """.trimIndent())))

    val xml = XMLFormat(prettyPrint = true)
    val written = xml.write(Example.CODEC, example)
    println(written.asString())
    println(xml.read(Example.CODEC, written))

    val properties = PropertiesFormat
    val written1 = properties.write(Example.CODEC, example)
    println(written1.asString())
    println(properties.read(Example.CODEC, written1))
}