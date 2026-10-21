# Rencana kartu cuaca dan pendaki Beranda

Tanggal: 18 Oktober 2026. Status: diimplementasikan pada kode mobile/backend; build dan 88 test mobile berhasil. Verifikasi tampilan langsung pada HP masih diperlukan. Hasil backend dan batas pemeriksaan dicatat di `docs/verification/home-card.md` pada repo backend.

Referensi: [Figma cuaca-pendaki, 1501:8547](https://www.figma.com/design/2EsUevHudLtxUICJwEf5AW/APP-PENDAKIAN-GUNUNG?node-id=1501-8547).

## Keputusan

Pertahankan satu kartu dan susunan Figma: suhu/kondisi di kiri, lokasi/status gunung di tengah, pasangan jumlah pendaki dan label di kanan. Perbaiki sumber data, makna angka, state gagal, dan kemampuan mengikuti ukuran layar. Gunakan Android XML serta komponen yang sudah tersedia.

Makna angka untuk lingkup ini tetap mengikuti data yang tersedia: X sedang mendaki; Y total kunjungan pendaki tercatat yang sudah check-in, termasuk yang sudah checkout. Y bukan kuota dan bukan jumlah hari ini. Satu kunjungan dihitung per anggota booking, bukan orang unik sepanjang waktu. Jangan mengubah perhitungan menjadi harian tanpa keputusan produk tersendiri. Ketukan kartu menjelaskan arti angka agar format X/Y tidak dianggap sebagai keterisian kuota.

## Fakta dari kode

- `HomeFragment.setupBeranda()` mengambil suhu/kondisi/lokasi dan jumlah dari `/api/beranda`; `getDestinasi()` mengisi status gunung lewat request lain.
- `homepageHelperController.getPendaki()` menghitung status terakhir anggota: 2 sedang mendaki, 3 sudah checkout. Query saat ini mengambil booking 6–8 dan belum membatasi periode.
- Kartu memiliki fallback XML “Waspada I”; respons kosong/gagal berpotensi dipetakan menjadi 0. Ikon cuaca menimpa `iconStatus`, padahal Figma menempatkan ikon bahaya gunung di situ.
- Tinggi kartu 100dp, sebagian ukuran teks memakai dp. Font Poppins dan Plus Jakarta sudah tersedia.
- `CuacaActivity` saat ini mengambil detail destinasi dan mencatat log tanpa mengikat hasil ke tampilan; jangan menganggap halaman itu sudah menampilkan detail kartu yang benar.
- API key cuaca masih ditulis di kode dan URL berisi key ikut dicatat saat gagal. Jangan menyalin nilainya ke dokumentasi atau log baru.

## 1. Mobile: tampilan

- Rapikan hanya area `cardCuaca` di `fragment_home.xml`: putih, radius 8dp, padding horizontal 20dp dan vertikal 8dp, bayangan ringan sesuai screenshot Figma.
- Tiga kelompok horizontal seperti desain. Suhu 18sp tebal; kondisi 10sp; lokasi dan judul pendaki 12sp; status dan label bawah 10sp. Pakai Poppins/Plus Jakarta sesuai node Figma, bukan font baru.
- Gunakan `wrap_content` dengan tinggi minimum sekitar 57dp dari konten 41dp + padding 16dp. Jangan memaksa seluruh kartu setinggi 100dp atau mematok posisi dengan margin atas 192dp; kaitkan terhadap gambar/header dengan constraint yang tetap mengikuti alur halaman.
- Sisakan ruang kondisi dua baris, izinkan nama lokasi/status panjang membungkus. Pada layar sempit/font besar, kelompok boleh mengalir ke baris berikut tanpa tumpang tindih atau mengecilkan font.
- Pakai aset ikon status 12dp persis dari Figma; periksa `ket_status` sebelum menggunakan ulang. Jangan memasukkan ikon cuaca ke slot status gunung. Tidak perlu menambah ikon yang tidak ada dalam desain.
- Nilai suhu memakai °C. Teks kondisi disediakan dalam bahasa Indonesia oleh backend/provider.
- Seluruh kartu menjadi target ketuk minimal 48dp; ikon 12dp dekoratif. TalkBack membaca arti angka secara lengkap, bukan sekadar “8 per 87”. Periksa kontras teks kecil; bila merah desain kurang kontras, gunakan token merah teks yang memenuhi kontras sambil mempertahankan bentuk/warna ikon desain.

## 2. Mobile: data dan interaksi

- Gunakan satu hasil `/api/beranda` untuk seluruh isi kartu; status gunung tidak lagi bergantung pada selesai/tidaknya request daftar destinasi. Request destinasi untuk konten lain tetap berjalan sesuai kebutuhan.
- Validasi HTTP berhasil, `success`, dan keberadaan field sebelum mengikat data. Simpan angka sebagai nullable: null ditampilkan “—”; 0 hanya untuk nilai nol yang benar-benar dikirim server.
- Status gunung mengikuti kode/label backend (Normal Level 1, Waspada Level 2, Siaga Level 3, Awas Level 4). Hapus fallback “Waspada I”. Field tidak dikenal/tidak ada menjadi “Belum tersedia”, bukan status aman atau bahaya buatan.
- Pemuatan awal memakai indikator lokal kecil. Saat refresh, data terakhir tetap terlihat; spinner berakhir pada sukses, gagal, atau pembatalan. Cegah request berulang saat request yang sama masih berjalan.
- Cuaca tidak tersedia tidak menghilangkan jumlah pendaki dan status gunung yang berhasil diperoleh. Gagal seluruh request menampilkan informasi gagal dan aksi Coba lagi. Data lama diberi keterangan waktu pembaruan pada detail.
- Ketuk kartu membuka dialog/bottom sheet sederhana berisi cuaca, status, waktu pembaruan, dan kalimat “X sedang mendaki dari Y total kunjungan tercatat; bukan kuota pendakian.” Gunakan komponen yang sudah ada. Hindari long-press tersembunyi dan teks yang berganti otomatis.
- Pertahankan lifecycle/guard binding yang ada. State cukup untuk kartu ini; tidak perlu state manager global, database cache mobile baru, atau migrasi UI ke Compose.

## 3. Backend

- Tetap gunakan `GET /api/beranda`. Pertahankan field lama agar pemakai lain tidak rusak; tambahkan objek `ringkasan_pendaki` dengan `sedang_mendaki`, `total_tercatat`, dan `cakupan: seluruh_riwayat`; tambahkan `status_gunung` (kode/label), waktu observasi cuaca, dan waktu pembuatan ringkasan.
- Isi status gunung dari model destinasi yang sudah tersedia pada handler, memakai pemetaan status yang sama dengan endpoint destinasi. Status yang tidak valid tidak diberi label default Waspada.
- Hitung dua angka dari himpunan kunjungan yang sama berdasarkan status terakhir anggota. Exclude draft/batal/no-show. Hindari query berulang per pendaki dengan eager loading status yang diurutkan jelas atau agregasi Eloquent yang sesuai. Hasil hitung kosong yang sukses tetap 0.
- Jangan mengganti helper bersama menjadi statistik harian karena berpotensi mengubah tampilan web/admin lain. Dokumentasikan arti field lama dan objek baru dalam `docs/api.md` backend.
- Cuaca tetap bersumber dari WeatherAPI melalui backend. Gunakan bahasa Indonesia, koordinat konfigurasi, timeout koneksi/request terbatas, dan cache hanya respons berhasil selama 5 menit melalui cache Laravel yang sudah tersedia. Jangan cache jumlah pendaki bersama cuaca. Cuaca gagal menghasilkan null tanpa menggagalkan ringkasan lainnya.
- API key dibaca melalui `config/services.php` dari environment. Hapus URL/key dari log; catat pesan/status HTTP yang sudah disanitasi. Key yang sudah tertulis dalam kode perlu diganti melalui akun penyedia oleh pemilik akses. Tidak memasukkan nilai key ke file contoh atau commit.
- Tidak membutuhkan tabel, migration, endpoint, scheduler, atau layanan baru. Admin tetap mengelola status gunung dan checkout lewat alur yang sudah ada.

## 4. Urutan pengerjaan dan penerimaan

1. Backend: dokumentasikan kontrak aditif, benahi ringkasan/status/cuaca, lalu periksa respons aktual untuk data normal, nol valid, cuaca gagal, dan data status tidak tersedia.
2. Mobile: rapikan layout/asset/font sesuai node Figma, hubungkan kontrak dan state, lalu tambahkan penjelasan lewat ketukan kartu.
3. Jalankan build debug dan test relevan yang sudah ada. Tidak menambah file atau unit test baru.
4. Periksa di HP USB: Figma pada font normal, font besar, kondisi/status panjang, jumlah besar, loading awal, refresh, retry, dan perpindahan layar saat request berlangsung.
5. Bukti selesai: screenshot kartu HP pada kondisi normal dan gagal; tidak ada tumpang tindih, tidak ada angka nol palsu, ikon gunung tetap benar, dan backend tidak mencetak key.

## ADHD: eksplorasi dan pemilihan

Eksplorasi diskalakan untuk satu komponen: tiga cabang agen terisolasi, masing-masing empat ide, dilanjutkan tiga pendalaman. N = kebaruan, V = kelayakan, F = kecocokan (0–10). Skor gabungan = 0,35N + 0,40V + 0,25F. Penilaian merupakan pertimbangan desain, bukan hasil pengukuran pengguna.

### Sudut kejelasan makna

- Definisi kontrak X aktif/Y akumulasi dapat ditelusuri. [N5 V10 F10]
- Penjelasan angka lewat tekan lama. [N7 V7 F5] Jebakan: sulit ditemukan.
- Status gunung memiliki penjelasan tersendiri. [N5 V9 F9]
- Label bergantian saat font besar. [N8 V5 F4] Jebakan: informasi tidak stabil dan sulit diakses.

### Sudut pembaruan yang dapat dipercaya

- Kedua angka berasal dari satu respons dan satu himpunan. [N6 V10 F10]
- Status muncul hanya jika tersedia, tanpa Waspada bawaan. [N4 V10 F10]
- Kegagalan terpisah per sumber data. [N6 V9 F10]
- Nilai tidak tersedia menjadi “—”, bukan 0. [N4 V10 F10]

### Sudut perubahan paling kecil

- XML dan binding dibatasi pada satu kartu. [N4 V10 F10]
- Slot ikon status terpisah dari binding cuaca. [N5 V10 F10]
- Pisahkan perapian UI dari konfigurasi key backend. [N4 V9 F9]
- Retry terlihat di area gagal. [N5 V10 F9]

### Tiga cabang yang diperdalam

1. **★ Satu ringkasan, makna tetap jelas**: kontrak aditif pada endpoint yang ada menggabungkan status dan kedua angka, dengan penjelasan saat kartu diketuk. Risiko utama adalah menyebut Y sebagai kuota/harian padahal akumulasi; langkah pertama mendokumentasikan cakupan dan query. Variasi: deskripsi TalkBack, waktu ringkasan, atau kelak statistik harian dengan field terpisah. Gagasan kohort harian ikut ditelaah lalu ditunda karena mengubah definisi produk tanpa kebutuhan eksplisit.
2. **Figma dengan layout yang bisa tumbuh**: tiga kelompok XML memakai aset/font yang tersedia, tinggi mengikuti isi, dan pengaturan ulang saat font/lebar memerlukan. Risiko utama teks kecil/panjang bertabrakan; langkah pertama membandingkan constraint dan slot aset terhadap Figma. Variasi: pengelompokan ulang dua baris, style teks lokal, dan pengukuran di HP aktual.
3. **State jujur saat koneksi gagal**: data lama tetap terlihat, bagian cuaca boleh gagal sendiri, nilai tidak diketahui berbeda dari nol, dan retry terlihat. Risiko utama state dari request lama mengubah view baru; langkah pertama menjaga lifecycle dan pembaruan hanya pada binding yang masih aktif. Variasi: detail waktu cuaca, teks data lama, dan dialog penjelasan tanpa layar baru.

Hindari: mengubah kartu menjadi tiga kartu baru, WebSocket/polling cepat, shimmer, penyimpanan offline global, dan penambahan test baru. Pertanyaan untuk fase berikutnya: apakah produk kelak memerlukan statistik pengunjung harian? Jika iya, tambahkan definisinya secara eksplisit tanpa mengubah makna field lama diam-diam.

## Rujukan

- [Figma kartu sumber](https://www.figma.com/design/2EsUevHudLtxUICJwEf5AW/APP-PENDAKIAN-GUNUNG?node-id=1501-8547): susunan, ukuran, font, slot ikon dan warna.
- [Android: aksesibilitas aplikasi](https://developer.android.com/guide/topics/ui/accessibility/apps): target sentuh minimal 48dp, deskripsi elemen, dan kontras minimal 4,5:1 untuk teks kecil.
