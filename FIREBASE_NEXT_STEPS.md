# LillyBD Android Firebase integration — status and remaining work

Work is on the `firebase-integration` branch. The intended product is an Android APK; this work does not require publishing a website.

## Implemented in the Android code
- Firebase Android configuration is in `app/google-services.json` for project `lillybd-f499d` and package `com.lillybd.app`.
- Firebase Authentication SDK is included. The app's email/password sign-up and login call Firebase Authentication through a local WebView-to-Android bridge; passwords are not stored in localStorage.
- Successful sign-in writes/updates the user's basic profile in Firestore.
- Text posts can be created and the feed loaded from Firestore.
- Post likes use Firestore array updates.
- AndroidX is enabled for Firebase dependencies.

## Remaining before this can be called working
1. In Firebase Console for `lillybd-f499d`, enable **Authentication → Email/Password**.
2. Create/initialize **Cloud Firestore** if it is not already initialized.
3. Review and deploy `firestore.rules` only after checking them against the app. The repository rules are not deployed by these code commits.
4. Configure Google sign-in in Firebase Console, including the Android SHA fingerprints/OAuth client, then implement and test the native Google sign-in flow.
5. Move stories, comments, profile edits, photos/media, and direct messages from local-only behavior to Firestore/Storage. These features are not yet fully cloud-backed.
6. Test on a real Android phone with a Firebase account that can access the project.

## Build
The GitHub Actions workflow builds a debug APK on pushes to this branch and uploads it as an artifact for 14 days when successful. Check the latest run from the repository Actions page.

## Important
- No Firebase Console services or rules have been enabled/deployed by this repository integration.
- A successful APK build alone does not mean Firebase services are configured or that every social feature is online.
- Do not commit service-account private keys or Admin SDK credentials.
- The JavaScript Firebase Web SDK files in the root are preparatory artifacts; the Android APK uses the native Firebase Android SDK.
