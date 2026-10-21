package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.Barcode.BarcodeTiketActivity
import com.dicoding.gunungkerinci.Ticket.SOP.TiketSOPActivity
import com.dicoding.gunungkerinci.databinding.FragmentTiketBinding
import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TiketFragment : Fragment() {

    private var _binding: FragmentTiketBinding? = null
    private val binding get() = _binding!!

    private var allBookings: List<MyTiketItem> = emptyList()
    private var myUserId: Int? = null
    private var isDraftTab = true
    private var loaded = false
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTiketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pulihkan tab terpilih saat rotasi; default tetap Draft.
        isDraftTab = savedInstanceState?.getBoolean(KEY_DRAFT_TAB, true) ?: true
        setMenuSelected(isDraftTab)

        binding.tvMenuLeft.setOnClickListener {
            isDraftTab = true
            setMenuSelected(true)
            if (loaded) render()
        }

        binding.tvMenuRight.setOnClickListener {
            isDraftTab = false
            setMenuSelected(false)
            if (loaded) render()
        }

        binding.buttonPesanTiket.setOnClickListener {
            startActivity(Intent(requireContext(), PilihTiketActivity::class.java))
        }

        binding.buttonBack.setOnClickListener {
            findNavController().navigate(
                R.id.navigation_beranda, null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.navigation_beranda, false)
                    .build()
            )
        }

        binding.btnRetry.setOnClickListener { loadData() }
        binding.btnRefreshRetry.setOnClickListener { loadData() }
        binding.floatingButton.setOnClickListener { startActivity(Intent(requireContext(), PilihTiketActivity::class.java)) }
        if (loaded) render()

        binding.rvTiket.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        // Refresh agar status terbaru admin (checkout/pembayaran) tampil.
        if (_binding != null) loadData()
    }

    private fun loadData() {
        val token = UserPreference(requireContext()).getToken().orEmpty()
        if (token.isBlank()) {
            showError("Sesi berakhir. Silakan login kembali.")
            return
        }

        if (loadJob?.isActive == true) return
        showLoading()

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val api = ApiConfig.getApiService(requireContext())
            val bearer = "Bearer $token"
            try {
                val identity = async { runCatching { api.getPendakiIdentity(bearer) } }
                val list = async { runCatching { api.getMyTiket(bearer) } }

                val idResult = identity.await()
                val listResult = list.await()

                if (!isAdded || _binding == null) return@launch

                myUserId = idResult.getOrNull()?.body()?.data?.id_user

                val listBody = listResult.getOrNull()?.body()
                if (listResult.isFailure || listBody == null) {
                    showError("Gagal memuat tiket. Coba lagi.")
                    return@launch
                }
                if (listBody.success != true) {
                    showError(listBody.message ?: "Gagal memuat tiket.")
                    return@launch
                }

                allBookings = listBody.data.orEmpty()
                loaded = true
                render()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isAdded && _binding != null) showError("Gagal memuat tiket. Coba lagi.")
            }
        }
    }

    private fun render() {
        val filtered = if (isDraftTab) {
            allBookings.filter { TiketStatus.isDraft(it.statusBooking) }
        } else {
            allBookings.filter { TiketStatus.isTicket(it.statusBooking) }
        }

        binding.layoutRefresh.visibility = View.GONE
        if (filtered.isEmpty()) {
            showEmpty()
            return
        }

        binding.progressLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.layoutBelumAda.visibility = View.GONE
        binding.buttonPesanTiket.visibility = View.GONE
        binding.rvTiket.visibility = View.VISIBLE
        binding.floatingButton.visibility = View.VISIBLE

        binding.rvTiket.adapter = TiketListAdapter(filtered) { item ->
            if (item.statusBooking == 9 || item.canContinue == false && TiketStatus.isDraft(item.statusBooking)) {
                AlertDialog.Builder(requireContext())
                    .setTitle(TiketStatus.cardLabel(item))
                    .setMessage(item.ticketNotice ?: item.cancellationReason ?: "Pesanan ini tidak dapat dilanjutkan.")
                    .setPositiveButton("Tutup", null)
                    .show()
            } else if (TiketStatus.isDraft(item.statusBooking)) continueDraft(item) else openBarcode(item.id)
        }
    }

    private fun continueDraft(item: MyTiketItem) {
        if (myUserId == null || item.idUser != myUserId) {
            Toast.makeText(
                requireContext(),
                "Hanya pemilik booking yang dapat melanjutkan draft ini.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val totalPendaki = (item.totalPendakiWni ?: 0) + (item.totalPendakiWna ?: 0)
        when (item.statusBooking) {
            0, 1 -> startActivity(
                Intent(requireContext(), TiketSOPActivity::class.java)
                    .putExtra("booking_id", item.id)
                    .putExtra("total_pendaki", totalPendaki)
            )

            2 -> startActivity(
                Intent(requireContext(), TiketDataPendakiActivity::class.java)
                    .putExtra("booking_id", item.id)
                    .putExtra("total_pendaki", totalPendaki)
            )

            3 -> startActivity(
                Intent(requireContext(), RincianPemesananTiketActivity::class.java)
                    .putExtra("booking_id", item.id)
            )
        }
    }

    private fun openBarcode(bookingId: String) {
        startActivity(
            Intent(requireContext(), BarcodeTiketActivity::class.java)
                .putExtra("booking_id", bookingId)
        )
    }

    private fun showLoading() {
        if (loaded) {
            render()
            binding.layoutRefresh.visibility = View.VISIBLE
            binding.progressRefresh.visibility = View.VISIBLE
            binding.tvRefresh.text = "Memperbarui…"
            binding.btnRefreshRetry.visibility = View.GONE
            return
        }
        binding.layoutRefresh.visibility = View.GONE
        binding.floatingButton.visibility = View.GONE
        binding.progressLoading.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.layoutBelumAda.visibility = View.GONE
        binding.buttonPesanTiket.visibility = View.GONE
        binding.rvTiket.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.floatingButton.visibility = View.GONE
        binding.progressLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.rvTiket.visibility = View.GONE
        binding.layoutBelumAda.visibility = View.VISIBLE
        binding.buttonPesanTiket.visibility = View.VISIBLE
        binding.tvEmptyText.text = if (isDraftTab) {
            "Anda Belum Membuat Pesanan\nPesan Tiket Anda Sekarang!"
        } else {
            "Anda Belum Memiliki Tiket\nPesan Tiket Anda Sekarang!"
        }
    }

    private fun showError(message: String) {
        if (loaded) {
            render()
            binding.layoutRefresh.visibility = View.VISIBLE
            binding.progressRefresh.visibility = View.GONE
            binding.tvRefresh.text = "$message Data sebelumnya tetap ditampilkan."
            binding.btnRefreshRetry.visibility = View.VISIBLE
            return
        }
        binding.layoutRefresh.visibility = View.GONE
        binding.floatingButton.visibility = View.GONE
        binding.progressLoading.visibility = View.GONE
        binding.layoutBelumAda.visibility = View.GONE
        binding.buttonPesanTiket.visibility = View.GONE
        binding.rvTiket.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun setMenuSelected(isLeftSelected: Boolean) {
        if (isLeftSelected) {
            binding.tvMenuLeft.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.primary))
            binding.tvMenuRight.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.darkgrey))
            moveIndicatorTo(binding.tvMenuLeft)
        } else {
            binding.tvMenuRight.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.primary))
            binding.tvMenuLeft.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.darkgrey))
            moveIndicatorTo(binding.tvMenuRight)
        }
    }

    private fun moveIndicatorTo(targetView: View) {
        val currentBinding = _binding ?: return
        targetView.post {
            if (_binding !== currentBinding) return@post
            val params = binding.viewIndicator.layoutParams
            params.width = targetView.width
            binding.viewIndicator.layoutParams = params

            binding.viewIndicator.animate()
                .x(targetView.x)
                .setDuration(150)
                .start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DRAFT_TAB, isDraftTab)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        loadJob = null
        _binding = null
    }

    private companion object {
        const val KEY_DRAFT_TAB = "is_draft_tab"
    }
}
