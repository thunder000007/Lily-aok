const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

exports.notifyNewChatMessage = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data && event.data.data();
    if (!message || !message.senderId) return;

    const chatSnap = await db.collection("chats").doc(event.params.chatId).get();
    if (!chatSnap.exists) return;

    const chat = chatSnap.data() || {};
    const participants = Array.isArray(chat.participants) ? chat.participants : [];
    const recipientIds = participants.filter((uid) => uid && uid !== message.senderId);
    if (!recipientIds.length) return;

    const senderSnap = await db.collection("users").doc(message.senderId).get();
    const sender = senderSnap.exists ? senderSnap.data() || {} : {};
    const senderName = String(sender.name || sender.email || "Someone").trim();
    const rawText = String(message.text || "").trim();
    const body = rawText.length > 120
      ? rawText.slice(0, 117) + "..."
      : (rawText || "Sent you a message");

    const tokens = [];
    const tokenOwners = new Map();
    for (const uid of recipientIds) {
      const userSnap = await db.collection("users").doc(uid).get();
      if (!userSnap.exists) continue;
      const user = userSnap.data() || {};
      const userTokens = [
        ...(Array.isArray(user.fcmTokens) ? user.fcmTokens : []),
        ...(user.fcmToken ? [user.fcmToken] : [])
      ].filter((token) => typeof token === "string" && token.length > 0);
      for (const token of userTokens) {
        if (!tokenOwners.has(token)) tokenOwners.set(token, new Set());
        tokenOwners.get(token).add(uid);
        tokens.push(token);
      }
    }

    const uniqueTokens = [...new Set(tokens)];
    if (!uniqueTokens.length) return;

    // A notification payload lets Android show the notification while the app is
    // backgrounded or closed. Data fields are also supplied for foreground handling.
    for (let i = 0; i < uniqueTokens.length; i += 500) {
      const tokenBatch = uniqueTokens.slice(i, i + 500);
      const response = await getMessaging().sendEachForMulticast({
        tokens: tokenBatch,
        notification: {
          title: senderName + " sent you a message",
          body
        },
        data: {
          title: senderName + " sent you a message",
          body,
          chatId: String(event.params.chatId),
          messageId: String(event.params.messageId)
        },
        android: { priority: "high", notification: { channelId: "lillybd_messages" } }
      });

      const invalidTokens = [];
      response.responses.forEach((result, index) => {
        if (!result.success && result.error &&
          ["messaging/registration-token-not-registered", "messaging/invalid-registration-token"].includes(result.error.code)) {
          invalidTokens.push(tokenBatch[index]);
        }
      });

      if (invalidTokens.length) {
        const batch = db.batch();
        for (const token of invalidTokens) {
          for (const uid of (tokenOwners.get(token) || [])) {
            batch.set(db.collection("users").doc(uid), {
              fcmTokens: FieldValue.arrayRemove(token),
              fcmToken: FieldValue.delete()
            }, { merge: true });
          }
        }
        await batch.commit();
      }
    }
  }
);
