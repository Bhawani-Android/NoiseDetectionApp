package com.example.data.utils.wavutils

import java.io.File
import java.io.FileInputStream

data class WavFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int
)

object WavFileReader {

    fun readWav(inputFile: File): Pair<WavFormat, ShortArray> {
        if (!inputFile.exists()) {
            error("File not found: ${inputFile.absolutePath}")
        }

        val fileData = FileInputStream(inputFile).use { it.readBytes() }
        if (fileData.size < 44) {
            error("Not a valid WAV file => ${inputFile.name}")
        }

        val channels = fileData[22].toInt() and 0xFF or ((fileData[23].toInt() and 0xFF) shl 8)
        val sampleRate = byteToIntLE(fileData, 24)
        val bitsPerSample = fileData[34].toInt() and 0xFF or ((fileData[35].toInt() and 0xFF) shl 8)

        val subChunk2Size = byteToIntLE(fileData, 40)
        val dataStart = 44
        val dataEnd = dataStart + subChunk2Size
        if (dataEnd > fileData.size) {
            error("WAV data chunk is truncated or invalid: subChunk2Size=$subChunk2Size fileSize=${fileData.size}")
        }

        // read raw PCM as short[] (16-bit)
        val rawPcmSize = subChunk2Size / 2
        val samples = ShortArray(rawPcmSize)

        var sampleIndex = 0
        var i = dataStart
        while (i < dataEnd) {
            val lo = fileData[i].toInt() and 0xFF
            val hi = fileData[i+1].toInt() and 0xFF
            val value = (hi shl 8) or lo
            samples[sampleIndex] = value.toShort()
            sampleIndex++
            i += 2
        }

        val format = WavFormat(
            sampleRate = sampleRate,
            channels = channels,
            bitsPerSample = bitsPerSample
        )
        return Pair(format, samples)
    }

    private fun byteToIntLE(bytes: ByteArray, start: Int): Int {
        val b0 = bytes[start].toInt() and 0xFF
        val b1 = bytes[start+1].toInt() and 0xFF
        val b2 = bytes[start+2].toInt() and 0xFF
        val b3 = bytes[start+3].toInt() and 0xFF
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }
}
