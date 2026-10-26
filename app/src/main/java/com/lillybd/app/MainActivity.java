package com.lillybd.app;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.os.Bundle;
import android.util.Base64;
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
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
    private WebView webView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String pendingLocationChatId = null;
    private LocationListener pendingLocationListener = null;

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
                u.put("emailVerified", user.isEmailVerified());
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
        profile.put("email", user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase());
        profile.put("name", (name != null && !name.trim().isEmpty()) ? name.trim() :
            (user.getDisplayName() == null ? "LillyBD User" : user.getDisplayName()));
        profile.put("photoURL", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
        profile.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(user.getUid()).set(profile,
            com.google.firebase.firestore.SetOptions.merge());
    }

    private interface UploadCallback { void complete(String url, String error); }

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
                                user.sendEmailVerification();
                                sendResult(true, "Account created. Check your email to verify it.", user);
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
                        user.reload().addOnCompleteListener(reloadTask -> {
                            FirebaseUser refreshed = auth.getCurrentUser();
                            saveProfile(refreshed, null);
                            sendResult(true, "Logged in", refreshed);
                        });
                    } else sendResult(false, task.getException() == null ?
                        "Login failed" : task.getException().getLocalizedMessage(), null);
                });
        }

        @JavascriptInterface
        public void loadAdminUsers() {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || !me.isEmailVerified() || me.getEmail() == null ||
                !me.getEmail().equalsIgnoreCase("boteditz999@gmail.com")) {
                sendAdminUsers("{\"ok\":false,\"message\":\"Admin access denied\"}");
                return;
            }
            db.collection("users").limit(200).get().addOnCompleteListener(task -> {
                org.json.JSONArray users = new org.json.JSONArray();
                if (task.isSuccessful() && task.getResult() != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            JSONObject u = new JSONObject();
                            u.put("uid", doc.getId());
                            u.put("name", doc.getString("name") == null ? "LillyBD User" : doc.getString("name"));
                            u.put("email", doc.getString("email") == null ? "" : doc.getString("email"));
                            u.put("blocked", Boolean.TRUE.equals(doc.getBoolean("blocked")));
                            users.put(u);
                        } catch (Exception ignored) { }
                    }
                    sendAdminUsers("{\"ok\":true,\"users\":" + users.toString() + "}");
                } else sendAdminUsers("{\"ok\":false,\"message\":\"Could not load users. Check Firestore rules.\"}");
            });
        }

        @JavascriptInterface
        public void setUserBlocked(String uid, boolean blocked) {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || !me.isEmailVerified() || me.getEmail() == null ||
                !me.getEmail().equalsIgnoreCase("boteditz999@gmail.com") ||
                uid == null || uid.trim().isEmpty() || uid.equals(me.getUid())) {
                sendAdminUsers("{\"ok\":false,\"message\":\"Admin access denied or invalid user\"}");
                return;
            }
            db.collection("users").document(uid).update("blocked", blocked)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) loadAdminUsers();
                    else sendAdminUsers("{\"ok\":false,\"message\":\"Could not update user status\"}");
                });
        }

        private void sendAdminUsers(String json) {
            runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                "window.onNativeAdminUsers && window.onNativeAdminUsers(" + JSONObject.quote(json) + ");", null); });
        }

        @JavascriptInterface
        public void loadAdminUsers() {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || !me.isEmailVerified() || me.getEmail() == null ||
                !me.getEmail().equalsIgnoreCase("boteditz999@gmail.com")) {
                sendAdminResult("{\"ok\":false,\"message\":\"Admin access denied. Verify the admin email first.\"}");
                return;
            }
            db.collection("users").limit(200).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    sendAdminResult("{\"ok\":false,\"message\":\"Could not load users. Publish the updated Firestore rules.\"}");
                    return;
                }
                org.json.JSONArray users = new org.json.JSONArray();
                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    try {
                        JSONObject u = new JSONObject();
                        u.put("uid", doc.getId());
                        u.put("name", doc.getString("name") == null ? "LillyBD User" : doc.getString("name"));
                        u.put("email", doc.getString("email") == null ? "" : doc.getString("email"));
                        u.put("blocked", Boolean.TRUE.equals(doc.getBoolean("blocked")));
                        users.put(u);
                    } catch (Exception ignored) { }
                }
                sendAdminResult("{\"ok\":true,\"users\":" + users.toString() + "}");
            });
        }

        @JavascriptInterface
        public void setUserBlocked(String uid, boolean blocked) {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || !me.isEmailVerified() || me.getEmail() == null ||
                !me.getEmail().equalsIgnoreCase("boteditz999@gmail.com") ||
                uid == null || uid.trim().isEmpty() || uid.equals(me.getUid())) {
                sendAdminResult("{\"ok\":false,\"message\":\"Admin access denied or invalid user.\"}");
                return;
            }
            db.collection("users").document(uid).update("blocked", blocked)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) loadAdminUsers();
                    else sendAdminResult("{\"ok\":false,\"message\":\"Could not update user status. Check Firestore rules.\"}");
                });
        }

        private void sendAdminResult(String json) {
            runOnUiThread(() -> {
                if (webView != null) webView.evaluateJavascript(
                    "window.onNativeAdminUsers && window.onNativeAdminUsers(" + JSONObject.quote(json) + ");", null);
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
                    Map<String,Object> names = new HashMap<>(); names.put(me.getUid(), me.getDisplayName() == null ? "LillyBD User" : me.getDisplayName()); names.put(otherId, otherName == null ? targetEmail : otherName); chat.put("participantNames", names);
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
        public void loadChats() {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null) { sendChats("[]"); return; }
            db.collection("chats").whereArrayContains("participants", me.getUid()).get().addOnCompleteListener(task -> {
                org.json.JSONArray arr = new org.json.JSONArray();
                if (task.isSuccessful() && task.getResult() != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            java.util.List<String> ids = (java.util.List<String>) doc.get("participants");
                            if (ids == null || ids.size() != 2) continue;
                            Map<String,Object> names = (Map<String,Object>) doc.get("participantNames");
                            String other = ids.get(0).equals(me.getUid()) ? ids.get(1) : ids.get(0);
                            String name = names == null ? "LillyBD User" : String.valueOf(names.containsKey(other) ? names.get(other) : "LillyBD User");
                            JSONObject item = new JSONObject(); item.put("id", doc.getId()); item.put("name", name);
                            item.put("lastMessage", doc.getString("lastMessage") == null ? "" : doc.getString("lastMessage"));
                            arr.put(item);
                        } catch (Exception ignored) { }
                    }
                }
                sendChats(arr.toString());
            });
        }

        private void sendChats(String json) {
            runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                "window.onNativeChatsLoaded && window.onNativeChatsLoaded(" + JSONObject.quote(json) + ");", null); });
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
        public void shareLocation(String chatId) {
            FirebaseUser me = auth.getCurrentUser();
            if (me == null || chatId == null || chatId.trim().isEmpty()) return;
            pendingLocationChatId = chatId;
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 7201);
                } else acquireAndSendLocation();
            });
        }

        private void acquireAndSendLocation() {
            String chatId = pendingLocationChatId;
            if (chatId == null) return;
            try {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location best = null;
                for (String provider : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
                    try {
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) break;
                        Location loc = lm.getLastKnownLocation(provider);
                        if (loc != null && (best == null || loc.getTime() > best.getTime())) best = loc;
                    } catch (Exception ignored) { }
                }
                if (best != null && System.currentTimeMillis() - best.getTime() < 10 * 60 * 1000) {
                    String link = "https://maps.google.com/?q=" + best.getLatitude() + "," + best.getLongitude();
                    pendingLocationChatId = null;
                    new LillyBridge().sendMessage(chatId, "📍 My location: " + link);
                    return;
                }
                final String targetChat = chatId;
                pendingLocationListener = new LocationListener() {
                    @Override public void onLocationChanged(Location location) {
                        try { lm.removeUpdates(this); } catch (Exception ignored) { }
                        pendingLocationListener = null;
                        pendingLocationChatId = null;
                        String link = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                        new LillyBridge().sendMessage(targetChat, "📍 My location: " + link);
                    }
                    @Override public void onProviderEnabled(String provider) { }
                    @Override public void onProviderDisabled(String provider) { }
                    @SuppressWarnings("deprecation")
                    @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) { }
                };
                String provider = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                lm.requestLocationUpdates(provider, 1000L, 0f, pendingLocationListener, Looper.getMainLooper());
                webView.evaluateJavascript("window.onNativeLocationStatus && window.onNativeLocationStatus('Waiting for location…');", null);
            } catch (SecurityException e) {
                webView.evaluateJavascript("window.onNativeLocationStatus && window.onNativeLocationStatus('Location permission is required.');", null);
            } catch (Exception e) {
                webView.evaluateJavascript("window.onNativeLocationStatus && window.onNativeLocationStatus('Could not get location. Turn on device location and try again.');", null);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == 7201) {
                boolean granted = false;
                for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
                if (granted) acquireAndSendLocation();
                else {
                    pendingLocationChatId = null;
                    if (webView != null) webView.evaluateJavascript(
                        "window.onNativeLocationStatus && window.onNativeLocationStatus('Location permission denied.');", null);
                }
            }
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
        public void createPost(String text) { createPostWithMedia(text, ""); }

        @JavascriptInterface
        public void createPostWithMedia(String text, String dataUrl) {
            FirebaseUser user = auth.getCurrentUser();
            String cleanText = text == null ? "" : text.trim();
            if (user == null || cleanText.length() > 10000 || (cleanText.isEmpty() && (dataUrl == null || dataUrl.isEmpty()))) {
                sendPostResult(false, "Sign in and add text or a photo first", null); return;
            }
            if (dataUrl == null || dataUrl.isEmpty()) { savePost(user, cleanText, ""); return; }
            uploadImage("posts/" + user.getUid() + "/" + UUID.randomUUID() + ".jpg", dataUrl, (url, error) -> {
                if (error != null) sendPostResult(false, "Photo upload failed: " + error, null);
                else savePost(user, cleanText, url);
            });
        }

        private void savePost(FirebaseUser user, String text, String mediaUrl) {
            Map<String, Object> post = new HashMap<>();
            post.put("authorId", user.getUid()); post.put("text", text); post.put("mediaURL", mediaUrl);
            post.put("likeUids", new java.util.ArrayList<String>()); post.put("createdAt", FieldValue.serverTimestamp());
            db.collection("posts").add(post).addOnCompleteListener(task -> {
                if (task.isSuccessful()) sendPostResult(true, "Post published", task.getResult().getId());
                else sendPostResult(false, task.getException() == null ? "Could not publish post" : task.getException().getLocalizedMessage(), null);
            });
        }

        @JavascriptInterface
        public void createStory(String text, String dataUrl) {
            FirebaseUser user = auth.getCurrentUser();
            String cleanText = text == null ? "" : text.trim();
            if (user == null || cleanText.length() > 2000 || (cleanText.isEmpty() && (dataUrl == null || dataUrl.isEmpty()))) {
                sendStoryResult(false, "Sign in and add text or a photo first"); return;
            }
            if (dataUrl == null || dataUrl.isEmpty()) { saveStory(user, cleanText, ""); return; }
            uploadImage("stories/" + user.getUid() + "/" + UUID.randomUUID() + ".jpg", dataUrl, (url, error) -> {
                if (error != null) sendStoryResult(false, "Photo upload failed: " + error);
                else saveStory(user, cleanText, url);
            });
        }

        private void saveStory(FirebaseUser user, String text, String mediaUrl) {
            Map<String, Object> story = new HashMap<>();
            story.put("authorId", user.getUid()); story.put("text", text); story.put("mediaURL", mediaUrl);
            story.put("createdAt", FieldValue.serverTimestamp());
            story.put("expiresAt", new com.google.firebase.Timestamp(new java.util.Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000)));
            db.collection("stories").add(story).addOnCompleteListener(task -> {
                if (task.isSuccessful()) sendStoryResult(true, "Story added for 24 hours");
                else sendStoryResult(false, task.getException() == null ? "Could not publish story" : task.getException().getLocalizedMessage());
            });
        }

        @JavascriptInterface
        public void loadStories() {
            if (auth.getCurrentUser() == null) { sendStories("[]"); return; }
            db.collection("stories").whereGreaterThan("expiresAt", com.google.firebase.Timestamp.now())
                .orderBy("expiresAt", com.google.firebase.firestore.Query.Direction.ASCENDING).limit(100)
                .get().addOnCompleteListener(task -> {
                    org.json.JSONArray arr = new org.json.JSONArray();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            try {
                                JSONObject item = new JSONObject();
                                item.put("id", doc.getId());
                                item.put("authorId", doc.getString("authorId") == null ? "" : doc.getString("authorId"));
                                item.put("text", doc.getString("text") == null ? "" : doc.getString("text"));
                                item.put("mediaURL", doc.getString("mediaURL") == null ? "" : doc.getString("mediaURL"));
                                Object created = doc.get("createdAt");
                                item.put("createdAt", created instanceof com.google.firebase.Timestamp ? ((com.google.firebase.Timestamp) created).toDate().getTime() : System.currentTimeMillis());
                                arr.put(item);
                            } catch (Exception ignored) { }
                        }
                    }
                    sendStories(arr.toString());
                });
        }

        private void uploadImage(String path, String dataUrl, UploadCallback callback) {
            try {
                int comma = dataUrl.indexOf(',');
                byte[] bytes = Base64.decode(comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl, Base64.DEFAULT);
                StorageReference ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child(path);
                StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
                ref.putBytes(bytes, metadata).continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException() != null ? task.getException() : new Exception("Upload failed");
                    return ref.getDownloadUrl();
                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) callback.complete(task.getResult().toString(), null);
                    else callback.complete(null, task.getException() == null ? "Upload failed" : task.getException().getLocalizedMessage());
                });
            } catch (Exception ex) { callback.complete(null, ex.getLocalizedMessage()); }
        }

        private void sendStoryResult(boolean ok, String message) {
            try {
                JSONObject result = new JSONObject(); result.put("ok", ok); result.put("message", message == null ? "" : message);
                String json = result.toString();
                runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                    "window.onNativeStoryResult && window.onNativeStoryResult(" + JSONObject.quote(json) + ");", null); });
            } catch (Exception ignored) { }
        }
        private void sendStories(String json) {
            runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(
                "window.onNativeStoriesLoaded && window.onNativeStoriesLoaded(" + JSONObject.quote(json) + ");", null); });
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
