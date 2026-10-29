# PBS Balap iOS v1.0.1 — Distribution Fix

**Application:** PBS Balap  
**Website:** https://balap.pbsgroup.id/  
**Bundle ID:** `id.pbsgroup.pbsbalap`  
**Source:** GitHub branch `pbs-balap-ios-v1`  
**Supported:** iOS/iPadOS 16 or newer.

## Why the previous BetaDrop installation failed

The earlier file `PBS-Balap-v1.0.0-UNSIGNED.ipa` contained no
`embedded.mobileprovision` and no `_CodeSignature/CodeResources`.
GitHub's earlier successful compilation did **not** make it installable.

BetaDrop only hosts and distributes IPA files; it does NOT add an Apple
certificate or register iPhones. Never upload the old unsigned IPA again.

## Correct workflow for direct iPhone installation via BetaDrop

### A. Register the iPhone(s) with the Apple Developer account

1. Use Safari on every target phone to open https://betadrop.app/udid-checker/
   or find each iPhone's UDID using Finder/Xcode on a Mac.
2. In https://developer.apple.com/account/resources/devices/list register
   the **device name and UDID**.
3. In the Apple Developer portal, create an App ID for
   `id.pbsgroup.pbsbalap`.
4. Export the Apple Distribution certificate **including its private key**
   into a password-protected `.p12` file from Keychain Access on a Mac.
5. Create an **Ad Hoc** provisioning profile, matching the App ID and
   distribution certificate, selecting all iPhones that should install
   PBS Balap. Download its `.mobileprovision` file.

The profile must exist **before** exporting the signed IPA. Adding a UDID
later requires generating a new profile and re-signing/rebuilding.

### B. Configure GitHub Actions without exposing secrets

In GitHub repository Settings → Secrets and variables → Actions, set:

| GitHub secret | Value |
| --- | --- |
| `IOS_TEAM_ID` | Your Apple Developer team ID (10 characters) |
| `IOS_DISTRIBUTION_P12_BASE64` | Full Base64 of your `.p12` |
| `IOS_DISTRIBUTION_P12_PASSWORD` | Password protecting that P12 |
| `IOS_ADHOC_PROFILE_BASE64` | Full Base64 of Ad Hoc `.mobileprovision` |

Example encoding on macOS Terminal (use **your own file paths**):

```bash
base64 -i /path/to/distribution.p12 | tr -d '\n'
base64 -i /path/to/PBSBalap_AdHoc.mobileprovision | tr -d '\n'
```

Paste each value into its respective **GitHub secret**, not a chat,
GitHub source file, issue, or shared Google document.

### C. Run the signed IPA workflow

GitHub Actions → **Sign and Verify PBS Balap IPA (BetaDrop)** → Run workflow
on branch `pbs-balap-ios-v1`.

If manual dispatch is not visible because the workflow is not registered
on the repository's default branch, the workflow also supports triggering
it with a commit that creates or updates `ios/signing/trigger.txt`
on `pbs-balap-ios-v1` **after** all four secrets are configured.

The workflow:
- Checks Apple secrets exist.
- Decodes P12/profile using the macOS-supported `base64 -D`.
- Verifies Apple Team ID, bundle ID, registered device count, and profile expiration.
- Signs the Xcode archive with Apple Distribution.
- Exports `release-testing` Ad Hoc IPA.
- Calls `ios/tools/verify_signed_ipa.py` to verify code signature
  and embedded Ad Hoc provisioning.
- Publishes `PBS_Balap_v1.0.1_BetaDrop_SIGNED_IPA`
  **only if all checks succeed**.

From its artifact ZIP, upload ONLY `PBS-Balap-v1.0.1-SIGNED-AdHoc.ipa`
to https://betadrop.app/upload-ipa/ . Open the new BetaDrop installation link
in Safari on registered iPhones. Do not reuse the old install link.

The output `VERIFICATION.json` confirms signing and provisioning checks
without exposing individual UDIDs.

## Normal automated Xcode builds

`Build PBS Balap iOS` still compiles a simulator app and an unsigned
device app for development, but no longer publishes a misleading unsigned IPA.
Its artifact is named `PBS_Balap_iOS_v1.0.1_BUILD_ONLY_NO_IPA`.
It also tests that unsigned IPA fails signature verification.

## Apple Developer account is not optional for this distribution method

A usable Ad Hoc installation requires a valid Apple signing account, an
associated distribution certificate, an Ad Hoc provisioning profile and
registered devices. The GitHub code cannot create these independently.

For many iPhone users, use **TestFlight** after creating App Store Connect
signing/upload credentials instead. That is separate from the BetaDrop/Ad Hoc
workflow; TestFlight builds cannot be installed directly via BetaDrop.

## Website GPS issue remains separate

GPS in an embedded Google Apps Script iframe may fail even on a valid
signed iOS app if `balap.pbsgroup.id` does not delegate cross-origin
geolocation via the iframe's `allow="geolocation"` and proper
Permissions-Policy. This cannot be bypassed by iOS signing.
