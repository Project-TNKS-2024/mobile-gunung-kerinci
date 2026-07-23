package com.dicoding.gunungkerinci.Ticket

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.dicoding.gunungkerinci.Ticket.SOP.TiketSOPActivity
import com.dicoding.gunungkerinci.databinding.ActivityCekKuotaTiketBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.model.CreateBookingRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import android.util.Log
import com.dicoding.gunungkerinci.model.TiketPendaki

class CekKuotaTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCekKuotaTiketBinding

    private var tanggalMasuk: Long? = null
    private var tanggalKeluar: Long? = null

    private var tanggalMasukApi = ""
    private var tanggalKeluarApi = ""

    private var countWNA = 0
    private  var countWNI = 0

    //================ API =================//

    private var gateMasukId: Int? = null
    private var gateKeluarId: Int? = null

    private var minPendaki = 2
    private var maxPendaki = Int.MAX_VALUE

    private var paketId: Int? = null

    private var daftarGate = mutableListOf<String>()
    private var daftarGateObject = mutableListOf<com.dicoding.gunungkerinci.model.Gate>()

    private var hargaWNI: TiketPendaki? = null

    private var hargaWNA: TiketPendaki? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCekKuotaTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //tombol back
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setUpTanggalMasuk()
        setUpTanggalKeluar()

        setUpCounterWNI()
        setUpCounterWNA()

        loadGate()

        loadPaketTiket()

        setUpBtnSelanjutnya()

        setUpMenuTabs()
    }

    private fun loadGate() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@CekKuotaTiketActivity)
                    .getDetailDestinasi(1)

                if (response.isSuccessful) {
                    val destinasi = response.body()?.data ?: return@launch

                    daftarGate.clear()
                    daftarGateObject.clear()

                    destinasi.gates.forEach {
                        daftarGate.add(it.nama)
                        daftarGateObject.add(it)
                    }
                    setUpDropdownGerbang()
                }
            } catch (e: Exception) {
                Log.e("BOOKING", e.message.toString())
            }
        }
    }

    private fun loadPaketTiket() {
        lifecycleScope.launch {
            try {

                paketId = intent.getIntExtra("paket_id", 0)

                val response =
                    ApiConfig.getApiService(this@CekKuotaTiketActivity)
                        .getPaketDestinasi(1)

                if (!response.isSuccessful) return@launch

                val paket =
                    response.body()
                        ?.data
                        ?.paket
                        ?.firstOrNull { it.id == paketId }
                        ?: return@launch

                minPendaki = paket.min_pendaki
                maxPendaki = paket.max_pendaki

                hargaWNI =
                    paket.tiket_pendaki.firstOrNull {
                        it.kategori_pendaki.equals("wni", true)
                    }

                hargaWNA =
                    paket.tiket_pendaki.firstOrNull {
                        it.kategori_pendaki.equals("wna", true)
                    }

                Log.d("BOOKING", "Paket = ${paket.nama}")
                Log.d("BOOKING", "Harga WNI = $hargaWNI")
                Log.d("BOOKING", "Harga WNA = $hargaWNA")

            } catch (e: Exception) {
                Log.e("BOOKING", e.toString())
            }
        }
    }

    private fun createBooking() {
        lifecycleScope.launch {
            try {
                if (paketId == null) {
                    Toast.makeText(
                        this@CekKuotaTiketActivity,
                        "Paket tiket belum dipilih",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (gateMasukId == null) {
                    Toast.makeText(
                        this@CekKuotaTiketActivity,
                        "Pilih gerbang masuk",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (gateKeluarId == null) {
                    Toast.makeText(
                        this@CekKuotaTiketActivity,
                        "Pilih gerbang keluar",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val idPaket = paketId ?: return@launch
                val idGateMasuk = gateMasukId ?: return@launch
                val idGateKeluar = gateKeluarId ?: return@launch

                val request = CreateBookingRequest(
                    date_start = tanggalMasukApi,
                    date_end = tanggalKeluarApi,
                    wni = countWNI,
                    wna = countWNA,
                    jenis_tiket = idPaket,
                    gerbang_masuk = idGateMasuk,
                    gerbang_keluar = idGateKeluar
                )

                Log.d("BOOKING", "Tanggal Masuk = $tanggalMasukApi")
                Log.d("BOOKING", "Tanggal Keluar = $tanggalKeluarApi")
                Log.d("BOOKING", "WNI = ${request.wni}")
                Log.d("BOOKING", "WNA = ${request.wna}")
                Log.d("BOOKING", "Paket = ${request.jenis_tiket}")
                Log.d("BOOKING", "Gate Masuk = ${request.gerbang_masuk}")
                Log.d("BOOKING", "Gate Keluar = ${request.gerbang_keluar}")

                val response =
                    ApiConfig.getApiService(this@CekKuotaTiketActivity)
                        .createBooking(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val booking = body.data ?: return@launch
                        Toast.makeText(
                            this@CekKuotaTiketActivity,
                            body.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(
                            this@CekKuotaTiketActivity,
                            TiketSOPActivity::class.java
                        )

                        //========================
                        // BOOKING
                        //========================
                        intent.putExtra("booking_id", booking.id)

                        intent.putExtra(
                            "tanggal_masuk",
                            booking.tanggal_masuk
                        )

                        intent.putExtra(
                            "tanggal_keluar",
                            booking.tanggal_keluar
                        )

                        intent.putExtra(
                            "total_hari",
                            booking.total_hari
                        )

                        intent.putExtra(
                            "total_pembayaran",
                            booking.total_pembayaran
                        )

                        intent.putExtra(
                            "total_wni",
                            booking.total_pendaki_wni
                        )

                        intent.putExtra(
                            "total_wna",
                            booking.total_pendaki_wna
                        )

                        intent.putExtra(
                            "total_pendaki",
                            booking.total_pendaki_wni + booking.total_pendaki_wna
                        )

                        //========================
                        // GATE
                        //========================
                        intent.putExtra(
                            "gate_masuk_id",
                            booking.gate_masuk.id
                        )

                        intent.putExtra(
                            "gate_masuk",
                            booking.gate_masuk.nama
                        )

                        intent.putExtra(
                            "gate_keluar_id",
                            booking.gate_keluar.id
                        )

                        intent.putExtra(
                            "gate_keluar",
                            booking.gate_keluar.nama
                        )

                        //========================
                        // PAKET
                        //========================
                        intent.putExtra(
                            "paket_id",
                            booking.gktiket.id
                        )

                        intent.putExtra(
                            "paket_nama",
                            booking.gktiket.nama
                        )

                        intent.putExtra(
                            "min_pendaki",
                            booking.gktiket.min_pendaki
                        )

                        //========================
                        // DESTINASI
                        //========================
                        intent.putExtra(
                            "destinasi_id",
                            booking.destinasi.id
                        )

                        intent.putExtra(
                            "destinasi_nama",
                            booking.destinasi.nama
                        )

                        startActivity(intent)

                    } else {
                        Toast.makeText(
                            this@CekKuotaTiketActivity,
                            body?.message ?: "Booking gagal",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this@CekKuotaTiketActivity,
                        "Response ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {

                Log.e("BOOKING", e.toString())

                Toast.makeText(
                    this@CekKuotaTiketActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setUpMenuTabs() {
        val menuKuota = binding.tvMenuLeft
        val menuTiket = binding.tvMenuRight

        val indicator = binding.viewIndicator
        val layoutKuota = binding.layoutKuota
        val layoutTiket = binding.layoutTiketSaya

        // Default: Tiket Saya aktif
        moveIndicatorTo(menuTiket)

        menuKuota.setOnClickListener {
            menuKuota.setTextColor(getColor(com.dicoding.gunungkerinci.R.color.primary))
            menuTiket.setTextColor(getColor(com.dicoding.gunungkerinci.R.color.softgrey))

            layoutKuota.visibility = View.VISIBLE
            layoutTiket.visibility = View.GONE

            moveIndicatorTo(menuKuota)
        }

        menuTiket.setOnClickListener {
            menuTiket.setTextColor(getColor(com.dicoding.gunungkerinci.R.color.primary))
            menuKuota.setTextColor(getColor(com.dicoding.gunungkerinci.R.color.softgrey))

            layoutKuota.visibility = View.GONE
            layoutTiket.visibility = View.VISIBLE

            moveIndicatorTo(menuTiket)
        }
    }

    private fun moveIndicatorTo(view: TextView) {
        view.post {
            binding.viewIndicator.animate()
                .x(view.x)
                .setDuration(200)
                .start()

            binding.viewIndicator.layoutParams.width = view.width
            binding.viewIndicator.requestLayout()
        }
    }

    private fun setUpBtnSelanjutnya() {
        binding.btnSelanjutnya.setOnClickListener {

            val masuk = binding.tglMasukEditText.text.toString()
            val keluar = binding.tglKeluarEditText.text.toString()
            val gerbangMasuk = binding.dropdownGerbangMasuk.text.toString()
            val gerbangKeluar = binding.dropdownGerbangKeluar.text.toString()

            if (masuk.isEmpty() ||
                keluar.isEmpty() ||
                gerbangMasuk.isEmpty() ||
                gerbangKeluar.isEmpty() ||
                (countWNI == 0 && countWNA == 0)
            ) {
                Toast.makeText(this, "Harap lengkapi semua data terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (gateMasukId == null) {
                Toast.makeText(
                    this,
                    "Silakan pilih gerbang masuk",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (gateKeluarId == null) {
                Toast.makeText(
                    this,
                    "Silakan pilih gerbang keluar",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val totalPendaki = countWNI + countWNA

            if (totalPendaki < minPendaki) {
                Toast.makeText(
                    this,
                    "Minimal pendaki $minPendaki orang",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

           createBooking()
        }
    }

    private fun setUpDropdownGerbang() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            daftarGate
        )

        val masukView = binding.dropdownGerbangMasuk
        val keluarView = binding.dropdownGerbangKeluar

        masukView.setAdapter(adapter)
        keluarView.setAdapter(adapter)

        //-------------------------------------
        // GERBANG MASUK
        //-------------------------------------

        masukView.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {
                if (countWNI == 0 && countWNA == 0) {
                    Toast.makeText(
                        this,
                        "Masukkan jumlah pendaki terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnTouchListener true
                }
                masukView.showDropDown()
                true
            } else false
        }

        masukView.setOnItemClickListener { _, _, position, _ ->
            val gate = daftarGateObject[position]
            gateMasukId = gate.id

            Log.d("BOOKING", "Gate Masuk = ${gate.nama}")
            Log.d("BOOKING", "Gate Masuk ID = $gateMasukId")
        }

        //-------------------------------------
        // GERBANG KELUAR
        //-------------------------------------

        keluarView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (masukView.text.isNullOrEmpty()) {
                    Toast.makeText(
                        this,
                        "Pilih gerbang masuk terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnTouchListener true
                }
                keluarView.showDropDown()
                true
            } else false
        }

        keluarView.setOnItemClickListener { _, _, position, _ ->
            val gate = daftarGateObject[position]
            gateKeluarId = gate.id

            Log.d("BOOKING", "Gate Keluar = ${gate.nama}")
            Log.d("BOOKING", "Gate Keluar ID = $gateKeluarId")
        }
    }

    //format angka harga
    private fun formatHarga(value: Int): String {
        return String.format("%,d", value).replace(",", ".")
    }

    private fun formatCount(value: Int): String =
        value.toString().padStart(2, '0')

    //validasi tanggal sebelum klik jumlah pendaki WNI dan WNA
    private fun tanggalSiap(): Boolean {
        if (tanggalMasuk == null || tanggalKeluar == null) {
            Toast.makeText(this, "Silahkan pilih tanggal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    //jumlah WNI
    private fun setUpCounterWNA() {

        binding.btnPlusWNA.setOnClickListener {
            if (!tanggalSiap()) return@setOnClickListener
            countWNA++
            binding.tvCountWNA.text = formatCount(countWNA)
            hitungHarga()
        }

        binding.btnMinWNA.setOnClickListener {
            if (!tanggalSiap()) return@setOnClickListener
            if (countWNA > 0) countWNA--
            binding.tvCountWNA.text = formatCount(countWNA)
            hitungHarga()
        }

    }

    //jumlah WNA
    private fun setUpCounterWNI() {

        binding.btnPlusWNI.setOnClickListener {
            if (!tanggalSiap()) return@setOnClickListener
            countWNI++
            binding.tvCountWNI.text = formatCount(countWNI)
            hitungHarga()
        }

        binding.btnMinWNI.setOnClickListener {
            if (!tanggalSiap()) return@setOnClickListener
            if (countWNI > 0) countWNI--
            binding.tvCountWNI.text = formatCount(countWNI)
            hitungHarga()
        }

    }

    //hitung harga final
    private fun hitungHarga() {
        if (tanggalMasuk == null || tanggalKeluar == null) return

        val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val fmtMasuk = df.parse(binding.tglMasukEditText.text.toString())
        val fmtKeluar = df.parse(binding.tglKeluarEditText.text.toString())

        val calMasuk = Calendar.getInstance().apply { time = fmtMasuk!! }
        val calKeluar = Calendar.getInstance().apply { time = fmtKeluar!! }

        var hargaMasukWNI = 0
        var hargaMasukWNA = 0

        val loopCal = calMasuk.clone() as Calendar

        while (!loopCal.after(calKeluar)) {
            val dayOfWeek = loopCal.get(Calendar.DAY_OF_WEEK)
            val isWeekend =
                (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

            if (isWeekend) {
                hargaMasukWNI += hargaWNI?.harga_masuk_wd ?: 0
                hargaMasukWNA += hargaWNA?.harga_masuk_wd ?: 0
            } else {
                hargaMasukWNI += hargaWNI?.harga_masuk_wk ?: 0
                hargaMasukWNA += hargaWNA?.harga_masuk_wk ?: 0
            }

            loopCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val selisih = calKeluar.timeInMillis - calMasuk.timeInMillis
        val totalMalam = (selisih / (24 * 60 * 60 * 1000)).toInt()

        val hargaKemahWNI =
            totalMalam * (hargaWNI?.harga_kemah ?: 0)

        val hargaKemahWNA =
            totalMalam * (hargaWNA?.harga_kemah ?: 0)

        val trackingWNI =
            hargaWNI?.harga_traking ?: 0

        val trackingWNA =
            hargaWNA?.harga_traking ?: 0

        val asuransiWNI =
            hargaWNI?.harga_ansuransi ?: 0

        val asuransiWNA =
            hargaWNA?.harga_ansuransi ?: 0

        val totalWNI =
            if (countWNI == 0) 0
            else (hargaMasukWNI + hargaKemahWNI + trackingWNI + asuransiWNI) * countWNI

        val totalWNA =
            if (countWNA == 0) 0
            else (hargaMasukWNA + hargaKemahWNA + trackingWNA + asuransiWNA) * countWNA

        val totalHarga = totalWNI + totalWNA

        binding.hrgTiketWNI.text = formatHarga(totalWNI)
        binding.hrgTiketWNA.text = formatHarga(totalWNA)
        binding.ttlHargaTiket.text = formatHarga(totalHarga)
    }

    //tanggal masuk
    private fun setUpTanggalMasuk() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        binding.tglMasukEditText.setOnClickListener {

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    val cal = Calendar.getInstance()
                    cal.set(year, month, dayOfMonth)

                    tanggalMasuk = cal.timeInMillis

                    val formatTampil =
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                    val formatApi =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    binding.tglMasukEditText.setText(
                        formatTampil.format(cal.time)
                    )

                    tanggalMasukApi =
                        formatApi.format(cal.time)

                    binding.tglKeluarEditText.setText("")
                    tanggalKeluar = null
                    tanggalKeluarApi = ""

                    // Reset tampilan hari & malam
                    binding.ttlHari.text = "0"
                    binding.ttlMalam.text = "0"

                    // RESET COUNTER WNI/WNA
                    countWNI = 0
                    countWNA = 0
                    binding.tvCountWNI.text = "00"
                    binding.tvCountWNA.text = "00"

                    // RESET SEMUA HARGA
                    binding.hrgTiketWNI.text = "0"
                    binding.hrgTiketWNA.text = "0"
                    binding.ttlHargaTiket.text = "0"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Minimal pilih hari ini
            datePicker.datePicker.minDate = today
            datePicker.show()
        }
    }

    //tanggal keluar
    private fun setUpTanggalKeluar() {
        binding.tglKeluarEditText.setOnClickListener {

            if (tanggalMasuk == null) {
                Toast.makeText(this, "Pilih tanggal masuk terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calMasuk = Calendar.getInstance()
            calMasuk.timeInMillis = tanggalMasuk!!

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    val cal = Calendar.getInstance()
                    cal.set(year, month, dayOfMonth)
                    tanggalKeluar = cal.timeInMillis

                    val formatTampil =
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                    val formatApi =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    binding.tglKeluarEditText.setText(
                        formatTampil.format(cal.time)
                    )

                    tanggalKeluarApi =
                        formatApi.format(cal.time)

                    // Hitung total hari & malam
                    hitungHariMalam()
                    hitungHarga()
                },
                calMasuk.get(Calendar.YEAR),
                calMasuk.get(Calendar.MONTH),
                calMasuk.get(Calendar.DAY_OF_MONTH)
            )

            // Minimal tanggal keluar adalah tanggal masuk + 1 hari
            datePicker.datePicker.minDate = tanggalMasuk!!
            datePicker.show()
        }
    }

    //hitung hari dan malam
    private fun hitungHariMalam() {
        if (tanggalMasuk == null || tanggalKeluar == null) return

        val selisih = tanggalKeluar!! - tanggalMasuk!!
        val jumlahHari = (selisih / (24 * 60 * 60 * 1000)).toInt()

        val totalHari: Int
        val totalMalam: Int

        if (jumlahHari == 0) {
            // Jika masuk & keluar di hari yang sama
            totalHari = 0
            totalMalam = 0
        } else {
            // Jika lebih dari 1 hari
            totalHari = jumlahHari + 1
            totalMalam = totalHari - 1
        }

        binding.ttlHari.text = totalHari.toString()
        binding.ttlMalam.text = totalMalam.toString()
    }
}