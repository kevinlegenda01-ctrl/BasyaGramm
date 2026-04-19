package com.basya.gramm.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.ApiClient;
import org.json.JSONObject;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etName, etDesc;
    private TextView tvAvatar, tvTypeLabel;
    private Button btnCreate;
    private ProgressBar progress;
    private String type;

    private final String[] AVATARS = {
        "👥","📢","🎮","🎵","📚","💼","🌍","🎨","⚽","🐱",
        "🔥","💎","🌟","🎭","🎬","📱","🏆","🌸","🦁","🤖"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_create_group);

        type = getIntent().getStringExtra("type");
        boolean isGroup = "group".equals(type);

        etName     = findViewById(R.id.et_name);
        etDesc     = findViewById(R.id.et_desc);
        tvAvatar   = findViewById(R.id.tv_avatar);
        tvTypeLabel= findViewById(R.id.tv_type_label);
        btnCreate  = findViewById(R.id.btn_create);
        progress   = findViewById(R.id.progress);

        tvTypeLabel.setText(isGroup ? "👥 Создать группу" : "📢 Создать канал");
        tvAvatar.setText(isGroup ? "👥" : "📢");

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        tvAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { pickAvatar(); }
        });

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String av   = tvAvatar.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(CreateGroupActivity.this,
                        "Введите название", Toast.LENGTH_SHORT).show();
                    return;
                }
                progress.setVisibility(View.VISIBLE);
                btnCreate.setEnabled(false);

                ApiClient.CB cb = new ApiClient.CB() {
                    @Override
                    public void ok(final JSONObject r) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.setVisibility(View.GONE);
                                try {
                                    if (r.getBoolean("success")) {
                                        Toast.makeText(CreateGroupActivity.this,
                                            (isGroup ? "Группа" : "Канал") + " создан! ✅",
                                            Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        btnCreate.setEnabled(true);
                                        Toast.makeText(CreateGroupActivity.this,
                                            r.optString("error", "Ошибка"),
                                            Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    btnCreate.setEnabled(true);
                                }
                            }
                        });
                    }
                    @Override
                    public void err(String e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.setVisibility(View.GONE);
                                btnCreate.setEnabled(true);
                                Toast.makeText(CreateGroupActivity.this,
                                    "Ошибка соединения", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                };

                if (isGroup) ApiClient.createGroup(App.username(), name, desc, av, cb);
                else         ApiClient.createChannel(App.username(), name, desc, av, cb);
            }
        });
    }

    private void pickAvatar() {
        android.widget.GridView grid = new android.widget.GridView(this);
        android.widget.ArrayAdapter<String> a =
            new android.widget.ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, AVATARS);
        grid.setAdapter(a);
        grid.setNumColumns(5);

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
}
