// LillyBD Firebase service layer.
// Requires firebase-config.js copied from firebase-config.example.js and populated
// from Firebase Console > Project settings > Your apps (Web app).
import { auth, db, storage, googleProvider, configureAuthPersistence } from "./firebase-bootstrap.js";
import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  updateProfile
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {
  collection, doc, getDoc, getDocs, addDoc, setDoc, updateDoc, deleteDoc,
  query, where, orderBy, limit, serverTimestamp, onSnapshot
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import {
  ref as storageRef, uploadBytes, getDownloadURL, deleteObject
} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-storage.js";

await configureAuthPersistence();

export const authState = (callback) => onAuthStateChanged(auth, callback);

export async function signUpWithEmail({ name, email, password }) {
  const credential = await createUserWithEmailAndPassword(auth, email.trim(), password);
  if (name?.trim()) await updateProfile(credential.user, { displayName: name.trim() });
  await saveUserProfile(credential.user, { name: name.trim() });
  return credential.user;
}

export async function loginWithEmail({ email, password }) {
  const credential = await signInWithEmailAndPassword(auth, email.trim(), password);
  await ensureUserProfile(credential.user);
  return credential.user;
}

export async function loginWithGoogle() {
  const credential = await signInWithPopup(auth, googleProvider);
  await ensureUserProfile(credential.user);
  return credential.user;
}

export const logout = () => signOut(auth);

export async function ensureUserProfile(user) {
  if (!user) throw new Error("A signed-in user is required.");
  const profileRef = doc(db, "users", user.uid);
  const snapshot = await getDoc(profileRef);
  if (!snapshot.exists()) {
    await saveUserProfile(user, {});
  } else {
    await setDoc(profileRef, {
      email: user.email ?? "",
      name: user.displayName || snapshot.data().name || "LillyBD User",
      photoURL: user.photoURL || snapshot.data().photoURL || "",
      updatedAt: serverTimestamp()
    }, { merge: true });
  }
  return (await getDoc(profileRef)).data();
}

export async function saveUserProfile(user, fields = {}) {
  if (!user) throw new Error("A signed-in user is required.");
  const profileRef = doc(db, "users", user.uid);
  await setDoc(profileRef, {
    uid: user.uid,
    email: user.email ?? "",
    name: fields.name?.trim() || user.displayName || "LillyBD User",
    photoURL: fields.photoURL ?? user.photoURL ?? "",
    bio: fields.bio ?? "",
    social: fields.social ?? {},
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  }, { merge: true });
}

export async function updateMyProfile(fields) {
  if (!auth.currentUser) throw new Error("Sign in first.");
  const safe = { updatedAt: serverTimestamp() };
  for (const key of ["name", "photoURL", "bio", "social"]) {
    if (Object.hasOwn(fields, key)) safe[key] = fields[key];
  }
  await setDoc(doc(db, "users", auth.currentUser.uid), safe, { merge: true });
  if (typeof fields.name === "string" && fields.name.trim()) {
    await updateProfile(auth.currentUser, { displayName: fields.name.trim() });
  }
}

export async function listDiscoverableUsers(max = 50) {
  if (!auth.currentUser) throw new Error("Sign in first.");
  const snapshot = await getDocs(query(collection(db, "users"), orderBy("name"), limit(max)));
  return snapshot.docs.map(d => ({ id: d.id, ...d.data() })).filter(u => u.id !== auth.currentUser.uid);
}

export async function createPost({ text = "", mediaFile = null }) {
  const user = auth.currentUser;
  if (!user) throw new Error("Sign in first.");
  if (!text.trim() && !mediaFile) throw new Error("Write a post or choose a photo.");
  let mediaURL = "";
  if (mediaFile) mediaURL = await uploadMedia(mediaFile, `posts/${user.uid}`);
  const post = {
    authorId: user.uid,
    text: text.trim().slice(0, 10000),
    mediaURL,
    createdAt: serverTimestamp(),
    likeUids: []
  };
  return (await addDoc(collection(db, "posts"), post)).id;
}

export async function subscribeToFeed(callback, max = 50) {
  if (!auth.currentUser) throw new Error("Sign in first.");
  return onSnapshot(query(collection(db, "posts"), orderBy("createdAt", "desc"), limit(max)),
    snapshot => callback(snapshot.docs.map(d => ({ id: d.id, ...d.data() }))));
}

export async function togglePostLike(postId, likeUids = []) {
  const user = auth.currentUser;
  if (!user) throw new Error("Sign in first.");
  const next = likeUids.includes(user.uid)
    ? likeUids.filter(uid => uid !== user.uid)
    : [...likeUids, user.uid];
  await updateDoc(doc(db, "posts", postId), { likeUids: next });
}

export async function createStory({ text = "", mediaFile = null }) {
  const user = auth.currentUser;
  if (!user) throw new Error("Sign in first.");
  if (!text.trim() && !mediaFile) throw new Error("Write a story or choose a photo.");
  const mediaURL = mediaFile ? await uploadMedia(mediaFile, `stories/${user.uid}`) : "";
  return (await addDoc(collection(db, "stories"), {
    authorId: user.uid,
    text: text.trim().slice(0, 2000),
    mediaURL,
    createdAt: serverTimestamp(),
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000)
  })).id;
}

