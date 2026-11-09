# PBS Galian Field – Android Build

Branch khusus Android PBS Galian Field (tanpa mengubah branch main / project Tes1).

## Build otomatis
GitHub Actions: **PBS Galian Field - Build APK Debug**.
Setiap perubahan pada `pbs-galian-field/android/**` memicu build APK debug.
Buka halaman **Actions**, pilih workflow, pilih run sukses, lalu unduh artifact **PBS-Galian-Field-v1.0.0-debug-APK**. ZIP artifact berisi `app-debug.apk`.

## Backend
APK merupakan Android WebView untuk backend Node.js dan frontend PBS Galian Field v1.0.0 (tersedia dalam paket full source terpisah). APK **tidak menyertakan backend** dan tidak dapat digunakan sebelum layanan HTTPS online.

URL awal dari GitHub repository variable `PBS_GALIAN_URL`; jika belum disetel, default `https://galian.pbsgroup.id/`. Untuk menggantinya perlu rebuild; dapat juga mengubah `pbsBaseUrl` pada perintah gradle.

## Catatan keamanan
APK ini hanya debug-build untuk uji coba internal. Debug signing dari runner dapat berubah antar build sehingga pembaruan APK tidak selalu dapat di-install tanpa uninstall. Untuk distribusi produksi, gunakan release signing key dalam GitHub Secrets dan build release terpisah. Jangan taruh keystore/password di repository publik.

Untuk source aplikasi server, lihat paket `PBS_GALIAN_FIELD_v1.0.0_FULL_SOURCE.zip`.
