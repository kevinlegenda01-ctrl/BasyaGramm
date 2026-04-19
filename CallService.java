package com.basya.gramm.service;

import android.app.Notification;
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
import com.basya.gramm.ui.VideoCallActivity;
import org.json.JSONObject;

public class CallService extends Service {

    private SocketManager.OnCall callListener = new SocketManager.OnCall() {
        @Override
        public void onIncomingCall(String from, String roomId, boolean isVideo) {
            showCallNotif(from, roomId, isVideo);
        }
        @Override public void onCallAccepted(String r) {}
        @Override public void onCallRejected() {}
        @Override public void onCallEnded() {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(9999);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SocketManager.get().addCallListener(callListener);
        return START_STICKY;
    }

    private void showCallNotif(String from, String roomId, boolean isVideo) {
        Intent accept = new Intent(this, VideoCallActivity.class);
        accept.putExtra("room_id", roomId);
        accept.putExtra("with_user", from);
        accept.putExtra("is_outgoing", false);
        accept.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent piAccept = PendingIntent.getActivity(
            this, 0, accept,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = isVideo ? "📹 Видеозвонок" : "📞 Звонок";
        Notification n = new NotificationCompat.Builder(this, App.NOTIF_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText("@" + from + " звонит вам")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(piAccept, true)
            .addAction(android.R.drawable.ic_menu_call, "Ответить", piAccept)
            .setAutoCancel(true)
            .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(9999, n);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SocketManager.get().removeCallListener(callListener);
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
