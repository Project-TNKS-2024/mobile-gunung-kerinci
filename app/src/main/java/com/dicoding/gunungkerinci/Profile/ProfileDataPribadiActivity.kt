package com.dicoding.gunungkerinci.Profile

import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.databinding.ActivityProfileDataPribadiBinding
import java.util.Calendar

class ProfileDataPribadiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileDataPribadiBinding

    private val listNegara = listOf(
        "Indonesia", "Malaysia", "Singapura", "Brunei", "Thailand",
        "Vietnam", "Laos", "Myanmar", "Filipina", "Timor Leste",
        "Jepang", "Korea Selatan", "China", "India", "Australia"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDataPribadiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //PRE-FILL EMAIL
        val emailUser = intent.getStringExtra("email_user")
        binding.emailEditText.setText(emailUser)

        //DROPDOWN KEWARGANEGARAAN
        val adapterNegara = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            listNegara
        )
        binding.wargaEditText.setAdapter(adapterNegara)
        binding.wargaEditText.setOnClickListener {
            binding.wargaEditText.showDropDown()
        }

        //DATA PICKER TANGGAL LAHIR
        binding.lahirEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    binding.lahirEditText.setText("$d-${m + 1}-$y")
                },
                year, month, day
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        //PILIH FILE LAMPIRAN
        binding.btnPilihFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, 101)
        }

        //SIMPAN BIODATA
        binding.btnSimpan.setOnClickListener {
            val namaLengkap =
                binding.namdepEditText.text.toString() + " " +
                        binding.nambelEditText.text.toString()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("from_biodata", true)
            intent.putExtra("nama_user", namaLengkap)
            startActivity(intent)
            finish()
        }

        binding.btnSimpan.setOnClickListener {

            // Validasi nomor identitas
            val noIdentitas = binding.identitasEditText.text.toString().trim()

            if (noIdentitas.isEmpty()) {
                binding.identitasEditText.error = "Nomor identitas harus diisi"
                binding.identitasEditText.requestFocus()
                return@setOnClickListener
            }

            if (noIdentitas.length < 12) {
                binding.identitasEditText.error = "Nomor identitas minimal 12 digit"
                binding.identitasEditText.requestFocus()
                return@setOnClickListener
            }

            if (noIdentitas.length > 16) {
                binding.identitasEditText.error = "Nomor identitas maksimal 16 digit"
                binding.identitasEditText.requestFocus()
                return@setOnClickListener
            }

            // Jika lolos semua validasi
            Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()

            finish()
        }

    }

    //Ambil Nama File
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.moveToFirst()

            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val fileName = cursor?.getString(nameIndex!!)

            binding.tvNamaFile.text = fileName ?: "Tidak dapat membaca file"

            cursor?.close()
        }
    }
}