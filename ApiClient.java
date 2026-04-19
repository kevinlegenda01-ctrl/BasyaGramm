package com.basya.gramm.network;

import android.util.Log;
import com.basya.gramm.App;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final ExecutorService exec = Executors.newFixedThreadPool(4);

    public interface CB {
        void ok(JSONObject r);
        void err(String e);
    }

    // ── POST JSON ──────────────────────────────────────────────────────
    private static void post(final String path, final JSONObject body, final CB cb) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(App.SERVER_URL + path);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    c.setDoOutput(true);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(15000);
                    OutputStream os = c.getOutputStream();
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    os.close();
                    int code = c.getResponseCode();
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        code < 400 ? c.getInputStream() : c.getErrorStream(),
                        StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    cb.ok(new JSONObject(sb.toString()));
                } catch (Exception e) {
                    Log.e(TAG, "POST " + path, e);
                    cb.err(e.getMessage());
                }
            }
        });
    }

    // ── GET ────────────────────────────────────────────────────────────
    private static void get(final String path, final CB cb) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(App.SERVER_URL + path);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(15000);
                    int code = c.getResponseCode();
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        code < 400 ? c.getInputStream() : c.getErrorStream(),
                        StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    cb.ok(new JSONObject(sb.toString()));
                } catch (Exception e) {
                    Log.e(TAG, "GET " + path, e);
                    cb.err(e.getMessage());
                }
            }
        });
    }

    // ── MULTIPART upload ───────────────────────────────────────────────
    public static void uploadFile(final String username, final File file,
                                   final String type, final CB cb) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String boundary = "BGBoundary" + System.currentTimeMillis();
                    URL url = new URL(App.SERVER_URL + "/api/upload");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    c.setDoOutput(true);
                    c.setConnectTimeout(30000);
                    c.setReadTimeout(30000);

                    OutputStream os = c.getOutputStream();
                    String nl = "\r\n";
                    String part1 = "--" + boundary + nl
                        + "Content-Disposition: form-data; name=\"username\"" + nl + nl
                        + username + nl;
                    os.write(part1.getBytes(StandardCharsets.UTF_8));

                    String part2 = "--" + boundary + nl
                        + "Content-Disposition: form-data; name=\"type\"" + nl + nl
                        + type + nl;
                    os.write(part2.getBytes(StandardCharsets.UTF_8));

                    String part3 = "--" + boundary + nl
                        + "Content-Disposition: form-data; name=\"file\"; filename=\""
                        + file.getName() + "\"" + nl
                        + "Content-Type: application/octet-stream" + nl + nl;
                    os.write(part3.getBytes(StandardCharsets.UTF_8));

                    FileInputStream fis = new FileInputStream(file);
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                    fis.close();

                    os.write((nl + "--" + boundary + "--" + nl).getBytes(StandardCharsets.UTF_8));
                    os.close();

                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        c.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    cb.ok(new JSONObject(sb.toString()));
                } catch (Exception e) {
                    Log.e(TAG, "Upload error", e);
                    cb.err(e.getMessage());
                }
            }
        });
    }

    // ── Auth ───────────────────────────────────────────────────────────
    public static void register(String u, String p, String d, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("username", u);
            b.put("password", p);
            b.put("display_name", d);
            post("/api/register", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }

    public static void login(String u, String p, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("username", u);
            b.put("password", p);
            post("/api/login", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }

    // ── Chats ──────────────────────────────────────────────────────────
    public static void getChats(String u, CB cb) {
        get("/api/get_chats?username=" + u, cb);
    }

    public static void getMessages(String chatId, int offset, CB cb) {
        get("/api/get_messages?chat_id=" + chatId + "&offset=" + offset + "&limit=50", cb);
    }

    // ── Search ─────────────────────────────────────────────────────────
    public static void search(String q, CB cb) {
        get("/api/search?q=" + q, cb);
    }

    // ── Groups / Channels ──────────────────────────────────────────────
    public static void createGroup(String owner, String name, String desc, String emoji, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("owner", owner);
            b.put("name", name);
            b.put("description", desc);
            b.put("avatar_emoji", emoji);
            b.put("type", "group");
            post("/api/create_chat", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }

    public static void createChannel(String owner, String name, String desc, String emoji, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("owner", owner);
            b.put("name", name);
            b.put("description", desc);
            b.put("avatar_emoji", emoji);
            b.put("type", "channel");
            post("/api/create_chat", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }

    // ── Coins ──────────────────────────────────────────────────────────
    public static void getBalance(String u, CB cb) {
        get("/api/coins/balance?username=" + u, cb);
    }

    public static void farm(String u, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("username", u);
            post("/api/coins/farm", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }

    public static void getTop(CB cb) {
        get("/api/coins/top", cb);
    }

    // ── Profile ────────────────────────────────────────────────────────
    public static void updateProfile(String u, String name, String bio, String emoji, CB cb) {
        try {
            JSONObject b = new JSONObject();
            b.put("username", u);
            b.put("display_name", name);
            b.put("bio", bio);
            b.put("avatar_emoji", emoji);
            post("/api/update_profile", b, cb);
        } catch (Exception e) { cb.err(e.getMessage()); }
    }
}
