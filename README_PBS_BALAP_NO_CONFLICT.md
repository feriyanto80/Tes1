# PBS Balap Forms v1.0.4 — Install tanpa menghapus PBS Balap lama

## Penyebab gagal update
PBS Balap v1.0.2 dan v1.0.3 sama-sama memiliki applicationId
`id.pbsgroup.pbsbalap`, tetapi sertifikat debug GitHub berbeda.
Android menolak update jika sertifikat tidak sama.

Diperiksa langsung dari paket APK:
- v1.0.2 signer SHA-256: ae908be2eeaa0ad9a7f82784a702a3480b5e66fb8c60d7c8c00288baa844c4e9
- v1.0.3 signer SHA-256: a108b2981cdd25b9fb44c48043d073eff432c792957d4393777efadc99a94696

## Solusi sekarang
v1.0.4 menggunakan package ID BARU `id.pbsgroup.pbsbalap.forms`
(label launcher `PBS Balap Forms`), sehingga **bisa diinstal bersama v1.0.2**
tanpa perlu menghapus aplikasi sebelumnya.

Logo resmi PBS sama seperti v1.0.2. Aplikasi mencakup:
- Beranda (dua kartu form)
- BALAP Digital — https://balap.pbsgroup.id/
- BA Survey Digital — https://basl.pbsgroup.id/
- GPS, kamera, pemilihan file, download, navigasi, responsif dari v1.0.3

**PENTING:** Ini aplikasi baru yang bisa diinstal berdampingan, bukan update
in-place. Cookies, status login, draft, dan data WebView tidak otomatis
berpindah dari aplikasi lama. Form yang tersimpan di server tetap di sistem masing-masing.

## Agar update versi 1.0.5 dst TIDAK bentrok
Paket release ke depan harus SELALU memakai private signing key yang sama.
Jangan mendistribusikan ulang debug APK dari GitHub Actions yang membuat
keystore sementara per runner karena setiap build dapat berbeda.

Opsi:
1. Buat keystore release sendiri secara lokal/Android Studio.
2. Simpan file keystore, alias, password sebagai GitHub Actions *secrets*
   (jangan push ke repository publik).
3. Tambahkan signingConfig release dalam Gradle yang membaca secrets dari env.
4. Build APK release menggunakan signingConfig tersebut.
5. Gunakan keystore yang sama untuk SEMUA versi berikutnya.
6. Untuk migrasi dari versi 1.0.4 DEBUG ke release signed, itu tetap tidak
   dapat diupdate in-place kecuali kunci signing-nya sama; rencanakan release
   production ber-key permanen SEBELUM pengguna memasang versi production.

Untuk memperbarui aplikasi LAMA secara langsung, diperlukan PRIVATE KEY dari
sertifikat v1.0.2. APK lama hanya menyimpan public cert/signature, bukan private
key, sehingga key tidak dapat diambil dari APK.

## GitHub
Branch: pbs-balap-unified-no-conflict-v1
Workflow: Build PBS Balap Forms No-Conflict APK
