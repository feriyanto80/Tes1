# PBS Yoosee / Gwell API Capability Test v1.2.5

Android diagnostic build based on the PBS Gwell Native P2P test application. Package: `com.pbsgroup.gwellcloudtest.p2pnative125`; versionName: `1.2.5`. This package installs side-by-side with earlier diagnostic packages.

## v1.2.5 changes

- Adds **Endpoint Inventory Scan** to the main screen.
- Includes the legacy `.ashx`, OpenAPI, VAS, Cloud Playback, AI Event, Network Config, Advertising and Auth endpoint paths supplied for PBS.
- Uses only HTTP `OPTIONS` and, for read-only candidates, `HEAD`.
- Does not send passwords, AppID/AppToken, AccessToken, signatures, Device IDs, request bodies or playback URLs in the inventory scan.
- Does not execute Login, Logout, Register, Password Reset/Modify, Bind/Unbind, Modify, Delete, Firmware Upgrade, SMS/Email, share, billing, token-generation, playback or secret-retrieval operations.
- Legacy API host candidates are checked in order: `api1.cloudlinks.cn`, then `api2.cloudlinks.cn`.
- OpenAPI paths are mapped to the observed OpenAPI host; vPAAS playback/AI Event paths to `saas-playback.cloudlinks.cn`.
- VAS API paths use `vasapi.cloudlinks.cn:10443` as an **unconfirmed candidate host**. The three `/v2/auth` paths are catalogued but not probed because their base host is unknown.

## Important interpretation

The Endpoint Inventory Scan is a **transport/route diagnostic**, not an authenticated API test:
- HTTP 2xx means the server returned a successful response to OPTIONS/HEAD; it does not prove the business endpoint supports the intended GET/POST method.
- HTTP 3xx means the server redirected the request.
- HTTP 401/403 means the host/path responded with an access restriction.
- HTTP 404 means that host returned a not-found response for the probe method; it may still be a method-specific route.
- HTTP 405/501 means the probe method is unsupported; it does not prove the API path is invalid.
- A network error indicates DNS, TLS, connection or timeout failure, not necessarily an invalid endpoint.
- A host-mapping-required result means no request was sent because the base host is not confirmed.

The scanner intentionally does not invoke business operations. In particular, sensitive legacy endpoints such as `Relationship/GetDevPwd.ashx`, account/password endpoints, device binding, owner switching, firmware upgrade and deletion are never called with GET/POST.

## Authenticated API Capability Test

The separate **API Capability Test** uses the currently logged-in account session:
- Calls authenticated Device List.
- Calls the documented `/openapi/app/user/device/infoQuery` endpoint, trying `devId` and then `deviceId`.
- Reports only sanitized API status, response field names and masked Device ID.
- AI Event and Cloud Playback authorization remain NOT_TESTED until Gwell provides a supported third-party signing method and confirms PBS VAS entitlement.

The public research scripts that call vPAAS APIs use a Frida signing oracle attached to a running Yoosee app. Their sample values are not PBS credentials. This app does not extract credentials from another app, reuse sample secrets or send unsigned VAS requests.

## Run

1. Install `PBS_Gwell_API_Capability_Test_v1.2.5.apk`.
2. For authenticated tests, enter the AppID/AppToken issued for PBS and log in using an account you own.
3. Use **API Capability Test** for authenticated Device List and Device Info checks.
4. Use **Endpoint Inventory Scan · HEAD/OPTIONS** to inspect the endpoint inventory.
5. Tap **Salin laporan JSON (redacted)** and share the report for analysis.

The inventory scan can run without login because it sends no account credentials. Do not share passwords, AppToken, AccessToken, signatures or complete Device IDs.

## Build

GitHub Actions workflow: `.github/workflows/pbs-gwell-api-capability-v1.2.5.yml`.

The workflow downloads the upstream GWTimes IoTVideo AAR, builds the APK, uploads an Actions artifact, and publishes the direct APK plus SHA-256 checksum.

## Licensing

The upstream IoTVideo AAR is a third-party SDK. This experimental compatibility build is for controlled testing. Obtain written redistribution/commercial-use permission and the current supported SDK package from the rights holder before production deployment.
