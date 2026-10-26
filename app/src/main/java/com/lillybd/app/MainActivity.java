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
