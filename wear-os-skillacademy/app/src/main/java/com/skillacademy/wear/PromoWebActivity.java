package com.skillacademy.wear;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

public class PromoWebActivity extends Activity {
    private static final String API_URL = "http://10.0.2.2:3000/api/widget/wearable/promociones";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ScrollView rootLayout;
    private LinearLayout contentLayout;
    private boolean isRound;
    private int padding;
    private List<Promo> allPromos = new ArrayList<>();
    private boolean showingDetail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRound = getResources().getConfiguration().isScreenRound();
        padding = isRound ? (int) (getResources().getDisplayMetrics().widthPixels * 0.10f) : 16;

        rootLayout = new ScrollView(this);
        rootLayout.setBackgroundColor(Color.rgb(16, 25, 35));
        rootLayout.setFillViewport(true);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(padding, padding, padding, padding);

        rootLayout.addView(contentLayout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(rootLayout);
        loadPromos();
    }

    private void loadPromos() {
        showLoading();
        executor.execute(() -> {
            try {
                String response = get(API_URL);
                List<Promo> fetched = parsePromos(response);
                mainHandler.post(() -> renderPromos(fetched));
            } catch (Exception error) {
                mainHandler.post(() -> showError());
            }
        });
    }

    private void showLoading() {
        contentLayout.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Cargando promociones...");
        loading.setTextSize(14);
        loading.setTextColor(Color.rgb(148, 229, 184));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, 20, 0, 20);
        contentLayout.addView(loading);
    }

    private void showError() {
        contentLayout.removeAllViews();
        TextView error = new TextView(this);
        error.setText("No se pudieron cargar las promociones.\nVerifica tu conexión.");
        error.setTextSize(13);
        error.setTextColor(Color.rgb(248, 113, 113));
        error.setGravity(Gravity.CENTER);
        error.setPadding(0, 20, 0, 20);
        contentLayout.addView(error);
    }

    private void renderPromos(List<Promo> promos) {
        allPromos = promos;
        showingDetail = false;
        contentLayout.removeAllViews();

        TextView header = new TextView(this);
        header.setText("Promociones");
        header.setTextSize(20);
        header.setTextColor(Color.WHITE);
        header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        contentLayout.addView(header, matchWrap(0));

        TextView subHeader = new TextView(this);
        subHeader.setText("Ofertas vigentes para ti");
        subHeader.setTextSize(12);
        subHeader.setTextColor(Color.rgb(148, 229, 184));
        subHeader.setGravity(Gravity.CENTER);
        contentLayout.addView(subHeader, matchWrap(4));

        if (promos.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No hay promociones vigentes en este momento.");
            empty.setTextSize(13);
            empty.setTextColor(Color.rgb(248, 113, 113));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 20, 0, 20);
            contentLayout.addView(empty, matchWrap(12));
        }

        for (Promo promo : promos) {
            contentLayout.addView(buildPromoCard(promo), matchWrap(10));
        }

        TextView footer = new TextView(this);
        footer.setText("Para conocer más detalles sobre las promociones, visita el sitio web en tu dispositivo de cómputo.");
        footer.setTextSize(11);
        footer.setTextColor(Color.rgb(148, 229, 184));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 8, 0, 16);
        contentLayout.addView(footer, matchWrap(16));

        Button exitButton = new Button(this);
        exitButton.setText("Salir");
        exitButton.setAllCaps(false);
        exitButton.setTextColor(Color.WHITE);
        exitButton.setBackgroundColor(Color.rgb(71, 85, 105));
        exitButton.setOnClickListener(v -> finish());
        contentLayout.addView(exitButton, matchWrap(12));
    }

    private View buildPromoCard(Promo promo) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.rgb(28, 42, 56));
        card.setPadding(16, 14, 16, 14);

        float radius = 16f * getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(Color.rgb(28, 42, 56));
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, Color.rgb(38, 53, 71));
        card.setBackground(drawable);

        TextView title = new TextView(this);
        title.setText(promo.title);
        title.setTextSize(15);
        title.setTextColor(Color.WHITE);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Promoción vigente");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(148, 229, 184));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 4, 0, 0);
        card.addView(subtitle);

        card.setOnClickListener(v -> showDetail(promo));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = 4;
        card.setLayoutParams(cardParams);

        return card;
    }

    private void showDetail(Promo promo) {
        showingDetail = true;
        contentLayout.removeAllViews();

        TextView header = new TextView(this);
        header.setText(promo.title);
        header.setTextSize(18);
        header.setTextColor(Color.WHITE);
        header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        contentLayout.addView(header, matchWrap(0));

        TextView message = new TextView(this);
        message.setText(promo.message);
        message.setTextSize(14);
        message.setTextColor(Color.rgb(220, 226, 230));
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(4, 1.2f);
        contentLayout.addView(message, matchWrap(14));

        TextView hint = new TextView(this);
        hint.setText("Para conocer más detalles sobre las promociones, visita el sitio web en tu dispositivo de cómputo.");
        hint.setTextSize(11);
        hint.setTextColor(Color.rgb(148, 229, 184));
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(2, 1.3f);
        contentLayout.addView(hint, matchWrap(16));

        Button backButton = new Button(this);
        backButton.setText("Volver a promociones");
        backButton.setAllCaps(false);
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.rgb(71, 85, 105));
        backButton.setOnClickListener(v -> {
            renderPromos(allPromos);
            rootLayout.scrollTo(0, 0);
        });
        contentLayout.addView(backButton, matchWrap(20));

        Button exitButton = new Button(this);
        exitButton.setText("Salir");
        exitButton.setAllCaps(false);
        exitButton.setTextColor(Color.WHITE);
        exitButton.setBackgroundColor(Color.rgb(185, 28, 28));
        exitButton.setOnClickListener(v -> finish());
        contentLayout.addView(exitButton, matchWrap(10));
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = topMargin;
        return params;
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

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
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
