# LilyBD Firebase setup and release status

## Firebase project
The configured Firebase CLI project is `lillybd-f499d` in `.firebaserc`. The repository configuration alone does not prove that Firebase services are enabled, that rules are deployed, or that the signed-in account has deployment access.

## Current app state — important security warning
The existing entry point `LillyBD_Rainbow_Login_Fixed.html` is a localStorage demo. It stores account objects including plaintext passwords in the browser and stores posts, stories, profiles, and chats on that one device. It is **not safe for public release** and does not provide real cross-device messaging. Do not use real passwords or private information with this prototype.

The current Firebase rules are a starting point for a future Firebase-backed implementation. They have not been emulator-tested or deployed as part of this repository change. The current front-end schema does not yet match these rules.

## Required Firebase Console setup
1. Open https://console.firebase.google.com/project/lillybd-f499d/overview.
2. Register a Web App named `LilyBD Web` under **Project settings → General → Your apps**, if one does not already exist.
3. Under **Build → Authentication → Sign-in method**, enable **Email/Password**.
4. Create a Cloud Firestore database and select its location carefully.
5. Configure Storage only when upload code and rules are ready.
6. Add the web app configuration to the client code when integration starts. Firebase client configuration is not a private key; never commit service-account credentials, private keys, or admin SDK secrets.

## Security rules and testing
- `firestore.rules` allows authenticated profile access, two-person chat membership, and member-only message reads. Client-side chat membership changes, message edits/deletes, and unknown collections are denied.
- `storage.rules` has path-based media rules for the future `users/{userId}/profile` and `chats/{chatId}` schema; test them against the final app before enabling uploads.
- Use the Firebase Emulator Suite to test anonymous access, cross-account access, sender spoofing, oversized messages, membership changes, and unexpected fields.
- The current app is still localStorage-based; do not deploy the rules and assume the app is secure. Integrate Firebase Authentication and Firestore in the client first.

## Release
See [PUBLIC_RELEASE.md](PUBLIC_RELEASE.md). Web Hosting and Android builds must not be announced as production-ready until the Firebase integration is complete and verified.
