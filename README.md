# LilyBD

LilyBD is a social/chat app prototype. **It is not ready for public production release yet.**

## Current state
- Existing UI: `LillyBD_Rainbow_Login_Fixed.html`
- Current demo data: browser `localStorage` (per device only)
- Real Firebase Authentication and cross-device real-time chat: not integrated yet
- Firebase project target: `lillybd-f499d` (configured target only; service/deployment state must be verified)
- Security-first release checklist: [PUBLIC_RELEASE.md](PUBLIC_RELEASE.md)
- Firebase setup and warnings: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

## Security warning
The prototype stores passwords in localStorage as plaintext. Do not publish it as a real social network, invite public users, or enter a reused/real password. The Firebase rules in this repository are not proof of a deployed or tested backend.

## Planned release
1. Integrate Firebase Authentication and remove localStorage account/password handling.
2. Migrate private chats and messages to Firestore with member-only Security Rules.
3. Migrate profiles, posts, stories, and interactions to a secure shared backend.
4. Test security rules with the Firebase Emulator Suite and two separate accounts.
5. Deploy the website over HTTPS and create a signed Android APK/AAB after acceptance tests pass.

Never commit service-account private keys, signing keys, passwords, or other secrets.
