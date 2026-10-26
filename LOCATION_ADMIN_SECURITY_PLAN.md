# LilyBD location recovery and separate admin security plan

## Status and scope

This document records the agreed requirements for the public-release-security branch. It is an implementation plan, not a claim that tracking, admin controls, Firebase services, or hosting are already implemented or deployed.

- Location: user-authorized device recovery plus optional location sharing with explicit consent.
- Admin: account moderation, security logs, and necessary moderation tools.
- Admin site: separate web application/deployment from the main user app; no public admin entry point in the main app.
- Release remains blocked until implementation and security tests pass.

## 1. Location design

### User controls and consent
- Provide separate settings for Find my device and Share my location. Both start OFF until the user deliberately enables them and grants OS permission.
- Explain what is collected, why, update frequency, who can view it, retention, and how to stop it before asking for permission.
- Show a clear status indicator. Android background location must follow current OS permission rules and use a visible foreground-service notification where required.
- Allow revocation from the app and device settings. Stop collecting promptly after revocation; delete location history according to the published retention policy.
- Do not enable covert tracking, hide tracking indicators, bypass OS permissions, or collect location from a user who has not opted in.

### Recovery access model
- A device-recovery location is visible only to the verified owner through a separately authenticated recovery flow, such as signing in on another device and completing re-authentication.
- Admins do not get routine access to precise location. Any future support access must be exceptional, purpose-limited, explicitly disclosed, separately authorized, and fully audited. Moderation permissions alone must not grant location access.
- Store only minimum required data: user/device identifier, coordinates, accuracy, timestamp, consent state, and collection metadata. Never put coordinates in public profile documents or general activity feeds.
- Enforce ownership in backend rules or trusted server code. A client-supplied user ID is never proof of ownership.
- Set a short, documented retention period and deletion process; avoid retaining a long location trail by default.

### Technical limitations to disclose
Location is best-effort, not guaranteed. It may be unavailable if the phone is powered off, offline, out of battery, permissions are revoked, location services are disabled, the app is force-stopped/uninstalled, or the OS restricts background work. Do not promise guaranteed continuous tracking.

## 2. Separate admin website

- Build a distinct admin frontend and deployment with no admin route/button in the public main app. A separate URL is useful isolation, but URL secrecy is not a security boundary.
- Require admin allowlisting and server-verified authorization. Never authorize an admin using a client-side isAdmin flag, hidden button, email text alone, or a Firestore document writable by the user.
- Grant admin roles only through a trusted server/Admin SDK process. Use least-privilege roles (e.g. moderator vs. security administrator) and require MFA for every admin account.
- Use short sessions, reauthentication for sensitive actions, rate limits, secure cookie/session settings where applicable, and monitoring for failed login attempts.
- All moderation operations must go through trusted backend functions/endpoints that verify the caller's role, validate the target and reason, and record an audit event. Never give the browser a service-account key or unrestricted Admin SDK credentials.

### Admin features
- Search users by stable UID or exact supported identifiers.
- Suspend, ban, restore, and disable accounts with a required reason and confirmation.
- Review user-submitted reports and take proportionate action.
- View security events such as login/security changes, repeated failures, reports, moderation actions, and suspicious access patterns.
- Security logs must be access-controlled and append-only from the client perspective. Record actor UID, action, target UID, reason, timestamp, and outcome; avoid logging passwords, tokens, message contents, or precise coordinates.
- Make moderation actions reversible where appropriate and provide a documented appeal/support path.

## 3. Suggested Firebase/backend data boundaries

Names below are proposed, not existing deployed resources:
- users/{uid}: public/ordinary profile fields only; never passwords, secrets, or private location history.
- userPrivate/{uid}: tightly restricted preferences and consent state.
- devices/{deviceId} or users/{uid}/devices/{deviceId}: owner-only device metadata and recovery status.
- deviceLocations/{deviceId} or a private per-user subcollection: latest opt-in recovery point and timestamp; owner-only reads. No public list/query.
- locationShares/{shareId}: explicit share owner, recipient, scope, expiry, and revocation state; only authorized parties can read while active.
- reports/{reportId}: reporter, target, category, evidence references, status, and timestamps with restricted access.
- adminAudit/{eventId}: trusted-server-created moderation/security audit records; clients cannot create, edit, or delete these directly.
- adminRoles/{uid}: administered only by a trusted server process; clients cannot grant roles or edit their own role.

Use Firebase Authentication for identity, Firestore Security Rules for direct data access, and Cloud Functions or another trusted backend for privileged operations. Validate all rules with the Firebase Emulator Suite before deploying. App Check is an additional abuse barrier, not a substitute for authentication or authorization.

## 4. Implementation sequence
1. Replace localStorage demo authentication with Firebase Authentication. The existing demo stores plaintext passwords and must not be released or migrated as trusted credentials.
2. Define and test the production Firestore/Storage schema and deny-by-default rules.
3. Implement real user profiles and real-time chat; test two accounts on separate devices.
4. Implement consent screens, location permissions, foreground/background behaviour, owner-only recovery APIs, expiry/retention, and revocation.
5. Build the admin frontend separately; implement trusted backend role checks, MFA, least-privilege roles, moderation endpoints, and audit logging.
6. Add abuse reporting, account deletion, privacy policy, terms, retention details, and support/appeal process.
7. Deploy to a staging project first. Run access-control tests, location-consent/revocation tests, admin authorization tests, and Android device tests.
8. Deploy to production only after tests pass; then verify the actual public website and signed Android artifact.

## 5. Required security tests
- Anonymous users cannot read/write private data or location.
- User A cannot read User B's device metadata, location, shares, private settings, or chats.
- Revoked or expired location shares stop working.
- Turning off consent stops future location collection; deletion/retention behaviour is verified.
- Ordinary users cannot assign themselves admin roles or invoke moderation endpoints.
- Admin access requires MFA and a server-verified role; moderators cannot perform security-administrator-only actions.
- Moderation actions create an audit event; clients cannot alter or erase audit events.
- Forged UIDs, changed request bodies, oversized payloads, rate-limit abuse, and unauthenticated requests are rejected.
- Rules and backend tests run in CI/emulators before release.

## Release warning
The current app remains a localStorage prototype, not a secure multi-user product. No location tracker, separate admin panel, privileged backend, or production deployment is confirmed by this document. Do not collect real users' location or publish the current prototype as a production social app.