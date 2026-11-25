
package com.example.myapplication1;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etToken, etGistId, etFileName, etDeviceId, etNote;
    private LinearLayout layoutSettings, layoutKeyList;
    private RadioGroup rgDuration;
    private RadioButton rb7Days, rb30Days, rbLifetime;
   // private Button btnGeneratePush;
    private TextView tvStatus;
    private SharedPreferences prefs;
    private JSONObject cachedKeys = new JSONObject();
    private Button btnGeneratePush, btnPaste, btnSearch;
    private boolean isSearchMode = false;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("GistConfig", MODE_PRIVATE);

         LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 36, 36, 36);

        // Header + Settings Toggle
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("Gist Key Updater");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        Button btnSettings = new Button(this);
        btnSettings.setText("Settings");
        header.addView(title);
        header.addView(btnSettings);
        root.addView(header);

        // Settings Panel
                boolean isNight = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int panelBg = isNight ? Color.parseColor("#1E1E1E") : Color.parseColor("#F2F2F2");
        int panelText = isNight ? Color.WHITE : Color.BLACK;

        // Settings Panel
        layoutSettings = new LinearLayout(this);
        layoutSettings.setOrientation(LinearLayout.VERTICAL);
        layoutSettings.setBackgroundColor(panelBg);
        layoutSettings.setPadding(24, 24, 24, 24);

        etToken = makeInput("GitHub Token");
        etToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etGistId = makeInput("Gist ID");
        etFileName = makeInput("Filename (e.g. key.json)");
        Button btnSave = new Button(this);
        btnSave.setText("Save Config");

        layoutSettings.addView(etToken);
        layoutSettings.addView(etGistId);
        layoutSettings.addView(etFileName);
        layoutSettings.addView(btnSave);
        root.addView(layoutSettings);

        // Device ID + Paste
        LinearLayout devRow = new LinearLayout(this);
        devRow.setOrientation(LinearLayout.HORIZONTAL);
        etDeviceId = makeInput("Device ID");
        etDeviceId.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnPaste = new Button(this);
        btnPaste.setText("Paste");
        devRow.addView(etDeviceId);
        devRow.addView(btnPaste);
        root.addView(devRow);

        etNote = makeInput("Buyer Name / Note");
        root.addView(etNote);

                // Duration Radios
        rgDuration = new RadioGroup(this);
        rgDuration.setOrientation(LinearLayout.HORIZONTAL);

        rb7Days = new RadioButton(this);
        rb7Days.setId(View.generateViewId());
        rb7Days.setText("7 Days");

        rb30Days = new RadioButton(this);
        rb30Days.setId(View.generateViewId());
        rb30Days.setText("30 Days");

        rbLifetime = new RadioButton(this);
        rbLifetime.setId(View.generateViewId());
        rbLifetime.setText("Lifetime");

        rgDuration.addView(rb7Days);
        rgDuration.addView(rb30Days);
        rgDuration.addView(rbLifetime);
        rb7Days.setChecked(true);
        root.addView(rgDuration);


        btnGeneratePush = new Button(this);
        btnGeneratePush.setText("Generate Key & Push to Gist");
        root.addView(btnGeneratePush);

                tvStatus = new TextView(this);
        tvStatus.setText("Ready.");
        tvStatus.setPadding(24, 20, 24, 20);
        tvStatus.setBackgroundColor(panelBg);
        tvStatus.setTextColor(panelText);
        root.addView(tvStatus);


                // List Header (Fixed)
        LinearLayout listHead = new LinearLayout(this);
        listHead.setOrientation(LinearLayout.HORIZONTAL);
        listHead.setPadding(0, 20, 0, 8);

        TextView listTitle = new TextView(this);
        listTitle.setText("Saved Keys");
        listTitle.setTypeface(null, Typeface.BOLD);
        listTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        btnSearch = new Button(this);
        btnSearch.setText("Search");

        Button btnRefresh = new Button(this);
        btnRefresh.setText("Refresh");

        listHead.addView(listTitle);
        listHead.addView(btnSearch);
        listHead.addView(btnRefresh);
        root.addView(listHead);


        // Scrollable Key List ONLY
        ScrollView listScroll = new ScrollView(this);
        listScroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        layoutKeyList = new LinearLayout(this);
        layoutKeyList.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(layoutKeyList);
        root.addView(listScroll);

        setContentView(root);
