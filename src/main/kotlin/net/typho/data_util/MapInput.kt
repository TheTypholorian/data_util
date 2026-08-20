package net.typho.data_util

import java.io.DataInput
import java.util.Optional

interface MapInput {
    fun readNextEntry(key: String): SingleValueInput

    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any?>): MapInput = object : MapInput {
            val read = mutableSetOf<String>()

            override fun readNextEntry(key: String): SingleValueInput {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                if (!map.containsKey(key)) {
                    throw DataReadException("Map does not contain key $key")
                }

                return SingleValueInput.fromObject(map[key]!!)
            }

            override fun readNextEntryOptional(key: String): Optional<SingleValueInput> {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                return if (map.containsKey(key)) Optional.of(SingleValueInput.fromObject(map[key]!!)) else Optional.empty()
            }
        }

        @JvmStatic
        fun fromStringMap(map: Map<String, String>): MapInput = object : MapInput {
            val read = mutableSetOf<String>()

            override fun readNextEntry(key: String): SingleValueInput {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                if (!map.containsKey(key)) {
                    throw DataReadException("Map does not contain key $key")
                }

                return SingleValueInput.fromString(map[key]!!)
            }

            override fun readNextEntryOptional(key: String): Optional<SingleValueInput> {
                if (!read.add(key)) {
                    throw DataReadException("Already read key $key, this would be an error when reading from a stream")
                }

                return if (map.containsKey(key)) Optional.of(SingleValueInput.fromString(map[key]!!)) else Optional.empty()
            }
        }

        @JvmStatic
        fun fromData(input: DataInput): MapInput = object : MapInput {
            override fun readNextEntry(key: String): SingleValueInput {
                return SingleValueInput.fromData(input)
            }

            override fun readNextEntryOptional(key: String): Optional<SingleValueInput> {
                val present = input.readBoolean()
                return if (present) Optional.of(SingleValueInput.fromData(input)) else Optional.empty()
            }
        }
    }
}