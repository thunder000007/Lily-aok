package com.lillybd.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private WebView webView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        webView.addJavascriptInterface(new LillyBridge(), "LillyNative");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void sendResult(boolean ok, String message, FirebaseUser user) {
        try {
            JSONObject result = new JSONObject();
            result.put("ok", ok);
            result.put("message", message == null ? "" : message);
            if (user != null) {
                JSONObject u = new JSONObject();
                u.put("uid", user.getUid());
                u.put("email", user.getEmail() == null ? "" : user.getEmail());
                u.put("name", user.getDisplayName() == null ? "" : user.getDisplayName());
                u.put("photoURL", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
                result.put("user", u);
            }
            final String json = result.toString();
            runOnUiThread(() -> {
                if (webView != null) webView.evaluateJavascript(
                    "window.onNativeAuthResult && window.onNativeAuthResult(" + JSONObject.quote(json) + ");", null);
            });
        } catch (Exception ignored) { }
    }

    private void saveProfile(FirebaseUser user, String name) {
        if (user == null) return;
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("email", user.getEmail() == null ? "" : user.getEmail());
        profile.put("name", (name != null && !name.trim().isEmpty()) ? name.trim() :
            (user.getDisplayName() == null ? "LillyBD User" : user.getDisplayName()));
        profile.put("photoURL", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
        profile.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(user.getUid()).set(profile,
            com.google.firebase.firestore.SetOptions.merge());
    }

    private class LillyBridge {
        @JavascriptInterface
        public void signUp(String name, String email, String password) {
            String cleanName = name == null ? "" : name.trim();
            auth.createUserWithEmailAndPassword(email == null ? "" : email.trim(), password == null ? "" : password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            com.google.firebase.auth.UserProfileChangeRequest request =
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(cleanName).build();
                            user.updateProfile(request).addOnCompleteListener(profileTask -> {
                                saveProfile(user, cleanName);
                                sendResult(true, "Account created", user);
                            });
                        } else sendResult(false, "Could not create account", null);
                    } else sendResult(false, task.getException() == null ?
                        "Sign-up failed" : task.getException().getLocalizedMessage(), null);
                });
        }

        @JavascriptInterface
        public void login(String email, String password) {
            auth.signInWithEmailAndPassword(email == null ? "" : email.trim(), password == null ? "" : password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        saveProfile(user, null);
                        sendResult(true, "Logged in", user);
                    } else sendResult(false, task.getException() == null ?
                        "Login failed" : task.getException().getLocalizedMessage(), null);
                });
        }

        @JavascriptInterface
        public void startChat(String email) {
            FirebaseUser me = auth.getCurrentUser();
            String targetEmail = email == null ? "" : email.trim().toLowerCase();
            if (me == null || targetEmail.isEmpty() || targetEmail.equalsIgnoreCase(me.getEmail())) {
                sendChatResult(false, "Enter another user's registered email", null, null);
                return;
            }
            db.collection("users").whereEqualTo("email", targetEmail).limit(1).get().addOnCompleteListener(find -> {
                if (!find.isSuccessful() || find.getResult() == null || find.getResult().isEmpty()) {
                    sendChatResult(false, "User not found. They must sign in to LillyBD first.", null, null); return;
                }
                String otherId = find.getResult().getDocuments().get(0).getId();
                String otherName = find.getResult().getDocuments().get(0).getString("name");
                db.collection("chats").whereArrayContains("participants", me.getUid()).get().addOnCompleteListener(existing -> {
                    if (existing.isSuccessful() && existing.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : existing.getResult().getDocuments()) {
                            java.util.List<String> ids = (java.util.List<String>) doc.get("participants");
                            if (ids != null && ids.contains(otherId) && ids.size() == 2) {
                                sendChatResult(true, "Conversation opened", doc.getId(), otherName); return;
                            }
                        }
                    }
                    Map<String,Object> chat = new HashMap<>();
                    chat.put("participants", java.util.Arrays.asList(me.getUid(), otherId));
                    chat.put("updatedAt", FieldValue.serverTimestamp());
                    chat.put("lastMessage", "");
                    db.collection("chats").add(chat).addOnCompleteListener(created -> {
                        if (created.isSuccessful()) sendChatResult(true, "Conversation created", created.getResult().getId(), otherName);
                        else sendChatResult(false, "Could not create conversation", null, null);
                    });
                });
            });
        }

        @JavascriptInterface
        public void loadMessages(String chatId) {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || chatId == null || chatId.trim().isEmpty()) { sendMessages("[]"); return; }
            db.collection("chats").document(chatId).collection("messages")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING).limit(200).get()
                .addOnCompleteListener(task -> {
                    org.json.JSONArray arr = new org.json.JSONArray();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            try {
                                JSONObject m = new JSONObject();
                                m.put("id", doc.getId());
                                m.put("text", doc.getString("text") == null ? "" : doc.getString("text"));
                                m.put("senderId", doc.getString("senderId") == null ? "" : doc.getString("senderId"));
                                Object when = doc.get("createdAt");
                                m.put("time", when instanceof com.google.firebase.Timestamp ?
                                    ((com.google.firebase.Timestamp) when).toDate().getTime() : System.currentTimeMillis());
                                arr.put(m);
                            } catch (Exception ignored) { }
                        }
                    }
                    sendMessages(arr.toString());
                });
        }

        @JavascriptInterface
        public void sendMessage(String chatId, String text) {
            FirebaseUser me = auth.getCurrentUser();
            String clean = text == null ? "" : text.trim();
            if (me == null || chatId == null || clean.isEmpty() || clean.length() > 5000) return;
            Map<String,Object> message = new HashMap<>();
            message.put("senderId", me.getUid());
            message.put("text", clean);
            message.put("createdAt", FieldValue.serverTimestamp());
            db.collection("chats").document(chatId).collection("messages").add(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    db.collection("chats").document(chatId).update("lastMessage", clean, "updatedAt", FieldValue.serverTimestamp());
                    loadMessages(chatId);
                } else sendChatResult(false, "Message failed to send", null, null);
            });
        }

        private void sendChatResult(boolean ok, String message, String chatId, String name) {
            try {
                JSONObject result = new JSONObject();
                result.put("ok", ok); result.put("message", message == null ? "" : message);
                result.put("chatId", chatId == null ? "" : chatId); result.put("name", name == null ? "" : name);
                String json = result.toString();
                runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                    "window.onNativeChatResult && window.onNativeChatResult(" + JSONObject.quote(json) + ");", null); });
            } catch (Exception ignored) { }
        }

        private void sendMessages(String json) {
            runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                "window.onNativeMessagesLoaded && window.onNativeMessagesLoaded(" + JSONObject.quote(json) + ");", null); });
        }

        @JavascriptInterface
        public void createPost(String text) {
            FirebaseUser user = auth.getCurrentUser();
            String cleanText = text == null ? "" : text.trim();
            if (user == null || cleanText.isEmpty() || cleanText.length() > 10000) {
                sendPostResult(false, "Sign in and enter a post first", null);
                return;
            }
            Map<String, Object> post = new HashMap<>();
            post.put("authorId", user.getUid());
            post.put("text", cleanText);
            post.put("mediaURL", "");
            post.put("likeUids", new java.util.ArrayList<String>());
            post.put("createdAt", FieldValue.serverTimestamp());
            db.collection("posts").add(post).addOnCompleteListener(task -> {
                if (task.isSuccessful()) sendPostResult(true, "Post published", task.getResult().getId());
                else sendPostResult(false, task.getException() == null ? "Could not publish post" :
                    task.getException().getLocalizedMessage(), null);
            });
        }

        @JavascriptInterface
        public void togglePostLike(String postId, boolean currentlyLiked) {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null || postId == null || postId.trim().isEmpty()) return;
            Object update = currentlyLiked ? FieldValue.arrayRemove(user.getUid()) : FieldValue.arrayUnion(user.getUid());
            db.collection("posts").document(postId).update("likeUids", update)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) loadPosts();
                    else sendPostResult(false, task.getException() == null ? "Like update failed" :
                        task.getException().getLocalizedMessage(), null);
                });
        }

        @JavascriptInterface
        public void loadPosts() {
            if (auth.getCurrentUser() == null) { sendPosts("[]"); return; }
            db.collection("posts").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50).get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) { sendPosts("[]"); return; }
                    org.json.JSONArray arr = new org.json.JSONArray();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            JSONObject item = new JSONObject();
                            item.put("id", doc.getId());
                            item.put("authorId", doc.getString("authorId"));
                            item.put("text", doc.getString("text") == null ? "" : doc.getString("text"));
                            item.put("mediaURL", doc.getString("mediaURL") == null ? "" : doc.getString("mediaURL"));
                            item.put("likeUids", new org.json.JSONArray(
                                (java.util.List<String>) doc.get("likeUids") == null ?
                                new java.util.ArrayList<String>() : (java.util.List<String>) doc.get("likeUids")));
                            Object created = doc.get("createdAt");
                            item.put("createdAt", created instanceof com.google.firebase.Timestamp ?
                                ((com.google.firebase.Timestamp) created).toDate().getTime() : System.currentTimeMillis());
                            arr.put(item);
                        } catch (Exception ignored) { }
                    }
                    sendPosts(arr.toString());
                });
        }

        private void sendPostResult(boolean ok, String message, String postId) {
            try {
                JSONObject result = new JSONObject();
                result.put("ok", ok);
                result.put("message", message == null ? "" : message);
                result.put("postId", postId == null ? "" : postId);
                String json = result.toString();
                runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                    "window.onNativePostResult && window.onNativePostResult(" + JSONObject.quote(json) + ");", null); });
            } catch (Exception ignored) { }
        }

        private void sendPosts(String json) {
            runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                "window.onNativePostsLoaded && window.onNativePostsLoaded(" + JSONObject.quote(json) + ");", null); });
        }

        @JavascriptInterface
        public void logout() {
            auth.signOut();
            sendResult(true, "Logged out", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
