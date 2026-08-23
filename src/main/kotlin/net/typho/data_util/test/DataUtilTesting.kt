package net.typho.data_util.test

import net.typho.data_util.codec.DataCodec
import net.typho.data_util.codec.FieldCodec
import net.typho.data_util.codec.FieldDefault
import net.typho.data_util.codec.FieldRange
import net.typho.data_util.codec.MapDataCodec
import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import net.typho.data_util.impl.JsonFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object TestCodecs {
    val STRING_CODEC = object : DataCodec<String> {
        override fun read(input: SingleValueInput): String {
            return input.readString() + "_world"
        }

        override fun write(output: SingleValueOutput, value: String) {
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
        val CODEC = MapDataCodec.reflect(OtherTestData::class.java)

        init {
            println(CODEC)
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
    val d: Boolean?,
    val other: OtherTestData?
) {
    companion object {
        val CODEC = MapDataCodec.reflect(TestData::class.java)

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

    val bytes = ByteArrayOutputStream()
    TestData.CODEC.write(SingleValueOutput.toData(DataOutputStream(bytes)), test)

    bytes.toByteArray().forEach { println("0x${it.toHexString()} ${it.toInt().toChar()}") }

    println(test)
    println(TestData.CODEC.read(SingleValueInput.fromData(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))))
}