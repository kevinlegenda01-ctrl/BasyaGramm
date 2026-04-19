package com.basya.gramm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rv;
    private TextView tvEmpty;
    private ProgressBar progress;
    private List<JSONObject> results = new ArrayList<JSONObject>();
    private SearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.et_search);
        rv       = findViewById(R.id.rv_results);
        tvEmpty  = findViewById(R.id.tv_empty);
        progress = findViewById(R.id.progress);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(this, results, new SearchAdapter.OnClick() {
            @Override
            public void onClick(JSONObject user) {
                try {
                    String uname = user.getString("username");
                    String dname = user.getString("display_name");
                    String emoji = user.optString("avatar_emoji", "😊");
                    String chatId = chatId(App.username(), uname);
                    Intent i = new Intent(SearchActivity.this, ChatActivity.class);
                    i.putExtra("chat_id", chatId);
                    i.putExtra("chat_title", dname);
                    i.putExtra("with_user", uname);
                    i.putExtra("chat_type", "direct");
                    i.putExtra("avatar", emoji);
                    startActivity(i);
                } catch (Exception e) { /* ignore */ }
            }
        });
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().trim();
                if (q.length() >= 2) doSearch(q);
                else {
                    results.clear();
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });
    }

    private void doSearch(String q) {
        progress.setVisibility(View.VISIBLE);
        ApiClient.search(q, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        try {
                            JSONArray arr = r.getJSONArray("users");
                            results.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                results.add(arr.getJSONObject(i));
                            }
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                        } catch (Exception e) { /* ignore */ }
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { progress.setVisibility(View.GONE); }
                });
            }
        });
    }

    private String chatId(String u1, String u2) {
        return u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1;
    }
}
