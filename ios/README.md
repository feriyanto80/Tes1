# PBS Balap native iOS v1.0.0

Repository: feriyanto80/Tes1, branch pbs-balap-ios-v1 (separate from Android).
Application name: PBS Balap
Bundle ID: id.pbsgroup.pbsbalap
Main URL: https://balap.pbsgroup.id/
Minimum supported iOS/iPadOS: 16.0

## Features
Native SwiftUI shell and WKWebView, native PBS icon, adaptive iPhone/iPad toolbar,
GPS native permissions diagnostics, camera/file uploads via iOS picker,
JavaScript alerts/prompts, network recovery, responsive portrait/landscape layout.
The real PBS logo asset comes from the original image supplied by the user
and saved in the Android branch asset. The icon generator is ios/tools/make_icons.py.

## Build
Actions > Build PBS Balap iOS
Artifacts: Simulator .app ZIP and unsigned .ipa.
UNSIGNED IPA CANNOT BE INSTALLED on an actual iPhone.

## Signing and installing on physical iPhones
Requires a valid Apple Developer team and a distribution certificate plus
an Ad Hoc provisioning profile including the intended iPhone device UDIDs.
Put these repository secrets under GitHub Settings > Secrets and variables > Actions:
1. IOS_DISTRIBUTION_P12_BASE64: Base64 P12 Apple Distribution certificate with its private key
2. IOS_DISTRIBUTION_P12_PASSWORD: password for that P12
3. IOS_ADHOC_PROFILE_BASE64: Base64 Ad Hoc provisioning profile for id.pbsgroup.pbsbalap
4. IOS_TEAM_ID: Apple Developer Team ID

Run Actions > Sign and Export PBS Balap IPA after setup.
The output signed Ad Hoc IPA can be installed only on iPhones
registered in the profile using Apple Configurator or authorized distribution.

For TestFlight/App Store, additional App Store Connect configuration
and a different export/upload procedure are required.
Never commit any signing certificates or private passwords to the public repository.

## GPS limitation
A Google Apps Script embedded cross-origin iframe may block HTML geolocation
when the parent page does not delegate geolocation via the iframe allow
attribute and Permissions-Policy headers. The Android and iOS apps cannot
bypass a browser/website permission-policy error. Fix the BALAP web integration
if the location error recurs.

## Local Xcode
Open ios/PBSBalap.xcodeproj with Xcode 16+,
and first generate icons with Python 3 + Pillow:
python ios/tools/make_icons.py
Then build for simulator or select an Apple signing team for physical iPhone.
