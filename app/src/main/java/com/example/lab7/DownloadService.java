package com.example.lab7;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    private static final int NOTIF_ID = 1;
    private boolean isPaused = false;
    private boolean isCanceled = false;
    private File outputFile;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationUtils.createChannel(this); // đảm bảo đã có Notification Channel

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case DownloadActionReceiver.ACTION_PAUSE:
                    isPaused = true;
                    return START_STICKY;
                case DownloadActionReceiver.ACTION_RESUME:
                    isPaused = false;
                    return START_STICKY;
                case DownloadActionReceiver.ACTION_CANCEL:
                    isCanceled = true;
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        String url = intent.getStringExtra("url");
        if (url != null)
            startForeground(NOTIF_ID, createNotification(0));

        new Thread(() -> downloadFile(url)).start();
        return START_STICKY;
    }

    private void downloadFile(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            String fileName = Uri.parse(urlStr).getLastPathSegment();
            if (fileName == null) fileName = "downloaded_file";

            outputFile = new File(getFilesDir(), fileName);
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int len;
            long total = conn.getContentLength();
            long downloaded = 0;

            while ((len = in.read(buffer)) != -1) {
                if (isCanceled) break;

                while (isPaused) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                }

                out.write(buffer, 0, len);
                downloaded += len;

                int progress = (int) (downloaded * 100 / total);
                updateNotification(progress);

                Intent i = new Intent("DOWNLOAD_PROGRESS");
                i.putExtra("progress", progress);
                sendBroadcast(i);
            }

            out.close();
            in.close();

            if (!isCanceled)
                showDoneNotification();

            stopSelf();

        } catch (Exception e) {
            Log.e("DownloadService", "Error downloading: " + e.getMessage());
        }
    }

    private Notification createNotification(int progress) {
        RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_download);
        view.setProgressBar(R.id.progressBar, 100, progress, false);

        // Tạo các PendingIntent cho các nút điều khiển
        PendingIntent pause = PendingIntent.getBroadcast(
                this, 0,
                new Intent(DownloadActionReceiver.ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent resume = PendingIntent.getBroadcast(
                this, 1,
                new Intent(DownloadActionReceiver.ACTION_RESUME),
                PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent cancel = PendingIntent.getBroadcast(
                this, 2,
                new Intent(DownloadActionReceiver.ACTION_CANCEL),
                PendingIntent.FLAG_IMMUTABLE
        );

        // Gán nút bấm vào layout
        view.setOnClickPendingIntent(R.id.btnPause, pause);
        view.setOnClickPendingIntent(R.id.btnResume, resume);
        view.setOnClickPendingIntent(R.id.btnCancel, cancel);

        return new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
                // Dùng icon hệ thống nếu bạn chưa có ic_download
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setCustomContentView(view)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(int progress) {
        Notification notification = createNotification(progress);
        // ✅ Kiểm tra quyền POST_NOTIFICATIONS trước khi notify
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notification);
        }
    }

    private void showDoneNotification() {
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
                .setContentTitle("Download complete")
                .setContentText("Your file has been saved.")
                // Dùng icon hệ thống nếu chưa có ic_done
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build();

        // ✅ Kiểm tra quyền trước khi hiển thị
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(NOTIF_ID + 1, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
