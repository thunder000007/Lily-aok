# LilyBD public release plan (security-first)

## Current release status: NOT READY FOR PUBLIC RELEASE

The current entry point `LillyBD_Rainbow_Login_Fixed.html` stores accounts, including plaintext passwords, posts, stories, profile data, and demo chats in browser `localStorage`. This is suitable only as a local prototype. It is not secure multi-user authentication and does not provide real-time cross-device chat. Do not publish this version as a real social network or invite users to create real accounts.

The repository's Firebase rules are not proof that rules have been deployed or tested. No deployment should be claimed until it is performed from an authorized Firebase account and verified.

## Phase 1 — Protect the existing project
- Keep all changes on the `public-release-security` branch until reviewed.
- Replace localStorage-based account/password handling with Firebase Authentication.
- Never write passwords, password hashes, tokens, or admin credentials into Firestore, Storage, browser storage, or Git.
- Use restrictive Firestore and Storage rules; deny access by default.
- Test rules in the Firebase Emulator Suite before deploying.
- Keep user profile fields allowlisted and validate message sender, membership, type, and size on the server rules.
- Add App Check after legitimate web and Android clients are registered.
- Add abuse controls: report/block, account deletion, rate limiting strategy, and basic monitoring.

## Phase 2 — Real-time social features
- Migrate chat conversations and messages to Firestore and subscribe using real-time listeners.
- Model conversations by authenticated user IDs, not display names.
- Only conversation members may read messages; only the authenticated sender may create a message as themselves.
- Migrate posts, stories, likes, comments, follows, and profiles before calling the full social app production-ready. LocalStorage content is per-device and must not be presented as shared data.
- Upload media only through Storage rules with size/type checks. Do not embed base64 photos in public documents.

## Phase 3 — Website release
- Register the Firebase Web App and configure Email/Password authentication.
- Test in the Emulator Suite and with two independent test accounts.
- Confirm all unauthenticated and cross-account access tests fail.
- Add privacy policy, terms, support contact, account deletion, and abuse reporting.
- Deploy Hosting and security rules only after review; verify HTTPS and mobile browser behaviour.

## Phase 4 — Android release
- Package the shared web client in an Android wrapper (e.g. Capacitor) only after the web app is stable.
- Register Android app details in Firebase if required by the chosen SDK, and configure SHA fingerprints for providers that need them.
- Build and test a signed APK for direct sharing or a signed AAB for Google Play.
- Keep signing keys and keystore passwords out of the repository; back them up securely.
- Test login, logout, chat membership, offline/reconnect behaviour, notifications if added, and account deletion on real devices.

## Release acceptance criteria
A release is blocked until all of these are true:
1. No plaintext passwords or account records in localStorage.
2. Real Firebase Authentication is enabled and tested.
3. Firestore/Storage rules are deployed and emulator-tested.
4. Two-account real-time chat works across different devices.
5. Cross-user and unauthenticated reads/writes are denied.
6. Public-facing privacy and abuse-handling information is present.
7. Web Hosting and Android build are both verified from their actual URLs/artifacts.
