package net.typho.data_util.test

import net.typho.data_util.SerializedData
import net.typho.data_util.codec.Codec
import net.typho.data_util.json.JsonFormat
import net.typho.data_util.xml.XMLFormat

data class Test(
    val value: Int
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect<Test>()
    }
}

data class ExampleObject(
    val exampleArray: List<Test>,
    val otherField: String
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect<ExampleObject> {
            field(ExampleObject::exampleArray, Test.CODEC.listOf())
        }
    }
}

data class Example(
    val abc: Int,
    val obj: ExampleObject
) {
    companion object {
        @JvmField
        val CODEC = Codec.reflect<Example> {
            field(Example::obj, ExampleObject.CODEC)
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
                Test(1),
                Test(2),
                Test(3),
                Test(4),
                Test(5)
            ),
            "test"
        )
    )

    println(format.write(Example.CODEC, example).asString())
    println(format.read(Example.CODEC, SerializedData("""
        {
            "abc": 123,
            "obj": {
                "exampleArray": [
                    {
                        "value": 1
                    },
                    {
                        "value": 2
                    },
                    {
                        "value": 3
                    },
                    {
                        "value": 4
                    },
                    {
                        "value": 5
                    }
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