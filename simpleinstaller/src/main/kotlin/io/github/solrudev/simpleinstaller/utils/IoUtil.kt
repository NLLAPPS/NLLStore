package io.github.solrudev.simpleinstaller.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

private const val BUFFER_LENGTH = 8192L

/**
 * @param onProgressChanged max will always be 100.
 */
@JvmSynthetic
internal suspend inline fun InputStream.copyTo(
	outputStream: OutputStream,
	totalSize: Long,
	progressOffsetBytes: Long = 0,
	crossinline onProgressChanged: suspend (progress: Int, max: Int) -> Unit
) = withContext(Dispatchers.IO) {
	val progressRatio = calculateProgressRatio(totalSize, BUFFER_LENGTH)
	val progressOffset = progressOffsetBytes / BUFFER_LENGTH
	source().buffer().use { source ->
		outputStream.sink().buffer().use { sink ->
			val progressMax = ceil(totalSize.toDouble() / (BUFFER_LENGTH * progressRatio))
				.toInt()
				.coerceAtLeast(1)
			var currentProgress = progressOffset
			Buffer().use { buffer ->
				while (source.read(buffer, BUFFER_LENGTH) > 0) {
					ensureActive()
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