package com.basya.gramm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.SocketManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        // ── Тема ──────────────────────────────────────────────────────
        RadioGroup rgTheme = findViewById(R.id.rg_theme);
        String cur = App.theme();
        if ("light".equals(cur)) rgTheme.check(R.id.rb_light);
        else rgTheme.check(R.id.rb_dark);

        rgTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int id) {
                String t = (id == R.id.rb_light) ? "light" : "dark";
                App.saveTheme(t);
                Toast.makeText(SettingsActivity.this,
                    "Тема изменена! Перезапустите приложение.", Toast.LENGTH_LONG).show();
            }
        });

        // ── Уведомления ───────────────────────────────────────────────
        Switch swNotif = findViewById(R.id.sw_notif);
        swNotif.setChecked(App.prefs().getBoolean("notif", true));
        swNotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                App.prefs().edit().putBoolean("notif", checked).apply();
            }
        });

        Switch swSound = findViewById(R.id.sw_sound);
        swSound.setChecked(App.prefs().getBoolean("sound", true));
        swSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                App.prefs().edit().putBoolean("sound", checked).apply();
            }
        });

        // ── Конфиденциальность ────────────────────────────────────────
        Switch swRead = findViewById(R.id.sw_read);
        swRead.setChecked(App.prefs().getBoolean("read_receipts", true));
        swRead.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                App.prefs().edit().putBoolean("read_receipts", checked).apply();
            }
        });

        Switch swOnline = findViewById(R.id.sw_online);
        swOnline.setChecked(App.prefs().getBoolean("show_online", true));
        swOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                App.prefs().edit().putBoolean("show_online", checked).apply();
            }
        });

        // ── Сервер ────────────────────────────────────────────────────
        EditText etServer = findViewById(R.id.et_server);
        etServer.setText(App.SERVER_URL);

        findViewById(R.id.btn_save_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = etServer.getText().toString().trim();
                if (!url.isEmpty()) {
                    App.saveServer(url);
                    Toast.makeText(SettingsActivity.this,
                        "Сервер сохранён ✅", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ── Версия ────────────────────────────────────────────────────
        TextView tvVer = findViewById(R.id.tv_version);
        tvVer.setText("BasyaGramm v1.0.0  🐱");

        // ── Выход ─────────────────────────────────────────────────────
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Выйти из аккаунта?")
                    .setMessage("Вы уверены? Придётся войти заново.")
                    .setPositiveButton("Выйти", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            App.logout();
                            SocketManager.get().disconnect();
                            Intent i = new Intent(SettingsActivity.this, AuthActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            }
        });
    }
}
