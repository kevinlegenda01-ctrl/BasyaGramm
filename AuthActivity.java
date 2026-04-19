package com.basya.gramm.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.ApiClient;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private static final int PERM_REQ = 100;
    private boolean isLogin = true;

    private EditText etUser, etPass, etName, etServer;
    private Button btnAction;
    private TextView tvSwitch, tvTitle, tvSub;
    private LinearLayout layoutName;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_auth);

        etUser    = findViewById(R.id.et_username);
        etPass    = findViewById(R.id.et_password);
        etName    = findViewById(R.id.et_display_name);
        etServer  = findViewById(R.id.et_server);
        btnAction = findViewById(R.id.btn_action);
        tvSwitch  = findViewById(R.id.tv_switch);
        tvTitle   = findViewById(R.id.tv_title);
        tvSub     = findViewById(R.id.tv_subtitle);
        layoutName= findViewById(R.id.layout_name);
        progress  = findViewById(R.id.progress);

        etServer.setText(App.SERVER_URL);
        updateUI();
        requestPerms();

        // Username — только латиница
        etUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {
                String t = s.toString();
                String f = t.replaceAll("[^a-zA-Z0-9_]", "");
                if (!t.equals(f)) {
                    etUser.setText(f);
                    etUser.setSelection(f.length());
                    Toast.makeText(AuthActivity.this,
                        "Только латинские буквы и цифры!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srv = etServer.getText().toString().trim();
                if (!srv.isEmpty()) App.saveServer(srv);
                if (isLogin) doLogin();
                else doRegister();
            }
        });

        tvSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogin = !isLogin;
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (isLogin) {
            tvTitle.setText("Добро пожаловать! 🐱");
            tvSub.setText("Войдите в BasyaGramm");
            btnAction.setText("Войти");
            tvSwitch.setText("Нет аккаунта? Зарегистрироваться →");
            layoutName.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Создать аккаунт");
            tvSub.setText("Присоединяйтесь к BasyaGramm");
            btnAction.setText("Зарегистрироваться");
            tvSwitch.setText("Уже есть аккаунт? Войти →");
            layoutName.setVisibility(View.VISIBLE);
        }
    }

    private void doLogin() {
        String u = etUser.getText().toString().trim();
        String p = etPass.getText().toString();
        if (u.isEmpty() || p.isEmpty()) {
            toast("Заполните все поля");
            return;
        }
        setLoading(true);
        ApiClient.login(u, p, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        try {
                            if (r.getBoolean("success")) {
                                JSONObject user = r.getJSONObject("user");
                                App.saveUser(
                                    user.getString("username"),
                                    user.getString("display_name"),
                                    user.optString("avatar_emoji", "😊"),
                                    user.optInt("coins", 0)
                                );
                                goMain();
                            } else {
                                toast(r.optString("error", "Ошибка входа"));
                            }
                        } catch (Exception e) {
                            toast("Ошибка обработки ответа");
                        }
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        toast("Нет соединения с сервером.\nПроверьте адрес в поле ниже.");
                    }
                });
            }
        });
    }

    private void doRegister() {
        String u = etUser.getText().toString().trim();
        String p = etPass.getText().toString();
        String n = etName.getText().toString().trim();
        if (u.isEmpty() || p.isEmpty()) { toast("Заполните все поля"); return; }
        if (u.length() < 3) { toast("Username минимум 3 символа"); return; }
        if (p.length() < 6) { toast("Пароль минимум 6 символов"); return; }
        if (n.isEmpty()) n = u;
        final String name = n;
        setLoading(true);
        ApiClient.register(u, p, name, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        try {
                            if (r.getBoolean("success")) {
                                JSONObject user = r.getJSONObject("user");
                                App.saveUser(
                                    user.getString("username"),
                                    user.getString("display_name"),
                                    user.optString("avatar_emoji", "😊"),
                                    user.optInt("coins", 100)
                                );
                                goMain();
                            } else {
                                toast(r.optString("error", "Ошибка регистрации"));
                            }
                        } catch (Exception e) {
                            toast("Ошибка");
                        }
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        toast("Нет соединения с сервером.");
                    }
                });
            }
        });
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btnAction.setEnabled(!on);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void requestPerms() {
        List<String> needed = new ArrayList<String>();
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            perms = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("🐱 BasyaGramm")
                .setMessage("Для полной работы нужны разрешения: камера, микрофон, контакты, файлы.")
                .setPositiveButton("Дать", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        ActivityCompat.requestPermissions(AuthActivity.this,
                            needed.toArray(new String[0]), PERM_REQ);
                    }
                })
                .setNegativeButton("Позже", null)
                .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_REQ) {
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                        "⚠️ Приложение будет некорректно работать без разрешений!",
                        Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }
}
