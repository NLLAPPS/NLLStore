package io.github.solrudev.simpleinstaller.data.utils

import io.github.solrudev.simpleinstaller.data.ProgressData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.yield

@JvmSynthetic
internal fun MutableSharedFlow<ProgressData>.tryEmit(currentProgress: Int, progressMax: Int) =
	tryEmit(ProgressData(currentProgress, progressMax))

@JvmSynthetic
internal suspend inline fun MutableSharedFlow<ProgressData>.makeIndeterminate() {
	yield()
	emit(ProgressData(isIndeterminate = true))
}