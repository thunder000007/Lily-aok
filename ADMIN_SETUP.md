# LillyBD Admin Website setup

The separate admin dashboard lives at `/admin/` and is intended for mobile Chrome. It does not use the normal LillyBD user interface.

## What's included

- Email/password sign-in, restricted in the UI to the verified administrator `boteditz999@gmail.com`.
- User profile listing and block/unblock controls.
- Post and story listing with delete controls.
- Firestore-backed counters and search.

**Important:** A UI email check is not the security boundary. Firestore Security Rules must be deployed and enforce admin-only operations. The rules in this repository use the verified admin email. The existing rules file was updated to allow this admin to read/moderate posts and stories.

## Required one-time Firebase setup

1. In [Firebase Console](https://console.firebase.google.com/project/lillybd-f499d/overview), open **Project settings → General → Your apps**.
2. If no Web app exists, register one named `LillyBD Admin` (or reuse the existing LillyBD Web app).
3. Copy its Web app configuration and replace the placeholder values in `admin/firebase-config.js`. The Firebase Web config is public client configuration; **never put a service-account private key here**.
4. Under **Authentication → Sign-in method**, enable Email/Password. Make sure `boteditz999@gmail.com` is an existing account and its email is verified.
5. Publish the repository's Firestore rules to project `lillybd-f499d` after reviewing them against the live data model.
6. Deploy Firebase Hosting from the repository root with a Firebase-authorized account: `firebase deploy --only hosting,firestore:rules --project lillybd-f499d`.

The Hosting deploy requires a Firebase CLI login with permission to deploy to the project. This GitHub integration does not itself grant Firebase Console access or deploy permissions.

## Limitations / next backend work

The browser dashboard can only perform actions allowed by Firestore Rules. It does not directly list/delete Firebase Authentication accounts, reset other users' passwords, or administer project settings. Those privileged operations require a protected server-side endpoint using the Firebase Admin SDK; never ship service-account credentials to the browser.

The dashboard loads up to 200 records per collection for a lightweight mobile interface. Pagination and audit logs can be added if the app grows.
