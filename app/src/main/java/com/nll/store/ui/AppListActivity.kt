package com.nll.store.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.nll.store.R
import com.nll.store.connectivity.InternetStateProvider
import com.nll.store.databinding.ActivityAppListBinding
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.StoreConnectionState
import io.github.solrudev.simpleinstaller.activityresult.InstallPermissionContract
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class AppListActivity : AppCompatActivity() {
    private val logTag = "AppListActivity"
    private lateinit var binding: ActivityAppListBinding
    private val viewModel: AppListActivityViewModel by viewModels {
        AppListActivityViewModel.Factory(application)
    }
    private lateinit var appsListAdapter: AppsListAdapter
    private val installPermissionLauncher = registerForActivityResult(InstallPermissionContract()) { isGranted ->
        if (CLog.isDebug()) {
            CLog.log(logTag, "installPermissionLauncher() -> isGranted: $isGranted")
        }
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

        if (CLog.isDebug()) {
            CLog.log(logTag, "notificationPermissionLauncher() -> isGranted: $isGranted")
        }
    }

    private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::installPackage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appsListAdapter = AppsListAdapter { data, position -> onAppClick(data) }

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
        observerInstallState()
        observeNetworkState()

    }

    private fun observerAppList() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerAppList()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.appsList.collect { appDatas ->
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
                viewModel.storeConnectionState.collect { storeConnectionState ->
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
                                    viewModel.loadAppList()
                                }

                                override fun onActionClick() {
                                    viewModel.loadAppList()
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                InternetStateProvider.networkStateFlow().collect { networkStateFlow ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "networkStateFlow() -> $networkStateFlow")
                    }
                    if (networkStateFlow.isDeviceOnline()) {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "networkStateFlow() -> Device is online. Call viewModel.loadAppList()")
                        }
                        viewModel.loadAppList()
                    } else {
                        askDeviceToBeMadeOnline()
                    }

                }
            }
        }
    }

    fun onInstallButtonClick() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "onInstallButtonClick()")
        }
        if (!viewModel.isInstalling) {
            try {
                pickApkLauncher.launch("application/vnd.android.package-archive")
            } catch (e: ActivityNotFoundException) {
                CLog.logPrintStackTrace(e)
            }
        } else {
            Toast.makeText(this, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observerInstallState() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerInstallState()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.installState.onEach { installState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observerInstallState() -> installState: $installState")
                    }
                }
            }
        }

    }

    private fun onAppClick(localAppData: AppData) {

    }

    private fun requestPermissions() {
        if (packageManager.canRequestPackageInstalls()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }

        }
        installPermissionLauncher.launch()
    }


    private fun askDeviceToBeMadeOnline() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "askDeviceToBeMadeOnline()")
        }
        Toast.makeText(this, R.string.internet_conn_required, Toast.LENGTH_SHORT).show()
        InternetStateProvider.openQuickInterNetConnectivityMenuIfYouCan(this)
    }

}