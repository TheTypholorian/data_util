package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import java.util.Optional

interface MapInput {
    fun readEntry(key: String): Optional<SingleValueInput>

    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>): MapInput = object : MapInput {
            val read = mutableSetOf<String>()

            override fun readEntry(key: String): Optional<SingleValueInput> {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                return if (map.containsKey(key)) Optional.of(SingleValueInput.fromObject(map[key]!!)) else Optional.empty()
            }
        }

        @JvmStatic
        fun fromStringMap(map: Map<String, String>): MapInput = object : MapInput {
            val read = mutableSetOf<String>()

            override fun readEntry(key: String): Optional<SingleValueInput> {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                return if (map.containsKey(key)) Optional.of(SingleValueInput.fromString(map[key]!!)) else Optional.empty()
            }
        }
    }
}