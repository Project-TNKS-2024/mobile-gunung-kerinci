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

        getProfile()


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

        binding.layanan.setOnClickListener {

            val nomorAdmin = "6285162839410"

            val pesan = "Halo admin, saya ingin menghubungi layanan bantuan pengguna aplikasi e-ticket Gunung Kerinci."

            val url = "https://wa.me/$nomorAdmin?text=${java.net.URLEncoder.encode(pesan, "UTF-8")}"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)

            startActivity(intent)
        }
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

    override fun onResume() {
        super.onResume()

        getProfile()
    }

    private fun getProfile() {
        val token = requireContext()
            .getSharedPreferences("auth", 0)
            .getString("token", null)

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(requireContext())
                    .getProfile("Bearer $token")

                if (response.isSuccessful &&
                    response.body()?.data != null) {

                    val data = response.body()!!.data

                    val fullName =
                        "${data.first_name} ${data.last_name}"

                    // UPDATE UI
                    binding.tvUserName.text = fullName

                    getPendakiIdentity(token)

                    binding.cardWarning.visibility = View.GONE
                    binding.btnIsiBiodata.visibility = View.GONE

                    /*
                    binding.tvStatusBelum.text =
                        "Akun sedang diverifikasi admin"

                    binding.tvStatusBelum.visibility = View.VISIBLE
                    */

                    // OPTIONAL → simpan ulang lokal
                    requireContext()
                        .getSharedPreferences("profile_data", 0)
                        .edit()
                        .putBoolean("is_biodata_filled", true)
                        .putString("user_name", fullName)
                        .putString("verification_status", "pending")
                        .apply()

                } else {
                    showDefaultProfile()
                }

            } catch (e: Exception) {
                showDefaultProfile()
            }
        }

    }

    private fun getPendakiIdentity(token: String) {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(requireContext())
                    .getPendakiIdentity("Bearer $token")

                if (response.isSuccessful &&
                    response.body()?.data != null) {
                    val data = response.body()!!.data

                    when (data?.status_verifikasi) {
                        "verified" -> {
                            binding.tvStatusBelum.visibility = View.GONE
                            binding.tvStatusSudah.visibility = View.VISIBLE
                            binding.tvStatusSudah.text = "ID Pendaki: ${data.id_bio}"
                            requireContext()
                                .getSharedPreferences("profile_data", 0)
                                .edit()
                                .putString(
                                    "verification_status",
                                    "verified"
                                )
                                .putString(
                                    "id_pendaki",
                                    data.id_bio
                                )
                                .apply()
                        }
                        else -> {
                            binding.tvStatusSudah.visibility = View.GONE
                            binding.tvStatusBelum.visibility = View.VISIBLE
                            binding.tvStatusBelum.text = "Akun sedang diverifikasi admin"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    //DEFAULT UI BELUM DIISI
    private fun showDefaultProfile() {
        binding.tvUserName.text = "Pengguna Baru"

        binding.btnIsiBiodata.visibility = View.VISIBLE

        binding.cardWarning.visibility = View.VISIBLE

        binding.tvStatusBelum.visibility = View.VISIBLE
        binding.tvStatusBelum.text = "Akun belum divalidasi"

        binding.tvStatusSudah.visibility = View.GONE
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
