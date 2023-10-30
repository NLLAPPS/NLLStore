package com.nll.store.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.nll.store.R
import com.nll.store.activityresult.InstallPermissionContract
import com.nll.store.api.StoreApiManager
import com.nll.store.connectivity.InternetStateProvider
import com.nll.store.databinding.ActivityAppListBinding
import com.nll.store.debug.DebugLogActivity
import com.nll.store.debug.DebugLogService
import com.nll.store.installer.AppInstallManager
import com.nll.store.installer.InstallationState
import com.nll.store.installer.PackageInstallFailureCause
import com.nll.store.installer.PackageInstallResult
import com.nll.store.installer.UriApkSource
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.model.StoreConnectionState
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.extTryStartActivity
import kotlinx.coroutines.launch


class AppListActivity : AppCompatActivity() {
    private val logTag = "AppListActivity"
    private lateinit var binding: ActivityAppListBinding
    private lateinit var storeApiManager: StoreApiManager
    private lateinit var appsListAdapter: AppsListAdapter
    private val installPermissionLauncher = registerForActivityResult(InstallPermissionContract(this)) { isGranted ->
        if (CLog.isDebug()) {
            CLog.log(logTag, "installPermissionLauncher() -> isGranted: $isGranted")
        }
        val message = if (isGranted) {
            R.string.install_permission_granted
        } else {
            R.string.permission_denied
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    }


    private val postNotificationPermission = activityResultRegistry.register("notification", ActivityResultContracts.RequestPermission()) { hasNotificationPermission ->
        if (hasNotificationPermission) {
            DebugLogService.startLogging(this)
        }
    }


    private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val hasReadPermission = checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
            if (hasReadPermission) {
                AppInstallManager.install(this, null, arrayOf(UriApkSource(it)))
            } else {
                Toast.makeText(this, R.string.unable_to_open_file, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storeApiManager = StoreApiManager.getInstance(this)


        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.selectApkAndInstall -> {
                    pickAndInstall()
                }

                R.id.openDebugLog -> {
                    startActivity(Intent(this, DebugLogActivity::class.java))
                }
            }

            true
        }



        appsListAdapter = AppsListAdapter(object : AppsListAdapter.CallBack {
            override fun onCardClick(data: AppData, position: Int) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "AppsListAdapter() -> onCardClick() -> data: $data")
                }
            }

