package com.skillacademy.wear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PromoService extends Service {
    private static final String TAG = "PromoService";
    private static final String CHANNEL_ID = "skillacademy_promos";
    private static final String PREFS_NAME = "skillacademy_prefs";
    private static final String KEY_NOTIFICATION_INTERVAL = "notification_interval";
    private static final String KEY_API_URL = "api_url";
    private static final int DEFAULT_INTERVAL_MS = 60000; // 60 seconds
    private static final String DEFAULT_URL = "http://10.0.2.2:3000/api/widget/wearable/promociones";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Promo> promociones = new ArrayList<>();
    private NotificationManager notificationManager;
    private SharedPreferences prefs;
    private boolean isRunning = false;
    private Runnable notificationRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startForegroundService();
            scheduleNotifications();
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SkillAcademy")
                .setContentText("Monitoreando promociones...")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(Color.rgb(33, 163, 102))
                .build();

        startForeground(1, notification);
    }

    private void scheduleNotifications() {
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    syncPromociones();
                    long interval = prefs.getLong(KEY_NOTIFICATION_INTERVAL, DEFAULT_INTERVAL_MS);
                    mainHandler.postDelayed(this, interval);
                }
            }
        };
        mainHandler.post(notificationRunnable);
    }

    private void syncPromociones() {
        executor.execute(() -> {
            try {
                String url = prefs.getString(KEY_API_URL, DEFAULT_URL);
                String response = get(url);
                List<Promo> fetched = parsePromos(response);
                mainHandler.post(() -> {
                    promociones.clear();
                    promociones.addAll(fetched);
                    showNotifications();
                });
            } catch (Exception error) {
                Log.e(TAG, "Error syncing promos: " + error.getMessage());
            }
        });
    }

    private String get(String endpoint) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);

        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8
        ));

        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        reader.close();
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status + ": " + body);
        }
        return body.toString();
    }

    private List<Promo> parsePromos(String response) throws Exception {
        JSONObject json = new JSONObject(response);
        JSONArray rows = json.optJSONArray("promociones");
        List<Promo> result = new ArrayList<>();

        if (rows == null) {
            Promo single = Promo.from(json);
            if (single != null) result.add(single);
            return result;
        }

        for (int i = 0; i < rows.length(); i++) {
            Promo promo = Promo.from(rows.getJSONObject(i));
            if (promo != null) {
                result.add(promo);
            }
        }
        return result;
    }

    private void showNotifications() {
        if (!canNotify()) return;

        int shown = 0;
        for (Promo promo : promociones) {
            Intent pinIntent = new Intent(this, PinLoginActivity.class);
            pinIntent.putExtra("force_auth", true);
            pinIntent.putExtra("promo_id", promo.id);
            pinIntent.putExtra("promo_title", promo.title);
            pinIntent.putExtra("promo_message", promo.message);
            pinIntent.putExtra("promo_url", promo.url != null ? promo.url : getString(R.string.default_web_url));

            PendingIntent pinPendingIntent = PendingIntent.getActivity(
                    this, promo.id, pinIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(promo.title)
                    .setContentText(promo.message)
                    .setStyle(new Notification.BigTextStyle().bigText(promo.message))
                    .setCategory(Notification.CATEGORY_PROMO)
                    .setColor(Color.rgb(33, 163, 102))
                    .setAutoCancel(true)
                    .setContentIntent(pinPendingIntent)
                    .build();

            notificationManager.notify(5000 + promo.id, notification);
            shown++;
        }
        Log.d(TAG, "Notificaciones enviadas: " + shown);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Promociones SkillAcademy",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Promociones activas publicadas desde SkillAcademy");
        notificationManager.createNotificationChannel(channel);
    }

    private boolean canNotify() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (notificationRunnable != null) {
            mainHandler.removeCallbacks(notificationRunnable);
        }
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void setNotificationInterval(Context context, long intervalMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_NOTIFICATION_INTERVAL, intervalMs).apply();
    }

    public static long getNotificationInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_NOTIFICATION_INTERVAL, DEFAULT_INTERVAL_MS);
    }

    public static void setApiUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_API_URL, url).apply();
    }

    public static String getApiUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_API_URL, DEFAULT_URL);
    }

    private static class Promo {
        final int id;
        final String title;
        final String message;
        final String url;

        Promo(int id, String title, String message, String url) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.url = url;
        }

        static Promo from(JSONObject json) {
            String title = json.optString("titulo", "").trim();
            if (title.isEmpty()) return null;
            String subtitle = json.optString("subtitulo", "Promocion activa").trim();
            String message = json.optString("mensaje", json.optString("descripcion", subtitle)).trim();
            if (message.isEmpty()) message = subtitle;
            String url = json.optString("enlace", "").trim();
            return new Promo(json.optInt("id", title.hashCode()), title, message, url.isEmpty() ? null : url);
        }
    }
}