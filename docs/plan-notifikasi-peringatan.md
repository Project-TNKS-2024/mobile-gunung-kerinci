# Notifikasi dan alarm tanpa Firebase

## Perilaku
- Foreground service memeriksa emergency/active setiap 15 detik selama sesi login dan pendakian aktif. Tidak ada perubahan server/dependensi Firebase baru.
- Notifikasi pemantauan tetap terlihat; dapat dihentikan dan diaktifkan kembali dari beranda.
- Peringatan baru menampilkan notifikasi prioritas tinggi, detail, tombol Hentikan alarm, dan tautan popup beranda.
- Audio/getaran berulang sampai Hentikan ditekan. Membuka detail/menutup popup tidak menghentikan alarm. Logout, pendakian berakhir, dan pencabutan peringatan server menghentikannya.
- Tanda terima per sesi mencegah pengulangan akibat polling, rotasi, restart layanan, dan cache. Perubahan severity memicu alarm baru.
- Gangguan jaringan mempertahankan peringatan dan menandai status koneksi tertunda.

## Implementasi dan validasi
1. Kanal notifikasi pemantauan/bahaya, izin Android 13, ikon monokrom.
2. Satu foreground service untuk pemantauan dan audio, agar tidak memulai service baru dari background.
3. Tombol Hentikan di popup/notifikasi, pengaturan pemantauan beranda, navigasi notifikasi, pembersihan sesi.
4. Tes deduplikasi, action, notification/service lifecycle; build APK.

## Batasan
Doze/hemat baterai dapat menunda jaringan; interval 15 detik bukan jaminan real-time saat layar mati. Force-stop/reboot perlu aplikasi dibuka lagi. Notifikasi mengikuti izin, kanal, layar kunci dan DND; audio mengikuti volume alarm/audio focus. Tidak memaksa volume atau melewati DND. Baterai lebih boros daripada push.

Monitoring keselamatan jangka panjang memakai foreground service specialUse dengan subtype pada manifest. Distribusi Google Play memerlukan deklarasi/review use case tersebut. Audio aktif menambahkan type mediaPlayback.

## Checklist perangkat
- Android 13+: izin diterima/ditolak; kanal bahaya diblokir lalu diaktifkan.
- Peringatan baru: heads-up, panel, layar kunci, audio/getaran; tap detail tidak menghentikan audio.
- Hentikan melalui notifikasi/popup; polling, rotasi, restart tidak membunyikan ulang.
- Peringatan baru dan perubahan severity memicu lagi setelah alarm lama dihentikan.
- Background/layar mati, offline/pulih, server mencabut peringatan, logout/ganti akun.
- Hentikan/aktifkan pemantauan; restart HP/force-stop lalu buka app lagi.

## Hasil verifikasi
- 34 unit test/Robolectric lolos (26 tes sebelumnya, 8 tes notifikasi/layanan baru), simulasi Android 13.
- APK debug berhasil dibangun.
- Lint penuh berjalan, tetapi gagal pada 7 MissingConstraints di activity_cuaca.xml yang sudah ada sebelum perubahan ini. Tidak ada lint error pada komponen notifikasi/alarm baru.
- Belum ada HP/emulator terhubung untuk memverifikasi heads-up, layar kunci, bunyi, dan kebijakan baterai perangkat asli.
