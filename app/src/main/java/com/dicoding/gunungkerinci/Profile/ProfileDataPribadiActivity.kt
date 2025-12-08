package com.dicoding.gunungkerinci.Profile

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
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

    private val REQ_CAMERA_PERMISSION = 999

    private var currentPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDataPribadiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fotoProfile.setImageResource(R.drawable.ic_profile)

        val emailUser = intent.getStringExtra("email_user")
        binding.emailEditText.setText(emailUser ?: "")

        enableEditingFields()

        // Dropdown kewarganegaraan
        val adapterNegara = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            listNegara
        )
        binding.wargaEditText.setAdapter(adapterNegara)
        binding.wargaEditText.setOnClickListener {
            binding.wargaEditText.showDropDown()
        }

        // Date Picker
        binding.lahirEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val formatted = String.format("%02d-%02d-%04d", d, m + 1, y)
                    binding.lahirEditText.setText(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // Pilih file
        binding.layoutPilihFile.setOnClickListener { pickFile() }
        binding.btnPilihFile.setOnClickListener { pickFile() }

        // Foto profile
        binding.fotoProfile.setOnClickListener { showImagePickDialog() }
        binding.buttonUbah.setOnClickListener { showImagePickDialog() }

        // Tombol simpan
        binding.btnSimpan.setOnClickListener { saveBiodata() }
    }

    // ============================================================
    //                  DIALOG PILIH FOTO (GALERI / KAMERA)
    // ============================================================

    private fun showImagePickDialog() {
        val options = arrayOf("Pilih dari Galeri", "Ambil Foto (Kamera)")
        AlertDialog.Builder(this)
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> checkCameraPermission()   // FIX: cek permission dulu
                }
            }
            .show()
    }

    // ============================================================
    //                      PERMISSION KAMERA
    // ============================================================

    private fun checkCameraPermission() {
        val permission = android.Manifest.permission.CAMERA

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            takePhotoWithCamera()
        } else {
            requestPermissions(arrayOf(permission), REQ_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoWithCamera()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============================================================
    //                          BUKA KAMERA
    // ============================================================

    private fun takePhotoWithCamera() {
        try {
            tempPhotoFile = File.createTempFile("profile_photo_", ".jpg", cacheDir)
                .apply { deleteOnExit() }
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file kamera", Toast.LENGTH_SHORT).show()
            return
        }

        val photoURI = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            tempPhotoFile!!
        )
        currentPhotoUri = photoURI

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        try {
            startActivityForResult(intent, REQ_TAKE_PHOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================
    //                        GALERI & FILE
    // ============================================================

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

    // ============================================================
    //                    HANDLE FILE / FOTO DIAMBIL
    // ============================================================

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQ_PICK_FILE -> {
                val uri = data?.data ?: return
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                binding.tvNamaFile.text = cursor?.getString(nameIndex ?: 0) ?: "Tidak dapat membaca file"
                cursor?.close()
            }

            REQ_PICK_GALLERY -> {
                val uri = data?.data ?: return
                binding.fotoProfile.setImageURI(uri)
                currentPhotoUri = uri
            }

            REQ_TAKE_PHOTO -> {
                currentPhotoUri?.let { binding.fotoProfile.setImageURI(it) }
            }
        }
    }

    // ============================================================
    //                        FIELDS ENABLE
    // ============================================================

    private fun enableEditingFields() {
        binding.namdepEditText.isFocusableInTouchMode = true
        binding.nambelEditText.isFocusableInTouchMode = true
        binding.alamatEditText.isFocusableInTouchMode = true
        binding.identitasEditText.isFocusableInTouchMode = true
        binding.inputTelepon.isFocusableInTouchMode = true
    }

    // ============================================================
    //                          VALIDASI SIMPAN
    // ============================================================

    private fun saveBiodata() {
        val depan = binding.namdepEditText.text.toString().trim()
        val belakang = binding.nambelEditText.text.toString().trim()
        val negara = binding.wargaEditText.text.toString().trim()
        val alamat = binding.alamatEditText.text.toString().trim()
        val genderSelected = binding.radioGroupGender.checkedRadioButtonId
        val tgl = binding.lahirEditText.text.toString().trim()
        val nik = binding.identitasEditText.text.toString().trim()
        val telp = binding.inputTelepon.text.toString().trim()

        if (depan.isEmpty()) {
            binding.namdepEditText.error = "Nama depan wajib diisi"
            return
        }
        if (belakang.isEmpty()) {
            binding.nambelEditText.error = "Nama belakang wajib diisi"
            return
        }
        if (negara.isEmpty()) {
            binding.wargaEditText.error = "Pilih kewarganegaraan"
            binding.wargaEditText.requestFocus()
            return
        }

        if (alamat.isEmpty()) {
            binding.alamatEditText.error = "Alamat wajib diisi"
            binding.alamatEditText.requestFocus()
            return
        }

        if (genderSelected == -1) {
            Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
            return
        }

        if (tgl.isEmpty()) {
            binding.lahirEditText.error = "Tanggal lahir wajib diisi"
            binding.lahirEditText.requestFocus()
            return
        }

        if (nik.isEmpty()) {
            binding.identitasEditText.error = "Nomor identitas wajib diisi"
            binding.identitasEditText.requestFocus()
            return
        }

        if (nik.length < 12) {
            binding.identitasEditText.error = "Minimal 12 digit"
            binding.identitasEditText.requestFocus()
            return
        }

        if (nik.length > 16) {
            binding.identitasEditText.error = "Maksimal 16 digit"
            binding.identitasEditText.requestFocus()
            return
        }

        if (telp.isEmpty()) {
            binding.inputTelepon.error = "Nomor telepon wajib diisi"
            binding.inputTelepon.requestFocus()
            return
        }

        // ===============================
        // Jika semua valid → simpan
        // ===============================

        Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
        finish()
    }
}