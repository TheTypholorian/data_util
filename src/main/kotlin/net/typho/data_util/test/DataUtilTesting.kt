package net.typho.data_util.test

import net.typho.data_util.codec.DataCodec
import net.typho.data_util.codec.FieldCodec
import net.typho.data_util.codec.FieldDefault
import net.typho.data_util.codec.FieldRange
import net.typho.data_util.codec.MapDataCodec
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import net.typho.data_util.impl.JsonFormat

object TestCodecs {
    val STRING_CODEC = object : DataCodec<String> {
        override fun read(input: SingleValueInput): String {
            return input.readString() + "_world"
        }

        override fun write(output: SingleValueOutput, value: String) {
            output.writeString(value)
        }
    }
}

data class TestData(
    @FieldCodec(TestCodecs::class, "STRING_CODEC")
    val a: String,
    @FieldRange(123.0, 123.0)
    val b: Int,
    @FieldDefault(value = "10203")
    val c: Float,
    val d: Boolean?
) {
    companion object {
        val CODEC = MapDataCodec.reflect(TestData::class.java)
    }
}

fun main(args: Array<String>) {
    val format = JsonFormat(
        allowComments = true,
        prettyPrint = true
    )
    val test = TestData("hello", 123, 456.789f, false)
    val serializer = format.map(TestData.CODEC)
    val json = serializer.write(test)
    println(json)
    println(serializer.read(json))
    println(serializer.read("""
        {
            "a": "hello",
            "b": 123
        }
    """.trimIndent()))
}