package com.basya.gramm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.basya.gramm.App;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && App.loggedIn()) {
            ctx.startService(new Intent(ctx, MessageService.class));
        }
    }
}
