package com.basya.gramm.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.SocketManager;
import com.basya.gramm.ui.MainActivity;
import org.json.JSONObject;

public class MessageService extends Service {

    private static int notifId = 2000;

    private SocketManager.OnMessage listener = new SocketManager.OnMessage() {
        @Override
        public void onNew(JSONObject msg) {
            try {
                String from = msg.optString("from", "");
                String text = msg.optString("text", "Новое сообщение");
                if (!from.equals(App.username())
                    && App.prefs().getBoolean("notif", true)) {
                    notify(from, text);
                }
            } catch (Exception e) { /* ignore */ }
        }
        @Override
        public void onSent(JSONObject msg) {}
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (App.loggedIn()) {
            SocketManager.get().connect();
            SocketManager.get().addMsgListener(listener);
        }
        return START_STICKY;
    }

    private void notify(String from, String text) {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, App.NOTIF_CH)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("🐱 @" + from)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId++, nb.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SocketManager.get().removeMsgListener(listener);
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
