package com.dicoding.gunungkerinci.Ticket

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityTiketDataPendakiBinding
import com.dicoding.gunungkerinci.databinding.FragmentHomeBinding


class TiketDataPendakiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTiketDataPendakiBinding

    private lateinit var itemView: android.view.View

    private val formLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selesai = result.data?.getBooleanExtra("pendaki_selesai", false) ?: false
            if (selesai) {
                updatePendakiSelesai()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol back di header
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tambahkan 1 item pendaki default saat halaman dibuka
        tambahPendakiDefault()

        // Tombol Batalkan → tampil popup
        binding.btnBatalkan.setOnClickListener {
            showPopupBatalkan()
        }

        // Tombol Simpan Draft → tampil popup draft
        binding.btnSimpanDraft.setOnClickListener {
            showPopupDraft()
        }

        binding.btnSelanjutnya.setOnClickListener {
            // Cek apakah checkbox sudah dicentang
            if (!binding.checkBoxPersetujuan.isChecked) {
                Toast.makeText(
                    this,
                    "Harap setujui barang bawaan wajib terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Jika sudah dicentang → menuju halaman detail data pendakian
            val intent = Intent(this, RincianPembayaranTiketActivity::class.java)
            startActivity(intent)
        }

    }

    //Popup draft
    private fun showPopupDraft() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_draft)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalDraft)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinDraft)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Draft pemesanan tiket berhasil disimpan", Toast.LENGTH_SHORT).show()

            // kembali ke home
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    //Popup batalkan
    private fun showPopupBatalkan() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_pemesanan)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalPemesanan)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinPemesanan)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Pemesanan tiket dibatalkan", Toast.LENGTH_SHORT).show()

            // kembali ke home fragment di main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun tambahPendakiDefault() {
        // Inflate layout item_pendaki.xml ke dalam containerPendaki
        itemView = layoutInflater.inflate(
            R.layout.item_pendaki,
            binding.containerPendaki,
            false
        )

        // Ambil view di dalam item
        val tvNamaPendaki = itemView.findViewById<TextView>(R.id.tvNamaPendaki)
        val tvIdPendaki = itemView.findViewById<TextView>(R.id.tvIdPendaki)
        val btnIsiData = itemView.findViewById<Button>(R.id.btnIsiData)
        val rbPendaki = itemView.findViewById<RadioButton>(R.id.rbPendaki)
        val btnUbahData = itemView.findViewById<Button>(R.id.btnUbahData)


        // Set nilai default (untuk demo)
        tvNamaPendaki.text = "Pendaki 1"
        tvIdPendaki.text = "F12345678"
        rbPendaki.visibility = android.view.View.GONE
        btnUbahData.visibility = android.view.View.GONE


        // Aksi tombol "Isi Data" (sementara hanya toast, nanti bisa diarahkan ke form detail)
        btnIsiData.setOnClickListener {
            val intent = Intent(this, TiketFormDataPendakiActivity::class.java)
            // Jika kamu ingin kirim nama/id pendaki:
            intent.putExtra("nama_pendaki", tvNamaPendaki.text.toString())
            intent.putExtra("id_pendaki", tvIdPendaki.text.toString())
            formLauncher.launch(intent)
        }

        // Masukkan view ke container
        binding.containerPendaki.addView(itemView)
    }

    private fun updatePendakiSelesai() {
        val rbPendaki = itemView.findViewById<RadioButton>(R.id.rbPendaki)
        val btnIsiData = itemView.findViewById<Button>(R.id.btnIsiData)
        val btnUbahData = itemView.findViewById<Button>(R.id.btnUbahData)

        rbPendaki.visibility = android.view.View.VISIBLE
        btnIsiData.visibility = android.view.View.GONE
        btnUbahData.visibility = android.view.View.VISIBLE

        // Klik ubah data kembali ke form
        btnUbahData.setOnClickListener {
            val intent = Intent(this, TiketFormDataPendakiActivity::class.java)
            formLauncher.launch(intent)
        }
    }
}