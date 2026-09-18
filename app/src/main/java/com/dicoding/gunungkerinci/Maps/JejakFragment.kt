package com.dicoding.gunungkerinci.Maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.dicoding.gunungkerinci.data.repository.ActiveHikeRepository 
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.data.repository.CheckpointQrRepository
import com.dicoding.gunungkerinci.data.repository.GpsCheckInRepository
import com.dicoding.gunungkerinci.data.repository.ManualCheckInRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.data.repository.TrackingProgressRepository
import com.dicoding.gunungkerinci.data.repository.TrackingRepository
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.PelacakanJejakScreen
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.PelacakanJejakViewModel
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.PelacakanJejakViewModelFactory
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class JejakFragment : Fragment() {

    private lateinit var viewModel: PelacakanJejakViewModel

    // QR scanner launcher
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@registerForActivityResult
        val loc = getLastKnownLocation()
        if (loc == null) {
            // Sesuai aturan: check-in hanya sah bila GPS aktif
            Toast.makeText(
                requireContext(),
                "Aktifkan GPS untuk check-in",
                Toast.LENGTH_LONG
            ).show()
        } else {
            viewModel.onQrScanned(
                rawValue = raw,
                latitude = loc.latitude,
                longitude = loc.longitude,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                batteryLevel = bateraiPersen()
            )
        }
    }

    // Permission launcher — dipakai saat user klik tombol GPS check-in
    private var pendingGpsAction: (() -> Unit)? = null
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            pendingGpsAction?.invoke()
        } else {
            Toast.makeText(
                requireContext(),
                "Izin lokasi diperlukan untuk GPS check-in",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingGpsAction = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val api = ApiConfig.getApiService(requireContext())
        val database = TnksDatabase.getInstance(requireContext())
        val activeHikeRepository = ActiveHikeRepository(api)
        viewModel = ViewModelProvider(
            this,
            PelacakanJejakViewModelFactory(
                activeHikeRepository = activeHikeRepository,
                trackingRepository = TrackingRepository(
                    api = api,
                    trackingPostDao = database.trackingPostDao(),
                    checkpointLogDao = database.checkpointLogDao()
                ),
                checkpointQrRepository = CheckpointQrRepository(api),
                trackingProgressRepository = TrackingProgressRepository(api),
                gpsCheckInRepository = GpsCheckInRepository(api),
                manualCheckInRepository = ManualCheckInRepository(api),
                trackingGpsRepository = TrackingGpsRepository(api),
                syncScheduler = SyncScheduler(requireContext())
            )
        )[PelacakanJejakViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                // LaunchedEffect — satu kali per perubahan key, bukan setiap recompose.
                LaunchedEffect(uiState.qrScanMessage) {
                    val msg = uiState.qrScanMessage ?: return@LaunchedEffect
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.clearQrMessage()
                }
                LaunchedEffect(uiState.qrScanErrorMessage) {
                    val err = uiState.qrScanErrorMessage ?: return@LaunchedEffect
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    viewModel.clearQrMessage()
                }
                LaunchedEffect(uiState.manualCheckInMessage) {
                    val msg = uiState.manualCheckInMessage ?: return@LaunchedEffect
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.clearGpsAndManualMessages()
                }
                LaunchedEffect(uiState.manualCheckInErrorMessage) {
                    val err = uiState.manualCheckInErrorMessage ?: return@LaunchedEffect
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    viewModel.clearGpsAndManualMessages()
                }
                LaunchedEffect(uiState.gpsCheckErrorMessage) {
                    val err = uiState.gpsCheckErrorMessage ?: return@LaunchedEffect
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    viewModel.clearGpsAndManualMessages()
                }

                PelacakanJejakScreen(
                    uiState = uiState,
                    onRetryLoadPosts = { loadActiveHike() },
                    onScanQrClick = { launchQrScanner() },
                    onGpsCheckInClick = { launchGpsCheckIn() },
                    onManualCheckInConfirm = { postId ->
                        val loc = getLastKnownLocation()
                        if (loc == null) {
                            viewModel.onLocationUnavailable()
                        } else {
                            viewModel.checkInManual(
                                postId = postId,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = if (loc.hasAltitude()) loc.altitude else null,
                                accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                                batteryLevel = bateraiPersen()
                            )
                        }
                    },
                    // Tutup dialog konfirmasi GPS tanpa check-in.
                    onDismissGpsDialog = { viewModel.clearGpsAndManualMessages() },
                    // Auto-deteksi radius begitu layar detail pos dibuka (aturan dosen).
                    onLocationDetailOpened = { launchGpsCheckIn(silent = true) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadActiveHike()
    }

    /**
     * Titik masuk layar Jejak: minta ViewModel mengambil daftar booking (GET /mytiket),
     * lalu pakai untuk kartu tiket + pendakian aktif. Bukan prefs/hardcode.
     */
    private fun loadActiveHike() {
        val token = UserPreference(requireContext()).getToken().orEmpty()
        viewModel.start(token)
    }

    private fun launchQrScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan QR checkpoint pendakian")
            .setCameraId(0)
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
            .setOrientationLocked(true)
            .setCaptureActivity(PortraitCaptureActivity::class.java)
        qrScanLauncher.launch(options)
    }

    /**
     * Minta permission lokasi jika belum ada, lalu cek GPS radius pos terdekat.
     *
     * @param silent true untuk auto-deteksi saat layar pos dibuka — status gate
     *   tetap diperbarui, tapi alasan gagal tidak muncul sebagai toast berulang.
     */
    private fun launchGpsCheckIn(silent: Boolean = false) {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val action = {
            val loc = getLastKnownLocation()
            if (loc == null) {
                viewModel.onLocationUnavailable(silent = silent)
            } else {
                viewModel.checkNearbyPostGps(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy.toDouble(),
                    silent = silent
                )
            }
        }

        if (hasFine || hasCoarse) {
            action()
        } else if (silent) {
            // Auto-deteksi tidak boleh memaksa dialog izin; cukup kunci tombol.
            viewModel.onLocationUnavailable(silent = true)
        } else {
            pendingGpsAction = action
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Persen baterai saat ini (0..100) — dikirim bersama titik posisi ke gk_tracking
     * supaya dashboard petugas bisa menampilkan sisa daya pendaki.
     *
     * Membaca sticky broadcast ACTION_BATTERY_CHANGED: tanpa permission, tanpa polling,
     * tidak menambah pemakaian baterai. Gagal baca → null; posisi & check-in tetap jalan.
     */
    private fun bateraiPersen(): Int? {
        val intent = requireContext()
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level < 0 || scale <= 0) null else level * 100 / scale
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val lm = requireContext().getSystemService<LocationManager>() ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
