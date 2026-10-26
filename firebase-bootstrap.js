// Firebase browser SDK bootstrap for LillyBD.
// This file is intentionally not connected to the current UI until the Web App config
// is supplied and the authentication/data migration is tested.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  setPersistence,
  browserLocalPersistence
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { getStorage } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-storage.js";
import { firebaseConfig } from "./firebase-config.js";

const missingConfig = !firebaseConfig.apiKey ||
  firebaseConfig.apiKey.includes("PASTE_") ||
  !firebaseConfig.authDomain ||
  firebaseConfig.authDomain.includes("PASTE_") ||
  !firebaseConfig.appId ||
  firebaseConfig.appId.includes("PASTE_");

if (missingConfig) {
  throw new Error(
    "LillyBD Firebase is not configured yet. Add the Firebase Web App config in firebase-config.js."
  );
}

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const googleProvider = new GoogleAuthProvider();
export const db = getFirestore(firebaseApp);
export const storage = getStorage(firebaseApp);

// Call after Firebase Authentication is enabled in the Console.
export const configureAuthPersistence = () =>
  setPersistence(auth, browserLocalPersistence);
