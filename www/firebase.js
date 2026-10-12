// LillyBD Firebase integration layer.
// Set the real Web app config below to enable cloud mode. Placeholder config keeps demo mode disabled.
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-app.js";
import {
  getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword,
  signOut, onAuthStateChanged, GoogleAuthProvider, signInWithPopup
} from "https://www.gstatic.com/firebasejs/12.0.0/firebase-auth.js";
import {
  getFirestore, collection, addDoc, query, orderBy, onSnapshot, serverTimestamp,
  doc, setDoc, getDoc, getDocs, where, updateDoc, arrayUnion, arrayRemove,
  deleteDoc, limit
} from "https://www.gstatic.com/firebasejs/12.0.0/firebase-firestore.js";
import { getStorage, ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/12.0.0/firebase-storage.js";

const firebaseConfig = {
  apiKey: "PASTE_FIREBASE_WEB_API_KEY",
  authDomain: "PASTE_PROJECT_ID.firebaseapp.com",
  projectId: "PASTE_PROJECT_ID",
  storageBucket: "PASTE_PROJECT_ID.firebasestorage.app",
  messagingSenderId: "PASTE_MESSAGING_SENDER_ID",
  appId: "PASTE_APP_ID"
};
export const isConfigured = !Object.values(firebaseConfig).some(v => String(v).startsWith("PASTE_"));
let app, auth, db, storage;
if (isConfigured) {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
  db = getFirestore(app);
  storage = getStorage(app);
}
export { auth, db, storage };
const requireAuth = () => {
  if (!isConfigured || !auth?.currentUser) throw new Error("Firebase is not configured or you are signed out.");
  return auth.currentUser;
};
const profileRef = uid => doc(db, "users", uid);

export async function signUp(email, password, displayName) {
  const cred = await createUserWithEmailAndPassword(auth, email, password);
  await setDoc(profileRef(cred.user.uid), {
    uid: cred.user.uid, displayName: displayName.trim(), email: cred.user.email,
    bio: "Welcome to my LillyBD profile ✦", photoURL: "", following: [], social: {},
    createdAt: serverTimestamp()
  });
  return cred.user;
}
export async function signIn(email, password) {
  return (await signInWithEmailAndPassword(auth, email, password)).user;
}
export async function signInWithGoogle() {
  const cred = await signInWithPopup(auth, new GoogleAuthProvider());
  const ref = profileRef(cred.user.uid);
  if (!(await getDoc(ref)).exists()) {
    await setDoc(ref, { uid: cred.user.uid, displayName: cred.user.displayName || "LillyBD user",
      bio: "Welcome to my LillyBD profile ✦", photoURL: cred.user.photoURL || "",
      following: [], social: {}, createdAt: serverTimestamp() });
  }
  return cred.user;
}
export async function logOut() { if (auth) await signOut(auth); }
export function watchAuth(callback) { return isConfigured ? onAuthStateChanged(auth, callback) : () => {}; }
export async function getProfile(uid) {
  const snap = await getDoc(profileRef(uid));
  return snap.exists() ? snap.data() : null;
}
export async function saveProfile(uid, patch) {
  requireAuth();
  if (auth.currentUser.uid !== uid) throw new Error("You can only edit your own profile.");
  await updateDoc(profileRef(uid), patch);
}
export function watchUsers(callback) {
  return onSnapshot(collection(db, "users"), snap => callback(snap.docs.map(d => d.data())));
}
export function watchPosts(callback) {
  return onSnapshot(query(collection(db, "posts"), orderBy("createdAt", "desc"), limit(100)), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() }))));
}
export async function createPost(uid, text, imageURL = "") {
  requireAuth();
  if (auth.currentUser.uid !== uid) throw new Error("Sign in required");
  return addDoc(collection(db, "posts"), { uid, text, imageURL, createdAt: serverTimestamp(), likes: [], comments: [] });
}
export async function removePost(postId) {
  requireAuth();
  const ref = doc(db, "posts", postId), snap = await getDoc(ref);
  if (snap.exists() && snap.data().uid === auth.currentUser.uid) await deleteDoc(ref);
  else throw new Error("You can only delete your own post.");
}
export async function toggleLike(postId, liked) {
  requireAuth();
  await updateDoc(doc(db, "posts", postId), { likes: liked ? arrayRemove(auth.currentUser.uid) : arrayUnion(auth.currentUser.uid) });
}
export async function addComment(postId, comment) {
  requireAuth();
  await updateDoc(doc(db, "posts", postId), { comments: arrayUnion({ uid: auth.currentUser.uid, name: auth.currentUser.displayName || "User", text: comment }) });
}
export async function toggleFollow(targetUid, following) {
  requireAuth();
  await updateDoc(profileRef(auth.currentUser.uid), { following: following ? arrayRemove(targetUid) : arrayUnion(targetUid) });
}
export async function uploadImage(uid, file) {
  requireAuth();
  if (auth.currentUser.uid !== uid) throw new Error("Sign in required");
  if (!file.type.startsWith("image/") || file.size > 10 * 1024 * 1024) throw new Error("Choose an image under 10 MB.");
  const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
  const imageRef = ref(storage, `users/${uid}/uploads/${Date.now()}_${safeName}`);
  await uploadBytes(imageRef, file);
  return getDownloadURL(imageRef);
}
export function watchStories(callback) {
  return onSnapshot(query(collection(db, "stories"), orderBy("createdAt", "desc"), limit(100)), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() }))));
}
export async function createStory(uid, text, imageURL = "") {
  requireAuth();
  return addDoc(collection(db, "stories"), { uid, text, imageURL, createdAt: serverTimestamp(), expiresAt: Date.now() + 86400000 });
}
export function watchChats(callback) {
  requireAuth();
  return onSnapshot(query(collection(db, "chats"), where("members", "array-contains", auth.currentUser.uid)), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() }))));
}
export async function startChatWithName(name) {
  requireAuth();
  const people = await getDocs(query(collection(db, "users"), where("displayName", "==", name.trim()), limit(5)));
  const target = people.docs.find(d => d.id !== auth.currentUser.uid);
  if (!target) throw new Error("No other account found with that exact display name.");
  const members = [auth.currentUser.uid, target.id].sort();
  const chatId = members.join("_");
  const ref = doc(db, "chats", chatId);
  if (!(await getDoc(ref)).exists()) {
    await setDoc(ref, { members, createdAt: serverTimestamp(), names: {
      [auth.currentUser.uid]: auth.currentUser.displayName || "Me",
      [target.id]: target.data().displayName || name.trim()
    } });
  }
  return { id: chatId, target: target.data(), targetUid: target.id };
}
export function watchChatMessages(chatId, callback) {
  requireAuth();
  return onSnapshot(query(collection(db, "chats", chatId, "messages"), orderBy("createdAt", "asc"), limit(300)), snap =>
    callback(snap.docs.map(d => ({ id: d.id, ...d.data() }))));
}
export async function sendChatMessage(chatId, text) {
  requireAuth();
  const cleanText = text.trim();
  if (!cleanText || cleanText.length > 5000) throw new Error("Message must be 1–5000 characters.");
  return addDoc(collection(db, "chats", chatId, "messages"), { uid: auth.currentUser.uid, text: cleanText, createdAt: serverTimestamp() });
}
