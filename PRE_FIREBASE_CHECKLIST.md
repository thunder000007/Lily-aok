# LilyBD — Pre-Firebase Work Checklist

This checklist deliberately separates UI work that can be done before Firebase from features that require a trusted backend.

## Frontend work in this branch
- [x] Responsive social-feed layout and mobile bottom navigation.
- [x] Story row and local story preview interaction.
- [x] Post composer with text, emoji, and small local photo/video attachment preview.
- [x] Local demo likes, comments, save/unsave, share text, and hide/report prototype actions.
- [x] Friend discovery/search and local demo friend-request state.
- [x] Demo chat UI, local-only outgoing messages, and chat list.
- [x] Group discovery/join prototype.
- [x] Profile editing and local privacy preference controls.
- [x] Separate admin dashboard visual concept; no public admin entry in the app.
- [x] Android WebView assets synced with the frontend and Android file picker added.
- [x] JavaScript syntax checked for the inline app script and local interaction script.
- [ ] Android build must finish successfully and APK must be downloaded/installed/tested on a phone.
- [ ] Visual/interaction acceptance pass on a small Android screen and desktop browser.
- [ ] Review and merge the draft PR only after build and review.

## Explicitly not implemented before Firebase
These require authentication, trusted storage, backend authorization, scheduling, or Android OS integration; frontend mockups must not be represented as real functionality.

- Real user registration/sign-in, password reset, MFA, session/device management.
- Cross-device profile/feed/story sync and server-side media storage.
- Real friend requests, followers, group membership and notifications across users.
- Real-time 1:1/group messaging, delivery/read receipts, and private media access.
- Server-side reports, blocking enforcement, account suspension, appeals and immutable admin audit.
- Admin authentication/MFA and server-enforced role checks.
- Opt-in location sharing, Android permission/foreground-service flow, owner-only recovery, re-authentication, revocation and retention/deletion.
- Account export/deletion and abuse prevention/rate limiting.

## Security requirements for the backend phase
- No passwords or tokens in localStorage, logs, source code, or admin UI.
- Enforce ownership and membership in Security Rules/backend functions; do not trust client-side checks.
- Location sharing starts OFF, has explicit consent and clear purpose/access/retention, is revocable, and is not routinely accessible to admins.
- Admin privileges must be server-verified and protected by MFA and least privilege.
- Test unauthorized cross-user reads/writes and revoked consent using the Firebase Emulator Suite before production.
