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

        // Default UI state (no DB). Show placeholder profile icon already in drawable.
        showDefaultProfile()

        //USERNAME DEFAULT
        binding.tvUserName.text = "Pengguna Baru"


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

        // Terima data dari MainActivity ketika kembali setelah simpan biodata
        // Kita cek argumen Intent dari activity host (MainActivity bisa set extras)
        val hostIntent = activity?.intent
        hostIntent?.let {
            val fromBiodata = it.getBooleanExtra("from_biodata", false)
            if (fromBiodata) {
                val namaUser = it.getStringExtra("nama_user") ?: ""
                applyBiodataToUI(namaUser)
                // clear the flag to avoid re-applying on future opens:
                it.removeExtra("from_biodata")
            }
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
        binding.tvStatusBelum.visibility = View.VISIBLE
        binding.tvStatusSudah.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
