package com.example.lab7;

import android.content.*;
import android.os.Build;

public class DownloadActionReceiver extends BroadcastReceiver {
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent svc = new Intent(context, DownloadService.class);
        svc.setAction(intent.getAction());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(svc);
        else
            context.startService(svc);
    }
}
