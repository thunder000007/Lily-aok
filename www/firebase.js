// LillyBD Firebase starter module.
// Replace the placeholder config with values from Firebase Console.
// The existing demo UI is not connected to these functions yet.
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-app.js";
import { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, signOut, onAuthStateChanged, GoogleAuthProvider, signInWithPopup } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-auth.js";
import { getFirestore, collection, addDoc, query, orderBy, onSnapshot, serverTimestamp, doc, setDoc } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-firestore.js";
import { getStorage, ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-storage.js";

const firebaseConfig = {
  apiKey: "PASTE_FIREBASE_WEB_API_KEY",
  authDomain: "PASTE_PROJECT_ID.firebaseapp.com",
  projectId: "PASTE_PROJECT_ID",
  storageBucket: "PASTE_PROJECT_ID.firebasestorage.app",
  messagingSenderId: "PASTE_MESSAGING_SENDER_ID",
  appId: "PASTE_APP_ID"
};
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);

export async function signUp(email, password, displayName) {
  const credential = await createUserWithEmailAndPassword(auth, email, password);
  await setDoc(doc(db, "users", credential.user.uid), {
    uid: credential.user.uid, displayName, email, bio: "", photoURL: "",
    following: [], createdAt: serverTimestamp()
  });
  return credential.user;
}
export async function signIn(email, password) {
  return (await signInWithEmailAndPassword(auth, email, password)).user;
}
export async function signInWithGoogle() {
  return (await signInWithPopup(auth, new GoogleAuthProvider())).user;
}
export async function logOut() { return signOut(auth); }
export function watchAuth(callback) { return onAuthStateChanged(auth, callback); }

export async function createPost(uid, text, imageURL = "") {
  if (!auth.currentUser || auth.currentUser.uid !== uid) throw new Error("Sign in required");
  return addDoc(collection(db, "posts"), { uid, text, imageURL, createdAt: serverTimestamp(), likes: [] });
}
export function watchFeed(callback) {
  return onSnapshot(query(collection(db, "posts"), orderBy("createdAt", "desc")), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() })))
  );
}
export async function uploadImage(uid, file) {
  if (!auth.currentUser || auth.currentUser.uid !== uid) throw new Error("Sign in required");
  const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
  const imageRef = ref(storage, `users/${uid}/uploads/${Date.now()}_${safeName}`);
  await uploadBytes(imageRef, file);
  return getDownloadURL(imageRef);
}
export async function sendChatMessage(chatId, text) {
  if (!auth.currentUser) throw new Error("Sign in required");
  const cleanText = text.trim();
  if (!cleanText) throw new Error("Message cannot be empty");
  return addDoc(collection(db, "chats", chatId, "messages"), {
    uid: auth.currentUser.uid, text: cleanText, createdAt: serverTimestamp()
  });
}
export function watchChatMessages(chatId, callback) {
  return onSnapshot(query(collection(db, "chats", chatId, "messages"), orderBy("createdAt", "asc")), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() })))
  );
}
