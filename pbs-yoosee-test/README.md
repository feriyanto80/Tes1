# PBS Yoosee Diagnostic — Android v1.0.0

APK uji kamera Yoosee milik PBS: ONVIF Device Service dan pemutaran RTSP secara langsung. **Bukan aplikasi klien P2P Gwell yang telah terautentikasi.**

## Kemampuan saat ini
- Pemeriksaan jenis koneksi HP (WiFi/seluler).
- ONVIF SOAP GetDeviceInformation memakai WS-Security UsernameToken digest; hasil HTTP 401/Fault ditampilkan sebagai kegagalan/indikasi kebutuhan autentikasi lain.
- Live RTSP dengan ExoPlayer Media3 (transport TCP) dan log status.
- Menyimpan input hanya di memori selama aplikasi hidup (tidak ada server PBS, analytics, atau penyimpanan password).
- Bagikan laporan diagnostik tanpa password atau P2P Device ID.
- Tombol membuka aplikasi resmi Yoosee jika telah terpasang. **Keberhasilan melalui Yoosee resmi tidak membuktikan SDK PBS berhasil.**

## Mengapa belum login P2P
Gwell GWP2PSDK/Android P2P-Core lama memerlukan AppID, AppToken, AppVersion dan package name yang telah disetujui penyedia. Repositori IoTVideo Android juga tidak menjamin kompatibilitas dengan kamera Yoosee consumer milik pengguna. Tidak memasang SDK vendor lama atau kredensial contoh yang tidak valid merupakan keputusan disengaja.

## Cara mencoba dengan satu kamera
1. Di aplikasi Yoosee resmi, pastikan kamera online; catat Device ID untuk identifikasi saja.
2. Hubungkan HP ke WiFi **ONT yang sama** dengan kamera.
3. Temukan IP kamera dari daftar DHCP ONT, mis. 192.168.1.100.
4. Buka PBS Yoosee Diagnostic, isi IP, port ONVIF (coba 80 atau 5000), username, password. ONVIF harus aktif pada firmware kamera.
5. Ketuk Uji ONVIF Device Service. HTTP 200 dengan Manufacturer/Model merupakan bukti respons perangkat (bukan bukti akses lintas internet).
6. Isi RTSP path yang sesuai firmware; contoh beberapa model: /onvif1. Ketuk Putar Live RTSP (TCP).
7. Ulangi melalui internet seluler. Tanpa VLAN CCTV, VPN atau P2P bridge, RTSP lokal kemungkinan tidak terjangkau—**ini hasil yang diharapkan**.
8. Ketuk Bagikan hasil diagnostik; periksa isi sebelum membagikan ke pihak lain.

## Bangun APK dengan GitHub Actions
Pada branch pbs-yoosee-sdk-test, buka Actions > Build PBS Yoosee Diagnostic APK > run/artifacts. Unduh artifact PBS_Yoosee_Diagnostic_v1.0.0, unzip, pasang app-debug.apk (debug signing). APK debug bukan release produksi.

## Tahap berikutnya: P2P SDK resmi
Minta vendor menyediakan SDK Android ARM64 yang relevan dengan model, AppID/AppToken untuk package **com.pbsgroup.yooseediagnostic**, dokumentasi autentikasi akun/device binding, hak redistribusi dan mekanisme akses video frames. Simpan kredensial aplikasi privat di backend / GitHub Actions secrets sesuai ketentuan vendor, **jangan commit AppToken ke repository publik**.

## Batasan
- RTSP/ONVIF berjalan jika IP kamera benar-benar dapat dirutekan dari HP; tidak dapat memakai Device ID cloud sebagai pengganti IP.
- Beberapa perangkat menggunakan HTTP Digest/Basic dan bukan WS-Security UsernameToken atau path ONVIF berbeda; hasil 401/Fault bukan bukti ONVIF tidak didukung.
- Test RTSP default port 554; kamera dengan port lain memerlukan penyesuaian source.
- ONVIF Discovery WS-Discovery lintas VLAN tidak otomatis disediakan; input IP manual.
- Aplikasi ini tidak membuka port router dan tidak mengirim password ke cctv.pbsgroup.id.
