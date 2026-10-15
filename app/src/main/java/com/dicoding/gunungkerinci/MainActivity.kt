package com.dicoding.gunungkerinci

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.repository.DisasterReportRepository
import com.dicoding.gunungkerinci.data.repository.SosCallRepository
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import com.dicoding.gunungkerinci.databinding.ActivityMainBinding
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.util.LokasiProvider
import com.dicoding.gunungkerinci.ui.common.CustomBottomBar
import com.dicoding.gunungkerinci.ui.common.NavTab
import com.dicoding.gunungkerinci.ui.panic_button.DisasterReportSheet
import com.dicoding.gunungkerinci.ui.panic_button.DisasterReportViewModel
import com.dicoding.gunungkerinci.ui.panic_button.DisasterReportViewModelFactory
import com.dicoding.gunungkerinci.ui.panic_button.PanicButtonViewModel
import com.dicoding.gunungkerinci.ui.panic_button.PanicButtonViewModelFactory
import com.dicoding.gunungkerinci.ui.panic_button.SosBottomSheet
import com.dicoding.gunungkerinci.ui.panic_button.SosConfirmDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedTab by mutableStateOf(NavTab.BERANDA)
    private var pendingSosSeverity: String? = null

    private val panicButtonViewModel: PanicButtonViewModel by viewModels {
        val api = ApiConfig.getApiService(applicationContext)
        PanicButtonViewModelFactory(
            sosRepository = SosRepository(
                api = api,
                pendingSosDao = TnksDatabase.getInstance(applicationContext).pendingSosDao()
            ),
            syncScheduler = SyncScheduler(applicationContext),
            sosCallRepository = SosCallRepository(api)
        )
    }

    private val disasterReportViewModel: DisasterReportViewModel by viewModels {
        DisasterReportViewModelFactory(
            DisasterReportRepository(ApiConfig.getApiService(applicationContext))
        )
    }

    private val sosPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val severity = pendingSosSeverity
        pendingSosSeverity = null
        if (granted && severity != null) {
            doSendSos(severity)
        } else if (!granted) {
            Toast.makeText(this, "Izin lokasi diperlukan untuk mengirim SOS", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment)

        binding.composeBottomNav.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val panicState by panicButtonViewModel.uiState.collectAsState()
                val reportState by disasterReportViewModel.uiState.collectAsState()

                var showSosSheet by remember { mutableStateOf(false) }
                var showReportSheet by remember { mutableStateOf(false) }
                var showSosDialog by remember { mutableStateOf(false) }

                LaunchedEffect(panicState.successMessage) {
                    panicState.successMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                        panicButtonViewModel.clearMessages()
                    }
                }
                LaunchedEffect(panicState.errorMessage) {
                    panicState.errorMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                        panicButtonViewModel.clearMessages()
                    }
                }

                CustomBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        when (tab) {
                            NavTab.BERANDA -> navController.navigateToTab(R.id.navigation_beranda)
                            NavTab.TIKET -> navController.navigateToTab(R.id.navigation_tiket)
                            NavTab.CHECK_POINT -> navController.navigateToTab(R.id.navigation_jejak)
                            NavTab.AKUN -> navController.navigateToTab(R.id.navigation_profile)
                            // SOS tidak bernavigasi — dibuka lewat bottom sheet (onSosClick).
                            NavTab.SOS -> Unit
                        }
                    },
                    onSosClick = {
                        showSosSheet = true
                        panicButtonViewModel.clearMessages()
                        panicButtonViewModel.loadCallOptions(
                            UserPreference(this@MainActivity).getToken().orEmpty()
                        )
                    }
                )

                if (showSosSheet) {
                    SosBottomSheet(
                        uiState = panicState,
                        onDismiss = { showSosSheet = false },
                        onKirimPesan = {
                            showSosSheet = false
                            navController.navigate(R.id.navigation_pesan_darurat)
                        },
                        onLaporan = {
                            showSosSheet = false
                            disasterReportViewModel.clearMessages()
                            showReportSheet = true
                        },
                        onSosTrigger = {
                            showSosSheet = false
                            showSosDialog = true
                        },
                        onOpenLink = ::openLink
                    )
                }

                if (showSosDialog) {
                    SosConfirmDialog(
                        isSending = panicState.isSending,
                        onDismiss = { showSosDialog = false },
                        onConfirm = { severity ->
                            showSosDialog = false
                            kirimSos(severity)
                        }
                    )
                }

                if (showReportSheet) {
                    DisasterReportSheet(
                        uiState = reportState,
                        onDismiss = {
                            showReportSheet = false
                            disasterReportViewModel.clearMessages()
                        },
                        onSubmit = { potensi, deskripsi, lokasi, lampiran ->
                            val loc = LokasiProvider.lastKnown(this@MainActivity)
                            disasterReportViewModel.submit(
                                token = UserPreference(this@MainActivity).getToken().orEmpty(),
                                potensiBencana = potensi,
                                deskripsi = deskripsi,
                                lokasi = lokasi,
                                latitude = loc?.latitude,
                                longitude = loc?.longitude,
                                lampiran = lampiran
                            )
                        }
                    )
                }
            }
        }

        // Jaga tab terpilih tetap sinkron saat fragment berubah (mis. dari deep link).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.composeBottomNav.visibility =
                if (destination.id == R.id.navigation_pesan_darurat) View.GONE else View.VISIBLE

            selectedTab = when (destination.id) {
                R.id.navigation_beranda -> NavTab.BERANDA
                R.id.navigation_tiket -> NavTab.TIKET
                R.id.navigation_jejak -> NavTab.CHECK_POINT
                R.id.navigation_profile -> NavTab.AKUN
                else -> selectedTab
            }
        }

        handleOAuthDeepLink(intent)

        // CEK JIKA KEMBALI DARI PROFILE DATA → BUKA PROFILE FRAGMENT
        if (intent.getBooleanExtra("from_biodata", false)) {
            val bundle = Bundle().apply {
                putBoolean("from_biodata", true)
                putString("nama_user", intent.getStringExtra("nama_user") ?: "")
            }
            selectedTab = NavTab.AKUN
            navController.navigateToTab(R.id.navigation_profile, bundle)
        }

        when (intent.getStringExtra("open_fragment")) {
            "profile" -> {
                selectedTab = NavTab.AKUN
                navController.navigateToTab(R.id.navigation_profile)
            }
        }
    }

    // ================= navigasi tab =================

    /**
     * Navigasi pola bottom-nav:
     * - launchSingleTop: pakai instance yang sudah ada, jangan buat Fragment baru;
     * - popUpTo(start): rapikan back stack (tidak menumpuk);
     * - saveState/restoreState: simpan & pulihkan state tiap tab (Fragment + ViewModel),
     *   supaya state seperti peringatan dini tidak hilang saat berpindah tab.
     */
    private fun NavController.navigateToTab(destinationId: Int, args: Bundle? = null) {
        navigate(
            destinationId,
            args,
            NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(graph.startDestinationId, inclusive = false, saveState = true)
                .setRestoreState(true)
                .build()
        )
    }

    // ================= SOS panic button =================

    private fun kirimSos(severity: String) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            doSendSos(severity)
        } else {
            pendingSosSeverity = severity
            sosPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // SOS: cari lokasi BARU dulu (batas 8 detik). Kalau GPS mati atau sinyal tak dapat,
    // cariSekarang() sendiri memakai catatan lokasi terakhir yang masih segar.
    private fun doSendSos(severity: String) {
        Toast.makeText(this, "Mencari sinyal lokasi…", Toast.LENGTH_SHORT).show()
        LokasiProvider.cariSekarang(this, timeoutMs = 8_000L) { lokasi ->
            if (isFinishing || isDestroyed) return@cariSekarang
            if (lokasi == null) {
                Toast.makeText(this, "Lokasi belum tersedia. Nyalakan GPS lalu coba lagi.", Toast.LENGTH_LONG).show()
                return@cariSekarang
            }
            panicButtonViewModel.sendSos(
                token = UserPreference(this).getToken().orEmpty(),
                latitude = lokasi.latitude,
                longitude = lokasi.longitude,
                severity = severity,
                message = null
            )
        }
    }

    private fun openLink(link: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
            .onFailure { Toast.makeText(this, "Tidak bisa membuka tautan", Toast.LENGTH_SHORT).show() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthDeepLink(intent)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun handleOAuthDeepLink(intent: Intent) {
        val data = intent.data ?: return

        Log.d("OAUTH_DEBUG", "Deep link diterima: $data")

        if (data.scheme == "gunungkerinci" && data.host == "oauth") {

            val token = data.getQueryParameter("token")
            val isNewUser = data.getQueryParameter("is_new_user")

            if (!token.isNullOrEmpty()) {
                val pref = getSharedPreferences("auth", MODE_PRIVATE)
                pref.edit().putString("token", token).apply()

                val navController = findNavController(R.id.nav_host_fragment)

                if (isNewUser == "true") {
                    selectedTab = NavTab.AKUN
                    navController.navigateToTab(R.id.navigation_profile)
                } else {
                    selectedTab = NavTab.BERANDA
                    navController.navigateToTab(R.id.navigation_beranda)
                }
            }
        }
    }
}
