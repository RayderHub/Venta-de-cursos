package com.skillacademy.wear;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PinLoginActivity extends Activity {
    private static final String PREFS_NAME = "skillacademy_prefs";
    private static final String KEY_PIN = "user_pin";
    private static final String KEY_PIN_SET = "pin_is_set";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    private TextView pinDots;
    private TextView subtitleText;
    private TextView statusText;
    private Button confirmButton;
    private boolean isSettingPin = false;
    private String firstPinEntry = "";
    private String pinBuffer = "";
    private SharedPreferences prefs;
    private boolean isRound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isRound = getResources().getConfiguration().isScreenRound();

        boolean pinIsSet = prefs.getBoolean(KEY_PIN_SET, false);
        boolean isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);

        // If already logged in and not forced to re-authenticate, go to web
        if (isLoggedIn && !getIntent().getBooleanExtra("force_auth", false)) {
            navigateToWeb();
            return;
        }

        if (!pinIsSet) {
            isSettingPin = true;
            showSetupPinUI();
        } else {
            showEnterPinUI();
        }
    }

    private void showSetupPinUI() {
        LinearLayout root = buildRoot();

        TextView title = new TextView(this);
        title.setText("Configurar PIN");
        title.setTextSize(20);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0));

        subtitleText = new TextView(this);
        subtitleText.setText("Crea un PIN de 4 dígitos");
        subtitleText.setTextSize(12);
        subtitleText.setTextColor(0xFF94E5B8);
        subtitleText.setGravity(Gravity.CENTER);
        root.addView(subtitleText, matchWrap(2));

        addDots(root);

        buildKeypad(root, "Continuar");

        statusText = new TextView(this);
        statusText.setText("");
        statusText.setTextSize(13);
        statusText.setTextColor(0xFF94E5B8);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, matchWrap(2));

        setContentView(root);
    }

    private void showEnterPinUI() {
        LinearLayout root = buildRoot();

        TextView title = new TextView(this);
        title.setText("SkillAcademy");
        title.setTextSize(20);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0));

        subtitleText = new TextView(this);
        subtitleText.setText("Ingresa tu PIN de 4 dígitos");
        subtitleText.setTextSize(12);
        subtitleText.setTextColor(0xFF94E5B8);
        subtitleText.setGravity(Gravity.CENTER);
        root.addView(subtitleText, matchWrap(2));

        addDots(root);

        buildKeypad(root, "Ingresar");

        TextView forgotText = new TextView(this);
        forgotText.setText("¿Olvidaste tu PIN?");
        forgotText.setTextSize(12);
        forgotText.setTextColor(0xFF64748B);
        forgotText.setGravity(Gravity.CENTER);
        forgotText.setOnClickListener(v -> resetPin());
        root.addView(forgotText, matchWrap(4));

        statusText = new TextView(this);
        statusText.setText("");
        statusText.setTextSize(13);
        statusText.setTextColor(0xFFEF4444);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, matchWrap(2));

        setContentView(root);
    }

    private LinearLayout buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int sidePad = isRound ? (int) (getResources().getDisplayMetrics().widthPixels * 0.05f) : 10;
        root.setPadding(sidePad, 6, sidePad, 6);
        root.setBackgroundColor(0xFF101923);
        return root;
    }

    private void addDots(LinearLayout root) {
        pinDots = new TextView(this);
        pinDots.setTextSize(24);
        pinDots.setTextColor(0xFF94E5B8);
        pinDots.setGravity(Gravity.CENTER);
        pinDots.setIncludeFontPadding(false);
        root.addView(pinDots, matchWrap(4));
        updateDots();
    }

    private void buildKeypad(LinearLayout root, String confirmLabel) {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int heightPx = getResources().getDisplayMetrics().heightPixels;
        int gap = Math.max(4, (int) (screenW * 0.015f));
        int btnH = Math.max(36, (heightPx - 190) / 4);

        addDigitRow(root, new String[]{"1", "2", "3"}, btnH, gap, 0);
        addDigitRow(root, new String[]{"4", "5", "6"}, btnH, gap, gap);
        addDigitRow(root, new String[]{"7", "8", "9"}, btnH, gap, gap);

        LinearLayout special = new LinearLayout(this);
        special.setOrientation(LinearLayout.HORIZONTAL);
        special.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams specialLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        specialLp.topMargin = gap;
        root.addView(special, specialLp);

        Button back = keyButton("⌫", 0xFF475569, 18);
        back.setOnClickListener(v -> deleteDigit());
        special.addView(back, keyLp(btnH, gap));

        Button zero = keyButton("0", 0xFF1C2A38, 22);
        zero.setOnClickListener(v -> appendDigit("0"));
        special.addView(zero, keyLp(btnH, gap));

        confirmButton = keyButton(confirmLabel, 0xFF21A366, 14);
        confirmButton.setOnClickListener(v -> confirmAction());
        special.addView(confirmButton, keyLp(btnH, gap));
    }

    private void addDigitRow(LinearLayout root, String[] digits, int btnH, int gap, int topMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowLp.topMargin = topMargin;
        root.addView(row, rowLp);

        for (String digit : digits) {
            Button b = keyButton(digit, 0xFF1C2A38, 22);
            b.setOnClickListener(v -> appendDigit(digit));
            row.addView(b, keyLp(btnH, gap));
        }
    }

    private Button keyButton(String label, int bg, int textSize) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundColor(bg);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private LinearLayout.LayoutParams keyLp(int height, int gap) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
        lp.setMargins(gap / 2, 0, gap / 2, 0);
        return lp;
    }

    private void appendDigit(String digit) {
        if (pinBuffer.length() >= 4) return;
        pinBuffer += digit;
        updateDots();
    }

    private void deleteDigit() {
        if (pinBuffer.length() == 0) return;
        pinBuffer = pinBuffer.substring(0, pinBuffer.length() - 1);
        updateDots();
    }

    private void updateDots() {
        if (pinDots == null) return;
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < pinBuffer.length(); i++) {
            dots.append("●");
        }
        for (int i = pinBuffer.length(); i < 4; i++) {
            dots.append("○");
        }
        pinDots.setText(dots.toString());
    }

    private void confirmAction() {
        String pin = pinBuffer;

        if (pin.length() != 4) {
            statusText.setText("El PIN debe tener 4 dígitos");
            return;
        }

        if (isSettingPin && firstPinEntry.isEmpty()) {
            firstPinEntry = pin;
            pinBuffer = "";
            updateDots();
            subtitleText.setText("Confirma tu PIN");
            confirmButton.setText("Confirmar");
            statusText.setText("");
            return;
        }

        if (isSettingPin) {
            if (pin.equals(firstPinEntry)) {
                prefs.edit()
                        .putString(KEY_PIN, pin)
                        .putBoolean(KEY_PIN_SET, true)
                        .putBoolean(KEY_LOGGED_IN, true)
                        .commit();
                Toast.makeText(this, "PIN configurado correctamente", Toast.LENGTH_SHORT).show();
                navigateToWeb();
            } else {
                statusText.setText("Los PINs no coinciden. Intenta de nuevo.");
                firstPinEntry = "";
                pinBuffer = "";
                updateDots();
                subtitleText.setText("Crea un PIN de 4 dígitos");
                confirmButton.setText("Continuar");
            }
        } else {
            String savedPin = prefs.getString(KEY_PIN, "");
            if (pin.equals(savedPin)) {
                prefs.edit().putBoolean(KEY_LOGGED_IN, true).commit();
                navigateToWeb();
            } else {
                statusText.setText("PIN incorrecto. Intenta de nuevo.");
                pinBuffer = "";
                updateDots();
            }
        }
    }

    private void resetPin() {
        prefs.edit().putBoolean(KEY_PIN_SET, false).remove(KEY_PIN).putBoolean(KEY_LOGGED_IN, false).commit();
        isSettingPin = true;
        firstPinEntry = "";
        pinBuffer = "";
        showSetupPinUI();
    }

    private void navigateToWeb() {
        try {
            Intent intent = new Intent(this, PromoWebActivity.class);
            intent.putExtra("from_notification", getIntent().getBooleanExtra("from_notification", false));
            intent.putExtra("promo_id", getIntent().getIntExtra("promo_id", -1));
            intent.putExtra("promo_title", getIntent().getStringExtra("promo_title"));
            intent.putExtra("promo_message", getIntent().getStringExtra("promo_message"));
            intent.putExtra("promo_url", getIntent().getStringExtra("promo_url"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            if (statusText != null) {
                statusText.setText("Error al abrir promociones: " + e.getMessage());
            }
        }
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = topMargin;
        return params;
    }
}
