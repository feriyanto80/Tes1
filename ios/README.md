# PBS Balap Native iOS v1.0.4 — BALAP + BA Survey (BASL)

**Nama:** PBS Balap  
**Bundle ID:** `id.pbsgroup.pbsbalap`  
**Versi:** 1.0.4 (build 4)  
**Minimal:** iOS/iPadOS 16  
**Logo:** logo PBS dari aset yang sudah dipakai aplikasi Android.

## Form yang digabungkan
1. BALAP Digital — https://balap.pbsgroup.id/ (opsi HTTP jika HTTPS tidak dapat diakses)
2. BA Survey Digital — https://basl.pbsgroup.id/

Satu halaman beranda native, tab Beranda/BALAP/BA Survey, dua WKWebView
yang tetap hidup ketika berganti tab, upload foto dan file,
izin lokasi iOS, toolbar responsif, serta notifikasi error koneksi.

Data formulir tetap di kedua server web masing-masing. Perpindahan tab
mempertahankan tampilan web selama aplikasi tetap aktif; saat proses iOS
ditutup paksa, state web form yang tidak disimpan server tidak dijamin pulih.

## Build melalui GitHub
Branch: pbs-balap-ios-unified-v1.0.4  
Workflow: **Build PBS Balap iOS v1.0.4 (BALAP+BASL)**  
Keluaran hanya paket aplikasi simulator dan aplikasi device unsigned untuk
pengembangan. **Keluaran workflow ini TIDAK siap dipasang pada iPhone.**

Build unsigned dapat berjalan tanpa akun Apple Developer, tetapi tidak
memungkinkan instalasi OTA melalui BetaDrop: iPhone akan menolak IPA tanpa
tanda tangan, sertifikat valid, dan provisioning perangkat.

## Membuat file .ipa yang BENAR-BENAR bisa diinstal di iPhone
Gunakan workflow: **Sign PBS Balap iOS v1.0.4 IPA (BetaDrop)**

Syarat Apple Developer Program:
- Apple Developer Team ID.
- Sertifikat Apple Distribution dalam bentuk P12, termasuk private key.
- Ad Hoc provisioning profile untuk Bundle ID `id.pbsgroup.pbsbalap`.
- UDID setiap iPhone yang akan menginstal, terdaftar di profile tersebut.

Masukkan di GitHub Settings → Secrets and variables → Actions:
- `IOS_TEAM_ID`
- `IOS_DISTRIBUTION_P12_BASE64`
- `IOS_DISTRIBUTION_P12_PASSWORD`
- `IOS_ADHOC_PROFILE_BASE64`

Jangan kirim P12, kata sandi, atau profile melalui chat/publik.
Nilai Base64 dapat dibuat di Mac dengan `base64 -i /path/file | tr -d '\n'`.

Jalankan `workflow_dispatch` pada branch
`pbs-balap-ios-unified-v1.0.4`.
Jika workflow tidak muncul karena bukan default branch, perbarui file
`ios/signing/trigger-v1.0.4.txt` pada branch tersebut untuk memicu signing
setelah GitHub Secrets lengkap.

Workflow signing menolak sertifikat dan profile tidak sesuai; hanya
mengunggah `PBS-Balap-v1.0.4-SIGNED-AdHoc.ipa` jika seluruh pemeriksaan
`codesign`, App ID, versi, masa berlaku, dan daftar device lolos.

Upload hanya signed IPA ke BetaDrop untuk instalasi lewat Safari pada
perangkat yang terdaftar; file unsigned tidak boleh diupload ke BetaDrop.

## GPS
WebView dapat meminta izin lokasi dari sistem operasi. Untuk form Google Apps
Script yang berada di iframe lintas domain, website induk harus mengizinkan
geolocation melalui iframe allow dan Permissions-Policy. Native iOS tidak dapat
melewati aturan browser tersebut.
