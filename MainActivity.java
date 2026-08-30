package com.example.braviaminiremote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private EditText ipEdit, pskEdit;
    private TextView status;
    private SharedPreferences prefs;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("tv", MODE_PRIVATE);

        status = findViewById(R.id.status);
        findViewById(R.id.power).setOnClickListener(v -> send("setPowerStatus",
                "{\"status\":true}"));
        findViewById(R.id.volUp).setOnClickListener(v -> send("setAudioVolume",
                "{\"target\":\"speaker\",\"volume\":\"+1\"}"));
        findViewById(R.id.volDown).setOnClickListener(v -> send("setAudioVolume",
                "{\"target\":\"speaker\",\"volume\":\"-1\"}"));
        findViewById(R.id.mute).setOnClickListener(v -> toggleMute());

        // Long-press the title to open setup.
        findViewById(R.id.title).setOnLongClickListener(v -> { showSetup(); return true; });

        if (prefs.getString("ip", "").isEmpty()) showSetup();
    }

    private void showSetup() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 10, 40, 10);

        ipEdit = new EditText(this);
        ipEdit.setHint("TV IP address, e.g. 192.168.1.50");
        ipEdit.setText(prefs.getString("ip", ""));
        box.addView(ipEdit);

        pskEdit = new EditText(this);
        pskEdit.setHint("PSK (if your TV requires one)");
        pskEdit.setText(prefs.getString("psk", ""));
        box.addView(pskEdit);

        new AlertDialog.Builder(this)
            .setTitle("Bravia setup")
            .setMessage("Phone and TV must be on the same Wi‑Fi.")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit().putString("ip", ipEdit.getText().toString().trim())
                    .putString("psk", pskEdit.getText().toString().trim()).apply();
                status.setText("Saved. Long-press SONY BRAVIA to change settings.");
            }).show();
    }

    private void toggleMute() {
        String ip = prefs.getString("ip", "");
        if (ip.isEmpty()) { showSetup(); return; }
        executor.execute(() -> {
            try {
                String current = post(ip, "/sony/audio",
                    "{\"method\":\"getVolumeInformation\",\"id\":1,\"params\":[],\"version\":\"1.0\"}");
                boolean muted = current.contains("\"mute\":true");
                send("setAudioMute", "{\"status\":" + (!muted) + "}");
            } catch (Exception e) { fail(e); }
        });
    }

    private void send(String method, String params) {
        String ip = prefs.getString("ip", "");
        if (ip.isEmpty()) { runOnUiThread(this::showSetup); return; }
        executor.execute(() -> {
            try {
                String path = method.startsWith("setAudio") ? "/sony/audio" : "/sony/system";
                String body = "{\"method\":\"" + method + "\",\"id\":1,\"params\":[" + params + "],\"version\":\"1.0\"}";
                post(ip, path, body);
                runOnUiThread(() -> status.setText("Command sent ✓"));
            } catch (Exception e) { fail(e); }
        });
    }

    private String post(String ip, String path, String body) throws Exception {
        URL url = new URL("http://" + ip + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(2500);
        c.setReadTimeout(2500);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        String psk = prefs.getString("psk", "");
        if (!psk.isEmpty()) c.setRequestProperty("X-Auth-PSK", psk);
        try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes("UTF-8")); }
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (is != null) { byte[] buf = new byte[1024]; int n; while ((n=is.read(buf))!=-1) out.write(buf,0,n); }
        c.disconnect();
        return out.toString("UTF-8");
    }

    private void fail(Exception e) {
        runOnUiThread(() -> status.setText("Couldn't reach TV. Check Wi‑Fi/IP/PSK."));
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
