const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.notifyNewChatMessage = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data && event.data.data();
    if (!message || !message.senderId) return;
    const chatRef = db.collection("chats").doc(event.params.chatId);
    const chatSnap = await chatRef.get();
    if (!chatSnap.exists) return;
    const chat = chatSnap.data() || {};
    const participants = Array.isArray(chat.participants) ? chat.participants : [];
    const recipientIds = participants.filter((uid) => uid && uid !== message.senderId);
    if (!recipientIds.length) return;

    const senderSnap = await db.collection("users").doc(message.senderId).get();
    const sender = senderSnap.exists ? senderSnap.data() : {};
    const senderName = (sender && sender.name) || (sender && sender.email) || "Someone";
    const rawText = String(message.text || "").trim();
    const body = rawText.length > 120 ? rawText.slice(0, 117) + "..." : (rawText || "Sent you a message");
    const tokens = [];
    for (const uid of recipientIds) {
      const userSnap = await db.collection("users").doc(uid).get();
      if (!userSnap.exists) continue;
      const user = userSnap.data() || {};
      if (Array.isArray(user.fcmTokens)) tokens.push(...user.fcmTokens.filter((t) => typeof t === "string" && t.length > 0));
      if (user.fcmToken) tokens.push(user.fcmToken);
    }
    const uniqueTokens = [...new Set(tokens)];
    if (!uniqueTokens.length) return;

    const response = await getMessaging().sendEachForMulticast({
      tokens: uniqueTokens,
      data: {
        title: senderName + " sent you a message",
        body,
        chatId: event.params.chatId,
        messageId: event.params.messageId
      },
      android: { priority: "high" }
    });
    const invalid = [];
    response.responses.forEach((result, index) => {
      if (!result.success && result.error &&
          ["messaging/registration-token-not-registered", "messaging/invalid-registration-token"].includes(result.error.code)) {
        invalid.push(uniqueTokens[index]);
      }
    });
    if (invalid.length) {
      const batch = db.batch();
      for (const uid of recipientIds) {
        batch.set(db.collection("users").doc(uid), {
          fcmTokens: require("firebase-admin/firestore").FieldValue.arrayRemove(...invalid)
        }, { merge: true });
      }
      await batch.commit();
    }
  }
);
