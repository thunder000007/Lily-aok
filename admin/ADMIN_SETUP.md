# LillyBD Admin setup (phone-friendly)

## 1. Admin APK build

The APK is a WebView shell for the separate admin website. Latest successful build:
https://github.com/thunder000007/Lily-aok/actions/runs/40413280751

Open the run, scroll to **Artifacts**, and download **LillyBD-Admin-debug-apk**. Extract the ZIP and install the APK on Android. Android may ask you to allow installation from your browser or file manager.

## 2. Admin account

The dashboard currently authorizes the verified Firebase Authentication email:
`boteditz999@gmail.com`.

In Firebase Console → Authentication, enable **Email/Password**, then create this account (or ensure it already exists) and verify its email. Do not share its password. The email check in the website is only a user-interface check; Firestore Rules are the actual security boundary.

## 3. Deploy the admin website

The GitHub Actions deployment workflow needs a Google Cloud service-account JSON key stored as a repository Actions secret. Never commit or paste the JSON key into chat.

1. Open Google Cloud Console for project `lillybd-f499d`: https://console.cloud.google.com/iam-admin/serviceaccounts?project=lillybd-f499d
2. Create/use a dedicated service account with the minimum permissions needed to deploy Firebase Hosting, and create a JSON key if your organization allows keys.
3. In GitHub repository Settings → Secrets and variables → Actions, add a repository secret named exactly:
   `FIREBASE_SERVICE_ACCOUNT_LILLYBD_F499D`
4. Set its value to the complete JSON key contents.
5. Open Actions → **Deploy LillyBD Admin to Firebase Hosting** → **Run workflow**.
6. Confirm the run succeeds, then open https://lillybd-f499d.web.app/admin/.

If you cannot create service-account keys, use an approved Workload Identity Federation setup instead of creating a long-lived key; the current workflow would need to be changed for that method.

## 4. Security and functionality limits

- Firestore Rules restrict admin reads/moderation to the verified admin email above. Keep those rules deployed and test them before production use.
- The dashboard reads up to 200 records per collection. Its displayed counts are only the loaded records, not guaranteed total database counts.
- User blocking only works if the main app checks the user's `blocked` field; the current main app may still use localStorage and is not yet fully integrated with Firebase.
- The dashboard cannot delete Firebase Authentication accounts; that needs a protected server-side Admin SDK endpoint.
- Do not put Firebase service-account keys in the website or APK.
