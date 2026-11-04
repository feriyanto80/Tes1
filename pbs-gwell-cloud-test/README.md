# PBS Yoosee v1.3.0 — Native GWell P2P-Core Integration

Cumulative Android source based on PBS Yoosee v1.2.9. Built for diagnostic/research only.

## What changed

- **Actual GWell P2P-Core Android SDK integration** (archived 2019 version)
  based on the vendor sample at https://github.com/jwkj/GwellDemo .
- An independently accessible Native GWell screen (not only DNS/read-only).
  Auth flow: GWell SDK `P2PSpecial.init(app, AppID, AppToken, APPVersion)`,
  `HttpSend.login` to obtain **GWell-specific** UserID, SessionID1/2,
  P2PVerifyCode1/2; `P2PHandler.p2pInit` and `p2pConnect()`; get
  device status and P2P protocol version, `MediaPlayer.setP2PLibVersion`,
  `P2PHandler.call`, `vAccept`, `vConnectReady`, `P2PView`, native PTS.
- Maintains the earlier **IoTVideo HD/SD diagnostics, user/guest preflight,
  Cloud API list/detail, API capability, endpoint scan, RTSP/ONVIF**.
- Distinguishes *SDK init*, *vendor login accepted*, *P2P registered*,
  *protocol version received*, *monitor signaling accepted*, *monitor ready*,
  and *actual video timing callback*. None of the earlier states proves video.
- No default passwords, no copying demo AppID/AppToken, no bypass of guest
  permissions, no unbind/reset, no session secrets in report.
- No session persistence: vendor account password, device monitor password,
  session IDs and verify codes are held only during the live process.
- The native SDK requires a **vendor registered** AppID/AppToken and
  **APPVersion** `XX.XX.XX.XX`; Android `versionName=1.3.0` is unrelated.
  The SDK may also bind credentials to the registered package name.

## Camera 38819486: what is known

Official Yoosee shows guest live on the same account/phone/network.
Cloudlinks shows `relationCode=2`, device listed. PBS IoTVideo shows error
`8003` before frames even for HD and SD.

The GWell SDK is a different backend/session flow: Cloudlinks
`AccessID/AccessToken` MUST NOT be blindly reused as GWell native
`SessionID1/2` and `P2PVerifyCode1/2`.

## Test

1. Install `PBS_Yoosee_Native_GWell_v1.3.0.apk` alongside v1.2.9.
2. Select Device ID 38819486 in main PBS screen; choose **GWell Native
   P2P-Core LIVE (eksperimental)**.
3. Enter **AppID**, **AppToken**, and **APPVersion** assigned by the GWell
   vendor for the actual PBS application and an authorized guest Yoosee
   account/password. The existing Cloudlinks AppID/AppToken can be
   prefilled but **may not be entitled** to this native GWell package.
4. Enter the **device monitoring password** supplied by the camera owner.
   It is never guessed, logged, or stored.
5. Tap **Init GWell SDK & login akun**, watch for `GWELL_LOGIN_OK` and
   `GWELL_P2P_CONNECTED`. Even a successful connection isn't live proof.
6. Tap **Periksa P2P Device & Versi**. Wait for
   `GWELL_DEVICE_VERSION_VERIFIED` from the SDK. This sets the P2P
   protocol generation for the selected camera.
7. Tap **Mulai Live GWell P2P**; inspect `GWELL_V_ACCEPT`,
   `GWELL_MONITOR_READY`, `GWELL_VIDEO_PTS_RECEIVED` and the video.
8. Tap **Salin laporan**; report contains only redacted, safe diagnostics.

Without valid GWell credentials and device monitor password, live remains
unverified. Never reset/transfer/unbind the working official camera.

## Build / dependency scope

This is a source-cumulative release, **not a standalone redistributable
license** for third-party native binaries. The workflow fetches the publicly
available archived sample dependencies temporarily:
- `GWTimes/IoTVideo-Android/app/libs/iotvideo-release.aar`
- `jwkj/GwellDemo/app/libs/p2p-core-release.aar`
- `jwkj/GwellDemo/app/libs/libhttp-release.aar`

These SDK binaries are NOT included in the source ZIP; they are included
in the diagnostic debug APK. Validate redistribution/commercial licensing
with the vendor before use in production. Android 16/runtime/firmware and
server compatibility are **not guaranteed by a successful build**.

Workflow: `.github/workflows/pbs-yoosee-native-gwell-v1.3.0.yml`.
Android package: `com.pbsgroup.gwellcloudtest.p2pnative130gwell`.
versionCode: 13000; versionName: 1.3.0.

## Sources and references

- https://github.com/jwkj/GwellDemo (Android P2P 0.5.4 AAR, 2019)
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/com/example/dansesshou/jcentertest/LoginActivity.java
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/com/example/dansesshou/jcentertest/MonitorActivity.java
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/service/MainService.java
- https://github.com/GWTimes/GWP2PSDK (vendor AppID/AppToken/version restrictions)

Experimental release scope: this implements a native GWell call path, but
neither confirms server auth nor guarantees first frame for the user's
camera until real-device tests pass.
