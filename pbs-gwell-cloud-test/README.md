# PBS Gwell Native P2P v1.2.0 — experimental APK

Cumulative Android release based on **PBS Gwell Live PTZ v1.1.1**. Adds **genuine native IoTVideo SDK LivePlayer**, connected to Device IDs returned by the user's already-working Gwell Cloud account session. **Not RTSP or ONVIF**.

## Implementation
- Existing cloud login and 8-device inventory retained.
- New **Native Live P2P** button per device (plus original RTSP/ONVIF Live + PTZ).
- Native AAR from GWTimes/IoTVideo-Android, archived public upstream binary (`app/libs/iotvideo-release.aar`), downloaded in GitHub Actions. This is proprietary third-party code with no confirmed redistribution/commercial license. Do not deploy commercially without agreement from the rights holder.
- Native `IoTVideoSdk.init(context,null)` then `IoTVideoSdk.register(accessId,accessToken)`, then `LivePlayer.setDataResource(deviceId,...)` and `play()`.
- Cloud session passed only through in-memory app process; no password or token extras in Android intents; no persistent credentials.
- SDK callbacks show explicit statuses: `SDK_INIT`, `REGISTER_CALLED`, `P2P_CONNECTING`, `P2P_STATE_PLAY`, `P2P_ERROR <code>`. A successful `register()` call alone is **NOT** proof of backend registration.
- The existing local RTSP/ONVIF module remains separate and unchanged.

## Crucial compatibility disclaimer
`openapi-iot.cloudlinks.cn` cloud login (as used in YooseeExploit client) and 2020 GWTimes/Tencent IoTVideo SDK login may issue incompatible tokens or belong to different IoTVideo product namespaces. We have **not** verified that the 8 cameras' IDs are accessible via this native SDK. A negative callback can indicate incompatible access credential, product scope, cloud namespace or firmware, not that P2P is impossible.

**PTZ over the native P2P channel is NOT implemented**: GWTimes/IoTVideo Android demo's `directionCtl()` is empty. The existing ONVIF PTZ remains available via local IP only. Do not confuse UI controls with actual P2P PTZ.

## Test procedure
1. Install `PBS_Gwell_Native_P2P_v1.2.0.apk` on Android; package `com.pbsgroup.gwellcloudtest.p2pnative` can coexist with prior debug APKs.
2. Enable cellular internet, **disable WiFi**, to ensure this tests remote cloud P2P.
3. Login to your *own* Yoosee/Gwell account using developer credentials that you are authorized to use.
4. Click *Ambil daftar perangkat akun ini* and pick one of your device IDs.
5. Click *Uji Live P2P native*. The app will try to register SDK and start native LivePlayer.
6. If status changes to **P2P_STATE_PLAY** and a real camera image appears, the P2P pathway works. Otherwise copy the diagnostic report, not your login password or vendor tokens.
7. Do not mistake success on inventory API for streaming success. Do not repeatedly brute-force tokens or accounts.

## Deployment notes
- Build artifact (debug signed) placed in GitHub repository `pbs-gwell-cloud-test/releases`, avoiding ChatGPT Library links.
- This source builds against a publicly archived SDK. SDK binary distribution and continued service access are subject to upstream rights and policy; obtain an official license and compatible SDK for production.
- If platform/vendor does not accept the current tokens, proper SDK credentials, device binding, product ID/namespace and SDK version must come from Gwell/Yoosee. We cannot bypass authentication.
