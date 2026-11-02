# PBS Balap iOS v1.0.4 — Instal Gratis Menggunakan Apple ID

Repository GitHub yang terhubung: `feriyanto80/Tes1`  
Branch: `pbs-balap-ios-free-personal-team-v1.0.4`  
Aplikasi: PBS Balap (BALAP Digital + BA Survey Digital/BASL)  
Versi: 1.0.4; iOS 16+; logo PBS sebelumnya tetap digunakan.

## Apakah Apple Developer gratis bisa membuat sertifikat dan provisioning?

**Bisa, hanya untuk pengembangan dan pengujian dengan Xcode pada perangkat pribadi.**
Saat menggunakan Apple ID gratis di Xcode, pilih `Personal Team`.
Xcode akan mengurus development signing certificate dan provisioning profile
secara otomatis untuk iPhone yang terhubung.

Batas dari Apple untuk Personal Team: profil berakhir setelah 7 hari, 
maksimal 3 aplikasi yang terpasang per perangkat, maksimal 3 perangkat,
dan maksimal 10 App IDs. Setelah profil berakhir aplikasi harus
ditandatangani dan diinstal ulang melalui Xcode. Angka bisa berubah oleh Apple.

Apple Developer gratis **tidak** menyediakan Apple Distribution certificate
atau Ad Hoc provisioning yang diperlukan agar IPA dapat diinstal langsung
via BetaDrop, ataupun TestFlight. Tidak mungkin menjadikan hasil unsigned
GitHub Actions sebagai IPA yang dapat diinstal hanya dengan mengganti ekstensi.

## Langkah pada Mac, tanpa membayar Apple Developer Program

1. Daftar gratis di https://developer.apple.com/register dengan Apple ID pribadi.
2. Unduh artifact ZIP dari workflow GitHub
   **PBS Balap iOS v1.0.4 — Free Apple ID Xcode Package** (branch tersebut).
   Artifact sudah menyertakan logo, source SwiftUI, dan file Xcode project.
3. Ekstrak ZIP di Mac.
4. Instal Xcode dari Mac App Store. Buka `ios/PBSBalap.xcodeproj`.
5. Di Xcode → Settings → Accounts, masuk dengan Apple ID tadi.
6. Pilih target `PBSBalap` → Signing & Capabilities.
   Centang **Automatically manage signing** dan pilih **[akun] (Personal Team)**.
7. Jika Xcode mengatakan Bundle Identifier sudah dipakai, ubah dari
   `id.pbsgroup.pbsbalap` menjadi ID unik misalnya
   `id.pbsgroup.pbsbalap.devpribadi` pada tab General → Identity.
   **Jangan ubah nama tampilan**; tetap PBS Balap.
8. Hubungkan iPhone ke Mac dengan kabel, buka kunci dan setujui Trust.
   Jika diminta, aktifkan Settings → Privacy & Security → Developer Mode.
9. Pilih perangkat iPhone pada device selector Xcode, tekan **Run (▶)**.
   Xcode mengompilasi, menandatangani, lalu memasang aplikasi di iPhone.
10. Bila profil kedaluwarsa setelah 7 hari, jalankan Run kembali dari Xcode.

## GitHub dan data sensitif

GitHub digunakan untuk menyimpan source dan memvalidasi kompilasi.
**Tidak diperlukan koneksi Apple ID ke GitHub**, tidak perlu menyimpan
password Apple di Github Secrets untuk jalur gratis.
Jangan memberikan password Apple ID, kode 2FA, .p12 atau private key ke chat.

Workflow ini hanya menghasilkan **paket Xcode dan build-verification**,
tidak memublikasikan IPA unsigned untuk BetaDrop.

## Jika ingin memasang di banyak iPhone dari jarak jauh
Gunakan Apple Developer Program berbayar dan jalur
Ad Hoc signed IPA/BetaDrop (UDID terdaftar) atau TestFlight.
Lihat workflow signing yang sudah disiapkan pada branch
`pbs-balap-ios-unified-v1.0.4`.

Sumber Apple:
- https://developer.apple.com/help/account/basics/about-your-developer-account/
- https://developer.apple.com/get-started/
