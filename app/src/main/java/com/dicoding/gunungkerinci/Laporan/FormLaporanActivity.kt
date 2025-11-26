package com.dicoding.gunungkerinci.Laporan

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityFormLaporanBinding
import java.io.File

class FormLaporanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormLaporanBinding
    private lateinit var imageUri: Uri

    private val CAMERA_CODE = 100
    private val GALLERY_CODE = 200
    private val PERMISSION_CODE = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdown()
        loadRecentImages()
        setupButtons()
    }

    //HANDLE BUTTON KAMER, GALERI, POSTING
    private fun setupButtons() {
        binding.buttonBack.setOnClickListener { finish() }

        binding.buttonPosting.setOnClickListener {
            Toast.makeText(this, "Laporan berhasil diposting (mode demo)", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.buttonKamera.setOnClickListener {
            if (checkPermission()) openCamera()
        }

        binding.buttonGaleri.setOnClickListener {
            if (checkPermission()) openGallery()
        }
    }

    //GALERI
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_CODE)
    }

    //KAMERA
    private fun openCamera() {
        val imageFile = File.createTempFile("IMG_", ".jpg", externalCacheDir)
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        startActivityForResult(intent, CAMERA_CODE)
    }

    //LOAD 10 FOTO TERBARU
    private fun loadRecentImages() {

        val recentImages = getRecentImages(10)

        binding.fotoTerbaruList.removeAllViews()

        for (uri in recentImages) {
            val img = ImageView(this)

            img.layoutParams = binding.sampleFoto.layoutParams
            img.setPadding(0, 0, 10, 0)
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setImageURI(uri)
            img.setBackgroundResource(R.drawable.bg_image_border)

            // klik → tambah preview
            img.setOnClickListener { addPreviewImage(uri) }

            binding.fotoTerbaruList.addView(img)
        }
    }

    //TAMBAHKAN FOTO PREVIEW
    private fun addPreviewImage(uri: Uri) {
        val img = ImageView(this)
        img.layoutParams = binding.sampleFoto.layoutParams
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.setPadding(0, 0, 10, 10)
        img.setImageURI(uri)

        binding.layoutPreviewFoto.addView(img)
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
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            var count = 0
            while (it.moveToNext() && count < limit) {
                val id = it.getLong(idColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                uriList.add(uri)
                count++
            }
        }

        return uriList
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
    }

    //PERMISSION
    private fun checkPermission(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missing.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                PERMISSION_CODE
            )
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            CAMERA_CODE -> addPreviewImage(imageUri)
            GALLERY_CODE -> {
                data?.data?.let { uri -> addPreviewImage(uri) }
            }
        }
    }
}