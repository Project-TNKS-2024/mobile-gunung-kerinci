package com.dicoding.gunungkerinci.profile

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityProfileDataPribadiBinding
import com.dicoding.gunungkerinci.model.Country
import com.dicoding.gunungkerinci.model.Kabupaten
import com.dicoding.gunungkerinci.model.Kecamatan
import com.dicoding.gunungkerinci.model.ProfileData
import com.dicoding.gunungkerinci.model.Provinsi
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Calendar

class ProfileDataPribadiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileDataPribadiBinding
    private var loginEmail = ""
    private var photoUri: Uri? = null
    private var identityUri: Uri? = null

    private var uploadedFileName: String? = null

    private val REQ_GALLERY = 100
    private val REQ_CAMERA = 101
    private val REQ_FILE = 102

    private var selectedProvinsi: Provinsi? = null
    private var selectedKabupaten: Kabupaten? = null
    private var selectedKecamatan: Kecamatan? = null

    private val api by lazy { ApiConfig.getApiService(this) }
    private val token by lazy { "Bearer ${retrieveToken()}" }

    private val countryList = mutableListOf<Country>()
    private var selectedCountry: Country? = null
    private var existingIdentityAttachment: String? = null


    fun String.toTextBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDataPribadiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedFileName = getSharedPreferences("profile_temp", MODE_PRIVATE)
            .getString("uploaded_file_name", null)

        if (!savedFileName.isNullOrEmpty()) {
            binding.tvNamaFile.text = savedFileName
            binding.suksesUp.visibility = View.VISIBLE
        }

        setContentView(binding.root)

        loginEmail = getEmailLogin()

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.emailEditText.setText(getEmailLogin())

        Log.d("EMAIL_DEBUG", "Email = ${getEmailLogin()}")

        binding.emailEditText.isEnabled = false
        binding.fotoProfile.setImageResource(R.drawable.akundefault)

        binding.alamatEditText.setOnClickListener {
            showProvinsiDialog()
        }

        binding.wargaEditText.setOnClickListener {
            showKewarganegaraanDialog()
        }

        setupUI()
        getNegaraFromApi()
        //getProfile()

        binding.btnSimpan.setOnClickListener { showPopupData() }

        binding.suksesUp.visibility = View.GONE
    }

    private fun showProvinsiDialog() {
        lifecycleScope.launch {
            try {
                val response = api.getProvinsi()
                if (response.isSuccessful) {
                    val list = response.body()!!.data
                    val names = list.map { it.name }.toTypedArray()

                    AlertDialog.Builder(this@ProfileDataPribadiActivity)
                        .setTitle("Pilih Provinsi")
                        .setItems(names) { _, index ->
                            selectedProvinsi = list[index]
                            showKabupatenDialog(selectedProvinsi!!.id)
                        }
                        .show()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun showKabupatenDialog(provinsiId: Int) {
        lifecycleScope.launch {
            try {
                val response = api.getKabupatenByProvinsi(provinsiId)
                if (response.isSuccessful) {
                    val list = response.body()!!.data.values.toList()
                    val names = list.map { it.name }.toTypedArray()

                    AlertDialog.Builder(this@ProfileDataPribadiActivity)
                        .setTitle("Pilih Kabupaten")
                        .setItems(names) { _, index ->
                            selectedKabupaten = list[index]
                            showKecamatanDialog(selectedKabupaten!!.id)
                        }
                        .show()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun showKecamatanDialog(kabupatenId: Int) {
        lifecycleScope.launch {
            try {
                val response = api.getKecamatanByKabupaten(kabupatenId)
                if (response.isSuccessful) {
                    val list = response.body()!!.data.values.toList()
                    val names = list.map { it.name }.toTypedArray()

                    AlertDialog.Builder(this@ProfileDataPribadiActivity)
                        .setTitle("Pilih Kecamatan")
                        .setItems(names) { _, index ->
                            selectedKecamatan = list[index]

                            // ⬇️ FINAL SET TEXT
                            val alamatLengkap =
                                "${selectedKecamatan!!.name}, " +
                                        "${selectedKabupaten!!.name}, " +
                                        "${selectedProvinsi!!.name}"

                            binding.alamatEditText.setText(alamatLengkap)
                        }
                        .show()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun getProfile() {
        lifecycleScope.launch {
            try {
                val response = api.getProfile(token)
                if (response.isSuccessful && response.body()?.data != null) {
                    fillForm(response.body()!!.data)
                }
                Log.d("PROFILE_DEBUG", "Response = ${response.body()}")
            } catch (e: Exception) {

            }
        }
    }


    private fun fillForm(data: ProfileData) {
        val emailFinal = data.email ?: getEmailLogin()
        binding.emailEditText.setText(emailFinal)

        binding.namdepEditText.setText(data.first_name)
        binding.nambelEditText.setText(data.last_name)

        // Restore country selection
        selectedCountry = data.dataNegara?.let { Country(it.name ?: "", it.flag ?: "", it.code ?: "", it.dial_code ?: "") }
        binding.wargaEditText.setText(data.dataNegara?.let { "${it.flag} ${it.name}" }, false)

        // Show/hide address layout based on nationality
        if (data.kenegaraan == "ID") {
            binding.layoutAlamat.visibility = View.VISIBLE

            // Restore domisili selections
            data.dataProvinsi?.let { selectedProvinsi = Provinsi(it.id ?: 0, it.name ?: "", "") }
            data.dataKabupaten?.let { selectedKabupaten = Kabupaten(it.id ?: 0, it.name ?: "", "", 0) }
            data.dataKecamatan?.let { selectedKecamatan = Kecamatan(it.id ?: 0, it.name ?: "", 0) }

            val alamat = listOfNotNull(
                data.dataKecamatan?.name,
                data.dataKabupaten?.name,
                data.dataProvinsi?.name
            ).joinToString(", ")
            binding.alamatEditText.setText(alamat)
        } else {
            binding.layoutAlamat.visibility = View.GONE
        }

        binding.lahirEditText.setText(data.tanggal_lahir?.take(10))
        binding.identitasEditText.setText(data.nik)
        existingIdentityAttachment = data.lampiran_identitas

        // Parse phone: strip country code, set local number
        val rawPhone = data.no_hp ?: ""
        val spaceIdx = rawPhone.indexOf(' ')
        if (spaceIdx != -1) {
            binding.inputTelepon.setText(rawPhone.substring(spaceIdx + 1).trim())
        } else {
            binding.inputTelepon.setText(rawPhone)
        }
        data.dataNegara?.dial_code?.replace("+", "")?.let { code ->
            try {
                binding.ccp.setCountryForPhoneCode(code.toInt())
            } catch (e: Exception) {
                Log.e("PHONE_DEBUG", "Kode negara tidak valid: $code")
            }
        }
        if (data.dataNegara?.dial_code.isNullOrEmpty()) {
            binding.ccp.setCountryForPhoneCode(62) // default Indonesia
        }

        if (data.jenis_kelamin?.lowercase() == "l") {
            binding.radioPria.isChecked = true
        } else {
            binding.radioWanita.isChecked = true
        }

        if (!data.lampiran_identitas.isNullOrEmpty()) {
            binding.tvNamaFile.text = data.lampiran_identitas
            binding.suksesUp.visibility = View.VISIBLE
        } else {
            binding.tvNamaFile.text = "file_anda.png"
            binding.suksesUp.visibility = View.GONE
        }
    }

    private fun getNegaraFromApi() {
        lifecycleScope.launch {
            try {
                val response = api.getNegara()
                if (response.isSuccessful && response.body()?.success == true) {

                    countryList.clear()
                    countryList.addAll(response.body()!!.data)

                    val adapter = ArrayAdapter(
                        this@ProfileDataPribadiActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        countryList.map { "${it.flag} ${it.name}" }
                    )

                    binding.wargaEditText.setAdapter(adapter)
                    binding.wargaEditText.threshold = 0

                    binding.wargaEditText.setOnItemClickListener { _, _, pos, _ ->
                        setSelectedCountry(countryList[pos])
                    }

                    getProfile()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileDataPribadiActivity, "Gagal load negara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showKewarganegaraanDialog() {
        if (countryList.isEmpty()) {
            Toast.makeText(this, "Daftar kewarganegaraan belum tersedia", Toast.LENGTH_SHORT).show()
            getNegaraFromApi()
            return
        }

        val names = countryList.map { "${it.flag} ${it.name}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Kewarganegaraan")
            .setItems(names) { _, index ->
                setSelectedCountry(countryList[index])
            }
            .show()
    }

    private fun setSelectedCountry(country: Country) {
        selectedCountry = country
        binding.wargaEditText.setText("${country.flag} ${country.name}", false)

        if (country.code == "ID") {
            binding.layoutAlamat.visibility = View.VISIBLE
        } else {
            binding.layoutAlamat.visibility = View.GONE
            binding.alamatEditText.setText("")
            selectedProvinsi = null
            selectedKabupaten = null
            selectedKecamatan = null
        }
    }

    // SUBMIT PROFILE
    // =====================================================
    private fun submitProfile() {

        val firstName = binding.namdepEditText.text.toString().trim()
        val lastName = binding.nambelEditText.text.toString().trim()
        val birthDate = binding.lahirEditText.text.toString().trim()
        val identityNumber = binding.identitasEditText.text.toString().trim()

        val phoneLocal = binding.inputTelepon.text.toString().trim()
        val telpCountry ="+${binding.ccp.selectedCountryCode}"

        val gender = if (binding.radioPria.isChecked) "l" else "p"
        val nationality = selectedCountry?.code ?: ""

        // ===== VALIDASI =====
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (nationality.isEmpty()) {
            Toast.makeText(this, "Pilih kewarganegaraan", Toast.LENGTH_SHORT).show()
            return
        }

        if (birthDate.isEmpty()) {
            Toast.makeText(this, "Tanggal lahir wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // ================= VALIDASI UMUR =================
        try {
            val birthYear = birthDate.substring(0, 4).toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            if (currentYear - birthYear < 12) {
                Toast.makeText(this, "Usia minimal pendaki adalah 12 tahun", Toast.LENGTH_LONG).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Format tanggal lahir tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        if (identityNumber.isEmpty()) {
            Toast.makeText(this, "Nomor Identitas wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (phoneLocal.length < 8) {
            binding.inputTelepon.error = "Nomor telepon tidak valid"
            return
        }

        // ================= VALIDASI DOMISILI INDONESIA =================
        if (nationality == "ID") {
            if (selectedProvinsi == null) {
                Toast.makeText(this, "Provinsi wajib dipilih", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedKabupaten == null) {
                Toast.makeText(this, "Kabupaten wajib dipilih", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedKecamatan == null) {
                Toast.makeText(this, "Kecamatan wajib dipilih", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Log.d("FILE_DEBUG", "identityUri = $identityUri")

        // ================= VALIDASI FILE IDENTITAS =================
        val identityPart = identityUri?.let { uriToMultipart(it, "lampiran_identitas") }

        Log.d("FILE_DEBUG", "identityPart = ${identityPart?.headers}")

        if (identityPart == null && existingIdentityAttachment.isNullOrBlank()) {
            Toast.makeText(this, "Lampiran identitas wajib diunggah", Toast.LENGTH_SHORT).show()
            return
        }

        // ================= REQUEST BODY =================
        val provinsiBody =
            if (nationality == "ID") selectedProvinsi!!.id.toString().toTextBody()
            else null

        val kabupatenBody =
            if (nationality == "ID") selectedKabupaten!!.id.toString().toTextBody()
            else null

        val kecamatanBody =
            if (nationality == "ID") selectedKecamatan!!.id.toString().toTextBody()
            else null

        //val photoPart = photoUri?.let { uriToMultipart(it, "profile_photo") }

        Log.d("PROFILE_DEBUG", """
            firstName=$firstName
            lastName=$lastName
            gender=$gender
            nationality=$nationality
            birthDate=$birthDate
            nik=$identityNumber
            phone=$phoneLocal
            telp_country=$telpCountry
            provinsi=${selectedProvinsi?.id}
            kabupaten=${selectedKabupaten?.id}
            kecamatan=${selectedKecamatan?.id}
            provinsiBodyType=${'$'}{provinsiBody?.contentType()}
            kabupatenBodyType=${'$'}{kabupatenBody?.contentType()}
            kecamatanBodyType=${'$'}{kecamatanBody?.contentType()}
            """.trimIndent())

        // ===== API CALL =====
        lifecycleScope.launch {
            try {
                val response = api.updateProfile(
                    token,
                    firstName.toTextBody(),
                    lastName.toTextBody(),
                    nationality.toTextBody(),
                    gender.toTextBody(),
                    birthDate.toTextBody(),
                    identityNumber.toTextBody(),
                    phoneLocal.toTextBody(),
                    telpCountry.toTextBody(),
                    provinsiBody,
                    kabupatenBody,
                    kecamatanBody,
                    identityPart
                )

                if (response.isSuccessful) {

                    uploadedFileName = binding.tvNamaFile.text.toString()

                    val fullName = "$firstName $lastName"

                    getSharedPreferences("profile_data", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_biodata_filled", true)
                        .putString("user_name", fullName)
                        .putString("verification_status", "pending")
                        .apply()

                    getSharedPreferences("profile_temp", MODE_PRIVATE)
                        .edit()
                        .putString("uploaded_file_name", uploadedFileName)
                        .apply()

                    binding.suksesUp.visibility = View.VISIBLE

                    Log.d("PROFILE_RESULT", response.body().toString()) // ⬅️ TARUH DI SINI

                    Toast.makeText(
                        this@ProfileDataPribadiActivity,
                        "Profil berhasil disimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()

                    Log.e("PROFILE_SAVE", "HTTP ${response.code()} - $error")

                    handleProfileError(error)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileDataPribadiActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isProfileBaru(): Boolean {
        return selectedProvinsi == null &&
                selectedKabupaten == null &&
                selectedKecamatan == null
    }


    private fun handleProfileError(errorBody: String?) {
        when {
            errorBody == null -> {
                Toast.makeText(this, "Gagal menyimpan profil", Toast.LENGTH_LONG).show()
            }
            errorBody.contains("Nomor Identitas", true) -> {
                Toast.makeText(this, "Nomor Identitas sudah terdaftar", Toast.LENGTH_LONG).show()
            }
            errorBody.contains("Validasi", true) ||
                    errorBody.contains("Undefined", true) ||
                         errorBody.contains("provinsi", true) -> {
                Toast.makeText(
                    this,
                    "Data domisili belum valid, periksa kembali isian Anda",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                Toast.makeText(this, "Gagal menyimpan profil, silakan coba lagi", Toast.LENGTH_LONG).show()
            }
        }
    }


    // FILE & PHOTO HANDLER
    // =====================================================
    private fun uriToMultipart(uri: Uri, name: String): MultipartBody.Part {
        val file = File(cacheDir, getFileName(uri))

        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output) }
        }
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val requestBody = file.asRequestBody(mimeType.toMediaType())

        return MultipartBody.Part.createFormData(name, file.name, requestBody)
    }

    private fun getFileName(uri: Uri): String {
        var name = "file"
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    private fun retrieveToken(): String =
        getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", "") ?: ""

    private fun getEmailLogin(): String =
        getSharedPreferences("auth", MODE_PRIVATE).getString("email", "") ?: ""


    // UI & CAMERA
    // =====================================================
    private fun setupUI() {
        binding.wargaEditText.setOnClickListener { binding.wargaEditText.showDropDown() }

        binding.lahirEditText.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, y, m, d ->
                    binding.lahirEditText.setText(
                        String.format("%04d-%02d-%02d", y, m + 1, d)
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.layoutPilihFile.setOnClickListener { pickIdentityFile() }
        binding.fotoProfile.setOnClickListener { showImageDialog() }
        binding.buttonUbah.setOnClickListener { showImageDialog() }
    }

    private fun showPopupData() {

        val dialogView = layoutInflater.inflate(R.layout.popup_data, null)

        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatalData)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpanData)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // tombol batal
        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        // tombol simpan
        btnSimpan.setOnClickListener {
            dialog.dismiss()

            // baru simpan ke API
            submitProfile()
        }

        dialog.show()
    }

    private fun pickIdentityFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, REQ_FILE)
    }

    private fun showImageDialog() {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Galeri", "Kamera")) { _, i ->
                if (i == 0) pickFromGallery() else checkCameraPermission()
            }.show()
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQ_GALLERY)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA_PERMISSION)
        }
    }

    private fun openCamera() {
        try {
            val file = File.createTempFile("photo_", ".jpg", cacheDir)
            photoUri = FileProvider.getUriForFile(
                this, "${packageName}.provider", file
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, REQ_CAMERA)
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file untuk kamera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera dibutuhkan untuk menggunakan fitur ini", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK) return

        when (req) {
            REQ_FILE -> {
                val uri = data?.data ?: return
                if (!isValidFile(uri)) return

                identityUri = uri
                binding.tvNamaFile.text = getFileName(uri)

                binding.suksesUp.visibility = View.VISIBLE
                binding.tvNamaFile.text = getFileName(uri)
            }
            REQ_GALLERY -> {
                photoUri = data?.data
                binding.fotoProfile.setImageURI(photoUri)
            }
            REQ_CAMERA -> binding.fotoProfile.setImageURI(photoUri)
        }
    }

    companion object {
        private const val REQ_CAMERA_PERMISSION = 999
    }
}

// Extension function to check file validity
private fun AppCompatActivity.isValidFile(uri: Uri): Boolean {
    val cursor = contentResolver.query(uri, null, null, null, null)
    val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
    cursor?.moveToFirst()
    val size = sizeIndex?.let { cursor.getLong(it) } ?: 0
    cursor?.close()

    if (size > 5 * 1024 * 1024) { // 5MB
        Toast.makeText(this, "Ukuran file tidak boleh lebih dari 5MB", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}
