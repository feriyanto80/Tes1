# PBS Yoosee / Gwell API Capability Scanner v1.2.6

Android diagnostic build based cumulatively on v1.2.5. Package: `com.pbsgroup.gwellcloudtest.p2pnative126`; versionCode: `126`; versionName: `1.2.6`. It installs side-by-side with v1.2.5 and earlier diagnostic packages.

## v1.2.6 changes

- Adds DNS resolution diagnostics for the known candidate hosts: `api1.cloudlinks.cn`, `api2.cloudlinks.cn`, `openapi-iot.cloudlinks.cn`, `saas-playback.cloudlinks.cn`, and `vasapi.cloudlinks.cn`.
- Fixes the v1.2.5 inventory summary defect: endpoints whose candidate hosts fail DNS/network resolution are no longer silently counted as `other`; endpoint-level and individual-attempt counters are reported separately.
- Separates DNS lookup failure, connection failure, TLS error, network timeout, HTTP response and scan timeout.
- Keeps the complete endpoint catalog and read-only OPTIONS/HEAD behavior.
- Authenticated API Capability Test now tries `device/getDeviceInfo` first, then `device/infoQuery` with the existing `devId` and `deviceId` fallback parameters if the first route is rejected.
- Reports only the presence and shape of device-status fields. It does not reinterpret undocumented numeric vendor status enums as online/offline.
- Adds a direct entry to the native IoTVideo Live P2P diagnostic screen from API Capability Test.
- Updates application package/version metadata and GitHub Actions build workflow.

## Safety boundaries

- Endpoint Inventory Scan sends only OPTIONS and, for read-only candidates, HEAD.
- Authenticated Device List and Device Info tests use the session created by the user after logging into their own account.
- No credentials, complete Device IDs, raw API response bodies, AccessToken, AppToken, signature, playback URL or private key are included in exported reports.
- No account mutation, binding/unbinding, sharing, firmware update, password change/reset, SMS/email, billing, delete, token generation or secret retrieval is performed by the scanner.
- Cloud Playback and AI Event remain `NOT_TESTED_VENDOR_SIGNER_REQUIRED` until Gwell confirms PBS VAS entitlement and provides a supported third-party signing method.
- Live P2P is a manual SDK test. A returned `register()` call is not authentication proof. Successful live video requires the SDK to reach `STATE_PLAY`, report video dimensions greater than zero, and receive AV data.

## Interpreting Endpoint Inventory

- `DNS_LOOKUP_FAILED`: Android could not resolve the candidate hostname on the current network. This does not prove the service or endpoint has been discontinued.
- `NETWORK_TIMEOUT`, `TLS_ERROR`, and `CONNECTION_ERROR`: transport diagnostics only.
- HTTP 2xx/3xx/401/403/404/405/5xx: the server returned a response to OPTIONS/HEAD; this does not prove the business method, authentication or entitlement.
- `dnsDiagnostics` lists host resolution results without exporting resolved IP addresses.
- `summary` contains endpoint totals and an `attemptSummary` so repeated OPTIONS/HEAD attempts are not confused with endpoint counts.
- `HOST_MAPPING_REQUIRED`: no request was sent because the base host is unknown.

## Run

1. Install `PBS_Gwell_API_Capability_Scanner_v1.2.6.apk`.
2. Enter the AppID/AppToken issued for PBS and log in using an account you own.
3. Use **API Capability Test** to test Device List and Device Info.
4. Use **Buka Uji Live P2P Native** and start a test for the selected Device ID.
5. Use **Endpoint Inventory Scan · HEAD/OPTIONS** to run DNS and route diagnostics.
6. Copy the redacted JSON report and share it for analysis.

Do not share passwords, AppToken, AccessToken, signatures or complete Device IDs.

## Build

GitHub Actions workflow: `.github/workflows/pbs-gwell-api-capability-v1.2.6.yml`. It downloads the upstream GWTimes IoTVideo AAR, builds the debug APK, uploads a workflow artifact, and publishes the APK plus SHA-256 checksum to this branch.

## Licensing

The upstream IoTVideo AAR is a third-party SDK. This experimental compatibility build is for controlled testing. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
