package com.dicoding.gunungkerinci.Profile

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityProfileDataPribadiBinding
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class ProfileDataPribadiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileDataPribadiBinding

    private val listNegara = listOf(
        "Indonesia", "Malaysia", "Singapura", "Brunei", "Thailand",
        "Vietnam", "Laos", "Myanmar", "Filipina", "Timor Leste",
        "Jepang", "Korea Selatan", "China", "India", "Australia"
    )

    private val REQ_PICK_FILE = 101
    private val REQ_PICK_GALLERY = 102
    private val REQ_TAKE_PHOTO = 103
    private var currentPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDataPribadiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SET FOTO PROFIL DEFAULT SAAT HALAMAN DIBUKA
        binding.fotoProfile.setImageResource(R.drawable.ic_profile)

        //PRE-FILL EMAIL
        val emailUser = intent.getStringExtra("email_user")
        binding.emailEditText.setText(emailUser ?: "")

        // Pastikan input bisa diketik (XML default mungkin focusable=false)
        enableEditingFields()

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
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // format dd-MM-yyyy
                    val formatted = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year)
                    binding.lahirEditText.setText(formatted)
                },
                y, m, d)

            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // PILIH FILE LAMPIRAN (file manager)
        binding.layoutPilihFile.setOnClickListener {
            pickFile()
        }
        // juga klik child tv btn_pilih_file
        binding.btnPilihFile.setOnClickListener {
            pickFile()
        }

        /*
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
         */

        // PILIH FOTO PROFILE: klik imageview atau tombol ubah
        binding.fotoProfile.setOnClickListener { showImagePickDialog() }
        binding.buttonUbah.setOnClickListener { showImagePickDialog() }

        // SIMPAN BIODATA
        binding.btnSimpan.setOnClickListener {
            saveBiodata()
        }

    }

    private fun saveBiodata() {

        val depan = binding.namdepEditText.text.toString().trim()
        val belakang = binding.nambelEditText.text.toString().trim()
        val negara = binding.wargaEditText.text.toString().trim()
        val alamat = binding.alamatEditText.text.toString().trim()
        val genderSelected = binding.radioGroupGender.checkedRadioButtonId
        val tgl = binding.lahirEditText.text.toString().trim()
        val nik = binding.identitasEditText.text.toString().trim()
        val telp = binding.inputTelepon.text.toString().trim()

        // --- VALIDASI WAJIB ISI ---
        when {
            depan.isEmpty() -> {
                binding.namdepEditText.error = "Nama depan wajib diisi"
                binding.namdepEditText.requestFocus()
                return
            }
            belakang.isEmpty() -> {
                binding.nambelEditText.error = "Nama belakang wajib diisi"
                binding.nambelEditText.requestFocus()
                return
            }
            negara.isEmpty() -> {
                binding.wargaEditText.error = "Pilih kewarganegaraan"
                binding.wargaEditText.requestFocus()
                return
            }
            alamat.isEmpty() -> {
                binding.alamatEditText.error = "Alamat wajib diisi"
                binding.alamatEditText.requestFocus()
                return
            }
            genderSelected == -1 -> {
                Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
                return
            }
            tgl.isEmpty() -> {
                binding.lahirEditText.error = "Tanggal lahir wajib diisi"
                binding.lahirEditText.requestFocus()
                return
            }
            nik.isEmpty() -> {
                binding.identitasEditText.error = "Nomor identitas wajib diisi"
                binding.identitasEditText.requestFocus()
                return
            }
            nik.length < 12 -> {
                binding.identitasEditText.error = "Minimal 12 digit"
                binding.identitasEditText.requestFocus()
                return
            }
            nik.length > 16 -> {
                binding.identitasEditText.error = "Maksimal 16 digit"
                binding.identitasEditText.requestFocus()
                return
            }
            telp.isEmpty() -> {
                binding.inputTelepon.error = "Nomor telepon wajib diisi"
                binding.identitasEditText.requestFocus()
                return
            }
        }

        // Semua lolos → tampilkan popup
        AlertDialog.Builder(this)
            .setTitle("Berhasil")
            .setMessage("Silahkan menunggu untuk mendapatkan ID Pendaki.")
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()

                val namaLengkap = "$depan $belakang"

                val intent = Intent(this, ProfileFragment::class.java)
                intent.putExtra("from_biodata", true)
                intent.putExtra("nama_user", namaLengkap)
                startActivity(intent)
                finish()
            }
            .show()

    }

    private fun showImagePickDialog() {
        val options = arrayOf("Pilih dari Galeri", "Ambil Foto (Kamera)")
        AlertDialog.Builder(this)
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> takePhotoWithCamera()
                }
            }
            .show()
    }

    private fun takePhotoWithCamera() {
        // buat file sementara
        try {
            tempPhotoFile = File.createTempFile("profile_photo_", ".jpg", cacheDir).apply { deleteOnExit() }
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file kamera", Toast.LENGTH_SHORT).show()
            return
        }
        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            tempPhotoFile!!
        )
        currentPhotoUri = photoURI

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        try {
            startActivityForResult(intent, REQ_TAKE_PHOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQ_PICK_GALLERY)
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        try {
            startActivityForResult(Intent.createChooser(intent, "Pilih file"), REQ_PICK_FILE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "File manager tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableEditingFields() {
        // Pastikan EditText yang sebelumnya focusable=false bisa diisi
        binding.namdepEditText.isFocusable = true
        binding.namdepEditText.isFocusableInTouchMode = true

        binding.nambelEditText.isFocusable = true
        binding.nambelEditText.isFocusableInTouchMode = true

        binding.alamatEditText.isFocusable = true
        binding.alamatEditText.isFocusableInTouchMode = true

        binding.identitasEditText.isFocusable = true
        binding.identitasEditText.isFocusableInTouchMode = true

        binding.inputTelepon.isFocusable = true
        binding.inputTelepon.isFocusableInTouchMode = true
    }

    //Ambil Nama File
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        when(requestCode) {
            REQ_PICK_FILE -> {
                val uri = data?.data ?: return

                val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
                cursor?.moveToFirst()

                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = cursor?.getString(nameIndex ?: 0)

                binding.tvNamaFile.text = fileName ?: "Tidak dapat membaca file"

                cursor?.close()
            }

            REQ_PICK_GALLERY -> {
                val uri = data?.data ?: return
                binding.fotoProfile.setImageURI(uri)
                currentPhotoUri = uri
            }

            REQ_TAKE_PHOTO -> {
                currentPhotoUri?.let { uri ->
                    binding.fotoProfile.setImageURI(uri)
                }
            }
        }
    }
}