# PBS Yoosee API Capability Test v1.0.0 / Native P2P v1.2.3

Cumulative Android diagnostic build based on PBS Native P2P v1.2.2. Package: `com.pbsgroup.gwellcloudtest.p2pnative123`; versionCode: 1; versionName: `1.2.3`. It installs side-by-side with v1.2.2 and earlier test apps.

## New API Capability Test
The new screen runs read-only diagnostics using the currently logged-in account session:
- Confirms session/AppID/AppToken presence without revealing their values.
- Calls the existing authenticated Device List endpoint and reports HTTP/API code, latency and device count.
- Calls the existing authenticated Device Info endpoint for a selected Device ID (or first listed device) and reports only status/field names, not raw device metadata.
- Sends HTTPS HEAD probes to the OpenAPI base and Cloud Playback AI Event/Playback routes. These probes establish only TLS/HTTP reachability; they do not prove API authentication or entitlement.
- Explicitly marks AI Event and Cloud Playback authorization as NOT_TESTED until Gwell provides a supported third-party signing method and confirms VAS entitlement.
- Copies a redacted JSON report. It excludes passwords, AppID/AppToken values, AccessId/AccessToken, signatures, raw response bodies, playback URLs, and full Device ID.

## Why AI Event / Playback are not sent yet
The public `camera-hack` scripts call the v2 Cloudlinks APIs using a Frida signing oracle attached to a running Yoosee app. The sample values in that script are not PBS credentials. This build does not extract credentials from another app, reuse sample secrets, or send unsigned VAS requests. AppID/AppToken alone do not establish VAS entitlement or provide a supported signing interface.

The VAS routes remain explicitly marked NOT_TESTED. Ask Gwell for:
1. Confirmation that PBS AppID has VAS AI Event and Cloud Playback permission.
2. Supported third-party signing/authentication specification for `/openapi/vpaasPlayback/vas/aievent/list` and `/openapi/vpaasPlayback/vas/playback/play`.
3. Required product/package binding, request headers/body, pagination, rate limits and HLS URL lifetime.

## Run
1. Keep v1.2.2 installed; v1.2.3 is a separate package.
2. Install `PBS_Gwell_API_Capability_Test_v1.2.3.apk`.
3. Login using the account and AppID/AppToken authorized for PBS.
4. Select a Device ID or leave it blank to use the first listed camera.
5. Open **API Capability Test** and tap **Jalankan API Capability Test**.
6. Tap **Salin laporan JSON (redacted)** and share the report for analysis.

The test does not modify device settings, bind/unbind devices, control PTZ, request HLS playback, or download recordings.

## Build
GitHub Actions workflow: `.github/workflows/pbs-gwell-api-capability-v1.2.3.yml`.
The workflow downloads the upstream GWTimes IoTVideo AAR, builds the APK, uploads an Actions artifact, and publishes the direct APK plus SHA-256 checksum.

## Licensing
The upstream IoTVideo AAR is a third-party SDK. This experimental compatibility build is for controlled testing. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
