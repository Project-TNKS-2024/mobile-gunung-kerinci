package com.dicoding.gunungkerinci.Laporan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.FragmentLaporanBinding

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✔ BUTTON BACK → kembali ke halaman sebelumnya
        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Floating Button → buka form laporan
        binding.floatingButton.setOnClickListener {
            val intent = Intent(requireContext(), FormLaporanActivity::class.java)
            startActivity(intent)
        }

        //Cek status laporan saat pertama kali
        cekstatusLaporan()
    }


    private fun cekstatusLaporan() {
        val pref = requireContext().getSharedPreferences("laporan_pref", Context.MODE_PRIVATE)
        val sudahPosting = pref.getBoolean("sudah_posting", false)

        if (sudahPosting){
            // ✔ Jika sudah posting → tampilkan recyclerView
            binding.layoutBelumAda.visibility = View.GONE
            binding.rvLaporan.visibility = View.VISIBLE


            // RESET kembali agar next time layout tampil lagi
            pref.edit().putBoolean("sudah_posting", false).apply()
        } else {
            // ✔ Jika belum ada laporan
            binding.layoutBelumAda.visibility = View.VISIBLE
            binding.rvLaporan.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        // Cek ulang saat kembali dari Form Laporan
        cekstatusLaporan()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}