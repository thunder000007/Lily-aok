const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.notifyOnNewMessage = onDocumentCreated(
  { document: "chats/{chatId}/messages/{messageId}", region: "asia-south1" },
  async (event) => {
    const message = event.data && event.data.data();
    if (!message) return;
    const chatRef = db.collection("chats").doc(event.params.chatId);
    const chatSnap = await chatRef.get();
    if (!chatSnap.exists) return;

    const chat = chatSnap.data() || {};
    const participants = Array.isArray(chat.participants) ? chat.participants : [];
    const senderId = message.senderId;
    if (!senderId || !participants.includes(senderId)) return;

    const recipients = participants.filter((uid) => uid && uid !== senderId);
    if (!recipients.length) return;
    const senderSnap = await db.collection("users").doc(senderId).get();
    const sender = senderSnap.exists ? senderSnap.data() : {};
    const senderName = (sender && (sender.name || sender.email)) || "New message";
    const body = String(message.text || "Sent you a message").slice(0, 180);

    const tokenDocs = await Promise.all(recipients.map((uid) => db.collection("users").doc(uid).get()));
    const tokens = [];
    tokenDocs.forEach((snap) => {
      const values = snap.exists && Array.isArray(snap.data().fcmTokens) ? snap.data().fcmTokens : [];
      values.forEach((token) => { if (typeof token === "string" && token) tokens.push(token); });
    });
    const uniqueTokens = [...new Set(tokens)];
    if (!uniqueTokens.length) return;

    const response = await getMessaging().sendEachForMulticast({
      tokens: uniqueTokens,
      notification: { title: senderName, body },
      data: { type: "new_message", chatId: event.params.chatId, senderId: String(senderId) },
      android: { priority: "high", notification: { channelId: "lillybd_messages", sound: "default" } },
    });

    const invalid = [];
    response.responses.forEach((result, index) => {
      if (!result.success && result.error &&
          ["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(result.error.code)) {
        invalid.push(uniqueTokens[index]);
      }
    });
    if (invalid.length) {
      await Promise.all(recipients.map((uid) =>
        db.collection("users").doc(uid).set({ fcmTokens: FieldValue.arrayRemove(...invalid) }, { merge: true })
      ));
    }
  }
);
