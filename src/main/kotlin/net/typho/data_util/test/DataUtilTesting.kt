package net.typho.data_util.test

import net.typho.data_util.SerializedData
import net.typho.data_util.anno.FieldConfig
import net.typho.data_util.codec.Codec
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.json.JsonFormat

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
    @FieldConfig("""
        codec = TestCodecs.STRING_CODEC
    """)
    val a: String,
    @FieldConfig("""
        range = 123..123
        default = 123
    """)
    val b: Int,
    @FieldConfig("""
        default = 10203f
    """)
    val c: Float,
    val d: Boolean?,
    val other: OtherTestData?
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
    val test = TestData("hello", 123, 456.789f, null, null)
    val json = format.write(TestData.CODEC, test)
    println(json)
    println(format.read(TestData.CODEC, json))
    println(format.read(TestData.CODEC, SerializedData("""
        {
            "b": 123
        }
    """.trimIndent())))
}