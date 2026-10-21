# Indikator proses tiga fitur mobile TNKS

Implementasi 18 Oktober 2026. Tidak ditemukan AGENTS.md pada direktori proyek, direktori induk yang diperiksa, maupun subdirektori app.

## Pelacakan Jejak

- Daftar pos menampilkan indikator lokal saat pertama dimuat. Refresh mempertahankan timeline dan menampilkan “Memperbarui…”. Progres dan kegagalan refresh tampil tanpa menghapus konten yang valid.
- Check-in manual menampilkan spinner “Mencatat…” dalam tombol dengan lebar tetap. QR dan manual saling mencegah request bersamaan.
- Identitas anggota yang sedang diproses disimpan untuk indikator per baris, sementara serialisasi perubahan kehadiran tetap berlaku.
- Status checkpoint pending pada mapper v2 yang sudah ada dipertahankan; baris anggota memakai “Menunggu sinkronisasi”. Antrean lokal tidak diberi spinner permanen.
- Flag proses dilepas melalui finally, termasuk saat coroutine dibatalkan atau operasi penyimpanan gagal.

## Peringatan Dini

- Pengambilan peringatan, pencarian GPS, dan pengiriman lokasi memiliki state terpisah. Pembaruan peringatan tidak mereset operasi lokasi.
- Data peringatan dan waktu pembaruan terakhir tetap terlihat saat refresh. Kesalahan sementara tetap memakai status data lama.
- Indikator refresh hanya muncul dalam area peringatan yang sudah dibuka. Polling tidak membuka dialog baru hanya untuk loading.
- Area tanpa data membedakan belum diperiksa, sedang diperiksa, gagal, dan hasil kosong yang sudah dikonfirmasi.
- Tombol Update Lokasi dinonaktifkan selama proses. Label membedakan “Mencari lokasi…” dan “Mengirim lokasi…”. Isi peringatan dan Hentikan alarm tetap tersedia.
- Callback GPS milik view yang sudah ditutup diabaikan agar tidak mengubah state pencarian milik view baru.

## SOS, chat, laporan bencana

- Dialog SOS tetap terlihat selama persiapan GPS dan pengiriman; tombol menampilkan spinner dan tahap proses. Guard ViewModel mencegah ketukan berulang.
- Konfirmasi request hanya menyatakan “SOS terkirim ke server.” Antrean menyatakan “SOS tersimpan di perangkat, menunggu koneksi.” Perubahan antrean lokal diobservasi; setelah worker selesai, status aktif diperiksa kembali dari server.
- Kontak petugas memiliki loading lokal dan alasan gagal/tombol Coba lagi. Menu darurat lain tetap tersedia.
- Chat mempertahankan pesan saat refresh. Persiapan foto berlangsung di IO, memiliki label proses, dan berbagi guard dengan pengiriman teks/gambar. Gagal persiapan ditampilkan sebagai error.
- Pesan offline memakai “Menunggu koneksi”. Gambar chat memiliki area tetap dan placeholder loading/error.
- Laporan bencana menampilkan spinner sejak persiapan lampiran. Pembacaan lampiran dilakukan di IO dalam operasi ViewModel. Lampiran yang tidak terbaca menggagalkan pengiriman, bukan diam-diam dihilangkan. Form tetap terbuka dan isian tetap ada saat gagal. Tidak ditambahkan antrean offline untuk laporan.

## Verifikasi otomatis

Perintah: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline`.

Build debug dan seluruh 88 unit test yang sudah ada berhasil dijalankan ulang pada 18 Oktober 2026, termasuk setelah penghapusan file LoadingLifecycleTest berisi 10 test tambahan sesuai permintaan pengguna. Tidak ada unit test baru yang ditambahkan untuk perubahan ini.

Pengujian mapper checkpoint offline dan pengujian fitur lain yang sudah ada ikut dijalankan dalam seluruh suite.

## Pemeriksaan perangkat yang masih diperlukan

Belum dilakukan pengujian visual, emulator, atau HP fisik. Periksa ukuran tombol/teks dengan font besar, loading awal dan refresh, ketukan berulang, GPS lambat/tidak aktif, izin lokasi ditolak, peringatan dan Hentikan alarm saat lokasi diperbarui, pemilihan foto rusak/akses dicabut, perpindahan layar/rotasi saat proses, dan pemulihan koneksi nyata untuk antrean checkpoint/SOS/chat. Sinkronisasi worker dalam jaringan nyata belum diverifikasi; verifikasi otomatis tidak menggantikan pemeriksaan pada perangkat.
