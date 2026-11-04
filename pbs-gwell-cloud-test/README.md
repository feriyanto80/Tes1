# PBS Yoosee / Gwell API Capability Scanner v1.2.7

Android diagnostic build based cumulatively on v1.2.6. Package: `com.pbsgroup.gwellcloudtest.p2pnative127`; versionCode: `127`; versionName: `1.2.7`. Installs side-by-side with v1.2.6 and earlier diagnostic packages.

## v1.2.7 changes

- Preserves the v1.2.6 API inventory, DNS diagnostics, authenticated device-list/info tests, and native IoTVideo P2P screen.
- Adds native P2P session timing markers for SDK register call return, player prepared callback, and first STATE_PLAY callback.
- Captures Android active-network transport and validated-internet state at session start and play request.
- Adds a 25-second watchdog diagnostic when STATE_PLAY is not reached, including session timing and current network state.
- Redacts the Device ID in the copied native P2P diagnostic report.
- Explicitly classifies SDK register-call return as NOT authentication proof. No undocumented authentication method or vendor API is fabricated.

## Important interpretation

The IoTVideo SDK's `IoTVideoSdk.register()` method used by this baseline returns void. Therefore v1.2.7 can measure the call returning and subsequent player callbacks, but cannot independently assert server-side authentication unless the SDK exposes a documented authentication callback or Gwell supplies a supported verification method. A successful live stream is evidenced by STATE_PLAY, non-zero video dimensions, and received AV data.

## Safety boundaries

- Endpoint Inventory Scan sends only OPTIONS and read-only HEAD requests.
- Authenticated Device List and Device Info tests use the session created by the user after logging into their own account.
- Reports do not include passwords, AppToken, AccessToken, signatures, playback URLs, private keys, raw SDK logs, or complete Device IDs.
- No account mutation, binding/unbinding, sharing, firmware update, password change/reset, SMS/email, billing, delete, token generation or secret retrieval is performed.
- Cloud Playback and AI Event remain `NOT_TESTED_VENDOR_SIGNER_REQUIRED` until Gwell confirms PBS VAS entitlement and provides a supported third-party signing method.

## Run

1. Install `PBS_Gwell_API_Capability_Scanner_v1.2.7.apk`.
2. Enter the AppID/AppToken issued for PBS and log in using an account you own.
3. Use **API Capability Test** to test Device List and Device Info.
4. Open **Buka Uji Live P2P Native**, then start the test for the selected camera.
5. Copy the redacted diagnostic report and share it for analysis.

Do not share passwords, AppToken, AccessToken, signatures, or complete Device IDs.

## Build

GitHub Actions workflow: `.github/workflows/pbs-gwell-api-capability-v1.2.7.yml`. It downloads the upstream GWTimes IoTVideo AAR, builds the debug APK, uploads a workflow artifact, and publishes the APK plus SHA-256 checksum to this branch.

## Licensing

The upstream IoTVideo AAR is a third-party SDK. This experimental compatibility build is for controlled testing. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
