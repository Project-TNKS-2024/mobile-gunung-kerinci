package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.Ticket.Pembayaran.BuktiPembayaranActivity
import com.dicoding.gunungkerinci.databinding.FragmentTiketBinding
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class TiketFragment : Fragment() {

    private val binding get() = _binding!!
    private var _binding: FragmentTiketBinding? = null
    private val listTiket = mutableListOf<BookingDetailData>()
    private lateinit var tiketAdapter: TiketAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Menu Kiri ("Draft") dipilih default
        setMenuSelected(true)

        //Ambil daftar tiket dari API
        getMyTiket()

        tiketAdapter = TiketAdapter(listTiket) { booking ->
            val intent = Intent(
                requireContext(),
                BuktiPembayaranActivity::class.java
            )

            intent.putExtra("booking_id", booking.id)

            startActivity(intent)
        }

        binding.rvLaporan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tiketAdapter
        }

        ambilDataTiket()

        binding.tvMenuLeft.setOnClickListener {
            setMenuSelected(true)
            ambilDataTiket(draft = true)
        }

        binding.tvMenuRight.setOnClickListener {
            setMenuSelected(false)
            ambilDataTiket(draft = false)
        }

        // ➜ BUTTON PESAN TIKET
        binding.buttonPesanTiket.setOnClickListener {
            val intent = Intent(activity, PilihTiketActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getMyTiket() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {

                val response = ApiConfig.getApiService(requireContext())
                    .getMyTiket()

                Log.d("MY_TIKET_API", "HTTP Code: ${response.code()}")

                if (response.isSuccessful) {

                    val body = response.body()

                    Log.d(
                        "MY_TIKET_API",
                        "Success: ${body?.success}"
                    )

                    Log.d(
                        "MY_TIKET_API",
                        "Message: ${body?.message}"
                    )

                    Log.d(
                        "MY_TIKET_API",
                        "Jumlah booking: ${body?.data?.size ?: 0}"
                    )

                    body?.data?.forEach { booking ->

                        Log.d(
                            "MY_TIKET_API",
                            """
                        Booking ID: ${booking.id}
                        Tanggal Masuk: ${booking.tanggal_masuk}
                        Tanggal Keluar: ${booking.tanggal_keluar}
                        Gate Masuk: ${booking.gate_masuk.nama}
                        Gate Keluar: ${booking.gate_keluar.nama}
                        Status Booking: ${booking.status_booking}
                        Status Pembayaran: ${booking.status_pembayaran}
                        Total Pembayaran: ${booking.total_pembayaran}
                        Jumlah Pendaki: ${booking.pendakis.size}
                        """.trimIndent()
                        )
                    }

                } else {

                    Log.e(
                        "MY_TIKET_API",
                        "HTTP Error: ${response.code()}"
                    )

                    Log.e(
                        "MY_TIKET_API",
                        "Error Body: ${response.errorBody()?.string()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "MY_TIKET_API",
                    "Exception: ${e.message}",
                    e
                )
            }
        }
    }

    private fun ambilDataTiket(draft: Boolean = true) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(requireContext())
                    .getMyTiket()

                Log.d("MY_TIKET_FRAGMENT", "HTTP Code: ${response.code()}")

                if (response.isSuccessful) {

                    val body = response.body()

                    Log.d(
                        "MY_TIKET_FRAGMENT",
                        "Success: ${body?.success}"
                    )

                    if (body?.success == true) {

                        val semuaBooking = body.data ?: emptyList()

                        Log.d(
                            "MY_TIKET_FRAGMENT",
                            "Total booking: ${semuaBooking.size}"
                        )

                        val tiketDraft = semuaBooking.filter { booking ->
                            val statusPembayaran = booking.pembayaran
                                ?.lastOrNull()
                                ?.status
                                ?.lowercase()

                            if (draft) {
                                statusPembayaran == "pending" ||
                                        statusPembayaran == "failed"
                            } else {
                                statusPembayaran == "success"
                            }
                        }

                        Log.d(
                            "MY_TIKET_FRAGMENT",
                            if (draft) {
                                "Tiket draft: ${tiketDraft.size}"
                            } else {
                                "Tiket saya: ${tiketDraft.size}"
                            }
                        )

                        listTiket.clear()
                        listTiket.addAll(tiketDraft)
                        tiketAdapter.notifyDataSetChanged()

                        if (listTiket.isEmpty()) {
                            binding.rvLaporan.visibility = View.GONE
                            binding.layoutBelumAda.visibility = View.VISIBLE
                        } else {
                            binding.rvLaporan.visibility = View.VISIBLE
                            binding.layoutBelumAda.visibility = View.GONE
                        }

                    } else {

                        Log.e(
                            "MY_TIKET_FRAGMENT",
                            "API gagal: ${body?.message}"
                        )
                    }

                } else {

                    Log.e(
                        "MY_TIKET_FRAGMENT",
                        "HTTP Error: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "MY_TIKET_FRAGMENT",
                    "Exception mengambil tiket",
                    e
                )
            }
        }
    }

    private fun setMenuSelected(isLeftSelected: Boolean) {
        if (isLeftSelected) {
            // Draft aktif
            binding.tvMenuLeft.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.primary))
            binding.tvMenuRight.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.darkgrey))

            moveIndicatorTo(binding.tvMenuLeft)
        } else {
            // Tiket Saya aktif
            binding.tvMenuRight.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.primary))
            binding.tvMenuLeft.setTextColor(resources.getColor(com.dicoding.gunungkerinci.R.color.darkgrey))

            moveIndicatorTo(binding.tvMenuRight)
        }
    }

    private fun moveIndicatorTo(targetView: View) {
        targetView.post {
            val params = binding.viewIndicator.layoutParams
            params.width = targetView.width
            binding.viewIndicator.layoutParams = params

            binding.viewIndicator.animate()
                .x(targetView.x)
                .setDuration(150)
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}