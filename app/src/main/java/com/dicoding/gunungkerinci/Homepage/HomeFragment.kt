package com.dicoding.gunungkerinci.Homepage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dicoding.gunungkerinci.Homepage.Panduan.PanduanActivity
import com.dicoding.gunungkerinci.Homepage.Sop.SopActivity
import com.dicoding.gunungkerinci.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.apply {
            cardSOP.setOnClickListener {
                val intent = Intent(requireContext(), SopActivity::class.java)
                startActivity(intent)
            }

            cardPanduan.setOnClickListener {
                val intent = Intent(requireContext(), PanduanActivity::class.java)
                startActivity(intent)
            }

            textViewSelengkapnya.setOnClickListener {

            }
        }
        return binding.root
    }
}