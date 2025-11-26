package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dicoding.gunungkerinci.databinding.FragmentTiketBinding

class TiketFragment : Fragment() {

    private val binding get() = _binding!!
    private var _binding: FragmentTiketBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Menu Kiri ("Draft") dipilih default
        setMenuSelected(true)

        binding.tvMenuLeft.setOnClickListener {
            setMenuSelected(true)
        }

        binding.tvMenuRight.setOnClickListener {
            setMenuSelected(false)
        }

        // ➜ BUTTON PESAN TIKET
        binding.buttonPesanTiket.setOnClickListener {
            val intent = Intent(activity, CekKuotaTiketActivity::class.java)
            startActivity(intent)
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