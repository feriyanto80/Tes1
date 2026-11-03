# PBS Native P2P v1.2.2 — cumulative Cloud Access Readiness build

Cumulative update based on the v1.2.1 diagnostic build. Package: `com.pbsgroup.gwellcloudtest.p2pnative122`; versionCode: 1; versionName: `1.2.2`. This package installs side-by-side with v1.2.1, so neither earlier diagnostic app needs to be uninstalled.

## Included
- Retains existing Yoosee account login and Device List flow.
- Retains Native IoTVideo P2P test screen and its diagnostic callbacks.
- Adds a Cloud Recording / AI Event readiness screen.
- Adds a copyable vendor access request for Gwell.
- Clearly reports that Cloud VAS is not enabled until Gwell confirms the AppID's VAS entitlement, product/package binding, and supported third-party authentication/signature method.
- No Frida, no borrowed hard-coded credentials, no token extraction from another application, and no fabricated playback URL.

## Important scope limitation
This build does **not** claim Cloud AI Event, Cloud Playback, HLS, or MP4 download is operational. The user currently has only AppID and AppToken. The public `camera-hack` scripts use a hard-coded sample Access ID/token and a Frida signing oracle attached to another running application. Those sample credentials are not PBS credentials and must not be reused. The official GWTimes demo's VAS integration also initializes its AccountMgr/product environment separately. AppID/AppToken alone do not establish that PBS has the required VAS entitlement or an officially supported signing interface.

Once Gwell confirms access and supplies the supported integration specification, the VAS implementation can be added in a later cumulative release without changing the existing Native P2P path.

## Build
GitHub Actions workflow: `.github/workflows/pbs-gwell-cloud-recording-v1.2.2.yml`.
The workflow downloads the upstream `iotvideo-release.aar`, builds the debug APK, publishes it under `releases/`, and generates a SHA-256 checksum.

## Install and test
1. Keep v1.2.1 installed.
2. Install `PBS_Gwell_Native_P2P_v1.2.2.apk` as a separate app.
3. Log in with the user's own account and authorized AppID/AppToken.
4. Confirm Device List still loads.
5. Run Native P2P Live and capture the on-screen safe diagnostic report.
6. Open `Cloud Recording / AI Event` to see the exact VAS access status and copy the vendor request.

A successful SDK initialization or a void `register()` return is not proof of successful P2P authentication. A real stream requires the SDK PLAY state and visible video frames.

## SDK licensing
The upstream IoTVideo AAR is a third-party SDK. This is an experimental compatibility build. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
