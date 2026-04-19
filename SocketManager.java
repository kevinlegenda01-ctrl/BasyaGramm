package com.basya.gramm.network;

import android.util.Log;
import com.basya.gramm.App;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private boolean connected = false;

    private List<OnMessage> msgListeners = new ArrayList<OnMessage>();
    private List<OnTyping> typingListeners = new ArrayList<OnTyping>();
    private List<OnCall> callListeners = new ArrayList<OnCall>();

    public interface OnMessage {
        void onNew(JSONObject msg);
        void onSent(JSONObject msg);
    }

    public interface OnTyping {
        void onTyping(String from);
    }

    public interface OnCall {
        void onIncomingCall(String from, String roomId, boolean isVideo);
        void onCallAccepted(String roomId);
        void onCallRejected();
        void onCallEnded();
    }

    private SocketManager() {}

    public static SocketManager get() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect() {
        if (connected && socket != null && socket.connected()) return;
        try {
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 1000;
            opts.reconnectionAttempts = Integer.MAX_VALUE;
            opts.timeout = 20000;
            opts.transports = new String[]{"websocket"};

            socket = IO.socket(App.SERVER_URL, opts);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    connected = true;
                    Log.d(TAG, "Connected");
                    auth();
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    connected = false;
                    Log.d(TAG, "Disconnected");
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Connection error");
                }
            });

            socket.on("new_message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        try {
                            JSONObject msg = (JSONObject) args[0];
                            for (int i = 0; i < msgListeners.size(); i++) {
                                msgListeners.get(i).onNew(msg);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "new_message error", e);
                        }
                    }
                }
            });

            socket.on("message_sent", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        try {
                            JSONObject msg = (JSONObject) args[0];
                            for (int i = 0; i < msgListeners.size(); i++) {
                                msgListeners.get(i).onSent(msg);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "message_sent error", e);
                        }
                    }
                }
            });

            socket.on("user_typing", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        try {
                            JSONObject d = (JSONObject) args[0];
                            String from = d.getString("from");
                            for (int i = 0; i < typingListeners.size(); i++) {
                                typingListeners.get(i).onTyping(from);
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                }
            });

            socket.on("incoming_call", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        try {
                            JSONObject d = (JSONObject) args[0];
                            String from = d.getString("from");
                            String roomId = d.getString("room_id");
                            boolean isVideo = d.optBoolean("is_video", true);
                            for (int i = 0; i < callListeners.size(); i++) {
                                callListeners.get(i).onIncomingCall(from, roomId, isVideo);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "incoming_call error", e);
                        }
                    }
                }
            });

            socket.on("call_accepted", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        try {
                            JSONObject d = (JSONObject) args[0];
                            String roomId = d.getString("room_id");
                            for (int i = 0; i < callListeners.size(); i++) {
                                callListeners.get(i).onCallAccepted(roomId);
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                }
            });

            socket.on("call_rejected", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    for (int i = 0; i < callListeners.size(); i++) {
                        callListeners.get(i).onCallRejected();
                    }
                }
            });

            socket.on("call_ended", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    for (int i = 0; i < callListeners.size(); i++) {
                        callListeners.get(i).onCallEnded();
                    }
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Bad URL", e);
        }
    }

    private void auth() {
        String u = App.username();
        if (!u.isEmpty() && socket != null) {
            try {
                JSONObject d = new JSONObject();
                d.put("username", u);
                socket.emit("auth", d);
            } catch (Exception e) {
                Log.e(TAG, "Auth error", e);
            }
        }
    }

    public void sendMessage(String from, String to, String text, String type) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("from", from);
            d.put("to", to);
            d.put("text", text);
            d.put("type", type != null ? type : "text");
            socket.emit("send_message", d);
        } catch (Exception e) {
            Log.e(TAG, "Send error", e);
        }
    }

    public void sendTyping(String from, String to) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("from", from);
            d.put("to", to);
            socket.emit("typing", d);
        } catch (Exception e) { /* ignore */ }
    }

    public void startCall(String from, String to, boolean isVideo) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("from", from);
            d.put("to", to);
            d.put("is_video", isVideo);
            socket.emit("start_call", d);
        } catch (Exception e) {
            Log.e(TAG, "Call error", e);
        }
    }

    public void acceptCall(String roomId) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("room_id", roomId);
            socket.emit("accept_call", d);
        } catch (Exception e) { /* ignore */ }
    }

    public void rejectCall(String roomId) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("room_id", roomId);
            socket.emit("reject_call", d);
        } catch (Exception e) { /* ignore */ }
    }

    public void endCall(String roomId) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("room_id", roomId);
            socket.emit("end_call", d);
        } catch (Exception e) { /* ignore */ }
    }

    public void markRead(String chatId) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("chat_id", chatId);
            d.put("username", App.username());
            socket.emit("mark_read", d);
        } catch (Exception e) { /* ignore */ }
    }

    public void addMsgListener(OnMessage l) {
        if (!msgListeners.contains(l)) msgListeners.add(l);
    }
    public void removeMsgListener(OnMessage l) { msgListeners.remove(l); }

    public void addTypingListener(OnTyping l) {
        if (!typingListeners.contains(l)) typingListeners.add(l);
    }
    public void removeTypingListener(OnTyping l) { typingListeners.remove(l); }

    public void addCallListener(OnCall l) {
        if (!callListeners.contains(l)) callListeners.add(l);
    }
    public void removeCallListener(OnCall l) { callListeners.remove(l); }

    public boolean isConnected() {
        return connected && socket != null && socket.connected();
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
        connected = false;
    }
}
