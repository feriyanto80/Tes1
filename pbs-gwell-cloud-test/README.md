# PBS Yoosee v1.3.3 — Android 16 Dynamic Receiver Compatibility

Cumulative release based on v1.3.2. Existing Cloud API/IoTVideo/RTSP/GWell native
code and protective native process isolation are preserved.

## Crash addressed (device: Android API 36)

The v1.3.2 crash report contains:
- `process=GWELL_ISOLATED`
- `java.lang.SecurityException`
- `ContextImpl.registerReceiver` / `IActivityManager.registerReceiverWithFeature`
- It happens **before** native login and before GWell video starts.

Official Android 14 behavior change: apps targeting API 34+ must specify
`Context.RECEIVER_NOT_EXPORTED` or `Context.RECEIVER_EXPORTED` for dynamically
registered receivers that are not exclusively for system broadcasts.

Documentation:
https://developer.android.com/about/versions/14/behavior-changes-14

## Changes in v1.3.3

- Overridden pre-Android-14 `registerReceiver` overloads in both
  `GwellNativeActivity` and `PbsGwellApplication`.
- Invoke the original Android registration first. **Only if** its exception
  specifically mentions missing `RECEIVER_EXPORTED` and
  `RECEIVER_NOT_EXPORTED`, retry using `RECEIVER_NOT_EXPORTED`.
- Does **not** blindly export SDK receivers to other applications.
- Preserves the Android system-only broadcast exception because a successful
  original call is never intercepted.
- A genuine unrelated `SecurityException` continues to fail and gets
  captured by the isolated-process crash ledger.
- Report now includes `receiverCompat=DYNAMIC_RECEIVER_NOT_EXPORTED_RETRY_APPLIED`
  if a legacy receiver registration was repaired.
- Expands safely redacted Java crash trace up to 32 class/method frames,
  so the first vendor class can be identified without exporting exception
  messages, user account, passwords, tokens, or full camera Device ID.
- Retains process isolation `:gwell`, live GWell SDK integration, IoTVideo
  diagnostic, Cloudlinks, camera list, RTSP/ONVIF, and report export.

### What it does not prove

The crash's **exception message** was intentionally excluded from the report.
The receiver export requirement matches the Android OS behavior and the
stack location, but a different type of `SecurityException` is still possible.
This compatibility shim only acts when the OS error specifically contains both
required flag names. Android runtime testing is needed to confirm the crash
is gone. It does not resolve the earlier unrelated IoTVideo error 8003.

Some GWell SDK features might depend on external app broadcasts that cannot
be received by `RECEIVER_NOT_EXPORTED`. If the app launches but callbacks
do not arrive, the vendor must document which broadcast actions need external
senders before assigning an exported receiver and permission protection.

## Test instructions

1. Install `PBS_Yoosee_Native_GWell_v1.3.3.apk` side by side with v1.3.2.
   Distinct applicationId: `com.pbsgroup.gwellcloudtest.p2pnative133gwell`.
2. On the main screen select camera 38819486 and tap **GWell Native
   P2P-Core LIVE**. The stable launcher will open.
3. Tap **Buka GWell Native Player**.
4. If native player opens, verify its form is visible *before entering any
   sensitive credentials*. This tests the original crash.
5. Return to launcher and use **Salin diagnostik & crash terakhir**.
   Look for `receiverCompat`, `state=NATIVE_UI_READY`, and `javaCrash`.
6. Only after UI stability is proven, attempt GWell vendor login with
   the properly authorized AppID, AppToken and APPVersion. Do not enter
   credentials into an untrusted app or share them in diagnostics.
7. If the native process crashes with `javaCrash=NOT_RECORDED`,
   capture native `adb logcat -b crash` output after redacting secrets.

This is an experimental debug APK based on archived GWell SDK binaries.
Successful GitHub compilation is **not** proof of device runtime
compatibility, working P2P authentication, or a live camera stream.
Vendor licensing and current Android support should be validated for
production use.

---

## Historical release notes

## PBS Yoosee v1.3.2 — Native GWell Crash Guard

**Cumulative release**, based on v1.3.1, retaining all Cloudlinks API, IoTVideo, RTSP/ONVIF, and GWell P2P-Core integration.

## Device crash report, 4 November 2026

The user confirmed that Device ID 38819486 was entered successfully, but launching
**GWell Native P2P-Core** on Android 16 immediately displayed "keeps stopping",
before any GWell credentials could be entered.

**Crash root cause is not yet proven without Android crash stack / native tombstone.**
Do not attribute this to IoTVideo error 8003, guest sharing or an invalid APPVersion
until a stack trace supports that conclusion. The archived GWell SDK is from 2019.

## v1.3.2 fix

- Legacy GWell native Activity launches in a **dedicated Android process** via
  `android:process=":gwell"`, so a Java or native fatal failure does not
  directly terminate the main PBS UI process.
- A **stable SDK-free launcher** retains the Device ID and never loads
  `GwellNativeActivity.class` while showing its initial screen.
- Global uncaught Java exception handler writes **exception class and method
  locations only**, omitting messages, tokens, passwords and full device IDs.
  Persisted in app-private storage and displayed from the main process on return.
- Lifecycle breadcrumbs: `NATIVE_START_REQUESTED`, `NATIVE_ACTIVITY_ONCREATE_OK`,
  `NATIVE_UI_READY`, `NATIVE_EXITED`, `JAVA_FATAL_RECORDED`.
  If a native SIGSEGV/abort occurs, this Java handler cannot capture a stack;
  use ADB logcat or Android tombstones, while the main process remains usable.
- IoTVideo SDK initialization is skipped inside the isolated GWell process,
  to avoid initializing two competing SDK engines during native entry.
- No automatic replay, owner modification, credential extraction, or
  unsafe authentication bypass.
- All features and native GWell login/P2P flows from 1.3.0/1.3.1 are retained.

## Install and test

1. Install `PBS_Yoosee_Native_GWell_v1.3.2.apk` (distinct package
   `com.pbsgroup.gwellcloudtest.p2pnative132gwell`; installs side-by-side
   with previous debug APKs, preventing debug signing-key collision).
2. Choose Device ID 38819486 and tap **GWell Native P2P-Core LIVE**.
3. In the stable launcher press **Buka GWell Native Player**.
4. If the native process stops, go back to the launcher and press
   **Salin diagnostik & crash terakhir**. Share the copied text.
5. If it does not stop, follow the GWell login and device-version flow from
   v1.3.0. **AppID, AppToken and APPVersion must be vendor-authorized**.
   Because the GWell engine now runs in a separate process, credentials
   held only in the Cloudlinks main process are not automatically shared;
   enter vendor credentials directly on the native screen.
6. Only claim successful live when the video actually appears.

If the diagnostic shows `javaCrash=NOT_RECORDED` but native process is gone,
a native C/C++ fatal crash may have occurred. On a computer with ADB:

```bash
adb logcat -b crash -d -v time > PBS_GWell_crash.txt
```

Review/redact any private device IDs, credentials, URLs, or tokens in the
ADB output before sharing. Android crashes before the Java handler starts
may also not be recorded.

## SDK sources and limitations

- https://github.com/jwkj/GwellDemo
- https://github.com/GWTimes/GWP2PSDK

Third-party AAR binaries are fetched only by the build workflow, excluded
from the source ZIP. The experimental APK embeds these libraries for test
purposes; production use requires vendor licensing and confirmation of
Android 16 compatibility. **Build success is not proof of functional live
video or native SDK runtime compatibility.**

---

## Historical v1.3.1 README

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
