# LillyBD Firebase upgrade

This branch is reserved for the Firebase-backed version of LillyBD. The existing HTML demo is preserved on main.

## Current state
- Main UI is in LillyBD_Rainbow_Login_Fixed.html.
- The current app stores accounts, posts, stories, and chats in browser localStorage.
- Passwords are currently stored in plain text in that local browser storage. Do not use this demo for real accounts.
- Google sign-in is a placeholder and does not authenticate users.

## Firebase setup needed before wiring the app
1. Open https://console.firebase.google.com/ and create a project named LillyBD.
2. In Build → Authentication → Sign-in method, enable Email/Password. Enable Google only if you want Google sign-in.
3. In Build → Firestore Database, create a database. Start in production mode; do not publish open read/write rules.
4. In Project settings → Your apps, register a Web app and copy its Firebase web configuration.
5. Do not create or share a service-account private key. The web configuration is intended for client apps; access must be protected with Authentication and Firestore/Storage Security Rules.

## Planned app data
- users/{uid} — display name, bio, profile photo URL, social links
- posts/{postId} — author UID, text, image URL, timestamps, likes/comments
- stories/{storyId} — author UID, text/image URL, creation/expiry timestamps
- chats/{chatId} and chats/{chatId}/messages/{messageId} — participants and real-time messages
- follows/{followId} — follower/followed UIDs

## Before public launch
- Add and test restrictive Firestore and Storage Security Rules.
- Use Firebase Authentication, never browser-stored passwords.
- Upload photos to Firebase Storage instead of putting Base64 images in localStorage.
- Test account creation, login, logout, posting, follow, and two-account messaging.
- Then package the web app for Android with Capacitor and build the APK in Android Studio.