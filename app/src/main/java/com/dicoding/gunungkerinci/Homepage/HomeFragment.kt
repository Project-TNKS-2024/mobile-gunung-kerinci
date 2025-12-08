package com.dicoding.gunungkerinci.Homepage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.Homepage.Panduan.PanduanActivity
import com.dicoding.gunungkerinci.Homepage.Pemberitahuan.PemberitahuanActivity
import com.dicoding.gunungkerinci.Homepage.Sop.SopActivity
import com.dicoding.gunungkerinci.Homepage.Wisata.WisataActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.PilihTiketActivity
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

        setupMenu()
        setupWisataList()

        return binding.root
    }

    private fun setupWisataList() {
        val listWisata = listOf(
            WisataItem(
                "Danau Gunung Tujuh",
                "Danau tertinggi di Asia Tenggara, destinasi favorit pendaki.",
                "Danau Gunung Tujuh adalah danau vulkanik di ketinggian 1.950 mdpl yang dikelilingi tujuh puncak gunung. Panorama alamnya sangat indah dan menjadi favorit pendaki dari berbagai daerah.",
                R.drawable.splash4
            ),
            WisataItem(
                "Gunung Kerinci",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia dengan ketinggian 3.805 meter di atas permukaan laut. Terletak di Provinsi Jambi, gunung ini menjadi bagian dari Taman Nasional Kerinci Seblat.",
                R.drawable.splash1
            )
        )

        val adapter = WisataAdapter(listWisata)

        binding.recyclerViewWisata.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerViewWisata.adapter = adapter
    }

    private fun setupMenu() {
        binding.cardSOP.setOnClickListener {
            startActivity(Intent(requireContext(), SopActivity::class.java))
        }

        binding.cardPanduan.setOnClickListener {
            startActivity(Intent(requireContext(), PanduanActivity::class.java))
        }

        binding.textViewSelengkapnya.setOnClickListener {
            startActivity(Intent(requireContext(), WisataActivity::class.java))
        }

        // 👉 BUTTON PESAN TIKET
        binding.buttonPesanTiket.setOnClickListener {
            startActivity(Intent(requireContext(), PilihTiketActivity::class.java))
        }

        binding.buttonNotif.setOnClickListener {
            startActivity(Intent(requireContext(), PemberitahuanActivity::class.java))
        }

    }
}