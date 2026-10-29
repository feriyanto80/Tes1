# PBS BALAP Android v1.0.1

Aplikasi Android WebView untuk https://balap.pbsgroup.id/ .
Tidak membuka endpoint Google Apps Script lama sebagai laman utama.

## Perbaikan
- Memperbaiki whitelist origin geolocation: `balap.pbsgroup.id`, `script.google.com`, dan origin dinamis Google Apps Script `*-script.googleusercontent.com`.
- Mempertahankan dukungan upload foto, kamera, dan file.
- Memiliki tombol GPS di header untuk memeriksa izin lokasi Android serta status provider GPS/jaringan.
- Menjaga pembatasan HTTPS dan penolakan error sertifikat SSL.

## Batasan teknis
Jika website menyematkan Google Apps Script lintas origin tanpa `allow="geolocation"` atau header Permissions-Policy yang sesuai, akses lokasi tetap dapat gagal dari browser maupun APK. APK tidak dapat melewati kebijakan iframe dari website.

## Build
Branch: `pbs-mobile-android-v1`.
GitHub Actions: `Build PBS Mobile APK`.
Artifact: `PBS_BALAP_v1.0.1_APK`.
Android 8 atau lebih baru.

## Instalasi
Build debug GitHub Actions memakai keystore sementara setiap run; apabila `APK v1.0.0` sudah terpasang, tanda tangan build baru mungkin tidak cocok. Jika muncul `App not installed`, uninstall versi lama terlebih dahulu lalu instal v1.0.1. Akun atau data yang disimpan dalam WebView lama dapat terhapus oleh uninstall.
