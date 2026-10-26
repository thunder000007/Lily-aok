# LillyBD Admin APK

This Android shell opens the existing LillyBD Admin web dashboard at
https://lillybd-f499d.web.app/admin/.

## Build
The GitHub Actions workflow `.github/workflows/build-admin-apk.yml` builds a debug APK and uploads it as the artifact `LillyBD-Admin-debug-apk`.

## Important
This APK is a WebView wrapper, not a standalone native admin client. The admin website must be successfully deployed and must enforce admin authorization and secure Firebase rules. This project does not bypass Firebase authentication or permissions. Do not place service-account keys in the APK.
