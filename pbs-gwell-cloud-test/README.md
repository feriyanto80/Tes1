# PBS Gwell Live + PTZ v1.1.0

Pengembangan kumulatif dari **PBS Gwell Cloud Device List v1.0.1**. Aplikasi ini menampilkan inventaris kamera milik akun Yoosee yang berhasil login ke Gwell Cloud, serta menyediakan **live RTSP dan kontrol ONVIF PTZ** untuk kamera yang IP-nya dapat dijangkau dari HP.

## Fitur

- Login satu akun Yoosee/Gwell Cloud (API cloud **tidak resmi** pada `openapi-iot.cloudlinks.cn`) memakai kredensial AppID/AppToken terotorisasi.
- Daftar Device ID, salin ID, cek metadata kamera.
- Tombol **Live + PTZ** pada tiap kamera mengisi Device ID ke layar player.
- Player Android Media3 RTSP/RTP TCP (H.264 tergantung firmware/codec).
- ONVIF `GetProfiles`, profile token otomatis jika tersedia, `ContinuousMove`, `Stop`; kontrol arah 8 arah, tekan-tahan dan lepas untuk stop; 3 kecepatan.
- Tampilan video dengan kontrol playback dan mode layar penuh.
- Pesan error terpisah untuk RTSP dan ONVIF. Kontrol berhenti saat berpindah halaman.
- Tidak menyimpan password, P2P session atau RTSP URL ke persistent storage.
- Tidak memerlukan root; APK debug Android 8+.
- UI dan nama aplikasi PBS sendiri; **tidak mengkloning merek/logo atau kode proprietary Yoosee**.

## Batasan penting

**Cloud Device ID != RTSP URL.** Gwell Cloud login/listDevice hanya menyediakan inventaris dan metadata, **tidak menyediakan frame video**. Aplikasi ini **BELUM** memiliki SDK P2P Gwell yang berlisensi/berfungsi; karena itu 8 kamera yang berasal dari ONT berbeda **tidak otomatis bisa dilihat lewat seluler**.

Untuk live dan PTZ, HP harus bisa merutekan IP lokal kamera melalui **WiFi ONT yang sama**, VPN site-to-site/subnet route, atau VLAN CCTV privat yang dirutekan dengan baik. Tanpa konektivitas tersebut, tombol video akan gagal dengan pesan error, bukan fallback otomatis ke P2P.

Tidak semua firmware Yoosee mendukung RTSP/ONVIF/PTZ. Contoh RTSP path `/onvif1` hanya berlaku di beberapa model; port ONVIF sering 5000 atau 80. Aplikasi tidak mencoba menebak password, mengeksploitasi perangkat atau mem-bypass autentikasi.

Bagi pengguna yang membutuhkan streaming **P2P Cloud per Device ID dari internet tanpa VPN**, pengembangan lanjutan memerlukan SDK Android Gwell/Yoosee yang kompatibel dengan firmware dan AppID/AppToken serta package name khusus yang disetujui vendor.

## Pengujian
1. Install APK dari `releases/PBS_Gwell_Live_PTZ_v1.1.0.apk`.
2. Login cloud lalu Ambil daftar perangkat; pastikan ID milik sendiri muncul.
3. Ketuk **Live + PTZ** pada salah satu kamera.
4. Masukkan IP kamera yang **dapat dijangkau HP**, port RTSP (default 554), RTSP path, ONVIF port (default 5000), username dan password kamera (bisa berbeda dari akun Yoosee).
5. Tekan Live RTSP. Jika video muncul, jalur video lokal berhasil.
6. Tekan Deteksi profil ONVIF, lalu tahan tombol arah. Lepaskan untuk stop.
7. Coba akses dari seluler tanpa VPN; kegagalan akses IP lokal adalah hasil yang wajar.
8. Catat error yang muncul untuk penyesuaian path/port/codec dan kemampuan firmware.

## Paket & signing
Package `com.pbsgroup.gwellcloudtest.liveptz` berbeda dari v1.0.1 agar bisa dipasang berdampingan. Untuk update APK dalam jalur produksi sebaiknya signing key tetap dan applicationId sama.

GitHub branch: `pbs-gwell-live-ptz-v1.1.0`. Alur GitHub Actions mempublikasikan file APK ke folder `pbs-gwell-cloud-test/releases`, tidak melalui file Library ChatGPT.
