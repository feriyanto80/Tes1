# PBS Gwell Cloud Test + Facebook OAuth v1.1.0

Aplikasi Android pengujian untuk **login Facebook (Meta SDK resmi)** dan diagnostik **Yoosee/Gwell Cloud**. Dua sesi ini **tidak saling menggantikan**: autentikasi Facebook tidak memberikan akses ke akun atau kamera Yoosee.

## Apa yang baru
- Facebook Login native menggunakan `com.facebook.android:facebook-login:19.0.0`; tombol login, logout, callback status berhasil/batal/gagal.
- Input Facebook App ID dan Client Token secara lokal dalam HP, **tidak ditanam dalam source publik**. Jangan memasukkan **App Secret**.
- Menampilkan package name dan **Facebook Android Key Hash** dari sertifikat APK terpasang. Tombol salin Key Hash.
- Meta SDK tidak mengirim token ke server PBS; token disimpan pada sisi device menggunakan mekanisme SDK Meta. Tidak diikutkan dalam laporan diagnostik.
- Tetap membawa pengujian Yoosee: login satu akun sendiri melalui API tidak resmi, daftar device akun itu, dan device info.
- Dibuat **berbeda package ID** (`com.pbsgroup.gwellcloudtest.facebook`) sehingga dapat dipasang berdampingan dengan APK v1.0.0 tanpa menghapusnya.

## Konfigurasi OAuth sebelum uji
1. Masuk ke [Meta for Developers](https://developers.facebook.com/apps/), buat aplikasi dan tambahkan Facebook Login untuk Android.
2. Masukkan **Package Name**: `com.pbsgroup.gwellcloudtest.facebook`.
3. **Class Name**: `com.pbsgroup.gwellcloudtest.MainActivity`.
4. Ambil **Key Hash** dengan membuka APK setelah instal dan menekan **Salin Key Hash Facebook**, lalu daftarkan pada konfigurasi Android app di Meta.
5. Cari **App ID** (dashboard aplikasi) dan **Client Token** (Settings > Advanced > Security). Jangan gunakan **App Secret**.
6. Tambahkan akun Facebook penguji sebagai Admin/Developer/Tester dalam Meta app apabila masih Development Mode.
7. Isi App ID dan Client Token pada APK, lalu ketuk **Masuk dengan Facebook**. Beri izin profil publik sesuai alur Meta.
8. Jika Meta menolak redirect/URI, periksa app Android dan pengaturan Facebook Login; pada Facebook SDK 19 custom tab redirect menggunakan skema `fbconnect` dan host `cct.<applicationId>`.

## Penting
- API Gwell dari [nyzxor/YooseeExploit / Client/yoosee.py](https://github.com/nyzxor/YooseeExploit/blob/main/Client/yoosee.py) **tidak punya operasi exchange token Facebook → token Gwell** yang telah tervalidasi. Facebook Login di sini hanya tes OAuth Meta.
- Tidak ada credential checking massal, ekstraksi AppToken pihak ketiga, atau penyimpanan password Yoosee.
- Keberhasilan build Android belum berarti login Facebook/Yoosee sudah sukses pada akun nyata. Butuh Meta App dan perangkat Android.
- Paket debug signed (GitHub runner). Jika build ulang berubah signature, **key hash harus didaftarkan ulang** kecuali menggunakan signing key tetap.
- Untuk benar-benar menggunakan Facebook OAuth sebagai sesi produksi `cct.pbsgroup.id`, backend PBS harus memvalidasi access token dan menerbitkan sesi PBS sendiri. UI saja tidak cukup.

## Build & download
Branch `pbs-gwell-facebook-oauth-v1.1.0` pada `feriyanto80/Tes1`. Build GitHub Actions dengan Gradle 8.10.2, Android SDK35, dan Java17.

APK otomatis dipublikasikan sebagai file langsung ke GitHub pada `pbs-gwell-cloud-test/releases/PBS_Gwell_Facebook_v1.1.0.apk` sehingga tidak bergantung pada Library ChatGPT.
