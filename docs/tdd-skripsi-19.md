# Penyelarasan TDD mobile dengan skripsi

Diperbarui 20 Oktober 2026. Acuan: tabel TDD pada skripsi-v32-revisi-uat.docx. Satu metode pengujian ber-ID mewakili satu baris skenario skripsi; beberapa masukan digabung jika menguji aturan yang sama.

| Kelas | Sebelum penyelarasan mobile | Sesudah | Hasil eksekusi |
|---|---:|---:|---|
| PelacakanJejakTddTest | 10 | 7 | 7 lulus |
| PeringatanDiniTddTest | 8 | 6 | 6 lulus |
| PanicButtonTddTest | 8 | 6 | 6 lulus |
| Total TDD skripsi | 26 | 19 | 19 lulus |

Jumlah awal kelas peringatan di mobile adalah 8, sedangkan tabel rancangan lama skripsi berisi 7. Penyelarasan memakai isi tabel revisi 7–6–6, bukan sekadar pengurangan jumlah.

## Pemetaan skenario

| ID | Perilaku yang diperiksa |
|---|---|
| TDD-A01 | QR valid menghasilkan identitas pos; format salah dan kosong ditolak. |
| TDD-A02 | Koordinat dalam rentang diterima, koordinat di luar rentang ditolak; GPS opsional pada QR. |
| TDD-A03 | Hasil pemeriksaan radius dipetakan ke status dalam/luar radius sebagai dasar konfirmasi. |
| TDD-A04 | Itinerary terurut naik–turun, puncak tunggal, progres per fase, dan kunjungan berikutnya sesuai perjalanan. |
| TDD-A05 | Identitas booking/pendaki/pos/fase dibedakan; respons berhasil/ulang cocok diterima dan konflik/penolakan ditolak. |
| TDD-A06 | Kehadiran server dan penanda lokal dipetakan pada pos/fase tepat; pending berbeda dari status server. |
| TDD-A07 | Pembatalan membersihkan kunjungan terpilih tanpa menghapus fase lainnya. |
| TDD-B01 | Daftar kosong dan pencabutan membersihkan peringatan serta popup tanpa dianggap gagal jaringan. |
| TDD-B02 | Tingkat bahaya didahulukan, lalu waktu terbaru pada tingkat yang sama. |
| TDD-B03 | Popup dapat ditutup, polling yang sama tidak membukanya otomatis, dan pengguna dapat membukanya kembali. |
| TDD-B04 | Cache tetap tersedia dengan waktu dan penanda usang saat gagal sementara; berbeda dari daftar kosong sukses. |
| TDD-B05 | Respons 401/403 membersihkan state peringatan dan menghentikan pemantauan melalui siklus hidup layanan. |
| TDD-B06 | Peringatan baru memicu alarm, penghentian mempertahankan informasi, polling sama tidak mengulang, pencabutan menghapus alarm/notifikasi. |
| TDD-C01 | Lokasi dan tingkat low/medium/high valid; lokasi hilang/tidak valid dan tingkat tidak dikenal ditolak. |
| TDD-C02 | Pesan tambahan pemicu SOS opsional dan dibatasi 1.000 karakter. |
| TDD-C03 | Isian laporan wajib; lampiran opsional harus memenuhi format dan batas ukuran. |
| TDD-C04 | Kegagalan jaringan diputuskan untuk antrean SOS, bukan penolakan biasa. |
| TDD-C05 | Jaringan/401/408/429/5xx dapat dicoba ulang; 403/422 tidak dicoba ulang. |
| TDD-C06 | Chat lokal terikat sesi SOS, berstatus PENDING, dan dapat dihapus tanpa menghapus sesi lain. |

## Pelaksanaan dan bukti

Pemeriksaan awal terhadap tiga kelas lama berhasil sebelum perubahan. Setelah penyelarasan, pengujian terarah berhasil dengan 19 metode, tanpa kegagalan, error, atau pengujian dilewati. Seluruh unit test debug kemudian dijalankan: **81 pengujian dalam 14 kelas lulus**, failures=0, errors=0, skipped=0.

Perintah pengujian terarah:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests com.dicoding.gunungkerinci.PelacakanJejakTddTest --tests com.dicoding.gunungkerinci.PeringatanDiniTddTest --tests com.dicoding.gunungkerinci.PanicButtonTddTest --console=plain
```

Perintah seluruh unit test:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain
```

Laporan HTML: app/build/reports/tests/testDebugUnitTest/index.html.
Laporan XML: app/build/test-results/testDebugUnitTest/TEST-com.dicoding.gunungkerinci.*TddTest.xml.
Ringkasan tersimpan: docs/tdd-skripsi-19-results.json.

Pelacakan dan SOS memakai JVM dengan DAO in-memory pada batas penyimpanan. Peringatan memakai Robolectric SDK 33 agar penghentian layanan dan notifikasi Android diuji melalui perilaku yang ada. Pengujian ini tidak membuktikan keluaran audio perangkat fisik, penerimaan backend nyata, atau keberhasilan pertolongan. Pengujian regresi lain tetap dipertahankan; 19 adalah jumlah suite TDD skripsi, bukan seluruh unit test aplikasi.

Perubahan produksi tidak diperlukan. Dokumen skripsi dan Notion tidak diperbarui dalam pekerjaan mobile ini. Angka hasil lama di skripsi harus diselaraskan terpisah dengan bukti eksekusi terbaru jika diminta.


## Pembaruan penghapusan atas permintaan pengguna

Enam berkas berisi 36 pengujian dihapus sesuai instruksi pengguna: BiayaTambahanTest.kt, BottomNavigationTest.kt, JejakTimelineTest.kt, TicketNotificationSelectionTest.kt, TiketStatusTest.kt, WarningNotificationTest.kt. Tiga berkas TDD skripsi tetap berjumlah 7, 6, dan 6; lima berkas pengujian lain tetap berjumlah 26. Eksekusi ulang seluruh unit test setelah penghapusan: **45/45 lulus dalam 8 kelas**, failures=0, errors=0, skipped=0. Angka 81 pada bagian sebelumnya merupakan hasil historis sebelum penghapusan. Ringkasan JSON dan laporan Gradle sudah memuat hasil terbaru.

## Status akhir setelah penghapusan 26 pengujian lainnya

Atas permintaan lanjutan pengguna, lima file berikut juga dihapus: CheckpointOfflineStateTest.kt, CheckpointResponseHandlingTest.kt, LocationDetailDistanceTest.kt, ProfileAndTicketRegressionTest.kt, TrackingVisitUiMapperTest.kt. Kini hanya tiga kelas TDD skripsi yang dipertahankan (7 Pelacakan, 6 Peringatan, 6 SOS). Eksekusi ulang seluruh unit test debug setelah penghapusan: **19/19 lulus dalam 3 kelas**, failures=0, errors=0, skipped=0. Angka 81 dan 45 pada bagian sebelumnya merupakan hasil historis. Total 62 pengujian di luar tiga kelas TDD telah dihapus atas instruksi pengguna.
