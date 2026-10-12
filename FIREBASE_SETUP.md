# LillyBD Firebase setup

## Selected project

This repository is configured to use **lillybd-1c18e** as its single Firebase project for Authentication, Cloud Firestore, and Hosting. This is a choice based on the project ID you supplied first; repository access does not let us verify that this project exists in your Firebase account or that your Google account has Firebase permissions.

## Current repository findings

- The current app is a single HTML file: `LillyBD_Rainbow_Login_Fixed.html`.
- Its email/password login, profiles, posts, and chat messages currently use browser `localStorage`.
- Google login is a placeholder alert.
- The current chat is a local demo; it is not real-time or shared across devices.
- No Firebase web app configuration is present in the repository.

## Firebase Console steps still required

1. Open https://console.firebase.google.com/ and select project `lillybd-1c18e`.
2. Register a Web app named `LillyBD Web` if one is not already registered.
3. In **Build → Authentication → Sign-in method**, enable **Email/Password**.
4. In **Build → Firestore Database**, create the database and choose its location carefully.
5. Copy the Web app configuration from **Project settings → Your apps**. Add it to the app during the integration step; do not paste service-account credentials into client code.
6. Before deployment, replace the localStorage authentication and chat logic with Firebase Authentication and Firestore listeners, and test the security rules against the final document schema.

## Hosting

`firebase.json` points Firebase Hosting at the repository root and rewrites routes to the existing HTML file. `.firebaserc` selects `lillybd-1c18e`. Once the Firebase CLI is installed and the project is accessible, deploy with:

```sh
firebase login
firebase use lillybd-1c18e
firebase deploy --only hosting,firestore:rules
```

Do not deploy this as a production chat app yet: the existing HTML still uses localStorage and has not been wired to Firebase. The Firestore rules are a baseline for a two-person chat schema and must be tested against the implementation before use.
