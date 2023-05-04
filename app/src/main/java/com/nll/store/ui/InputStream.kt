package com.nll.store.ui

import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

inline fun InputStream.extCopyTo(outputStream: OutputStream, totalSize: Long, progressOffsetBytes: Long = 0, crossinline onProgressChanged: (progress: Int, max: Int) -> Unit) {
    val bufferLength = 8192L
    val progressRatio = ceil(totalSize.toDouble() / (bufferLength.coerceAtLeast(1) * 100)).toInt().coerceAtLeast(1)
    val progressOffset = progressOffsetBytes / bufferLength
    source().buffer().use { source ->
        outputStream.sink().buffer().use { sink ->
            val progressMax = ceil(totalSize.toDouble() / (bufferLength * progressRatio))
                .toInt()
                .coerceAtLeast(1)
            var currentProgress = progressOffset
            Buffer().use { buffer ->
                while (source.read(buffer, bufferLength) > 0) {
                    sink.write(buffer, buffer.size)
                    currentProgress++
                    if (currentProgress % progressRatio == 0L) {
                        val progress = (currentProgress.toDouble() / (progressRatio * progressMax) * 100).toInt()
                        onProgressChanged(progress, 100)
                    }
                }
                sink.flush()
            }
        }
    }
}