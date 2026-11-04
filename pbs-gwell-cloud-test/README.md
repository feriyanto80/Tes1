# PBS Yoosee P2P v1.2.8 — Dual Backend Diagnostic

**Release type:** cumulative Android diagnostics package, derived from v1.2.7.
**Android package:** `com.pbsgroup.gwellcloudtest.p2pnative128diag`
**versionCode:** `12800` — **versionName:** `1.2.8`.

## Purpose

This release investigates why a camera that streams in the vendor Yoosee app can
still return error 8003 when PBS attempts to play with an archived
`com.tencentcs.iotvideo` SDK.

**Important: Dual Backend Diagnostic is NOT dual-backend live streaming.**
The existing IoTVideo SDK is included for actual HD/SD live tests. The vendor's
separate legacy GWell `com.p2p.core` runtime, compatible native binaries and
the proprietary GWell session/verification API are not included. Consequently,
the legacy side performs **read-only assessment only** and explicitly reports
`NOT_PACKAGED_NO_LEGACY_P2P_LIVE` instead of simulating successful playback.
This prevents a misleading 'fix' that bypasses vendor authentication.

## v1.2.8 additions

- **Diagnostik Dual Backend (IoTVideo / GWell)** button on the Native P2P screen.
- Metadata extraction from the authenticated device list, with a
  `device/getDeviceInfo` read-only fallback. Sanitized `relationCode`,
  `deviceCate`, and recognized protocol hints are reported, but no raw API
  response is copied.
- Explicit separation of **account listing**, **server authorization**,
  **backend selection**, **P2P session state**, and **media verification**.
- Legacy GWell capability assessment: older numeric-ID *heuristic*,
  first-generation `deviceCate` *heuristic*, optional explicit protocol hint,
  availability of legacy `com.p2p.core.P2PHandler`, presence of the official
  Yoosee app, and DNS-only diagnostics for known candidate infrastructure.
- Confidence levels distinguish explicit backend hints from heuristics:
  numeric Device IDs of eight digits or fewer **do not prove GWell Legacy**.
- SDK legacy credentials `UserID`, `SessionID1`, `SessionID2`, verification
  codes are deliberately not fabricated from IoTVideo AccessID/AccessToken.
- The v1.2.7 read-only relationship preflight, HD/SD IoTVideo player callbacks,
  timing, and media verification are retained.
- Existing API inventory, Cloud API login, device list and info tests,
  RTSP/ONVIF alternative, and credential redaction are retained.

## Interpreting `8003`

GWTimes IoTVideo defines ASrv code `8003` as
`IVASrvErr_dst_error_relation` ("invalid relationship chain") in its publicly
available [error header](https://github.com/GWTimes/IoTVideoSDK-iOS/blob/master/Frameworks/IoTVideo.framework/Headers/IVError.h).
That is evidence for a **possible** account/device/namespace rejection; the
specific Android SDK callback's origin and backend equivalence have not been
verified for the PBS camera.

A camera being `LISTED` in authenticated Cloudlinks API responses does **not**
prove that the IoTVideo P2P server recognizes playback rights.

The legacy GWell developer example
[dxsdyhm/GwellDemo](https://github.com/dxsdyhm/GwellDemo)
uses a different account/session flow and an old `p2p-core` artifact (0.4.4,
2018). That dependency's compatible, licensed native runtime has not been
verified for modern Android. It is **not** copied into this package.

## Usage

1. Install the **v1.2.8 APK** from `releases/`. New package identity permits
   side-by-side diagnostic installation without Android debug-signature conflict.
2. Log in using **your own valid** GWell/Yoosee AppID, AppToken, and account.
3. Select the camera from the authenticated device list.
4. Open **Native Live P2P** and tap **Diagnostik Dual Backend**.
5. Read `BACKEND_ASSESSMENT`, `confidence`, `legacyRuntime`,
   `legacyCredentials`, `legacyDns` — the legacy button does not attempt live.
6. Tap **Diagnostik Live P2P HD** or **SD** to test the bundled IoTVideo SDK
   independently. Verify `STATE_PLAY`, non-zero dimensions and AV throughput.
7. Tap **Salin Hasil Uji** to copy the redacted diagnostic. The report masks
   the Device ID and never exports credentials.

Do not reset the camera, change its owner, share it, or automatically bind/unbind
devices as a troubleshooting shortcut. Consult the vendor if a legitimate
relationship is rejected.

## Building

`./gradlew` is not shipped; GitHub Actions uses Gradle 8.10.2 directly.

```bash
gradle -p pbs-gwell-cloud-test :app:assembleDebug --no-daemon --stacktrace
```

Before running Gradle locally, supply the **properly licensed** GWTimes
IoTVideo AAR as `pbs-gwell-cloud-test/app/libs/iotvideo-release.aar`.
GitHub Actions temporarily retrieves the publicly available *archived demo*
AAR referenced by the original v1.2.6 project; the AAR is not redistributed
in this source ZIP. For commercial or production use, obtain a supported vendor
SDK and authorization.

Build workflow:
`.github/workflows/pbs-gwell-dual-backend-v1.2.8.yml`.

It publishes the diagnostic APK, the cumulative source ZIP (without
vendor binaries and historical APK files), and SHA-256 checksums to this
release branch.

## Limitations

- No tested GWell Legacy live-session implementation without the vendor SDK.
- DNS resolution is infrastructure evidence, not an authenticated P2P handshake.
- Vendor relation enums and backend hints must be confirmed with product API docs.
- A "camera listed" state is not an authorization grant.
- No hardware end-to-end verification is possible from GitHub Actions.
- Use for controlled technical diagnostics only.
