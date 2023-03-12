package io.github.solrudev.simpleinstaller.data

/**
 * Represents progress data.
 */
public data class ProgressData(
	public val progress: Int = 0,
	public val max: Int = 100,
	public val isIndeterminate: Boolean = false
)