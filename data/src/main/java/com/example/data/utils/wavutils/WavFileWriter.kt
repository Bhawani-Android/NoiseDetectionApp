package com.example.data.utils.wavutils

import java.io.File
import java.io.FileOutputStream

object WavFileWriter {

    fun writeWav(outputFile: File, format: WavFormat, samples: ShortArray) {
        val fos = FileOutputStream(outputFile)
        val header = createWavHeader(format, samples.size)
        fos.write(header, 0, 44)

        val buffer = ByteArray(samples.size * 2)
        var idx = 0
        for (sample in samples) {
            buffer[idx] = (sample.toInt() and 0xFF).toByte()
            buffer[idx+1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            idx += 2
        }
        fos.write(buffer)
        fos.close()

        fixSizesInHeader(outputFile, format, samples.size)
    }

    private fun createWavHeader(format: WavFormat, numSamples: Int): ByteArray {
        val channels = format.channels
        val sampleRate = format.sampleRate
        val bitsPerSample = format.bitsPerSample
        val header = ByteArray(44)
        // "RIFF"
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // chunkSize => placeholder
        // "WAVE"
        header[8]  = 'W'.code.toByte()
        header[9]  = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // subChunk1Size=16 => PCM
        header[16] = 16
        // audioFormat=1 => PCM
        header[20] = 1
        // numChannels
        header[22] = channels.toByte()
        // sampleRate
        writeIntLE(header, 24, sampleRate)
        // byteRate
        val byteRate = sampleRate * channels * bitsPerSample / 8
        writeIntLE(header, 28, byteRate)
        // blockAlign
        val blockAlign = channels * bitsPerSample / 8
        writeShortLE(header, 32, blockAlign.toShort())
        // bitsPerSample
        writeShortLE(header, 34, bitsPerSample.toShort())
        // "data"
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        return header
    }

    private fun fixSizesInHeader(file: File, format: WavFormat, numSamples: Int) {
        val raf = file.inputStream().use { it.readBytes() }
        val channels = format.channels
        val bitsPerSample = format.bitsPerSample
        val subChunk2Size = numSamples * channels * bitsPerSample / 8
        val chunkSize = subChunk2Size + 36

        writeIntLE(raf, 4, chunkSize)
        writeIntLE(raf, 40, subChunk2Size)
        file.writeBytes(raf)
    }

    private fun writeIntLE(buffer: ByteArray, start: Int, value: Int) {
        buffer[start]   = (value and 0xFF).toByte()
        buffer[start+1] = ((value shr 8) and 0xFF).toByte()
        buffer[start+2] = ((value shr 16) and 0xFF).toByte()
        buffer[start+3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buffer: ByteArray, start: Int, value: Short) {
        buffer[start]   = (value.toInt() and 0xFF).toByte()
        buffer[start+1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}
