package com.dicoding.gunungkerinci.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.Login.LoginActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.FragmentProfileBinding
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // nothing here for now
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FOTO PROFIL DEFAULT
        binding.fotoProfile.setImageResource(R.drawable.akundefault)

        cekBiodataStatus()


        //KLIK ISI BIODATA -> PINDAH PAGE
        binding.btnIsiBiodata.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDataPribadiActivity::class.java)
            startActivity(intent)
        }

        //KLIK DATA PRIBADI
        binding.dataPribadi.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDataPribadiActivity::class.java)
            startActivity(intent)
        }

        //KLIK KATA SANDI
        binding.kataSandi.setOnClickListener {
            val intent = Intent(requireContext(), ProfileKataSandiActivity::class.java)
            startActivity(intent)
        }

        //KLIK TENTANG APLIKASI
        binding.tentangApk.setOnClickListener {
            val intent = Intent(requireContext(), ProfileAboutAppActivity::class.java)
            startActivity(intent)
        }
/*
        //KLIK PENGATURAN BAHASA
        binding.bahasa.setOnClickListener {
            val intent = Intent(requireContext(), ProfileLanguageActivity::class.java)
            startActivity(intent)
        }

 */
        // Tombol Logout (langsung kembali ke LoginActivity)
        binding.textLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ ->
                    doLogout()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

    }

    private fun cekBiodataStatus() {
        val pref = requireContext()
            .getSharedPreferences("profile_data", 0)

        val isFilled = pref.getBoolean("is_biodata_filled", false)

        val userName = pref.getString("user_name", "USER") ?: "USER"

        val verificationStatus =
            pref.getString("verification_status", "none")

        if (isFilled) {

            // TAMPILKAN DATA USER
            binding.tvUserName.text = userName

            // HILANGKAN WARNING
            binding.cardWarning.visibility = View.GONE

            // HILANGKAN BUTTON ISI BIODATA
            binding.btnIsiBiodata.visibility = View.GONE

            // STATUS VERIFIKASI
            if (verificationStatus == "pending") {

                binding.tvStatusBelum.text =
                    "Akun sedang diverifikasi oleh admin"

                binding.tvStatusBelum.visibility = View.VISIBLE
            }

        } else {

            // DEFAULT
            showDefaultProfile()
        }
    }

    private fun doLogout() {
        val pref = requireContext().getSharedPreferences("auth", 0)
        val token = pref.getString("token", null)

        if (token.isNullOrEmpty()) {
            goToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                ApiConfig.getApiService(requireContext())
                    .logout("Bearer $token")
                // Apapun respon server → logout lokal
                clearSession()
                goToLogin()
            } catch (e: Exception) {
                // Kalau API error → tetap logout lokal
                clearSession()
                goToLogin()
            }
        }
    }

    private fun clearSession() {
        val pref = requireContext().getSharedPreferences("auth", 0)
        pref.edit()
            .clear()
            .apply()
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    //UI SETELAH BIODATA DIISI
    private fun applyBiodataToUI(namaLengkap: String) {
        // Update username / hide warning, tampilkan status sudah
        if (namaLengkap.isNotBlank()) {

            //Nama berubah
            binding.tvUserName.text = namaLengkap

            //Sembunyikan warning
            binding.cardWarning.visibility = View.GONE

            //Update status
            binding.tvStatusBelum.visibility = View.GONE
            binding.tvStatusSudah.visibility = View.VISIBLE

            //Sembunyikan button isi biodata
            binding.btnIsiBiodata.visibility = View.GONE
        }
    }

    //DEFAULT UI BELUM DIISI
    private fun showDefaultProfile() {
        binding.tvUserName.text = "Pengguna Baru"

        binding.btnIsiBiodata.visibility = View.VISIBLE

        binding.cardWarning.visibility = View.VISIBLE

        binding.tvStatusBelum.visibility = View.VISIBLE
        binding.tvStatusSudah.visibility = View.GONE
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
