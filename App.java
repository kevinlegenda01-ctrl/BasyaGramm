package com.basya.gramm;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

public class App extends Application {

    public static final String PREFS = "bg_prefs";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_DISPLAY = "display_name";
    public static final String KEY_AVATAR = "avatar_emoji";
    public static final String KEY_COINS = "coins";
    public static final String KEY_THEME = "theme";
    public static final String KEY_LOGGED = "logged_in";
    public static final String KEY_SERVER = "server_url";
    public static final String NOTIF_CH = "bg_messages";
    public static final String NOTIF_CALL = "bg_calls";

    // Сервер — можно менять в настройках приложения
    public static String SERVER_URL = "https://basyagramm.onrender.com";

    private static App inst;
    private static SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_SERVER, null);
        if (saved != null && !saved.isEmpty()) {
            SERVER_URL = saved;
        }
        createChannels();
    }

    public static App get() { return inst; }
    public static SharedPreferences prefs() { return prefs; }

    public static String username() { return prefs.getString(KEY_USERNAME, ""); }
    public static String displayName() { return prefs.getString(KEY_DISPLAY, ""); }
    public static String avatarEmoji() { return prefs.getString(KEY_AVATAR, "😊"); }
    public static int coins() { return prefs.getInt(KEY_COINS, 0); }
    public static String theme() { return prefs.getString(KEY_THEME, "dark"); }
    public static boolean loggedIn() { return prefs.getBoolean(KEY_LOGGED, false); }

    public static void saveUser(String u, String d, String a, int c) {
        prefs.edit()
            .putString(KEY_USERNAME, u)
            .putString(KEY_DISPLAY, d)
            .putString(KEY_AVATAR, a)
            .putInt(KEY_COINS, c)
            .putBoolean(KEY_LOGGED, true)
            .apply();
    }

    public static void saveTheme(String t) {
        prefs.edit().putString(KEY_THEME, t).apply();
    }

    public static void saveServer(String url) {
        SERVER_URL = url;
        prefs.edit().putString(KEY_SERVER, url).apply();
    }

    public static void logout() {
        prefs.edit().putBoolean(KEY_LOGGED, false).apply();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel msgCh = new NotificationChannel(
                NOTIF_CH, "Сообщения", NotificationManager.IMPORTANCE_HIGH);
            msgCh.setShowBadge(true);
            msgCh.enableVibration(true);

            NotificationChannel callCh = new NotificationChannel(
                NOTIF_CALL, "Звонки", NotificationManager.IMPORTANCE_MAX);
            callCh.setShowBadge(true);

            if (nm != null) {
                nm.createNotificationChannel(msgCh);
                nm.createNotificationChannel(callCh);
            }
        }
    }
}
