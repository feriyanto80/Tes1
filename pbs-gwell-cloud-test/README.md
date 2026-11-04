# PBS Yoosee v1.3.1 — Native GWell Launcher & P2P-Core Integration

Cumulative Android source based on PBS Yoosee v1.3.0, retaining the v1.2.9 features. Built for diagnostic/research only.

## v1.3.1 button not opening: root cause and resolution

In v1.3.0 the button listener called `openGwellNative(deviceId.getText().toString())`.
When no device had been selected, `openGwellNative()` simply wrote
"Pilih Device ID kamera dahulu" to a status field at the very bottom of the
main screen, then returned. The screen did not navigate and no toast appeared.
This is a verified source-level UX defect. It may not explain separate
device-side crashes when the old native Activity did launch.

v1.3.1 corrects this by routing all GWell entry points through
`GwellLauncherActivity`, a pure Android Activity that does NOT instantiate
the legacy SDK until the user presses a separate button.

- Initial GWell button now works whether the Device ID is filled or empty.
- The launcher's Device ID field is prefilled with the selected camera when
  provided. Otherwise enter the Device ID on the launcher.
- Cards in the authenticated device list offer **GWell P2P-Core (pilih kamera)**.
- Empty or invalid IDs display an immediate toast and inline status.
- Explicit Activity registration is verified before launching the native
  player; missing class, Java linkage error and synchronous launch exception
  are separately reported without copying sensitive exception messages.
- The Native GWell player remains the same v1.3.0 login/session monitor
  implementation. Successful build is NOT proof that GWell server login,
  guest rights, or streaming succeed.
- Installation uses distinct application ID
  `com.pbsgroup.gwellcloudtest.p2pnative131gwell` to avoid signing conflict
  with previous GitHub Actions debug builds; the two apps coexist.
- No change to camera owner/share, no credential capture from official Yoosee.

**Test the launcher first**: click GWell Native P2P-Core LIVE from the main
PBS screen with the Device ID field empty. The new launcher must open. Type
`38819486`, then press **Buka GWell Native Player**. If the native page
still crashes after launching, capture a sanitized Android crash stack
and share only class/method/exception information, never tokens/passwords.


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
  **APPVersion** `XX.XX.XX.XX`; Android `versionName=1.3.1` is unrelated.
  The SDK may also bind credentials to the registered package name.

## Camera 38819486: what is known

Official Yoosee shows guest live on the same account/phone/network.
Cloudlinks shows `relationCode=2`, device listed. PBS IoTVideo shows error
`8003` before frames even for HD and SD.

The GWell SDK is a different backend/session flow: Cloudlinks
`AccessID/AccessToken` MUST NOT be blindly reused as GWell native
`SessionID1/2` and `P2PVerifyCode1/2`.

## Test

1. Install `PBS_Yoosee_Native_GWell_v1.3.1.apk` alongside v1.2.9.
2. Choose **GWell Native P2P-Core LIVE (eksperimental)** on the main screen.
   The launcher opens even if the main-page Device ID was empty. Enter or
   verify Device ID `38819486` and press **Buka GWell Native Player**.
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

Workflow: `.github/workflows/pbs-yoosee-native-gwell-v1.3.1.yml`.
Android package: `com.pbsgroup.gwellcloudtest.p2pnative131gwell`.
versionCode: 13100; versionName: 1.3.1.

## Sources and references

- https://github.com/jwkj/GwellDemo (Android P2P 0.5.4 AAR, 2019)
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/com/example/dansesshou/jcentertest/LoginActivity.java
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/com/example/dansesshou/jcentertest/MonitorActivity.java
- https://github.com/jwkj/GwellDemo/blob/master/app/src/main/java/service/MainService.java
- https://github.com/GWTimes/GWP2PSDK (vendor AppID/AppToken/version restrictions)

Experimental release scope: this implements a native GWell call path, but
neither confirms server auth nor guarantees first frame for the user's
camera until real-device tests pass.
