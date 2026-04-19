package com.basya.gramm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.adapter.ChatListAdapter;
import com.basya.gramm.model.ChatItem;
import com.basya.gramm.network.ApiClient;
import com.basya.gramm.network.SocketManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatListAdapter adapter;
    private List<ChatItem> chats = new ArrayList<ChatItem>();
    private FloatingActionButton fab;
    private TextView tvUser, tvCoins;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private Runnable refreshTask;
    private boolean fabVisible = true;

    private SocketManager.OnMessage msgListener = new SocketManager.OnMessage() {
        @Override
        public void onNew(JSONObject msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { loadChats(); }
            });
        }
        @Override
        public void onSent(JSONObject msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { loadChats(); }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_main);

        rv       = findViewById(R.id.rv_chats);
        fab      = findViewById(R.id.fab);
        tvUser   = findViewById(R.id.tv_username);
        tvCoins  = findViewById(R.id.tv_coins);
        progress = findViewById(R.id.progress);

        tvUser.setText("@" + App.username());
        tvCoins.setText("🐱 " + App.coins());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);

        adapter = new ChatListAdapter(this, chats, new ChatListAdapter.OnClick() {
            @Override
            public void onClick(ChatItem c) {
                Intent i = new Intent(MainActivity.this, ChatActivity.class);
                i.putExtra("chat_id", c.chatId);
                i.putExtra("chat_title", c.displayName);
                i.putExtra("with_user", c.withUser);
                i.putExtra("chat_type", c.type);
                i.putExtra("avatar", c.avatarEmoji);
                startActivity(i);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
            }
        });
        rv.setAdapter(adapter);

        // Скрывать FAB при скролле вниз
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView r, int dx, int dy) {
                if (dy > 8 && fabVisible) {
                    fabVisible = false;
                    fab.hide();
                } else if (dy < -8 && !fabVisible) {
                    fabVisible = true;
                    fab.show();
                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewMenu();
            }
        });

        // Bottom nav
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_search) {
                    startActivity(new Intent(MainActivity.this, SearchActivity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
                return true;
            }
        });

        SocketManager.get().connect();
        SocketManager.get().addMsgListener(msgListener);

        loadChats();
        startRefresh();
    }

    private void loadChats() {
        progress.setVisibility(View.VISIBLE);
        ApiClient.getChats(App.username(), new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        try {
                            JSONArray arr = r.getJSONArray("chats");
                            chats.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                ChatItem item = new ChatItem();
                                item.chatId      = o.optString("chat_id", "");
                                item.type        = o.optString("type", "direct");
                                item.displayName = o.optString("display_name", "");
                                item.avatarEmoji = o.optString("avatar_emoji", "😊");
                                item.withUser    = o.optString("with_user", "");
                                item.lastMessage = o.optString("last_message", "");
                                item.lastTime    = o.optString("last_time", "");
                                item.unread      = o.optInt("unread", 0);
                                item.isBot       = o.optBoolean("is_bot", false);
                                chats.add(item);
                            }
                            adapter.notifyDataSetChanged();
                            // Показать/скрыть пустое состояние
                            View empty = findViewById(R.id.layout_empty);
                            if (empty != null) {
                                empty.setVisibility(chats.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void showNewMenu() {
        String[] opts = {"✉️ Новый чат", "👥 Создать группу", "📢 Создать канал"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Создать")
            .setItems(opts, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int w) {
                    if (w == 0) {
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                    } else {
                        Intent i = new Intent(MainActivity.this, CreateGroupActivity.class);
                        i.putExtra("type", w == 1 ? "group" : "channel");
                        startActivity(i);
                    }
                }
            })
            .show();
    }

    private void startRefresh() {
        refreshTask = new Runnable() {
            @Override
            public void run() {
                loadChats();
                handler.postDelayed(this, 20000);
            }
        };
        handler.postDelayed(refreshTask, 20000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats();
        if (!SocketManager.get().isConnected()) {
            SocketManager.get().connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.get().removeMsgListener(msgListener);
        handler.removeCallbacks(refreshTask);
    }
}
