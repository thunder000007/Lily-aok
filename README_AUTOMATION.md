# LillyBD automation

This branch contains the LillyBD UI, Firebase cloud-mode integration, Firebase starter services, Firestore/Storage rules, and a GitHub Actions workflow that builds a debug APK.

## Current status
- The original `main` branch is preserved.
- Cloud mode activates only after replacing the placeholder values in `www/firebase.js` with the Web app config from your own Firebase project.
- Cloud-mode code covers email/password signup and login, optional Google sign-in, profiles, posts, likes, comments, follows, stories, image uploads, and one-to-one real-time chat.
- Until Firebase config is supplied and the security rules are deployed, the UI remains in its local demo mode. Do not use local demo accounts for real users.
- The GitHub Actions workflow builds a debug APK artifact; this is not a signed release APK and is not ready for Play Store submission.

## Firebase setup
1. Create a Firebase project and register a Web app.
2. Enable Authentication → Email/Password (and Google only if desired).
3. Create Firestore Database and Storage.
4. Replace every `PASTE_...` placeholder in `www/firebase.js` with the matching Firebase Web app configuration values.
5. Publish `firestore.rules` and `storage.rules` from this branch in the Firebase Console. Review and test the rules before inviting real users.
6. Push the updated file to this branch; GitHub Actions will build a fresh debug APK.

## Build
Open the repository's Actions tab and select the latest successful **Build LillyBD Android APK** run. Download the `LillyBD-debug-apk` artifact and extract `app-debug.apk`.

## Security notes
- Never store passwords in localStorage.
- Never publish open Firestore or Storage rules.
- Firebase web config is not a service-account private key, but access still depends on Authentication and Security Rules.
- Validate the app with two separate accounts before sharing it widely.
