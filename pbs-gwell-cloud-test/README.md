# PBS Yoosee / Gwell API Capability Scanner v1.2.7

Android diagnostic build based cumulatively on v1.2.6. Package: `com.pbsgroup.gwellcloudtest.p2pnative127diag`; versionCode: `12702`; versionName: `1.2.7`. Installs side-by-side with v1.2.6 and earlier diagnostic packages.

## v1.2.7 changes (updated relationship diagnostic)

- Preserves the v1.2.6 API inventory, DNS diagnostics, authenticated device-list/info tests, and native IoTVideo P2P screen.
- Adds an authenticated, **read-only account-to-camera preflight** using the existing IoTVideo Cloudlinks session: device list plus read-only metadata fallback. List results may be paginated; absence on one page is not definitive evidence of missing access.
- Adds an error 8003 diagnostic hint: GWTimes' published IoTVideo Asrv table identifies 8003 as invalid user-device relationship. Android server-origin equivalence and actual entitlement remain unverified; there is no bypass or implicit binding.
- Adds manual HD and SD P2P tests, independent of automatic changes to owner-sharing permissions. Preflight failures are reported as UNKNOWN/REJECTED and do not masquerade as conclusive offline state.
- Adds MEDIA_VERIFIED only when STATE_PLAY, nonzero dimensions, and measured AV bitrate occur together.
- Adds native P2P session timing markers for SDK register call return, player prepared callback, and first STATE_PLAY callback.
- Captures Android active-network transport and validated-internet state at session start and play request.
- Adds a 25-second watchdog diagnostic when STATE_PLAY is not reached, including session timing and current network state.
- Redacts the Device ID in the copied native P2P diagnostic report.
- Explicitly classifies SDK register-call return as NOT authentication proof. No undocumented authentication method or vendor API is fabricated.

## Important interpretation

The IoTVideo SDK's `IoTVideoSdk.register()` method used by this baseline returns void. Therefore v1.2.7 can measure the call returning and subsequent player callbacks, but cannot independently assert server-side authentication unless the SDK exposes a documented authentication callback or Gwell supplies a supported verification method. A successful live stream is evidenced by STATE_PLAY, non-zero video dimensions, and received AV data.

## Evidence for error 8003

Official GWTimes IoTVideo iOS error header: [IVError.h](https://github.com/GWTimes/IoTVideoSDK-iOS/blob/master/Frameworks/IoTVideo.framework/Headers/IVError.h). It enumerates `IVASrvErr_dst_error_relation = 8003` (invalid relationship). This is **server-level reference evidence**, not proof that PBS's archived Android SDK returned the same category; use read-only preflight and SDK callbacks to confirm.

The prior v1.2.7 APK in this branch used an independently generated debug signing key. This updated diagnostic build has a separate applicationId so it can install side-by-side without Android signature conflict. It does not overwrite an earlier v1.2.7 APK.

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
4. Open **Buka Uji Live P2P Native**; test **Cek relasi akun–kamera** first, then run HD or SD P2P diagnostic. The play buttons repeat the read-only preflight automatically.
5. If SDK_ERROR 8003 appears, inspect AUTH_PREFLIGHT_RESULT. If account/device relationship is not authorized, use the official Yoosee owner-share/binding workflow or request vendor confirmation. Never force-bind a camera.
6. Copy the redacted diagnostic report and share it for analysis.

Do not share passwords, AppToken, AccessToken, signatures, or complete Device IDs.

## Build

GitHub Actions workflow: `.github/workflows/pbs-gwell-api-capability-v1.2.7.yml`. It downloads the upstream GWTimes IoTVideo AAR, builds the debug APK, packages the complete project source (third-party AAR excluded), uploads workflow artifacts, and publishes APK, source ZIP and their SHA-256 checksums to this branch.

## Licensing

The upstream IoTVideo AAR is a third-party SDK. This experimental compatibility build is for controlled testing. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
