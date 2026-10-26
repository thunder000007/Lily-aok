# LillyBD Firebase integration — next steps

Work is being prepared on the `firebase-integration` branch so the existing `main` branch stays unchanged.

## What has been prepared
- `firebase-config.example.js`: a safe template for the Firebase Web App configuration.
- `firebase-bootstrap.js`: modular Firebase SDK initialization for Authentication, Firestore, and Storage.
- No app behavior is switched over yet; the existing HTML demo still uses localStorage.

## Required before connecting the UI
1. In Firebase Console, open project `lillybd-f499d`.
2. Register a Web App (</>) if one does not already exist.
3. Copy its Web config into a new `firebase-config.js` file based on the example.
4. Enable Email/Password and Google providers in Authentication.
5. Create the Firestore database.
6. Test the final Firestore/Storage rules before deployment.

The Android `google-services.json` identifies the Android app and project, but it does not contain the Firebase Web App `appId` needed by this browser-based HTML app. Do not invent that value.

## Security and migration notes
- Do not store passwords in localStorage. Firebase Authentication must own password handling.
- Profile photos and post/story media should move from data URLs in localStorage to Firebase Storage.
- User profiles, posts, stories, likes, comments, chats, and messages need a defined Firestore schema and matching rules.
- Do not deploy these rules or publish this as a production app until the schema is wired and tested.
- Never commit service-account private keys or Admin SDK credentials.
