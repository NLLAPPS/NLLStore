package com.nll.store.ui

import android.content.Intent
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
import com.nll.store.installer.FileApkSource
import com.nll.store.installer.InstallationState
import com.nll.store.installer.PackageInstallFailureCause
import com.nll.store.installer.PackageInstallResult
import com.nll.store.log.CLog
import com.nll.store.utils.extHumanReadableByteCount
import com.nll.store.utils.extTryStartActivity
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
        viewLifecycleOwner.lifecycleScope .launch {
            /**
             * Must be Lifecycle.State.CREATED otherwise we miss download completed event while waiting fro user to confirm notification permission
             */
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {

                AppInstallManager.observeInstallState().collect { installState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerInstallState() -> installState: $installState")
                    }

                    when (installState) {
                        is InstallationState.Download.Completed -> {
                            val totalBytes = installState.downloadedFile.length().extHumanReadableByteCount(true)
                            binding.downloadProgressText.text = getString(R.string.downloaded)
                            binding.downloadedBytes.text = totalBytes
                            binding.downloadProgress.progress = 100
                            binding.totalBytes.text = totalBytes

                            with(binding.installDownloadedAppButton) {
                                isEnabled = true
                                setOnClickListener {
                                    if (CLog.isDebug()) {
                                        CLog.log(logTag, "installButton() clicked, hasActiveSession: ${AppInstallManager.hasActiveSession}, packageManager.canRequestPackageInstalls(): ${requireContext().packageManager.canRequestPackageInstalls()}")
                                    }

                                    if (!AppInstallManager.hasActiveSession) {
                                        AppInstallManager.install(requireContext(), installState.storeAppData.packageName, arrayOf(FileApkSource(installState.downloadedFile)))
                                    } else {
                                        Toast.makeText(requireContext(), R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        is InstallationState.Download.Error -> {
                            binding.downloadProgressText.text = installState.toString()
                            binding.downloadProgress.progress = 0
                            binding.installDownloadedAppButton.isEnabled = false
                            with(MaterialAlertDialogBuilder(requireContext()))
                            {
                                setMessage(R.string.download_manually)
                                setPositiveButton(R.string.download) { _, _ ->
                                    val openIntent = Intent(Intent.ACTION_VIEW, installState.storeAppData.downloadUrl.toUri()).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                                    }
                                    startActivity(openIntent)
                                    requireContext().extTryStartActivity(openIntent)
                                }
                                show()
                            }
                            val message = when (installState.message) {
                                is InstallationState.Download.Error.Message.GenericError -> installState.message.message
                                InstallationState.Download.Error.Message.MalformedFile -> requireContext().getString(R.string.malformed_file)
                                is InstallationState.Download.Error.Message.ServerError -> getString(R.string.server_error, installState.message.responseCode.toString())
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }

                        is InstallationState.Download.Progress -> {
                            val totalBytes = installState.totalBytes.extHumanReadableByteCount(true)
                            val downloadedBytes = installState.bytesCopied.extHumanReadableByteCount(true)
                            binding.downloadProgressText.text = getString(R.string.downloading)
                            binding.downloadedBytes.text = downloadedBytes
                            binding.downloadProgress.progress = installState.percent
                            binding.totalBytes.text = totalBytes
                        }

                        is InstallationState.Download.Started -> {

                            binding.toolbar.title = installState.storeAppData.name
                            binding.installVersionNotes.text = if (installState.storeAppData.versionNotes.isEmpty()) {
                                installState.storeAppData.description
                            } else {
                                installState.storeAppData.versionNotes
                            }


                        }

                        is InstallationState.Install.Completed -> {
                            when (installState.installResult) {
                                PackageInstallResult.Success -> {
                                    dismiss()
                                }

                                is PackageInstallResult.Failure -> {
                                    binding.installDownloadedAppButton.isEnabled = true

                                    val message = when (installState.installResult.cause) {
                                        is PackageInstallFailureCause.Aborted -> getString(R.string.install_error_aborted)
                                        is PackageInstallFailureCause.Blocked -> getString(R.string.install_error_blocked)
                                        is PackageInstallFailureCause.Conflict -> getString(R.string.install_error_conflict)
                                        is PackageInstallFailureCause.Generic -> installState.installResult.cause.message ?: getString(R.string.unknown_error)
                                        is PackageInstallFailureCause.Incompatible -> getString(R.string.install_error_incompatible)
                                        is PackageInstallFailureCause.Invalid -> getString(R.string.install_error_invalid)
                                        is PackageInstallFailureCause.Storage -> getString(R.string.install_error_storage)
                                        null -> getString(R.string.install_error_unknown_or_user_cancelled)
                                    }
                                    with(MaterialAlertDialogBuilder(requireContext()))
                                    {
                                        setTitle(R.string.install_error)
                                        setMessage(message)
                                        setPositiveButton(R.string.ok, null)
                                        show()
                                    }

                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        is InstallationState.Install.Progress -> {
                            binding.installDownloadedAppButton.isEnabled = false
                        }

                        InstallationState.Install.Started -> {
                            binding.installDownloadedAppButton.isEnabled = false

                        }

                        null -> {


                           //Unused


                        }
                    }


                }
            }
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