export async function subscribeToStories(callback, max = 50) {
  if (!auth.currentUser) throw new Error("Sign in first.");
  const q = query(collection(db, "stories"), where("expiresAt", ">", new Date()), orderBy("expiresAt"), limit(max));
  return onSnapshot(q, snapshot => callback(snapshot.docs.map(d => ({ id: d.id, ...d.data() }))));
}

export async function createDirectChat(otherUserId) {
  const user = auth.currentUser;
  if (!user) throw new Error("Sign in first.");
  if (!otherUserId || otherUserId === user.uid) throw new Error("Choose another user.");
  const participants = [user.uid, otherUserId].sort();
  const chatId = participants.join("_");
  const chatRef = doc(db, "chats", chatId);
  const existing = await getDoc(chatRef);
  if (!existing.exists()) {
    await setDoc(chatRef, { participants, createdAt: serverTimestamp(), updatedAt: serverTimestamp() });
  }
  return chatId;
}

export async function sendMessage(chatId, text) {
  const user = auth.currentUser;
  const cleanText = text.trim();
  if (!user) throw new Error("Sign in first.");
  if (!cleanText || cleanText.length > 5000) throw new Error("Message must be 1–5000 characters.");
  const chatRef = doc(db, "chats", chatId);
  const chat = await getDoc(chatRef);
  if (!chat.exists() || !chat.data().participants?.includes(user.uid)) throw new Error("You are not a participant in this chat.");
  await addDoc(collection(chatRef, "messages"), {
    senderId: user.uid,
    text: cleanText,
    createdAt: serverTimestamp()
  });
  await updateDoc(chatRef, { updatedAt: serverTimestamp(), lastMessage: cleanText.slice(0, 120) });
}

export function subscribeToMessages(chatId, callback) {
  if (!auth.currentUser) throw new Error("Sign in first.");
  return onSnapshot(query(collection(db, "chats", chatId, "messages"), orderBy("createdAt"), limit(200)),
    snapshot => callback(snapshot.docs.map(d => ({ id: d.id, ...d.data() }))));
}

export async function uploadMedia(file, folder) {
  const user = auth.currentUser;
  if (!user) throw new Error("Sign in first.");
  if (!file.type.startsWith("image/")) throw new Error("Only image uploads are supported by this helper.");
  if (file.size > 8 * 1024 * 1024) throw new Error("Image must be 8 MB or smaller.");
  const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
  const path = `${folder}/${Date.now()}_${safeName}`;
  const objectRef = storageRef(storage, path);
  await uploadBytes(objectRef, file, { contentType: file.type });
  return getDownloadURL(objectRef);
}
