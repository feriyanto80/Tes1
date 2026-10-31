# IPTV Player v1.0.0
Aplikasi Android native Kotlin + Jetpack Compose + Media3 ExoPlayer.

## Fitur
- Multi-playlist M3U URL, impor file M3U, login Xtream Codes.
- Live TV, kategori, pencarian, favorit, film & series berdasarkan kategori.
- Streaming HTTP(S) dengan ExoPlayer, fullscreen landscape, retry.
- EPG XMLTV (pengguna memasukkan URL EPG), halaman Pengaturan.
- Penyimpanan playlist dan cache channel secara lokal.

## Build APK GitHub Actions
Branch: `iptv-player-v1-build`.
Buka GitHub > Actions > Build IPTV Player APK > workflow terbaru > Artifacts > IPTV-Player-v1.0.0-APK.
Ekstrak ZIP artifact untuk mendapat `app-debug.apk`.

## Untuk update tanpa uninstall
APK debug dari GitHub-hosted runner tidak memiliki signing key permanen antarrun.
Untuk distribusi berkelanjutan, buat release keystore dan simpan key secara aman di GitHub Secrets, lalu build release dengan signing yang sama dan naikkan versionCode.
Jangan pernah unggah private keystore ke repository publik.

## Catatan
APK adalah pemutar; tidak mencakup channel atau layanan IPTV. Pengguna harus menggunakan playlist legal/berizin.
Aplikasi mendukung HTTP(S); server Xtream yang menolak HLS kemungkinan memerlukan format stream lain. Kredensial saat ini tersimpan di SharedPreferences lokal (untuk versi produksi sebaiknya migrasi ke penyimpanan terenkripsi). EPG diproses sampai 2000 program pertama.
