# PBS Gwell Cloud Test v1.0.0

Android APK diagnostik **satu akun pribadi** berdasarkan perilaku login dan query perangkat pada [Client/yoosee.py](https://github.com/nyzxor/YooseeExploit/blob/main/Client/yoosee.py).

**Batasan penting:** Proyek upstream `YooseeExploit` adalah alat validasi kredensial otomatis. APK ini **bukan** konversi penuh alat tersebut: fitur batch credential checker, combo parsing, multi-account automation, dan output password sengaja tidak disertakan.

## Fitur
- Login satu akun yang Anda kuasai melalui `openapi-iot.cloudlinks.cn`, mengikuti format API dalam referensi (unofficial/undocumented).
- `listDevice` menghitung perangkat akun setelah login berhasil.
- `getDeviceInfo` secara opsional menampilkan status online jika tersedia.
- Kode negara seluler default Indonesia +62 (referensi upstream default +55).
- AppID dan AppToken tidak disematkan. Masukkan kredensial pengembang **yang sah dan diotorisasi**, bukan kunci tertanam hasil ekstraksi aplikasi lain.
- TLS certificate verification **aktif** (upstream Python mengandung `verify=False`; tidak dipertahankan).
- Tidak menyimpan akun, password, akses token, daftar perangkat, atau token aplikasi ke berkas. Tidak ada telemetry.
- Login manual satu percobaan setiap 30 detik; tidak ada batch/credential stuffing atau auto retry.
- Respons tidak menampilkan token akses atau data rahasia.
- Tidak ada akses video P2P, RTSP proxy, ataupun SDK pemutar cloud dalam APK ini.

## Build di GitHub
Branch `pbs-yoosee-cloud-single-account`, workflow `.github/workflows/pbs-gwell-cloud-test.yml`, artifact `PBS_Gwell_Cloud_Test_v1.0.0`. File APK juga diterbitkan ke `pbs-gwell-cloud-test/releases/PBS_Gwell_Cloud_Test_v1.0.0.apk` di repo.

## Pengujian yang dapat dibuktikan
1. Masukkan AppID dan AppToken resmi dari penyedia Gwell/Yoosee, kemudian akun kamera milik Anda.
2. Login sekali ke cloud. Bila berhasil, ambil daftar perangkat.
3. Jika diperlukan, uji metadata satu Device ID yang terkait dengan akun.
4. Bagikan laporan yang tidak memuat password atau token.

## Keterbatasan
Kode upstream memakai AppID/AppToken bawaan aplikasi tertentu dan API tidak terdokumentasi. Mengambil nilai tersebut untuk APK baru dapat melanggar hak akses/lisensi, sehingga tidak dibundel.
Jika Anda tidak memiliki kredensial developer, APK ini tetap dapat dipasang namun permintaan login tidak dapat dilakukan.
Keberhasilan login dan daftar device **tidak membuktikan P2P streaming** atau integrasi ke `cctv.pbsgroup.id`.
