# LillyBD

LillyBD is currently a **single-page HTML/CSS/JavaScript demo**, not a native Android/Flutter project. The main UI is `LillyBD_Rainbow_Login_Fixed.html`.

## Current implementation status

- The UI includes home/feed, stories, profile, and chat screens.
- Sign-up, login, posts, and chat data currently use browser `localStorage`.
- The demo stores account passwords in browser storage. **Do not use real passwords or treat this as production authentication.**
- Google sign-in is a placeholder; it is not connected to Firebase.
- Firebase Hosting configuration targets project `lillybd-f499d`.
- `google-services.json` is Android-specific and does not configure Firebase for this HTML web app.
- No Firebase Web app configuration has been added, and Firebase Authentication/Firestore integration has not yet been completed.

## Planned Firebase integration

1. Register a Firebase **Web app** in project `lillybd-f499d` and obtain its web configuration.
2. Enable Email/Password and Google providers in Firebase Authentication.
3. Create the Cloud Firestore database.
4. Replace localStorage-based authentication with Firebase Authentication.
5. Move profiles, posts, stories, and conversations to Firestore, with real-time listeners where needed.
6. Configure and test security rules against the final data model before deployment.
7. Use Firebase Storage for profile photos and media instead of storing large data URLs in localStorage.

## Firebase project files

- `.firebaserc` selects `lillybd-f499d` as the default CLI project.
- `firebase.json` configures Hosting and references `firestore.rules` and `storage.rules`.
- `FIREBASE_SETUP.md` documents the remaining Console setup.
- Security rules are only a starting point; review and test them against the actual schema before deploying.

## Important

Do not commit Firebase service-account private keys, passwords, or other server secrets. Firebase client configuration is intended for client apps, but access must be protected with Authentication and correctly designed Security Rules.

A successful Hosting deployment alone does **not** mean authentication, Firestore, or messaging is working.
