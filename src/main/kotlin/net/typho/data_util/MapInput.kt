package net.typho.data_util

import java.io.DataInput

interface MapInput {
    fun readNextEntry(): SingleValueInput

    companion object {
        @JvmStatic
        fun fromMap(keys: List<String>, map: Map<String, Any?>): MapInput = object : MapInput {
            var index = 0

            override fun readNextEntry(): SingleValueInput {
                if (index == keys.size) {
                    throw DataReadException("Already read all entries from map")
                }

                return SingleValueInput.fromObject(map[keys[index++]])
            }
        }

        fun fromStringMap(keys: List<String>, map: Map<String, String>): MapInput = object : MapInput {
            var index = 0

            override fun readNextEntry(): SingleValueInput {
                if (index == keys.size) {
                    throw DataReadException("Already read all entries from map")
                }

                return SingleValueInput.fromObject(map[keys[index++]])
            }
        }

        @JvmStatic
        fun fromData(keys: List<String>, input: DataInput): MapInput = object : MapInput {
            var index = 0

            override fun readNextEntry(): SingleValueInput {
                if (index == keys.size) {
                    throw DataReadException("Already read all entries from map")
                }

                index++

                return SingleValueInput.fromData(input)
            }
        }
    }
}