# PBS Gwell Live + PTZ v1.1.1

Perbaikan kumulatif dari **PBS Gwell Live + PTZ v1.1.0** (termasuk seluruh fitur inventaris Gwell Cloud v1.0.1).

## Mengapa v1.1.0 menampilkan layar hitam?
Tombol Live pada v1.1.0 merupakan pemutar **RTSP berbasis IP**, sementara daftar perangkat Gwell Cloud hanya memberikan **Device ID**, bukan IP lokal atau sesi streaming. Pada layar sebelumnya, input IP kamera kosong. Kesalahan `IllegalArgumentException: IP/hostname tidak valid` terjadi **sebelum aplikasi mencoba koneksi RTSP**, bukan karena password atau Device ID salah.

## Penambahan v1.1.1
- **Cari kamera ONVIF di WiFi ONT ini**: WS-Discovery multicast ke `239.255.255.250:3702` melalui WiFi aktif. Membutuhkan WiFi yang sama dengan kamera, multicast dapat diblokir oleh AP isolation.
- Tampilkan kandidat IP/port hasil respons ONVIF; pengguna memilih perangkat yang sesuai. Tidak menganggap kandidat ONVIF otomatis identik dengan Device ID Gwell.
- **Ambil alamat RTSP otomatis dari ONVIF**: GetProfiles + GetStreamUri; mengisi RTSP Port dan Path dari kamera jika firmware mendukung. Host RTSP tetap IP kamera yang telah dipilih, bukan host pihak ketiga dari XML.
- Pesan kesalahan berbeda saat IP kosong: perangkat Gwell ditemukan di Cloud tetapi belum ada rute IP untuk live RTSP.
- Tombol **Buka Yoosee untuk Live P2P** sebagai cara menonton video jarak jauh menggunakan aplikasi resmi bila tersedia.
- Live RTSP TCP, ONVIF PTZ 8 arah, profile token, tombol Stop, fullscreen, daftar Device ID dan pemeriksaan cloud tetap dipertahankan.
- Tidak meminta password ke server PBS dan tidak menyimpan password lokal secara permanen.

## Sangat penting
**Versi ini bukan penyambungan P2P Gwell ke aplikasi PBS.** Kode SDK lama `GwellDemo` menggunakan sesi dan verifikasi P2P khusus, sedangkan `GWTimes/IoTVideo-Android` memerlukan integrasi SDK `IoTVideoSdk.register(accessId,accessToken)` dari backend produk terkait. Keduanya **belum terbukti dapat menerima begitu saja** token yang diperoleh dari API `openapi-iot.cloudlinks.cn` pada `YooseeExploit/Client/yoosee.py`. Jangan menganggap delapan Device ID sebagai URL stream P2P.

**Untuk video lintas ONT tanpa konfigurasi jaringan** perlu: SDK P2P Gwell yang diotorisasi dengan AppID/AppToken/package khusus, atau jaringan privat CCTV/VPN dari OLT/core operator.

## Uji lapangan
1. Instal APK v1.1.1 di HP. App ID Android terpisah dari v1.1.0 karena APK debug dari GitHub Actions menggunakan sertifikat yang dapat berubah.
2. Hubungkan HP ke SSID WiFi ONT tempat satu kamera fisik berada.
3. Login dan ambil daftar Device ID.
4. Tekan **Live + PTZ** pada kamera yang ingin diuji.
5. Ketuk **Cari kamera ONVIF di WiFi ONT ini**. Bila berhasil, pilih kandidat IP. Bila tidak ditemukan, kamera mungkin tidak mengaktifkan ONVIF, atau multicast diblokir; lihat IP di DHCP ONT.
6. Masukkan **username/password kamera**, yang mungkin berbeda dari akun Yoosee Cloud.
7. Ketuk **Ambil alamat RTSP otomatis dari ONVIF** bila tersedia, lalu **Live RTSP**.
8. Ketuk **Deteksi profil ONVIF + token PTZ**, tekan-tahan arah dan lepaskan untuk stop.
9. Jika akses melalui internet seluler tanpa VPN, hasil kosong adalah normal; gunakan aplikasi resmi Yoosee untuk sementara.

## Paket
Repository: `feriyanto80/Tes1`, branch `pbs-gwell-live-ptz-v1.1.1`.
Download APK langsung setelah build di `pbs-gwell-cloud-test/releases/PBS_Gwell_Live_PTZ_v1.1.1.apk`.
