package com.basya.gramm.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.ApiClient;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvCoins, tvRate, tvAvatar;
    private EditText etName, etBio;
    private Button btnSave, btnFarm, btnTop;
    private ProgressBar farmProgress;
    private LottieAnimationView lottie;
    private boolean farmCooldown = false;

    private final String[] AVATARS = {
        "😊","😎","🐱","🦁","🐉","🦋","🌟","🔥","💎","👑",
        "🤖","👻","🦊","🐺","🎭","🌸","⭐","🎃","🦄","🐸",
        "🐼","🦅","🐬","🦋","🌈","🎸","⚡","🌊","🏔","🎯"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_profile);

        tvUsername  = findViewById(R.id.tv_username);
        tvCoins     = findViewById(R.id.tv_coins);
        tvRate      = findViewById(R.id.tv_rate);
        tvAvatar    = findViewById(R.id.tv_avatar);
        etName      = findViewById(R.id.et_name);
        etBio       = findViewById(R.id.et_bio);
        btnSave     = findViewById(R.id.btn_save);
        btnFarm     = findViewById(R.id.btn_farm);
        btnTop      = findViewById(R.id.btn_top);
        farmProgress= findViewById(R.id.farm_progress);
        lottie      = findViewById(R.id.lottie_coin);

        tvUsername.setText("@" + App.username());
        etName.setText(App.displayName());
        tvAvatar.setText(App.avatarEmoji());

        // Запускаем Lottie анимацию кота
        lottie.setAnimation(R.raw.cat_coin);
        lottie.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
        lottie.playAnimation();

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        tvAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { pickAvatar(); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveProfile(); }
        });

        btnFarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { farmCoins(); }
        });

        btnTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTop(); }
        });

        loadBalance();
    }

    private void loadBalance() {
        ApiClient.getBalance(App.username(), new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int bal   = r.getInt("balance");
                            double rub = r.getDouble("in_rub");
                            tvCoins.setText("🐱 " + bal + " Бася-коинов");
                            tvRate.setText("≈ " + rub + " ₽   |   1🐱 = 0.50 ₽   |   курс стабильный");
                            App.prefs().edit().putInt(App.KEY_COINS, bal).apply();
                        } catch (Exception e) { /* ignore */ }
                    }
                });
            }
            @Override
            public void err(String e) { /* ignore */ }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String bio  = etBio.getText().toString().trim();
        String av   = tvAvatar.getText().toString();
        if (name.isEmpty()) { toast("Введите имя"); return; }

        btnSave.setEnabled(false);
        ApiClient.updateProfile(App.username(), name, bio, av, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnSave.setEnabled(true);
                        App.prefs().edit()
                            .putString(App.KEY_DISPLAY, name)
                            .putString(App.KEY_AVATAR, av)
                            .apply();
                        toast("Профиль сохранён ✅");
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnSave.setEnabled(true);
                        toast("Ошибка сохранения");
                    }
                });
            }
        });
    }

    private void farmCoins() {
        if (farmCooldown) {
            toast("Подождите 30 секунд перед следующим фармом!");
            return;
        }
        farmCooldown = true;
        btnFarm.setEnabled(false);
        farmProgress.setVisibility(View.VISIBLE);

        // Анимация кота при фарме
        lottie.setSpeed(2.5f);

        ApiClient.farm(App.username(), new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        farmProgress.setVisibility(View.GONE);
                        lottie.setSpeed(1f);
                        try {
                            int earned = r.getInt("earned");
                            int total  = r.getInt("total");
                            App.prefs().edit().putInt(App.KEY_COINS, total).apply();
                            toast("🐱 +" + earned + " Бася-коинов! Итого: " + total);
                            loadBalance();
                        } catch (Exception e) { /* ignore */ }
                        // Кулдаун 30 сек
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                farmCooldown = false;
                                btnFarm.setEnabled(true);
                            }
                        }, 30000);
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        farmProgress.setVisibility(View.GONE);
                        lottie.setSpeed(1f);
                        farmCooldown = false;
                        btnFarm.setEnabled(true);
                        toast("Ошибка. Проверьте соединение.");
                    }
                });
            }
        });
    }

    private void showTop() {
        ApiClient.getTop(new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            org.json.JSONArray arr = r.getJSONArray("top");
                            StringBuilder sb = new StringBuilder();
                            String[] medals = {"🥇","🥈","🥉","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣","🔟"};
                            for (int i = 0; i < arr.length() && i < 10; i++) {
                                JSONObject u = arr.getJSONObject(i);
                                sb.append(medals[i]).append(" @")
                                  .append(u.getString("username"))
                                  .append(" — ")
                                  .append(u.getInt("balance"))
                                  .append(" 🐱\n");
                            }
                            new androidx.appcompat.app.AlertDialog.Builder(ProfileActivity.this)
                                .setTitle("🏆 Топ Бася-коинов")
                                .setMessage(sb.toString())
                                .setPositiveButton("OK", null)
                                .show();
                        } catch (Exception e) { /* ignore */ }
                    }
                });
            }
            @Override
            public void err(String e) { toast("Ошибка загрузки топа"); }
        });
    }

    private void pickAvatar() {
        android.widget.GridView grid = new android.widget.GridView(this);
        android.widget.ArrayAdapter<String> a =
            new android.widget.ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, AVATARS);
        grid.setAdapter(a);
        grid.setNumColumns(5);
        grid.setPadding(16,16,16,16);

        final androidx.appcompat.app.AlertDialog dlg =
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выбери аватар")
                .setView(grid)
                .setNegativeButton("Отмена", null)
                .create();

        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                tvAvatar.setText(AVATARS[pos]);
                dlg.dismiss();
            }
        });
        dlg.show();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