;

        // Listeners
        loadConfig();
        btnSettings.setOnClickListener(v -> layoutSettings.setVisibility(
                layoutSettings.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        btnSave.setOnClickListener(v -> saveConfig());
                btnPaste.setOnClickListener(v -> {
            if (isSearchMode) {
                etDeviceId.setText("");
            } else {
                pasteClipboard();
            }
        });

        btnSearch.setOnClickListener(v -> toggleSearchMode());

        etDeviceId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSearchMode) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.US);
                    renderList();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnGeneratePush.setOnClickListener(v -> addNewKey());
        btnRefresh.setOnClickListener(v -> fetchGistData());
    }
    private void toggleSearchMode() {
        isSearchMode = !isSearchMode;
        etDeviceId.setText("");
        searchQuery = "";

        if (isSearchMode) {
            btnSearch.setText("Exit Search");
            etDeviceId.setHint("Search Buyer, Device ID, or Key...");
            btnPaste.setText("Clear");
            etNote.setVisibility(View.GONE);
            rgDuration.setVisibility(View.GONE);
            btnGeneratePush.setVisibility(View.GONE);
            etDeviceId.requestFocus();
        } else {
            btnSearch.setText("Search");
            etDeviceId.setHint("Device ID");
            btnPaste.setText("Paste");
            etNote.setVisibility(View.VISIBLE);
            rgDuration.setVisibility(View.VISIBLE);
            btnGeneratePush.setVisibility(View.VISIBLE);
        }
        renderList();
    }

    private EditText makeInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setSingleLine(true);
        return et;
    }

    private void loadConfig() {
        String token = prefs.getString("token", "");
        String gistId = prefs.getString("gistId", "");
        String fileName = prefs.getString("fileName", "key.json");
        etToken.setText(token);
        etGistId.setText(gistId);
        etFileName.setText(fileName);
        if (!token.isEmpty() && !gistId.isEmpty()) {
            layoutSettings.setVisibility(View.GONE);
            fetchGistData();
        }
    }

    private void saveConfig() {
        String token = etToken.getText().toString().trim();
        String gistId = etGistId.getText().toString().trim();
        String fileName = etFileName.getText().toString().trim();
        if (fileName.isEmpty()) fileName = "key.json";
        prefs.edit().putString("token", token).putString("gistId", gistId).putString("fileName", fileName).apply();
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        layoutSettings.setVisibility(View.GONE);
        fetchGistData();
    }

    private void pasteClipboard() {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null) {
            CharSequence t = cb.getPrimaryClip().getItemAt(0).getText();
            if (t != null) etDeviceId.setText(t.toString().trim());
        }
    }

    private String generateKey() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder("MOD-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String getExpiry() {
        Calendar cal = Calendar.getInstance();
        if (rbLifetime.isChecked()) {
            cal.add(Calendar.DAY_OF_YEAR, 9000);
        } else {
            cal.add(Calendar.DAY_OF_YEAR, rb30Days.isChecked() ? 30 : 7);
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private boolean isExpired(String exp) {
        if (exp == null || exp.isEmpty() || exp.equalsIgnoreCase("LIFETIME")) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            java.util.Date expDate = sdf.parse(exp);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return expDate != null && expDate.before(today.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    private URL buildUrl(String gistId) throws Exception {
        String clean = gistId.contains("/") ? gistId.substring(gistId.lastIndexOf("/") + 1) : gistId;
        String base = "https://" + "api." + "github" + ".com/gists/";
        return new URL(base + clean);
    }

        private void fetchGistData() {
        String token = etToken.getText().toString().trim();
        String gistId = etGistId.getText().toString().trim();
        String file = etFileName.getText().toString().trim();
        if (token.isEmpty() || gistId.isEmpty()) return;
        final String finalFile = file.isEmpty() ? "key.json" : file;

        tvStatus.setText("Loading keys...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) buildUrl(gistId).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                if (code != 200) throw new Exception("HTTP " + code + ": " + sb);

                JSONObject gist = new JSONObject(sb.toString());
                String content = gist.getJSONObject("files").getJSONObject(finalFile).getString("content");
                cachedKeys = content.trim().isEmpty() ? new JSONObject() : new JSONObject(content);

                boolean hasExpiredUpdates = false;
                Iterator<?> it = cachedKeys.keys();
                while (it.hasNext()) {
                    String id = String.valueOf(it.next());
                    JSONObject item = cachedKeys.optJSONObject(id);
                    if (item != null && item.optBoolean("active", true)) {
                        if (isExpired(item.optString("expiry", ""))) {
                            item.put("active", false);
                            hasExpiredUpdates = true;
                        }
                    }
                }

                final boolean autoSync = hasExpiredUpdates;
                runOnUiThread(() -> {
                    if (autoSync) {
                        pushGist("Auto-deactivated expired keys!", null, false);
                    } else {
                        tvStatus.setText("Loaded " + cachedKeys.length() + " keys.");
                        renderList();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Load Error: " + e.getMessage()));
            }
        });
    }

private void addNewKey() {
String devId = etDeviceId.getText().toString().trim();
String note = etNote.getText().toString().trim();
if (devId.isEmpty()) {
Toast.makeText(this, "Ilagay ang Device ID", Toast.LENGTH_SHORT).show();
return;
}
String key = generateKey();
String exp = getExpiry();
try {
JSONObject entry = new JSONObject();
entry.put("key", key);
entry.put("note", note.isEmpty() ? "User" : note);
entry.put("expiry", exp);
entry.put("active", true);
cachedKeys.put(devId, entry);
pushGist("Added " + devId + "\nKey: " + key + " (Copied!)", key, true);
} catch (Exception e) {
tvStatus.setText("Error: " + e.getMessage());
}
}
private void pushGist(String msg, String copyKey, boolean clear) {
String token = etToken.getText().toString().trim();
String gistId = etGistId.getText().toString().trim();
String file = etFileName.getText().toString().trim();
if (token.isEmpty() || gistId.isEmpty()) return;
final String finalFile = file.isEmpty() ? "key.json" : file;
btnGeneratePush.setEnabled(false);
tvStatus.setText("Updating Gist...");
Executors.newSingleThreadExecutor().execute(() -> {
try {
JSONObject fc = new JSONObject();
fc.put("content", cachedKeys.toString(2));
JSONObject files = new JSONObject();
files.put(finalFile, fc);
JSONObject payload = new JSONObject();
payload.put("files", files);
HttpURLConnection conn = (HttpURLConnection) buildUrl(gistId).openConnection();
conn.setRequestMethod("PATCH");
conn.setRequestProperty("Authorization", "Bearer " + token);
conn.setRequestProperty("Accept", "application/vnd.github+json");
conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
conn.setDoOutput(true);
try (OutputStream os = conn.getOutputStream()) {
os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
}
int code = conn.getResponseCode();
runOnUiThread(() -> {
btnGeneratePush.setEnabled(true);
if (code == 200) {
if (copyKey != null) copyText(copyKey);
if (clear) {
etDeviceId.setText("");
etNote.setText("");
}
tvStatus.setText("SUCCESS!\n" + msg);
renderList();
} else {
tvStatus.setText("Push Error HTTP: " + code);
}
});
} catch (Exception e) {
runOnUiThread(() -> {
btnGeneratePush.setEnabled(true);
tvStatus.setText("Error: " + e.getMessage());
});
}
});
}
private void renderList() {
layoutKeyList.removeAllViews();
Iterator<?> keys = cachedKeys.keys();
        while (keys.hasNext()) {
            String devId = String.valueOf(keys.next());
            JSONObject item = cachedKeys.optJSONObject(devId);
            if (item == null) continue;

            String key = item.optString("key", "");
            String note = item.optString("note", "User");
            String exp = item.optString("expiry", "");
            boolean active = item.optBoolean("active", true);

            if (isSearchMode && !searchQuery.isEmpty()) {
                boolean match = devId.toLowerCase(Locale.US).contains(searchQuery)
                        || note.toLowerCase(Locale.US).contains(searchQuery)
                        || key.toLowerCase(Locale.US).contains(searchQuery);
                if (!match) continue;
            }

            TextView card = new TextView(this);

card.setText(note + " [" + (active ? "ACTIVE" : "BANNED") + "]\nID: " + devId + "\nKey: " + key + " | Exp: " + exp);
card.setPadding(24, 20, 24, 20);
card.setBackgroundColor(active ? Color.parseColor("#E8F5E9") : Color.parseColor("#FFEBEE"));
card.setTextColor(Color.BLACK);
LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
lp.setMargins(0, 0, 0, 14);
card.setLayoutParams(lp);
card.setOnClickListener(v -> showEditDialog(devId, item));
layoutKeyList.addView(card);
}
}
private void showEditDialog(String devId, JSONObject item) {
LinearLayout box = new LinearLayout(this);
box.setOrientation(LinearLayout.VERTICAL);
box.setPadding(40, 20, 40, 10);
EditText edNote = makeInput("Buyer Name / Note");
edNote.setText(item.optString("note", ""));
box.addView(edNote);
EditText edExp = makeInput("Expiry (YYYY-MM-DD or LIFETIME)");
edExp.setText(item.optString("expiry", ""));
box.addView(edExp);
Switch swActive = new Switch(this);
swActive.setText("Active (Off = Ban)");
swActive.setChecked(item.optBoolean("active", true));
box.addView(swActive);
new AlertDialog.Builder(this)
.setTitle("Edit: " + devId)
.setView(box)
.setPositiveButton("Save", (d, w) -> {
try {
item.put("note", edNote.getText().toString().trim());
item.put("expiry", edExp.getText().toString().trim());
item.put("active", swActive.isChecked());
cachedKeys.put(devId, item);
pushGist("Updated " + devId, null, false);
} catch (Exception ignored) {}
})
.setNeutralButton("Copy Key", (d, w) -> {
copyText(item.optString("key", ""));
Toast.makeText(this, "Key Copied!", Toast.LENGTH_SHORT).show();
})
.setNegativeButton("Delete", (d, w) -> {
cachedKeys.remove(devId);
pushGist("Deleted " + devId, null, false);
})
.show();
}
private void copyText(String t) {
ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("Key", t));
}
}

