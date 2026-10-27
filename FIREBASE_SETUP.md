# LillyBD Firebase setup

## Confirmed Firebase project

The user supplied the Firebase Console URL for project **lillybd-f499d**. This repository selects that project as its default Firebase CLI target. Repository access does not grant permission to enable Firebase services or deploy to the project.

## Current app findings

- The app entry point is `LillyBD_Rainbow_Login_Fixed.html`.
- Existing sign-in/profile/chat behavior uses browser `localStorage`; it is not yet backed by Firebase.
- Google sign-in is currently a placeholder.
- There is no Firebase web app configuration in the repository yet.

## Required Firebase Console setup

1. Open https://console.firebase.google.com/project/lillybd-f499d/overview.
2. Register a Web app named `LillyBD Web` under **Project settings → General → Your apps** if one is not already registered.
3. Under **Build → Authentication → Sign-in method**, enable **Email/Password**.
4. Under **Build → Firestore Database**, create the database and choose its location carefully.
5. Under **Build → Storage**, initialize the bucket if media uploads are needed.
6. Copy the Web app config from **Project settings → Your apps** for integration. Never put service-account private keys in browser code or commit them to GitHub.

## Rules and deployment

- `firestore.rules` is a starting point for user profiles and two-person chats. Test it against the final app schema before deployment.
- `storage.rules` restricts profile media to the profile owner and chat media to chat participants.
- The Firebase CLI config targets `lillybd-f499d`.

When Firebase CLI is installed and authenticated with an account that has access to this project, deploy rules only after reviewing/testing them:

```sh
firebase login
firebase use lillybd-f499d
firebase deploy --only firestore:rules,storage
```

Do not deploy Hosting as a production app yet. The HTML has not been integrated with Firebase Authentication/Firestore, and the app still relies on localStorage. No Firebase services have been enabled or deployed by this repository change.


## Android push notifications for new messages

The `message-notifications` branch adds Firebase Cloud Messaging (FCM) to the Android app and a Cloud Function that sends a push notification when a new Firestore message is created. The notification title uses the sender's profile name (or email as fallback), and the body previews the message. Android 13+ also asks the user for notification permission.

Before notifications can work in production:

1. Verify Firestore is initialized and its database location. The function trigger region in `functions/index.js` must match the Firestore database location; change `asia-south1` if the database is elsewhere.
2. Ensure the Firebase project is on the Blaze plan if required by Cloud Functions deployment and that billing is configured.
3. Install Node.js 20 and Firebase CLI on a trusted computer, sign in with an account that has access to `lillybd-f499d`, and run:
   ```sh
   firebase use lillybd-f499d
   firebase deploy --only functions:notifyOnNewMessage,firestore:rules
   ```
4. Build and install the APK from the GitHub Actions artifact for the `message-notifications` branch. Sign in on both test phones, allow notifications, then send a test message with the recipient app in the background.

The function source and Android code are committed, but source changes alone do not deploy a Cloud Function to Firebase. Do not treat push notifications as live until deployment succeeds and a background-message test passes. Keep service-account private keys out of GitHub.
