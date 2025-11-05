package com.dicoding.gunungkerinci.Homepage.Sop

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.gunungkerinci.R

class SopActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sop)

        val recyclerView = findViewById<RecyclerView>(R.id.rvSop)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val items = listOf(
            SopItem.Header,
            SopItem.Title("PENDAHULUAN"),
            SopItem.Subtitle("A. Latar Belakang"),
            SopItem.Text("Dalam rangka untuk melakukan pelayanan prima " +
                    "kepada pengunjung dan untuk mewujudkan pengelolaan ekowisata yang profesional " +
                    "efektif dan efisien dalam hal pengelolaan ekowisata Pendakian Gunung Kerinci " +
                    "maka diperlukan Standar Operasional Prosedur (yang selanjutnya di singkat SOP) " +
                    "pada seluruh proses penyelenggaraan pengelolaannya.\n" +
                    "\n" +
                    "Salah satu objek daya tarik wisata alam di Taman Nasional Kerinci Seblat (TNKS) " +
                    "adalah Gunung Kerinci yang merupakan gunung berapi aktif tertinggi di Indonesia " +
                    "(3.805 mdpl) yang terletak di dua Provinsi yaitu Jambi dan Sumatera Barat. " +
                    "Sehingga perlu disusun SOP yang merupakan pedoman atau acuan untuk melaksanakan " +
                    "tugas pekerjaan sesuai dengan Tugas dan fungsi pokok pengelola wisata. SOP juga " +
                    "merupakan alat penilaian kinerja berdasarkan indikator indikator teknis " +
                    "administrasif dan prosedural sesuai dengan tata kerja prosedur dan sistem " +
                    "kerja pada unit kerja yang bersangkutan. SOP berisi Prosedur Kerja yaitu " +
                    "urutan-urutan yang telah dibuat dalam melakukan suatu pekerjaan dimana " +
                    "terdapat tahapan demi tahapan yang harus dilalui sehingga terlihat jelas " +
                    "adanya aturan yang harus ditaati oleh orang yang akan menjalankan prosedur " +
                    "kerja pada bidang tugas yang telah mereka kerjakan dan membuat suatu pekerjaan " +
                    "itu mudah dimengerti dan dipahami. Dengan adanya standar operasional prosedur " +
                    "ini kedepannya bisa dilakukan evaluasi dan peningkatan kualitas kerja yang " +
                    "lebih baik seiring dengan berjalannya waktu."),

            SopItem.Subtitle("B. Jalur Pendakian"),
            SopItem.Text("Terdapat 2 (dua) jalur pendakian Gunung" +
                    "Kerinci:"),
            SopItem.Text( "1. Jalur pendakian melalui Pusat informasi R10 Kersik " +
                    "Tuo Kabupaten Kerinci  Provinsi Jambi."),
            SopItem.Text("2. Jalur pendakian melalui Camping Ground Bangun Rejo " +
                    "Kabupaten Solok  Selatan Provinsi Sumatera Barat."),

            SopItem.Title("KETENTUAN"),
            SopItem.Subtitle("A. Ketentuan"),
            SopItem.Text("1. Para pendaki wajib menjaga keamanan " +
                    "ketertiban dan menghormati nilai  nilai budaya atau kearifan lokal masyarakat " +
                    "sekitar jalur pendakian."),
            SopItem.Text("2. Setiap pendakian di Gunung Kerinci melalui jalur " +
                    "pendakian Pusat  informasi R10 Kersik Tuo Kabupaten Kerinci Provinsi Jambi wajib  " +
                    "melakukan booking online sesuai ketersediaan kuota tidak diperkenankan mendaftar " +
                    "secara offline."),
            SopItem.Text("3. Setiap pendakian di Gunung Kerinci melalui jalur " +
                    "pendakian Camping  Ground Bangun Rejo Kabupaten Solok Selatan Provinsi Sumatera " +
                    "Barat melakukan booking online sesuai ketersediaan kuota setelah mendapatkan  " +
                    "SIMAKSI. Pengurusan SIMAKSI dilakukan secara langsung di Kantor  BBTNKS di " +
                    "Sungai Penuh atau Kantor Bidang Pengelolaan TN Wilayah II  Sumatera Barat di " +
                    "Padang atau Kantor Seksi Pengelolaan TN Wilayah IV  Sangir pada hari atau jam " +
                    "kerja dan dapat dilakukan 2 bulan hingga 1 hari  sebelum pendakian. Setiap " +
                    "SIMAKSI pendakian dikeluarkan untuk grup  pendaki dengan jumlah minimum 3 (tiga) " +
                    "orang dan maksimum 10  (sepuluh) orang pendaki serta untuk mengurangi resiko " +
                    "kecelakaan  diwajibkan menggunakan jasa pemanduan."),
            SopItem.Text("4. Bagi pendaki yang naik dari pintu masuk Kersik Tuo " +
                    "dan turun melalui  pintu masuk Camping Ground Bangun Rejo Kabupaten Solok " +
                    "Selatan wajib  menggunakan SIMAKSI."),
            SopItem.Text("5. Batas waktu pendakian melalui Pos R10 Kersik Tuo " +
                    "Kabupaten Kerinci  Provinsi Jambi adalah 2 (dua) hari dan pendakian melalui " +
                    "Camping Ground  Bangun Rejo Kabupaten Solok Selatan Provinsi Sumatera Barat " +
                    "adalah 4  (empat) hari."),
            SopItem.Text("6. anggung jawab dan keselamatan pendaki menjadi " +
                    "tangggung jawab pribadi  dan tidak akan menuntut pihak pengelola (Balai Besar TNKS) " +
                    "pemandu  porter pemerintah dan pemerintah daerah."),
            SopItem.Text("7. Jumlah pendaki yang di izinkan naik maksimum 102 orang/hari " +
                    "untuk jalur  pendakian melalui Pos R10 Kersik Tuo Kabupaten Kerinci Provinsi Jambi  " +
                    "dan 46 orang/hari untuk jalur pendakian melalui Camping Ground Bangun  Rejo " +
                    "Kabupaten Solok Selatan Provinsi Sumatera Barat."),
            SopItem.Text("8. Bagi Warga Negara Asing (WNA) yang akan mendaki " +
                    "diwajibkan untuk  menggunakan jasa pemanduan."),
            SopItem.Text("9. Membayar karcis masuk sesuai dengan peraturan yang " +
                    "berlaku dan  asuransi keselamatan."),
            SopItem.Text("10. Seluruh calon pendaki wajib membuat akun melalui " +
                    "melalui  https://eticket.tnkerinciseblat.com/beranda menu register " +
                    "(https://eticket.tnkerinciseblat.com/register). "),
            SopItem.Text("11. Pengunjung melakukan login memakai akun yg sudah " +
                    "didaftarkan dengan  memasukkan email yg sudah didaftarkan  " +
                    "(https://eticket.tnkerinciseblat.com/login)."),
            SopItem.Text("12. Pengunjung booking online di menu booking online " +
                    "di  https://eticket.tnkerinciseblat.com/booking/1 ."),
            SopItem.Text("13. Pengunjung akan mendapatkan kode pembayaran " +
                    "setelah bayar akan  mendapatkan tiket elektronik."),
            SopItem.Text("14. Pendaki yang melakukan tujuan khusus seperti " +
                    "penelitian pengambilan  foto dan atau video dll harus mengurus izin ke " +
                    "kantor Balai Besar TNKS."),

            SopItem.Subtitle("B. Pendaki Diwajibkan"),
            SopItem.Text("1. Berbadan sehat pada saat melakukan " +
                    "pendakian dengan menunjukan Surat  Keterangan Dokter (asli)."),
            SopItem.Text("2. Masuk jalur pendakian antara pukul 07.30 s/d 15.00 WIB " +
                    "dan mendaki  pada jalur yang sudah ditentukan atau jalur resmi."),
            SopItem.Text("3. Mematuhi semua rambu-rambu dan informasi keselamatan " +
                    "yang ada di  sepanjang jalur pendakian."),
            SopItem.Text("4. Melakukan evakuasi mandiri terhadap diri dan rekannya " +
                    "yang sakit sebelum  mendapatkan bantuan dari petugas."),
            SopItem.Text("5. Memakai dan membawa perlengkapan standar pendakian " +
                    "gunung serta  perbekalan pendakian yang cukup."),
            SopItem.Text("6. Mengisi form isian barang bawaan yang berpotensi menghasilkan sampah."),
            SopItem.Text("7. Membawa trash bag kantong sampah dan membawa sampah " +
                    "bawaannya ke  luar kawasan taman nasional."),
            SopItem.Text("8. Memprioritaskan penanganan bagi wanita yang sedang " +
                    "menstruasi  utamanya segera membawa turun korban tersebut apabila sudah menderita  sakit."),
            SopItem.Text("9. Membawa kelengkapan P3K standar dan survival kit standar."),
            SopItem.Text("10. Menjaga norma agama norma susila dan kearifan lokal."),
            SopItem.Text("11. Mengikuti jalur pendakian yang sudah ditetapkan Balai " +
                    "Besar TNKS. Petugas Balai Besar TNKS akan memeriksa barang bawaan karcis dan  " +
                    "SIMAKSI sebelum dan sesudah memasuki kawasan."),
            SopItem.Text("Dalam rangka  penanggulangan sampah di Gunung Kerinci " +
                    "oleh para pendaki diwajibkan  untuk meninggalkan salah satu identitas asli pribadi " +
                    "kepada petugas akan  dikembalikan kepada pendaki apabila barang bawaan yang " +
                    "menghasilkan  sampah dibawa kembali keluar TNKS."),

            SopItem.Subtitle("C. Setiap Pendaki Dilarang"),
            SopItem.Text("1. Membawa satwa atau tumbuhan dan/atau bagian-bagiannya " +
                    "dari luar dan  dari dalam kawasan TNKS."),
            SopItem.Text( "2. Mengambil merusak semua jenis tanaman dan/atau " +
                    "bagian-bagiannya di  dalam kawasan TNKS."),
            SopItem.Text( "3. Membunuh melukai mengambil satwa beserta " +
                    "bagian-bagiannya dari  dalam kawasan TNKS."),
            SopItem.Text( "4. Memetik memindahkan memotong menebang mencabut " +
                    "tumbuhan di  dalam kawasan TNKS."),
            SopItem.Text( "5. Membawa senjata api senjata tajam (parang golok " +
                    "dan sejenisnya) kecuali  pisau lipat pisau saku atau pisau kecil."),
            SopItem.Text( "6. Melakukan aktifitas yang menyebabkan kebakaran " +
                    "hutan di dalam kawasan  TNKS."),
            SopItem.Text( "7. Membuang sampah di dalam kawasan TNKS."),
            SopItem.Text( "8. Mengganggu memindahkan dan merusak fasilitas yang " +
                    "tersedia di dalam  kawasan TNKS."),
            SopItem.Text( "9. Melakukan vandalisme dalam kawasan TNKS."),
            SopItem.Text( "10. Melakukan pendakian antara jam 15.00 s/d 07.30 WIB."),
            SopItem.Text( "11. Mengganti identitas diri pada saat booking online " +
                    "dan pengurusan SIMAKSI."),
            SopItem.Text( "12. Menggunakan SIMAKSI pendakian untuk kegiatan Diklat " +
                    "pencinta  alam/kegiatan orientasi pencinta alam."),
            SopItem.Text( "13. Buang Air Besar dialiran sungai/mata air dan di " +
                    "sepanjang jalur pendakian."),
            SopItem.Text( "14. Membawa dan menggunakan Narkotika dan obat-obatan " +
                    "terlarang  (NARKOBA) Miras dan bahan-bahan yang dilarang oleh Undang-Undang  " +
                    "Republik Indonesia didalam kawasan TNKS dan sekitarnya."),

            SopItem.Subtitle("D. Sanksi"),
            SopItem.Text("Sanksi dikenakan kepada Pendaki apabila " +
                    "melanggar ketentuan sebagai  berikut :"),
            SopItem.Text( "1. Bagi pendaki yang memasuki kawasan TNKS lebih dari " +
                    "pukul 15.00 WIB  diwajib untuk menunggu di lokasi berkemah (camping) atau " +
                    "homestay  terdekat sampai pukul 07.30 WIB."),
            SopItem.Text( "2. Bagi pendaki yang tidak membawa alat pendakian " +
                    "sesuai standar maka  harus melengkapinya atau tidak diizinkan melakukan pendakian."),
            SopItem.Text( "3. Bagi yang melanggar aturan tersebut pada point A B " +
                    "dan C maka pendaki  yang bersangkutan dan organisasinya akan masuk Daftar Hitam  " +
                    "(BLACKLIST) dan tidak diperbolehkan untuk melakukan pendakian  kembali ke Gunung " +
                    "Kerinci sampai batas waktu yang diberikan Balai Besar  TNKS"),
            SopItem.Text(  "Bagi para pendaki yang melakukan perbuatan hukum " +
                    "yang melanggar  Peraturan Perundang-undangan yang berlaku di Republik Indonesia  " +
                    "dilakukan tindakan sesuai dengan Peraturan Perundang-undangan  Republik Indonesia " +
                    "serta sesuai dengan Kitab Undang-Undang Hukum  Acara Indonesia.")
        )

        // Set adapter
        recyclerView.adapter = SopAdapter(items)
    }
}