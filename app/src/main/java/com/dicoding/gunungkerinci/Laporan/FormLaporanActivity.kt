package com.dicoding.gunungkerinci.Laporan

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityFormLaporanBinding
import com.google.android.flexbox.FlexboxLayout
import java.io.File

class FormLaporanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormLaporanBinding
    private var imageUri: Uri? = null

    private val selectedImage = mutableListOf<Uri>()

    private val CAMERA_CODE = 100
    private val GALLERY_CODE = 200
    private val PERMISSION_CODE = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDefaultProfile()
        setupDropdown()

        // sembunyikan placeholder sample di awal (biar tidak tampil saat kita isi dari gallery)
        binding.sampleFoto.visibility = View.GONE

        // minta permission dulu, setelah granted -> loadRecentImages()
        checkPermission {
            loadRecentImages()
        }

        setupButtons()
        setupPostingButtonState()
        setupDeskripsiHint()
    }

    //HINT DESKRIPSI
    private fun setupDeskripsiHint() {
        val hint = binding.hintDeskripsi
        val edit = binding.etDeskripsi

        // Hilangkan hint saat EditText di-klik (fokus)
        edit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hint.visibility = View.GONE
            } else {
                // Jika keluar fokus & teks kosong → tampilkan lagi
                hint.visibility = if (edit.text.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Saat diketik → hint hilang
        edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                hint.visibility = if (s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Saat pertama kali kembali ke halaman
        if (edit.text.isNullOrEmpty()) {
            hint.visibility = View.VISIBLE
        }
    }

    //Foto profile default
    private fun setDefaultProfile() {
        binding.fotoProfile.setImageResource(R.drawable.ic_profile)
    }

    //DROPDOWN LOKASI
    private fun setupDropdown() {
        val lokasiList = listOf(
            "Pos 1", "Pos 2", "Pos 3",
            "Shalter 1", "Shalter 2", "Shalter 3",
            "Tugu Yuda", "Puncak"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, lokasiList)

        binding.dropLokasiA.setAdapter(adapter)
        binding.dropLokasiB.setAdapter(adapter)

        // Supaya keyboard tidak muncul, tapi dropdown tetap bisa dibuka
        binding.dropLokasiA.showSoftInputOnFocus = false
        binding.dropLokasiB.showSoftInputOnFocus = false

        // Klik → buka dropdown (force show)
        binding.dropLokasiA.setOnClickListener {
            binding.dropLokasiA.showDropDown()
        }
        binding.dropLokasiB.setOnClickListener {
            binding.dropLokasiB.showDropDown()
        }
    }

    //BUTTON POSTING ENABLE/DISABLE
    private fun setupPostingButtonState() {
        binding.buttonPosting.isEnabled = false
        binding.buttonPosting.background =
            ContextCompat.getDrawable(this, R.drawable.border_disable)
        binding.buttonPosting.isClickable = false

        binding.etDeskripsi.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePotingButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    //HANDLE BUTTON KAMER, GALERI, POSTING
    private fun setupButtons() {

        //Back → kembali ke fragment
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Posting
        binding.buttonPosting.setOnClickListener {

            Toast.makeText(this, "Laporan berhasil diposting !", Toast.LENGTH_SHORT).show()

            val pref = getSharedPreferences("laporan_pref", MODE_PRIVATE)

            //pref.edit().putBoolean("sudah_diposting", false)
            pref.edit().putBoolean("sudah_posting", true).apply()

            finish()
        }

        binding.buttonKamera.setOnClickListener {
            checkPermission() {
                openCamera()
            }
        }

        binding.buttonGaleri.setOnClickListener {
            checkPermission() {
                openGallery()
            }
        }
    }

    //GALERI
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_CODE)
    }

    //KAMERA
    private fun openCamera() {
        val imageFile = File.createTempFile("IMG_", ".jpg", externalCacheDir)
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        // beri grant permission utk camera uri jika diperlukan
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        startActivityForResult(intent, CAMERA_CODE)
    }

    //LOAD 10 FOTO TERBARU
    private fun loadRecentImages() {

        val recentImages = getRecentImages(10)

        binding.fotoTerbaruList.removeAllViews()

        // sembunyikan placeholder sample (agar tidak tampil)
        binding.sampleFoto.visibility = View.GONE

        // tambahkan semua uri yang ditemukan (jika kosong -> tetap kosong)
        for (uri in recentImages) {
            val img = ImageView(this)
            img.layoutParams = binding.sampleFoto.layoutParams
            img.setPadding(0, 0, 10, 0)
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setImageURI(uri)
            img.setBackgroundResource(R.drawable.bg_image_border)

            // klik → tambah preview
            img.setOnClickListener {
                addPreviewImage(uri)
            }

            binding.fotoTerbaruList.addView(img)
        }
        // (tidak ada early return atau toast)
    }

    //TAMBAHKAN FOTO PREVIEW
    private fun addPreviewImage(uri: Uri) {

        if (selectedImage.size >= 4) {
            Toast.makeText(this, "Maksimal 4 foto.", Toast.LENGTH_SHORT).show()
            return
        }

        selectedImage.add(uri)
        renderPreviewFoto()
        updatePotingButtonState()

    }

    private fun renderPreviewFoto() {
        val container = binding.previewContainer
        container.removeAllViews()

        val inflater = layoutInflater

        for ((index, uri) in selectedImage.withIndex()) {
            val itemView = inflater.inflate(R.layout.item_preview_foto, container, false)

            val img = itemView.findViewById<ImageView>(R.id.imgPreview)
            val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDelete)

            img.setImageURI(uri)

            // Hapus foto
            btnDelete.setOnClickListener {
                selectedImage.removeAt(index)
                renderPreviewFoto()
                updatePotingButtonState()
            }

            // Klik foto → fullscreen
            img.setOnClickListener {
                openFullscreen(uri)
            }

            container.addView(itemView)
        }

    }

    private fun openFullscreen(uri: Uri) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val img = dialog.findViewById<ImageView>(R.id.fullImage)
        img.setImageURI(uri)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialog.show()
    }


    //BUTTON POSTING ENABLE / DISABLE
    private fun updatePotingButtonState() {
        val adaTeks = binding.etDeskripsi.text.toString().isNotEmpty()
        val adaFoto = selectedImage.isNotEmpty()

        if (adaTeks || adaFoto) {
            binding.buttonPosting.isEnabled = true
            binding.buttonPosting.isClickable = true
            binding.buttonPosting.background =
                ContextCompat.getDrawable(this, R.drawable.border_enable)
        } else {
            binding.buttonPosting.isEnabled = false
            binding.buttonPosting.isClickable = false
            binding.buttonPosting.background =
                ContextCompat.getDrawable(this, R.drawable.border_disable)
        }
    }

    private fun getRecentImages(limit: Int): List<Uri> {
        val uriList = mutableListOf<Uri>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"   // FIXED

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var count = 0

            while (cursor.moveToNext() && count < limit) {
                val id = it.getLong(idColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )

                uriList.add(uri)
                count++
            }
        }

        return uriList
    }


    //PERMISSION
    // request permission -> ketika granted panggil callback afterGranted
    private fun checkPermission(afterGranted: () -> Unit) {
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            afterGranted()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_CODE)
        }
    }

    //HASIL FOTO
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            CAMERA_CODE -> imageUri?.let { addPreviewImage(it) }
            GALLERY_CODE -> data?.data?.let { addPreviewImage(it) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE) {
            val grantedAll =
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (grantedAll) {
                // jika user mengizinkan → load foto terbaru
                // langsung load foto terbaru tanpa toast
                loadRecentImages()

            } else {
                //Toast.makeText(this, "Izin diperlukan agar aplikasi dapat mengakses foto", Toast.LENGTH_SHORT).show()
            }
        }
    }
}