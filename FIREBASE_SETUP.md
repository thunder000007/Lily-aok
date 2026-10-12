# LillyBD Firebase setup

The original `main` branch remains unchanged. The `lillybd-firebase-upgrade` branch contains the Firebase integration work.

## 1. Create Firebase project
1. Open https://console.firebase.google.com/ and create a project named LillyBD (or use an existing project).
2. Go to Build → Authentication → Sign-in method and enable **Email/Password**. Enable Google only if you want Google sign-in.
3. Go to Build → Firestore Database and create a database.
4. Go to Build → Storage and enable Storage for the project.
5. In Project settings → Your apps, register a **Web app** and copy its Firebase web configuration.

## 2. Add the web config
Open `www/firebase.js` on the `lillybd-firebase-upgrade` branch and replace each `PASTE_...` value in `firebaseConfig` with the matching value from the Firebase Web app config:
- `apiKey`
- `authDomain`
- `projectId`
- `storageBucket`
- `messagingSenderId`
- `appId`

Use the exact values Firebase gives you. Do not send or create a service-account private key.

## 3. Publish security rules
In the Firebase Console, open Firestore → Rules and publish the contents of this branch's `firestore.rules`. Open Storage → Rules and publish `storage.rules`. Review and test the rules with the Rules Playground before real users use the app. Do not enable public read/write access.

## Features wired in cloud mode
- Firebase email/password signup and login; optional Google sign-in
- Cloud profiles and profile photos
- Feed posts, likes, comments and follow lists
- Stories and image upload to Firebase Storage
- One-to-one chat messages using Firestore realtime listeners

Cloud mode is disabled until the Firebase web config placeholders are replaced. The local demo mode is only for trying the interface and must not be used for real accounts.

## Android APK
GitHub Actions builds a **debug APK** when relevant files change on this branch. Open the repository Actions tab, choose the latest successful **Build LillyBD Android APK** run, and download the `LillyBD-debug-apk` artifact. A debug APK can be shared for testing, but it is not a signed Play Store release.

## Before wider use
Test signup/login, profile edits, image upload, posts, likes/comments, follow/unfollow, story expiry, logout, and messaging between two different accounts. Revisit rules for privacy and abuse protection before public launch.
