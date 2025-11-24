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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FOTO PROFILE DEFAULT
        binding.fotoProfile.setImageResource(R.drawable.profile)

        //USERNAME DEFAULT
        binding.tvUserName.text = "Pengguna Baru"

        //CEK APA ADA DATA BARU DARI BOIDATA
        val namaBaru = activity?.intent?.getStringExtra("nama_user")
        val sudahIsi = activity?.intent?.getBooleanExtra("from_biodata", false) ?: false

        if (sudahIsi && !namaBaru.isNullOrEmpty()) {
            binding.cardWarning.visibility = View.GONE
            binding.tvStatusBelum.visibility = View.GONE

            binding.tvStatusSudah.visibility = View.VISIBLE
            binding.tvStatusSudah.text = "Akun sudah divalidasi"

            binding.tvUserName.text = namaBaru
        }

        //KLIK ISI BIODATA -> PINDAH PAGE
        binding.btnIsiBiodata.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDataPribadiActivity::class.java)
            intent.putExtra("email_user", "demo@gmail.com")  // Email sementara
            startActivity(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}