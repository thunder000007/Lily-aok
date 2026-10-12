# LillyBD automation scaffold

The `lillybd-firebase-upgrade` branch contains a copy of the current UI at `www/index.html`, a Capacitor Android wrapper configuration, dependencies, and a Firebase starter module.

**Important:** The current UI still uses localStorage. The Firebase module in `www/firebase.js` is scaffolding only and is not yet connected to the signup/feed/follow/chat event handlers. Do not treat this as a production-ready social network until integration and security testing are completed.

## Next steps
1. Create a Firebase project and register a Web app.
2. Replace the placeholders in `www/firebase.js` with the Firebase web configuration.
3. Enable Email/Password Authentication; enable Google only if desired.
4. Create Firestore and Storage, then deploy restrictive rules from `firestore.rules` and `storage.rules` after review.
5. Connect the UI to Firebase Authentication, Firestore and Storage, replacing localStorage-backed accounts/posts/chats.
6. From the repository folder on Windows, run `npm install`, then `npm run android:add`, `npm run android:sync`, and `npm run android:open`.
7. Build the APK in Android Studio.

## Security
- Never store user passwords in localStorage.
- Never deploy Firestore or Storage with public read/write rules.
- The Firebase web config is not a service-account secret, but security must come from Auth and Security Rules.