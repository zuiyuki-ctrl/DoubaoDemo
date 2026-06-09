package org.example.project

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var outputFile: File? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(outputFile: File) {
        stop()

        this.outputFile = outputFile

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        val bufferSize = minBufferSize * 2

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord = recorder
        isRecording = true

        writeWavHeader(outputFile, sampleRate)

        recorder.startRecording()

        recordingThread = thread(start = true) {
            val buffer = ByteArray(bufferSize)

            RandomAccessFile(outputFile, "rw").use { wavFile ->
                wavFile.seek(44)

                while (isRecording) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        wavFile.write(buffer, 0, read)
                    }
                }

                updateWavHeader(wavFile)
            }
        }
    }

    fun stop(): File? {
        val file = outputFile

        isRecording = false
        recordingThread?.join(500)

        runCatching {
            audioRecord?.stop()
        }
        audioRecord?.release()

        audioRecord = null
        recordingThread = null
        outputFile = null

        return file
    }

    fun cancel() {
        val file = stop()
        file?.delete()
    }

    private fun writeWavHeader(
        file: File,
        sampleRate: Int
    ) {
        RandomAccessFile(file, "rw").use { wavFile ->
            wavFile.setLength(0)
            wavFile.write(ByteArray(44))
            wavFile.seek(0)

            wavFile.writeBytes("RIFF")
            wavFile.writeIntLe(36)
            wavFile.writeBytes("WAVE")
            wavFile.writeBytes("fmt ")
            wavFile.writeIntLe(16)
            wavFile.writeShortLe(1)
            wavFile.writeShortLe(1)
            wavFile.writeIntLe(sampleRate)
            wavFile.writeIntLe(sampleRate * 2)
            wavFile.writeShortLe(2)
            wavFile.writeShortLe(16)
            wavFile.writeBytes("data")
            wavFile.writeIntLe(0)
        }
    }

    private fun updateWavHeader(wavFile: RandomAccessFile) {
        val fileSize = wavFile.length()
        val audioSize = fileSize - 44

        wavFile.seek(4)
        wavFile.writeIntLe((fileSize - 8).toInt())

        wavFile.seek(40)
        wavFile.writeIntLe(audioSize.toInt())
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }
}
