# LilyBD

LilyBD is a social-network and messaging app project. The current feature branch contains a refreshed, responsive frontend prototype and Android WebView packaging.

## Preview

- Main app UI: [app/index.html](app/index.html)
- Local-only interaction layer: [app/features.js](app/features.js)
- Separate admin dashboard concept: [admin/index.html](admin/index.html)
- Pre-Firebase implementation plan: [PRE_FIREBASE_CHECKLIST.md](PRE_FIREBASE_CHECKLIST.md)
- Location and admin security design: [LOCATION_ADMIN_SECURITY_PLAN.md](LOCATION_ADMIN_SECURITY_PLAN.md)

## Current frontend prototype

- Responsive home feed, stories, post composer, image/video attachment preview (local demo), likes, comments, saved posts, and share action.
- Friends discovery and demo friend requests.
- Demo inbox with local messages, group discovery, profile editing, notifications and privacy preference controls.
- Separate admin dashboard concept for moderation/report/security-log workflows.
- Android WebView points at packaged assets; file picker support is added for photo/video selection.

**Important:** prototype posts, comments, profile settings, and demo messages are stored locally on the current device/browser. They are not shared with other users. No real sign-in, cross-device real-time chat, production notifications, real friend requests, actual moderation actions, or location tracking is active. The admin page is a visual concept and has no authentication/backend. Do not use it for a live service.

## Firebase is intentionally postponed

Per the requested sequence, Firebase integration comes after the frontend and Android shell are reviewed. The next backend phase will need:
1. Firebase Authentication; remove legacy plaintext-password/localStorage account handling.
2. Firestore schema and tested Security Rules for profiles, posts, comments, friend relationships, messages, groups, reports and notification records.
3. Firebase Storage access rules for uploaded photos/videos.
4. Real-time member-authorized 1:1/group messaging and unread state.
5. Opt-in, revocable location sharing and owner-only lost-device recovery with re-authentication, limited retention and no routine admin access.
6. A separate admin website with server-verified roles, MFA, least privilege and tamper-resistant audit logs.
7. Account deletion/export, blocking/reporting/appeals, rate limits, abuse protections, and Emulator Suite security tests.

## Build

The Android debug APK is built by GitHub Actions. Check the latest run on the feature branch:
https://github.com/thunder000007/Lily-aok/actions

A passing build only proves the Android package compiles; it does not prove backend or security readiness. The public-release PR is intentionally a draft until the app and build are reviewed.

## Security warning

Do not deploy the existing legacy prototype as a public service. Its previous demo login stores passwords in browser localStorage in plaintext. Never use a real or reused password in a demo build, and never commit service-account private keys or signing keys.
