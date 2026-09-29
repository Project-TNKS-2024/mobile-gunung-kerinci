package com.dicoding.gunungkerinci.SOS

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.repository.SosChatRepository
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.ui.panic_button.SosChatScreen
import com.dicoding.gunungkerinci.ui.panic_button.SosChatViewModel
import com.dicoding.gunungkerinci.ui.panic_button.SosChatViewModelFactory
import java.io.File

/** Layar chat darurat (dibuka dari sheet SOS → "Kirim Pesan"). */
class PesanDaruratFragment : Fragment() {

    private lateinit var viewModel: SosChatViewModel
    private var currentToken: String = ""

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handlePickedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = ApiConfig.getApiService(requireContext())
        viewModel = ViewModelProvider(
            this,
            SosChatViewModelFactory(
                sosChatRepository = SosChatRepository(api, TnksDatabase.getInstance(requireContext()).pendingChatDao()),
                sosRepository = SosRepository(api),
                syncScheduler = SyncScheduler(requireContext())
            )
        )[SosChatViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentToken = UserPreference(requireContext()).getToken().orEmpty()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(currentToken) { viewModel.start(currentToken) }

                SosChatScreen(
                    uiState = uiState,
                    onBackClick = { findNavController().popBackStack() },
                    onSendText = { viewModel.sendText(currentToken, it) },
                    onAttachImageClick = { imagePicker.launch("image/*") },
                    onRefreshClick = { viewModel.refresh(currentToken) }
                )
            }
        }
    }

    private fun handlePickedImage(uri: Uri) {
        val resolver = requireContext().contentResolver
        val name = queryDisplayName(uri) ?: "sos_${System.currentTimeMillis()}.jpg"
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        // File disimpan di filesDir (bukan cache) supaya awet sampai worker
        // offline mengirimkannya (US-12).
        val dir = File(requireContext().filesDir, "sos_pending").apply { mkdirs() }
        val file = File(dir, safeName)

        runCatching {
            resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        }.getOrNull() ?: return

        viewModel.sendImage(currentToken, file, caption = null)
    }

    private fun queryDisplayName(uri: Uri): String? =
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
}
