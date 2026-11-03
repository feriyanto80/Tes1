# PBS Gwell Native P2P v1.2.1 — diagnostic cumulative build

Full Android APK release based on v1.2.0. Package is `com.pbsgroup.gwellcloudtest.p2pnative121`, versionCode 1, versionName 1.2.1. It installs side-by-side with v1.2.0, avoiding Android signature conflict because GitHub-hosted runners generate a fresh debug signing key for each build.

## v1.2.1 changes
- Initialize IoTVideo SDK once from `PbsGwellApplication`, before player screen.
- Select the SDK demo's RELEASE server environment.
- Enable SDK internal debug logging in app-private files directory `files/iotvideo-sdk-logs`.
- Decode SDK player states by comparing the official SDK constants: INITIALIZED, PREPARING, READY, LOADING, PLAY, PAUSE, STOP, IDLE.
- Add 25-second watchdog and player metrics: SDK state, isPlaying, video dimensions and AV bytes/sec.
- Safe diagnostic report contains event timeline, init status, callback error code and player metrics; it excludes password, AppToken, accessToken and raw SDK logs.
- Existing Gwell Cloud login/device inventory and RTSP/ONVIF alternative retained.

## Authentication compatibility still needs confirmation
The public GWTimes IoTVideo demo obtains its `accessId/accessToken` through its own IoTVideo AccountMgr login flow and initializes its product/server environment. This app currently supplies the session from the existing Cloudlinks/Yoosee OpenAPI login. Matching field names and token length do **not** prove the token belongs to the same IoTVideo account/product namespace. The SDK `register()` method returns no success result in this integration, so `REGISTER_CALL_RETURNED` is not proof of backend authentication. If the 25-second watchdog fires or an SDK error appears, obtain the correct IoTVideo/Gwell partner SDK credentials and product configuration from the vendor; do not try arbitrary tokens or credentials.

The SDK log is stored privately on the device and may contain sensitive vendor details. The app deliberately does not export raw SDK logs. Share only the on-screen diagnostic report copied by the button.

## Test
1. Keep v1.2.0 installed. Install v1.2.1 as a separate diagnostic app; no uninstall is required.
2. Login to the same account and select an owned Device ID.
3. Disable Wi-Fi and use mobile data to test remote P2P.
4. Open Native Live P2P, press Start and wait at least 25 seconds.
5. Press `Salin Hasil Uji (Tanpa Token)` and paste the report into ChatGPT.
6. A real stream requires both SDK `STATE_PLAY` and visible camera frames. `register()` returning without throwing is not enough.

## SDK licensing
The upstream IoTVideo AAR is an archived third-party SDK. This experimental APK is for controlled compatibility testing only. Obtain written redistribution/commercial-use permission and the correct supported SDK package from the rights holder before production deployment.
