package net.typho.data_util.test

import net.typho.data_util.SerializedData
import net.typho.data_util.anno.FieldCodec
import net.typho.data_util.codec.Codec
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.json.JsonFormat
import net.typho.data_util.xml.XMLFormat

object TestCodecs {
    val STRING_CODEC = object : Codec<String> {
        override fun read(input: ValueInput): String {
            return input.readString() + "_world"
        }

        override fun write(output: ValueOutput, value: String) {
            output.writeString(value)
        }

        override fun toString(): String {
            return "net.typho.data_util.test.TestCodecs.STRING_CODEC"
        }
    }
}

data class OtherTestData(
    @JvmField
    val name: String
) {
    companion object {
        val CODEC = Codec.reflect(OtherTestData::class.java)
    }
}

data class TestData(
    @FieldCodec("TestCodecs.STRING_CODEC.optional()")
    var a: String?,
    @FieldCodec("Codec.INT.withRange(123..123).optional(123)")
    var b: Int,
    @FieldCodec("Codec.FLOAT.optional(10203f)")
    var c: Float,
    @FieldCodec("Codec.BOOL.map({ !it }, { it }).optional(true)")
    var d: Boolean,
    @FieldCodec("OtherTestData.CODEC.map({ OtherTestData(it.name + \"_test\") }, { it }).optional()")
    var other: OtherTestData?
) {
    companion object {
        val CODEC = Codec.reflect(TestData::class.java)

        init {
            println(CODEC)
        }
    }
}

fun main(args: Array<String>) {
    val format = JsonFormat(
        allowComments = true,
        prettyPrint = true
    )
    val test = TestData("hello", 123, 456.789f, true, null)
    val json = format.write(TestData.CODEC, test)
    println(json.asString())
    println(format.read(TestData.CODEC, json))
    println(format.read(TestData.CODEC, SerializedData("""
        {
            "b": 123
        }
    """.trimIndent())))
}