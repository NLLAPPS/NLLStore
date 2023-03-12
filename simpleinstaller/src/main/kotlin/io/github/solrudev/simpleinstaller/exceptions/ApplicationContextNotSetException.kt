package io.github.solrudev.simpleinstaller.exceptions

public class ApplicationContextNotSetException : Exception() {
	public override val message: String = "SimpleInstaller was not properly initialized. " +
			"Check that SimpleInstallerInitializer is not disabled in your manifest file."
}