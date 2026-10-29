# PBS Balap Android v1.0.2

Application: **PBS Balap**
Package: `id.pbsgroup.pbsbalap`
Home URL: https://balap.pbsgroup.id/

This cumulative version retains the existing WebView, Google Apps Script location permission checks,
upload, camera, and download support. The icon is derived from the genuine PBS logo supplied by
the user in this conversation (without the small footer text, which is illegible on launchers).
The source asset is `pbs-mobile/assets/pbs-official-logo.webp.b64`; build-time script
generates PNG icons.

## Responsive changes
- Landscape and compact-screen native toolbar that keeps the current WebView alive while rotating.
- Responsive WebView viewport / overview scaling and pinch zoom.
- Small, non-destructive CSS additions on `balap.pbsgroup.id` only; cannot restyle cross-origin
  Google Apps Script iframes or repair website-owned fixed-width HTML automatically.

## GPS boundaries
Android native GPS permissions are requested only for trusted BALAP / Google Apps Script origins.
Web pages inside Google Apps Script iframes can still fail to access GPS if the parent site does
not delegate the geolocation feature via Permissions-Policy / iframe allow attribute. Fix the
site's iframe settings if the same error recurs.

## Installation
Built through GitHub Actions, download artifact `PBS_Balap_v1.0.2_APK`.
The package name changed from `id.pbsgroup.pbsmobile` to `id.pbsgroup.pbsbalap`, so
PBS Balap can be installed alongside the old PBS Mobile build. Old PBS Mobile can be
uninstalled separately after verifying the new app.

This GitHub Actions artifact is an internal **debug** APK; signing may vary across builds.
