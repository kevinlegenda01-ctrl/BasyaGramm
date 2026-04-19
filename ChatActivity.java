package com.basya.gramm.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.adapter.MessageAdapter;
import com.basya.gramm.model.Message;
import com.basya.gramm.network.ApiClient;
import com.basya.gramm.network.SocketManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final int REQ_IMG  = 201;
    private static final int REQ_VID  = 202;
    private static final int REQ_CAM  = 203;

    private RecyclerView rv;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<Message>();
    private EditText etMsg;
    private ImageButton btnSend, btnAttach, btnCam, btnGift, btnVoice, btnCall, btnVideoCall;
    private TextView tvTitle, tvStatus, tvTyping, tvAvatar;
    private ProgressBar progress;
    private LinearLayout voiceBar;
    private Chronometer chrono;
    private LinearLayoutManager lm;

    private String chatId, chatTitle, withUser, chatType, avatar;
    private Handler typingHandler = new Handler();
    private Uri cameraUri;
    private MediaRecorder recorder;
    private File voiceFile;
    private boolean recording = false;

    private final String[] GIFTS = {
        "🌸","🌟","💎","🏆","🦁","🐉","🌈","🎁","💝","🦋",
        "🌙","⭐","🎀","🍀","💫","🔥","✨","🎊","🎉","👑",
        "🐱","🦄","🤖","🎭","🎸","🎮","🏅","💐","🍭","🎯"
    };

    private SocketManager.OnMessage msgListener = new SocketManager.OnMessage() {
        @Override
        public void onNew(final JSONObject msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String from = msg.optString("from", "");
                        String to   = msg.optString("to", "");
                        boolean mine = chatId.contains(from) || chatId.contains(to) || to.equals(chatId);
                        if (mine) {
                            Message m = parse(msg);
                            boolean exists = false;
                            for (int i = 0; i < messages.size(); i++) {
                                if (messages.get(i).msgId.equals(m.msgId)) { exists = true; break; }
                            }
                            if (!exists) {
                                messages.add(m);
                                adapter.notifyItemInserted(messages.size() - 1);
                                rv.scrollToPosition(messages.size() - 1);
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }
            });
        }
        @Override
        public void onSent(final JSONObject msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Message m = parse(msg);
                        boolean exists = false;
                        for (int i = 0; i < messages.size(); i++) {
                            if (messages.get(i).msgId.equals(m.msgId)) { exists = true; break; }
                        }
                        if (!exists) {
                            messages.add(m);
                            adapter.notifyItemInserted(messages.size() - 1);
                            rv.scrollToPosition(messages.size() - 1);
                        }
                    } catch (Exception e) { /* ignore */ }
                }
            });
        }
    };

    private SocketManager.OnTyping typingListener = new SocketManager.OnTyping() {
        @Override
        public void onTyping(String from) {
            if (from.equals(withUser)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { showTyping(); }
                });
            }
        }
    };

    private SocketManager.OnCall callListener = new SocketManager.OnCall() {
        @Override public void onIncomingCall(String from, String roomId, boolean isVideo) {}
        @Override public void onCallAccepted(String roomId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(ChatActivity.this, VideoCallActivity.class);
                    i.putExtra("room_id", roomId);
                    i.putExtra("with_user", withUser);
                    startActivity(i);
                }
            });
        }
        @Override public void onCallRejected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { Toast.makeText(ChatActivity.this, "Звонок отклонён", Toast.LENGTH_SHORT).show(); }
            });
        }
        @Override public void onCallEnded() {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_chat);

        chatId    = getIntent().getStringExtra("chat_id");
        chatTitle = getIntent().getStringExtra("chat_title");
        withUser  = getIntent().getStringExtra("with_user");
        chatType  = getIntent().getStringExtra("chat_type");
        avatar    = getIntent().getStringExtra("avatar");

        rv           = findViewById(R.id.rv_messages);
        etMsg        = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        btnAttach    = findViewById(R.id.btn_attach);
        btnCam       = findViewById(R.id.btn_camera);
        btnGift      = findViewById(R.id.btn_gift);
        btnVoice     = findViewById(R.id.btn_voice);
        btnCall      = findViewById(R.id.btn_call);
        btnVideoCall = findViewById(R.id.btn_video_call);
        tvTitle      = findViewById(R.id.tv_title);
        tvStatus     = findViewById(R.id.tv_status);
        tvTyping     = findViewById(R.id.tv_typing);
        tvAvatar     = findViewById(R.id.tv_avatar);
        progress     = findViewById(R.id.progress);
        voiceBar     = findViewById(R.id.voice_bar);
        chrono       = findViewById(R.id.chrono);

        tvTitle.setText(chatTitle != null ? chatTitle : "Чат");
        if (avatar != null) tvAvatar.setText(avatar);

        lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new MessageAdapter(this, messages, App.username());
        rv.setAdapter(adapter);

        // Назад
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        // Отправить
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = etMsg.getText().toString().trim();
                if (!t.isEmpty()) {
                    sendText(t);
                    etMsg.setText("");
                }
            }
        });

        // Индикатор печати
        etMsg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (withUser != null && !withUser.isEmpty()) {
                    SocketManager.get().sendTyping(App.username(), withUser);
                }
            }
        });

        // Вложение
        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAttachMenu(); }
        });

        // Камера
        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openCamera(); }
        });

        // Подарки
        btnGift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showGifts(); }
        });

        // Голосовое
        btnVoice.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startRecording();
                return true;
            }
        });
        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) stopRecording();
                else Toast.makeText(ChatActivity.this, "Удерживайте для записи", Toast.LENGTH_SHORT).show();
            }
        });

        // Аудиозвонок
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (withUser != null && !withUser.isEmpty()) {
                    SocketManager.get().startCall(App.username(), withUser, false);
                    Toast.makeText(ChatActivity.this, "Звоним @" + withUser + "...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Видеозвонок
        btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (withUser != null && !withUser.isEmpty()) {
                    SocketManager.get().startCall(App.username(), withUser, true);
                    Intent i = new Intent(ChatActivity.this, VideoCallActivity.class);
                    i.putExtra("with_user", withUser);
                    i.putExtra("is_outgoing", true);
                    startActivity(i);
                }
            }
        });

        SocketManager.get().addMsgListener(msgListener);
        SocketManager.get().addTypingListener(typingListener);
        SocketManager.get().addCallListener(callListener);

        loadMessages();
    }

    private void sendText(String text) {
        String to = (withUser != null && !withUser.isEmpty()) ? withUser : chatId;
        SocketManager.get().sendMessage(App.username(), to, text, "text");
    }

    private void loadMessages() {
        progress.setVisibility(View.VISIBLE);
        ApiClient.getMessages(chatId, 0, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        try {
                            JSONArray arr = r.getJSONArray("messages");
                            messages.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                messages.add(parse(arr.getJSONObject(i)));
                            }
                            adapter.notifyDataSetChanged();
                            if (!messages.isEmpty()) rv.scrollToPosition(messages.size() - 1);
                            SocketManager.get().markRead(chatId);
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

    private Message parse(JSONObject o) throws Exception {
        Message m = new Message();
        m.msgId     = o.optString("msg_id", "");
        m.from      = o.optString("from", "");
        m.text      = o.optString("text", "");
        m.type      = o.optString("type", "text");
        m.timestamp = o.optString("timestamp", "");
        m.fileUrl   = o.optString("file_url", "");
        m.isOwn     = m.from.equals(App.username());
        return m;
    }

    private void showAttachMenu() {
        String[] items = {"🖼 Фото из галереи", "🎬 Видео из галереи"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Вложение")
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int w) {
                    if (w == 0) {
                        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(i, REQ_IMG);
                    } else {
                        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(i, REQ_VID);
                    }
                }
            }).show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет доступа к камере", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File f = File.createTempFile("photo_" + stamp, ".jpg", getCacheDir());
            cameraUri = FileProvider.getUriForFile(this, "com.basya.gramm.provider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            startActivityForResult(i, REQ_CAM);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка камеры: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showGifts() {
        android.widget.GridView grid = new android.widget.GridView(this);
        android.widget.ArrayAdapter<String> ga =
            new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, GIFTS);
        grid.setAdapter(ga);
        grid.setNumColumns(5);
        grid.setPadding(16, 16, 16, 16);

        final androidx.appcompat.app.AlertDialog dlg =
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🎁 NFT Подарки")
                .setView(grid)
                .setNegativeButton("Отмена", null)
                .create();

        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                String g = GIFTS[pos];
                String to = (withUser != null && !withUser.isEmpty()) ? withUser : chatId;
                SocketManager.get().sendMessage(App.username(), to, "🎁 NFT: " + g, "gift");
                dlg.dismiss();
                Toast.makeText(ChatActivity.this, "Подарок отправлен! " + g, Toast.LENGTH_SHORT).show();
            }
        });
        dlg.show();
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет доступа к микрофону", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            voiceFile = File.createTempFile("voice_" + stamp, ".3gp", getCacheDir());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(voiceFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recording = true;
            voiceBar.setVisibility(View.VISIBLE);
            chrono.setBase(SystemClock.elapsedRealtime());
            chrono.start();
            Toast.makeText(this, "🔴 Запись...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка записи", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!recording || recorder == null) return;
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            recording = false;
            chrono.stop();
            voiceBar.setVisibility(View.GONE);
            if (voiceFile != null && voiceFile.exists()) {
                uploadAndSend(voiceFile, "voice");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка остановки записи", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAndSend(File file, String type) {
        Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show();
        ApiClient.uploadFile(App.username(), file, type, new ApiClient.CB() {
            @Override
            public void ok(final JSONObject r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String url = r.getString("url");
                            String to = (withUser != null && !withUser.isEmpty()) ? withUser : chatId;
                            String label = "voice".equals(type) ? "🎤 Голосовое сообщение" :
                                          "image".equals(type) ? "📷 Фото" : "🎬 Видео";
                            SocketManager.get().sendMessage(App.username(), to,
                                label + "|" + url, type);
                        } catch (Exception e) { /* ignore */ }
                    }
                });
            }
            @Override
            public void err(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Отправляем без URL
                        String to = (withUser != null && !withUser.isEmpty()) ? withUser : chatId;
                        SocketManager.get().sendMessage(App.username(), to,
                            "voice".equals(type) ? "🎤 Голосовое" :
                            "image".equals(type) ? "📷 Фото" : "🎬 Видео", type);
                    }
                });
            }
        });
    }

    private void showTyping() {
        tvTyping.setVisibility(View.VISIBLE);
        tvTyping.setText(chatTitle + " печатает...");
        typingHandler.removeCallbacksAndMessages(null);
        typingHandler.postDelayed(new Runnable() {
            @Override
            public void run() { tvTyping.setVisibility(View.GONE); }
        }, 3000);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == Activity.RESULT_OK) {
            if (req == REQ_IMG && data != null) {
                uploadUri(data.getData(), "image");
            } else if (req == REQ_VID && data != null) {
                uploadUri(data.getData(), "video");
            } else if (req == REQ_CAM && cameraUri != null) {
                uploadUri(cameraUri, "image");
            }
        }
    }

    private void uploadUri(Uri uri, String type) {
        try {
            File f = File.createTempFile("upload", ".tmp", getCacheDir());
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
            is.close();
            fos.close();
            uploadAndSend(f, type);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка файла", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.get().removeMsgListener(msgListener);
        SocketManager.get().removeTypingListener(typingListener);
        SocketManager.get().removeCallListener(callListener);
        typingHandler.removeCallbacksAndMessages(null);
        if (recording && recorder != null) {
            try { recorder.stop(); recorder.release(); } catch (Exception e) { /* ignore */ }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
    }
}