            override fun onInstallClick(storeAppData: StoreAppData, position: Int) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "AppsListAdapter() -> onInstallClick() -> storeAppData: $storeAppData")
                }
                if (storeAppData.isNLLStoreApp) {
                    if (!AppInstallManager.hasActiveSession) {

                        if (packageManager.canRequestPackageInstalls()) {

                            /**
                             * Ask user to approve notification permission so we can use it when checking updates
                             */
                            if (ApiLevel.isTPlus() && !hasNotificationPermission()) {
                                postNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }

                            AppInstallManager.startDownload(this@AppListActivity, storeAppData, null)
                            InstallAppFragment.display(supportFragmentManager)
                        } else {
                            Toast.makeText(this@AppListActivity, R.string.install_permission_request, Toast.LENGTH_SHORT).show()
                            installPermissionLauncher.launch()
                        }

                    } else {
                        Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val openIntent = Intent(Intent.ACTION_VIEW, storeAppData.downloadUrl.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    }
                    extTryStartActivity(openIntent)
                }

            }

            override fun onOpenClick(localAppData: LocalAppData, position: Int) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "AppsListAdapter() -> onOpenClick() -> localAppData: $localAppData")
                }
                packageManager.getLaunchIntentForPackage(localAppData.packageName)?.let {
                    startActivity(it)
                }
            }

            override fun onUpdateClick(storeAppData: StoreAppData, localAppData: LocalAppData, position: Int) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "AppsListAdapter() -> onUpdateClick() -> storeAppData: $storeAppData, localAppData: $localAppData")
                }

                if (storeAppData.isNLLStoreApp) {
                    if (!AppInstallManager.hasActiveSession) {
                        AppInstallManager.startDownload(this@AppListActivity, storeAppData, localAppData)
                        InstallAppFragment.display(supportFragmentManager)
                    } else {
                        Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val openIntent = Intent(Intent.ACTION_VIEW, storeAppData.downloadUrl.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    }
                    extTryStartActivity(openIntent)
                }
            }

        })

        with(binding.recyclerView) {
            val linearLayoutManager = LinearLayoutManager(this@AppListActivity)
            val divider = DividerItemDecoration(this@AppListActivity, linearLayoutManager.orientation).apply {
                ContextCompat.getDrawable(this@AppListActivity, R.drawable.app_list_divider)?.let {
                    setDrawable(it)
                }

            }

            adapter = appsListAdapter
            layoutManager = linearLayoutManager
            addItemDecoration(divider)
        }

        observerStoreConnectionState()
        observerAppList()
        observeNetworkState()
        observerInstallState()

    }

    private fun observerInstallState() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerInstallState()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {

                AppInstallManager.observeInstallState().collect { installState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerInstallState() -> installState: $installState")
                    }

                    when (installState) {
                        /**
                         * We are only interested with results after user selects a file via Install menu
                         */
                        is InstallationState.Install.Completed -> {
                            when (installState.installResult) {
                                PackageInstallResult.Success -> {
                                    Toast.makeText(this@AppListActivity, R.string.install_success, Toast.LENGTH_SHORT).show()
                                }

                                is PackageInstallResult.Failure -> {
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

                                    Toast.makeText(this@AppListActivity, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        else -> {
                            // Unused
                        }
                    }
                }
            }
        }

    }

    private fun observerAppList() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerAppList()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                storeApiManager.observeAppList().collect { appDatas ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerAppList() -> appDatas: $appDatas")
                    }
                    appsListAdapter.submitList(appDatas)
                }
            }
        }

    }

    private fun observerStoreConnectionState() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerStoreConnectionState()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                storeApiManager.observeStoreConnectionState().collect { storeConnectionState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerStoreConnectionState() -> storeConnectionState: $storeConnectionState")
                    }
                    when (storeConnectionState) {
                        StoreConnectionState.Connected -> binding.loadingProgressIndicator.isVisible = false
                        StoreConnectionState.Connecting -> binding.loadingProgressIndicator.isVisible = true
                        is StoreConnectionState.Failed -> {

                            binding.loadingProgressIndicator.isVisible = false

                            val errorMessage = getString(R.string.error_placeholder, storeConnectionState.apiException.message ?: getString(R.string.unknown_error))
                            val actionText = getString(R.string.retry)
                            SnackProvider.provideDefaultSnack(root = binding.root, snackText = errorMessage, snackActionText = actionText, snackClickListener = object : SnackProvider.ViewClickListener {

                                override fun onSnackViewClick() {
                                    storeApiManager.loadAppList()
                                }

                                override fun onActionClick() {
                                    storeApiManager.loadAppList()
                                }

                            }).show()
                        }
                    }

                }
            }
        }

    }

    private fun observeNetworkState() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observeNetworkState()")
        }
        /**
         * Lifecycle.State.STARTED is important we don't want infinite loop since showing network dialog changes the resume state.
         * CREATED is not applicable as we might re-load when app becomes visible
         */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InternetStateProvider.networkStateFlow().collect { networkStateFlow ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observeNetworkState() -> $networkStateFlow")
                    }
                    if (networkStateFlow.isDeviceOnline()) {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "observeNetworkState() -> Device is online. Call storeApiManager.loadAppList()")
                        }
                        storeApiManager.loadAppList()
                    } else {
                        askDeviceToBeMadeOnline()
                    }

                }
            }
        }
    }

    private fun pickAndInstall() {
        if (!AppInstallManager.hasActiveSession) {
            try {
                pickApkLauncher.launch("application/vnd.android.package-archive")
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
                CLog.logPrintStackTrace(e)
            }
        } else {
            Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
        }
    }


    private fun askDeviceToBeMadeOnline() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "askDeviceToBeMadeOnline()")
        }
        Toast.makeText(this, R.string.internet_conn_required, Toast.LENGTH_SHORT).show()
        InternetStateProvider.openQuickInterNetConnectivityMenuIfYouCan(this)
    }

    private fun hasNotificationPermission() = if (ApiLevel.isTPlus()) {
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}