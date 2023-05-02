package com.nll.store.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nll.store.R
import com.nll.store.databinding.FragmentAppInstallerBinding
import com.nll.store.installer.AppInstallManager
import com.nll.store.log.CLog
import com.nll.store.utils.extHumanReadableByteCount
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import kotlinx.coroutines.launch


class InstallAppFragment : DialogFragment() {
    private val logTag = "InstallAppFragment"
    private lateinit var binding: FragmentAppInstallerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Widget_AppTheme_FullScreenDialog)
        if (CLog.isDebug()) {
            CLog.log(logTag, "onCreate()")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (CLog.isDebug()) {
            CLog.log(logTag, "onCreateView")
        }

        binding = FragmentAppInstallerBinding.inflate(requireActivity().layoutInflater)

        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        observerInstallState()

        return binding.root
    }


    private fun observerInstallState() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerInstallState()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {

                AppInstallManager.getInstance(requireContext()).observeInstallState().collect { installState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerInstallState() -> installState: $installState")
                    }

                    when (installState) {
                        is AppInstallManager.State.Download.Completed -> {
                            val totalBytes = installState.downloadedFile.length().extHumanReadableByteCount(true)
                            binding.downloadProgressText.text = getString(R.string.downloaded)
                            binding.downloadedBytes.text = totalBytes
                            binding.downloadProgress.progress = 100
                            binding.totalBytes.text = totalBytes

                            with(binding.installDownloadedAppButton) {
                                isEnabled = true
                                setOnClickListener {
                                    if (CLog.isDebug()) {
                                        CLog.log(logTag, "installButton() clicked, isInstalling: ${AppInstallManager.getInstance(requireContext()).isInstalling()}, packageManager.canRequestPackageInstalls(): ${requireContext().packageManager.canRequestPackageInstalls()}")
                                    }

                                    if (!AppInstallManager.getInstance(requireContext()).isInstalling()) {
                                        AppInstallManager.getInstance(requireContext()).install(installState.downloadedFile)
                                    } else {
                                        Toast.makeText(requireContext(), R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        is AppInstallManager.State.Download.Error -> {
                            binding.downloadProgressText.text = installState.toString()
                            binding.downloadProgress.progress = 0
                            binding.installDownloadedAppButton.isEnabled = false
                            with(MaterialAlertDialogBuilder(requireContext()))
                            {
                                setMessage(R.string.download_manually)
                                setPositiveButton(R.string.download) { _, _ ->
                                    startManualDownload(installState.storeAppData.downloadUrl.toUri())
                                }
                                show()
                            }
                            val message = when (installState.message) {
                                is AppInstallManager.State.Download.Error.Message.GenericError -> installState.message.message
                                AppInstallManager.State.Download.Error.Message.MalformedFile -> requireContext().getString(R.string.malformed_file)
                                is AppInstallManager.State.Download.Error.Message.ServerError -> getString(R.string.server_error, installState.message.responseCode.toString())
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }

                        is AppInstallManager.State.Download.Progress -> {
                            val totalBytes = installState.totalBytes.extHumanReadableByteCount(true)
                            val downloadedBytes = installState.bytesCopied.extHumanReadableByteCount(true)
                            binding.downloadProgressText.text = getString(R.string.downloading)
                            binding.downloadedBytes.text = downloadedBytes
                            binding.downloadProgress.progress = installState.percent
                            binding.totalBytes.text = totalBytes
                        }

                        is AppInstallManager.State.Download.Started -> {

                            binding.toolbar.title = installState.storeAppData.name
                            binding.installVersionNotes.text = if (installState.storeAppData.versionNotes.isEmpty()) {
                                installState.storeAppData.description
                            } else {
                                installState.storeAppData.versionNotes
                            }


                        }

                        is AppInstallManager.State.Install.Completed -> {
                            when (installState.installResult) {
                                InstallResult.Success -> {
                                    dismiss()
                                }

                                is InstallResult.Failure -> {
                                    binding.installDownloadedAppButton.isEnabled = true

                                    val message = when (installState.installResult.cause) {
                                        is InstallFailureCause.Aborted -> getString(R.string.install_error_aborted)
                                        is InstallFailureCause.Blocked -> getString(R.string.install_error_blocked)
                                        is InstallFailureCause.Conflict -> getString(R.string.install_error_conflict)
                                        is InstallFailureCause.Generic -> installState.installResult.cause?.message ?: getString(R.string.unknown_error)
                                        is InstallFailureCause.Incompatible -> getString(R.string.install_error_incompatible)
                                        is InstallFailureCause.Invalid -> getString(R.string.install_error_invalid)
                                        is InstallFailureCause.Storage -> getString(R.string.install_error_storage)
                                        null -> getString(R.string.unknown_error)
                                    }
                                    with(MaterialAlertDialogBuilder(requireContext()))
                                    {
                                        setTitle(R.string.install_error)
                                        setMessage(message)
                                        setPositiveButton(R.string.ok) { _, _ ->
                                            dismiss()
                                        }
                                        show()
                                    }

                                    Toast.makeText(requireContext(), installState.installResult.cause?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        is AppInstallManager.State.Install.Progress -> {
                            binding.installDownloadedAppButton.isEnabled = false

                            //TODO()


                        }

                        AppInstallManager.State.Install.Started -> {
                            binding.installDownloadedAppButton.isEnabled = false

                            //TODO()


                        }

                        null -> {


                            //TODO()


                        }
                    }


                }
            }
        }

    }

    private fun startManualDownload(downloadUrl: Uri) {
        try {
            /**
             * TODO Do we need FLAG_ACTIVITY_NEW_DOCUMENT
             * An activity that handles documents can use this attribute so that with every document you open you launch a separate instance of the same activity.
             * If you check your recent apps, then you will see various screens of the same activity of your app, each using a different document.
             */
            val openIntent = Intent(Intent.ACTION_VIEW, downloadUrl).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
            startActivity(openIntent)
        } catch (e: Exception) {
            CLog.logPrintStackTrace(e)
            Toast.makeText(requireContext(), R.string.no_url_handle, Toast.LENGTH_LONG).show()
        }

    }

    companion object {

        private const val logTag = "InstallAppFragment"
        private const val fragmentTag = "install-app-fragment"

        fun display(fragmentManager: FragmentManager) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "display()")
            }
            InstallAppFragment()
                .show(fragmentManager, fragmentTag)
        }

    }
}