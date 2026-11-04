# PBS Yoosee v1.2.9 — Guest P2P Authentication & Backend Compatibility

**Cumulative Android diagnostic release**, based on v1.2.8.
Package: `com.pbsgroup.gwellcloudtest.p2pnative129diag`.
Version Code: `12900`, Version Name: `1.2.9`.
Installs **side by side** with v1.2.8 (different package ID).

## Confirmed user observation (not hard-coded into the app)

Camera 38819486 is shared to a **guest** account. The **same account**
successfully plays live video through official Yoosee on the same phone/network,
but the PBS IoTVideo SDK returns `8003`. Device list `relation=2`
is consistent with a guest-like relation in the [GWTimes demo](https://github.com/GWTimes/IoTVideo-Android/blob/master/app/src/main/java/com/tencentcs/iotvideodemo/accountmgr/devicemanager/DeviceListFragment.java).
The exact Cloudlinks API enum semantics are not independently confirmed.

**Conclusion:** Guest access must **not** automatically be classified as
invalid. A successful official Yoosee guest stream does not automatically
authorize a separately registered IoTVideo client/app product namespace.

## Changes from v1.2.8

- Adds **Guest Auth & Backend Compatibility** button and a user-controlled
  confirmation checkbox: "same account + shared/guest camera + official
  Yoosee live works." This field is not assumed true without the user's
  confirmation on the device.
- Adds `GuestP2PCompatibilityDiagnostic`, an offline/read-only guest-session
  compatibility analyzer. Compares Cloud API listing / relationship codes,
  official Yoosee installation, PBS application package, validity **shape**
  of access ID and in-memory token (never token contents), AppID presence,
  and the **availability of public SDK registration status APIs**.
- SDK API introspection examines method names and signatures only; it does
  not invoke undocumented registration endpoints or inspect official
  Yoosee's private process data.
- Specifically classifies `errorCode=8003` when a guest relation is listed
  and the user confirms official same-account playback, identifying a
  **client/P2P authorization or backend mismatch requiring vendor confirmation**,
  *not* denying guest sharing rights.
- Retains read-only authenticated device-list and info-query preflight,
  verified HD/SD IoTVideo session callbacks, network metrics, first-frame
  evidence, and the v1.2.8 Legacy GWell family/runtime/DNS diagnostic.
- Downgrades short-numeric-Device-ID-only results to
  `UNCONFIRMED_BACKEND_LEGACY_ID_HINT` so the 8-digit heuristic is never
  portrayed as definitive backend discovery.
- Improves Android status/navigation safe-area padding on the native
  diagnostic screen.
- Preserves API capability inventory, endpoint probes, Cloud access checks,
  existing RTSP/ONVIF alternative, and redacted report export.

## Interpretation of code 8003

The [GWTimes IoTVideo error header](https://github.com/GWTimes/IoTVideoSDK-iOS/blob/master/Frameworks/IoTVideo.framework/Headers/IVError.h)
defines ASrv error `8003` as invalid device/user relationship, but exact
mapping of the archived Android player callback for the PBS device has not
been proven. For a working guest account, possible causes include a
different product namespace, IoTVideo permission/guest scope, token format
or transport/backend. **No one cause has been proven by the v1.2.8 log.**

GWell's legacy P2P uses a different session and verification flow
([GwellDemo](https://github.com/dxsdyhm/GwellDemo)).
The 2018 `com.p2p.core` SDK is not included; a supported, properly licensed
native SDK and documented session provider are necessary before PBS can
attempt **actual Legacy GWell live video**. The v1.2.9 GWell diagnostic
remains **read-only**. DNS resolution or camera listing does not prove
a native session.

## Install and use

1. Install `PBS_Yoosee_Guest_P2P_Compatibility_v1.2.9.apk`.
2. Log into PBS with **authorized** Yoosee/GWell AppID/AppToken and the same
   Yoosee account. Do not paste credentials into reports or messages.
3. Select Device ID 38819486 and open Guest P2P Diagnostic.
4. If you personally verified official Yoosee live using the **same account**,
   select the checkbox on the screen. It is OFF by default.
5. Press **Uji Guest Auth & Backend Compatibility**. Inspect
   `GUEST_P2P_COMPAT`, `AUTH_PREFLIGHT_RESULT`,
   and `BACKEND_ASSESSMENT`. Copy the redacted report.
6. For an independent IoTVideo reproduction, press HD or SD Live Test and
   export a second report. `REGISTER_CALL_RETURNED` is **not**
   proof of server login. `MEDIA_VERIFIED` requires PLAY, dimensions,
   and AV bytes.
7. Never unbind, reset, or change camera ownership as a diagnostic shortcut.

## Build

GitHub workflow:
`.github/workflows/pbs-yoosee-guest-p2p-v1.2.9.yml`.
It fetches the archived publicly available GWTimes demo IoTVideo AAR as a
build-time dependency, compiles with Android SDK 35 and Gradle 8.10.2,
publishes APK and cumulative source ZIP with SHA-256. **Third-party SDK
binaries are not redistributed in the source ZIP**. Production/commercial
deployment requires vendor SDK and licensing confirmation.

Locally, install a properly licensed compatible
`pbs-gwell-cloud-test/app/libs/iotvideo-release.aar`, then run:

```bash
gradle -p pbs-gwell-cloud-test :app:assembleDebug --no-daemon --stacktrace
```

The GitHub Action creates a debug-signed APK. Because package identity
changes, v1.2.9 is a separate diagnostic installation; it does not claim
to be an in-place update signed with an existing private release key.

## Scope and limits

**Implemented:** guest-aware evidence, safe session-shape checks,
app/backend differential diagnosis, SDK method-availability inspection,
redacted reporting, existing IoTVideo playback test.

**Not implemented:** taking live video via GWell Legacy; extracting
third-party app tokens; impersonating official Yoosee app identity;
bypassing owner/guest authorization; proving server-side SDK entitlement
without vendor API; hardware test on the user's own camera.

No raw API response bodies, complete Device IDs, passwords,
AccessTokens, AppTokens, signatures, private keys, or official Yoosee
internal data are included in exported diagnostic reports.
