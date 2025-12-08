package com.dicoding.gunungkerinci.Ticket

import android.R
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

class CekKuotaTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCekKuotaTiketBinding

    private var tanggalMasuk: Long? = null
    private var tanggalKeluar: Long? = null

    private var countWNA = 0
    private  var countWNI = 0

    private val listGerbang = listOf(
        "Kersik Tuo - Kayu Aro",
        "Camping Ground - Solok Selatan"
    )

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

        setUpDropdownGerbang()

        setUpBtnSelanjutnya()

        setUpMenuTabs()
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

            Toast.makeText(this, "Data lengkap — lanjut ⛰️", Toast.LENGTH_SHORT).show()

            // Pindah ke halaman Tiket Sop
            val intent = Intent(this, TiketSOPActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUpDropdownGerbang() {
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_dropdown_item_1line,
            listGerbang
        )

        val masukView = binding.dropdownGerbangMasuk
        val keluarView = binding.dropdownGerbangKeluar

        masukView.setAdapter(adapter)
        keluarView.setAdapter(adapter)

        //Dropdown gerbang masuk
        masukView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (countWNI == 0 && countWNA == 0) {
                    Toast.makeText(
                        this,
                        "Masukkan jumlah pendaki terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()

                    masukView.dismissDropDown()
                    return@setOnTouchListener true //event dikonsumsi, dropdown tidak buka
                }
                //semua valid -> buka dropdown
                masukView.showDropDown()
                return@setOnTouchListener true
            }
            false
        }

        //Dropdown gerbang keluar
        keluarView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                //1. Harus pilih gerbang masuk dulu
                if (masukView.text.isNullOrEmpty()) {
                    Toast.makeText(
                        this,
                        "Pilih gerbang masuk terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    keluarView.dismissDropDown()
                    return@setOnTouchListener true
                }

                //2. Harus ada jumlah pendaki
                if (countWNI == 0 && countWNA == 0) {
                    Toast.makeText(
                        this,
                        "Masukkan jumlah pendaki terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnTouchListener true
                }

                keluarView.showDropDown()
                return@setOnTouchListener true
            }
            false
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
                hargaMasukWNI += 15000
                hargaMasukWNA += 150000
            } else {
                hargaMasukWNI += 10000
                hargaMasukWNA += 150000
            }

            loopCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val selisih = calKeluar.timeInMillis - calMasuk.timeInMillis
        val totalMalam = (selisih / (24 * 60 * 60 * 1000)).toInt()

        val hargaKemah = totalMalam * 5000
        val hargaPendakianWNI = 10000
        val hargaPendakianWNA = 20000

        val totalWNI =
            if (countWNI == 0) 0
            else (hargaMasukWNI + hargaKemah + hargaPendakianWNI) * countWNI

        val totalWNA =
            if (countWNA == 0) 0
            else (hargaMasukWNA + hargaKemah + hargaPendakianWNA) * countWNA

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

                    // Set teks
                    binding.tglMasukEditText.setText("$dayOfMonth-${month + 1}-$year")

                    // Reset tanggal keluar jika sudah pernah diisi
                    binding.tglKeluarEditText.setText("")
                    tanggalKeluar = null

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

                    binding.tglKeluarEditText.setText("$dayOfMonth-${month + 1}-$year")

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