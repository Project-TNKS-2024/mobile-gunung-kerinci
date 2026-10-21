package com.dicoding.gunungkerinci.SOS

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.dicoding.gunungkerinci.ui.panic_button.simpanGambarKecil
import java.io.File
import kotlinx.coroutines.launch

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.pollMessages(currentToken)
            }
        }
    }

    private fun handlePickedImage(uri: Uri) {
        val context = requireContext().applicationContext
        viewModel.sendImage(currentToken, prepareImage = {
            val dir = File(context.filesDir, "sos_pending").apply { mkdirs() }
            val file = File(dir, "sos_${java.util.UUID.randomUUID()}.jpg")
            try {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> simpanGambarKecil(input, output) }
                } ?: false
                check(copied) { "Gagal menyiapkan foto. Pilih foto dan coba lagi." }
                file
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        })
    }

}
