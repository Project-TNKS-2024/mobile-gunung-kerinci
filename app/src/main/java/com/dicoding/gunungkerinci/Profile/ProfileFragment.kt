package com.dicoding.gunungkerinci.Profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dicoding.gunungkerinci.Login.LoginActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

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
        binding.fotoProfile.setImageResource(R.drawable.ic_profile)

        // Default UI state (no DB). Show placeholder profile icon already in drawable.
        showDefaultProfile()

        //USERNAME DEFAULT
        binding.tvUserName.text = "Pengguna Baru"

        //KLIK ISI BIODATA -> PINDAH PAGE
        binding.btnIsiBiodata.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDataPribadiActivity::class.java)
            intent.putExtra("email_user", "demo@gmail.com")  // Email sementara
            startActivity(intent)
        }

        //KLIK DATA PRIBADI
        binding.dataPribadi.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDataPribadiActivity::class.java)
            intent.putExtra("email_user", "demo@gmail.com")
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

        //KLIK PENGATURAN BAHASA
        binding.bahasa.setOnClickListener {
            val intent = Intent(requireContext(), ProfileLanguageActivity::class.java)
            startActivity(intent)
        }

        // Tombol Logout (langsung kembali ke LoginActivity)
        binding.textLogout.setOnClickListener {
            // Untuk demo: langsung ke LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
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