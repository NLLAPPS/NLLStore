package io.github.solrudev.simpleinstaller.data

/**
 * Options for install/uninstall session.
 */
public class SessionOptions private constructor(

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [ConfirmationStrategy.DEFERRED].
	 */
	public val confirmationStrategy: ConfirmationStrategy,

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmationStrategy] is [ConfirmationStrategy.IMMEDIATE].
	 */
	public val notificationData: NotificationData
) {

	override fun toString(): String {
		return "SessionOptions(confirmationStrategy=$confirmationStrategy, notificationData=$notificationData)"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as SessionOptions
		if (confirmationStrategy != other.confirmationStrategy) return false
		if (notificationData != other.notificationData) return false
		return true
	}

	override fun hashCode(): Int {
		var result = confirmationStrategy.hashCode()
		result = 31 * result + notificationData.hashCode()
		return result
	}

	public companion object {

		/**
		 * Default session options.
		 *
		 * Confirmation strategy is [ConfirmationStrategy.DEFERRED] and notification data is [NotificationData.DEFAULT].
		 */
		@JvmField
		public val DEFAULT: SessionOptions = SessionOptions(ConfirmationStrategy.DEFERRED, NotificationData.DEFAULT)
	}

	/**
	 * Builder for [SessionOptions].
	 */
	@SessionOptionsDslMarker
	public class Builder {

		/**
		 * A strategy for handling user's confirmation of installation or uninstallation.
		 *
		 * Default strategy is [ConfirmationStrategy.DEFERRED].
		 */
		@set:JvmSynthetic
		public var confirmationStrategy: ConfirmationStrategy = DEFAULT.confirmationStrategy

		/**
		 * Data for a high-priority notification which launches confirmation activity.
		 *
		 * Default value is [NotificationData.DEFAULT].
		 *
		 * Ignored when [confirmationStrategy] is [ConfirmationStrategy.IMMEDIATE].
		 */
		@set:JvmSynthetic
		public var notificationData: NotificationData = DEFAULT.notificationData

		/**
		 * Sets [SessionOptions.confirmationStrategy].
		 */
		public fun setConfirmationStrategy(confirmationStrategy: ConfirmationStrategy): Builder = apply {
			this.confirmationStrategy = confirmationStrategy
		}

		/**
		 * Sets [SessionOptions.notificationData].
		 */
		public fun setNotificationData(notificationData: NotificationData): Builder = apply {
			this.notificationData = notificationData
		}

		/**
		 * Constructs a new instance of [SessionOptions].
		 */
		public fun build(): SessionOptions = SessionOptions(confirmationStrategy, notificationData)
	}
}

@DslMarker
internal annotation class SessionOptionsDslMarker

/**
 * Constructs a new instance of [NotificationData] and sets it to [SessionOptions].
 */
@JvmSynthetic
public inline fun SessionOptions.Builder.notification(
	initializer: NotificationData.Builder.() -> Unit
): NotificationData {
	val notificationData = NotificationData(initializer)
	this.notificationData = notificationData
	return notificationData
}

/**
 * Constructs a new instance of [SessionOptions].
 */
@JvmSynthetic
public inline fun SessionOptions(initializer: SessionOptions.Builder.() -> Unit): SessionOptions {
	return SessionOptions.Builder().apply(initializer).build()
}