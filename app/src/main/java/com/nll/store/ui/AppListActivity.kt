package com.nll.store.ui

import android.content.ActivityNotFoundException
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
import com.nll.store.installer.AppInstallManager
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.model.StoreConnectionState
import io.github.solrudev.simpleinstaller.activityresult.InstallPermissionContract
import kotlinx.coroutines.launch


class AppListActivity : AppCompatActivity() {
    private val logTag = "AppListActivity"
    private lateinit var binding: ActivityAppListBinding
    private lateinit var appInstallManager : AppInstallManager
    private val storeApiViewModel: StoreApiViewModel by viewModels {
        StoreApiViewModel.Factory(application)
    }
    private lateinit var appsListAdapter: AppsListAdapter

    private val installPermissionLauncher = registerForActivityResult(InstallPermissionContract()) { isGranted ->
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


    private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            appInstallManager.install(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appInstallManager = AppInstallManager.getInstance(this)

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.selectApkAndInstall) {
                pickAndInstall()
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
                if (!appInstallManager.isInstalling()) {

                    if (packageManager.canRequestPackageInstalls()) {
                        appInstallManager.startDownload(storeAppData, null)
                        InstallAppFragment.display(supportFragmentManager)
                    } else {
                        Toast.makeText(this@AppListActivity, R.string.install_permission_request, Toast.LENGTH_SHORT).show()
                        installPermissionLauncher.launch()
                    }

                } else {
                    Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
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
                if (!appInstallManager.isInstalling()) {
                    //TODO("Implement ")
                } else {
                    Toast.makeText(this@AppListActivity, R.string.ongoing_installation, Toast.LENGTH_SHORT).show()
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

    }

    private fun observerAppList() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "observerAppList()")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                storeApiViewModel.appsList.collect { appDatas ->
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
                storeApiViewModel.storeConnectionState.collect { storeConnectionState ->
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
                                    storeApiViewModel.loadAppList()
                                }

                                override fun onActionClick() {
                                    storeApiViewModel.loadAppList()
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
                        storeApiViewModel.loadAppList()
                    } else {
                        askDeviceToBeMadeOnline()
                    }

                }
            }
        }
    }

    private fun pickAndInstall() {
        if (!appInstallManager.isInstalling()) {
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

}