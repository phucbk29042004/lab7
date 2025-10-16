package com.example.lab7;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.*;
import android.os.Build;

public class MainActivity extends AppCompatActivity {
    private EditText edtUrl;
    private ProgressBar progressBar;
    private Button btnDownload;

    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            progressBar.setProgress(progress);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationUtils.createChannel(this);

        edtUrl = findViewById(R.id.edtUrl);
        progressBar = findViewById(R.id.progressBar);
        btnDownload = findViewById(R.id.btnDownload);

        btnDownload.setOnClickListener(v -> {
            String url = edtUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập URL!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, DownloadService.class);
            intent.putExtra("url", url);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent);
            else
                startService(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(progressReceiver, new IntentFilter("DOWNLOAD_PROGRESS"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(progressReceiver);
    }
}
