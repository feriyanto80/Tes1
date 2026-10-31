# PBS Balap v1.0.3 — Unified Digital Forms (Android)

Branch: pbs-balap-unified-android-v1

## App
- Name: PBS Balap
- Package: id.pbsgroup.pbsbalap
- Version: 1.0.3 / versionCode 4
- Logo: same official PBS logo asset as v1.0.2, regenerated from pbs-mobile/assets/pbs-official-logo.webp.b64.
- Android minimum: 8.0.

## Navigation
The native home page has two independent, active form modules:
1. BALAP Digital — https://balap.pbsgroup.id/
2. BA Survey Digital (BASL) — https://basl.pbsgroup.id/

A bottom navigation bar provides Beranda, BALAP, BA Survey.
Each form uses its own WebView. Switching forms preserves loaded web page state
until the Android process is terminated. App state restoration may not preserve
form elements that are not stored by the respective websites.

## Existing features
Retains Android GPS permission diagnostics and origin allowlisting for
PBS + Google Apps Script, camera/photo/file upload, downloads, and PDF support
that is provided by the websites, Android responsive toolbar, rotation support,
TLS validation, and scoped network security.

IMPORTANT: BALAP previously used an HTTP URL. The new app tries HTTPS by
default; if loading fails, the user may deliberately select HTTP from a warning
screen. HTTP is only permitted for balap.pbsgroup.id. HTTP is NOT encrypted.
For GPS and privacy, HTTPS with valid certificate is recommended.

The app does not modify or migrate website data. Original forms remain hosted
at the PBS domains.

## Install
Build with GitHub Actions: Build PBS Balap Unified APK. Download the APK artifact.
This debug APK is automatically signed by GitHub's runner. A new GitHub run may
use a different signing key from v1.0.2, and Android then requires uninstalling
v1.0.2 before installing v1.0.3. That erases local app data including WebView
cookies and unsent form entries; save form data first.

If GPS in Google Apps Script iframe fails, the website needs geolocation
permission delegation via iframe allow and appropriate Permissions-Policy.
Native APK permissions cannot bypass cross-origin browser restrictions.

## Scope
'Riwayat' and 'Profil' mockup items were deliberately NOT included because no
backend/API requirements were supplied. Bottom navigation has only working tabs.


## No-conflict distribution 1.0.4
This branch ships a separate Android application package `id.pbsgroup.pbsbalap.forms` with display name `PBS Balap`. This can coexist with the original `id.pbsgroup.pbsbalap`, without uninstalling it, but DOES NOT in-place update or transfer local WebView data or sessions from the old app. The original v1.0.2 and v1.0.3 had different ephemeral GitHub-run debug signing keys. Android requires the private key used to sign the previously installed app to update that app in place. For all future updates, create and securely retain one signing key as GitHub Actions Secrets; auto-generated per-run debug keys are not suitable for continued distribution.
