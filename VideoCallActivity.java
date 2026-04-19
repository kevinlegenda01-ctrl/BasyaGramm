package com.basya.gramm.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.basya.gramm.App;
import com.basya.gramm.R;
import com.basya.gramm.network.SocketManager;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import java.util.ArrayList;
import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private SurfaceViewRenderer localView, remoteView;
    private ImageButton btnEndCall, btnMute, btnCamOff, btnSpeaker;
    private TextView tvCallerName, tvStatus;

    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer capturer;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private SurfaceTextureHelper surfaceHelper;

    private String withUser, roomId;
    private boolean isOutgoing;
    private boolean micOn  = true;
    private boolean camOn  = true;
    private boolean speakerOn = true;

    private SocketManager.OnCall callListener = new SocketManager.OnCall() {
        @Override
        public void onIncomingCall(String from, String rId, boolean isVideo) {}

        @Override
        public void onCallAccepted(String rId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText("Соединение...");
                    createOffer();
                }
            });
        }

        @Override
        public void onCallRejected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoCallActivity.this,
                        "Звонок отклонён", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        @Override
        public void onCallEnded() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { finish(); }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        withUser   = getIntent().getStringExtra("with_user");
        roomId     = getIntent().getStringExtra("room_id");
        isOutgoing = getIntent().getBooleanExtra("is_outgoing", false);

        localView  = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnMute    = findViewById(R.id.btn_mute);
        btnCamOff  = findViewById(R.id.btn_cam_off);
        btnSpeaker = findViewById(R.id.btn_speaker);
        tvCallerName = findViewById(R.id.tv_caller_name);
        tvStatus   = findViewById(R.id.tv_status);

        tvCallerName.setText("@" + (withUser != null ? withUser : ""));
        tvStatus.setText(isOutgoing ? "Вызов..." : "Входящий звонок...");

        initWebRTC();
        SocketManager.get().addCallListener(callListener);

        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (roomId != null) SocketManager.get().endCall(roomId);
                finish();
            }
        });

        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                micOn = !micOn;
                if (localAudioTrack != null) localAudioTrack.setEnabled(micOn);
                btnMute.setImageResource(micOn ? R.drawable.ic_mic : R.drawable.ic_mic_off);
            }
        });

        btnCamOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camOn = !camOn;
                if (localVideoTrack != null) localVideoTrack.setEnabled(camOn);
                btnCamOff.setImageResource(camOn ? R.drawable.ic_cam : R.drawable.ic_cam_off);
                localView.setVisibility(camOn ? View.VISIBLE : View.GONE);
            }
        });

        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakerOn = !speakerOn;
                android.media.AudioManager am =
                    (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
                if (am != null) am.setSpeakerphoneOn(speakerOn);
            }
        });

        if (!isOutgoing && roomId != null) {
            SocketManager.get().acceptCall(roomId);
            createOffer();
        }
    }

    private void initWebRTC() {
        eglBase = EglBase.create();

        localView.init(eglBase.getEglBaseContext(), null);
        localView.setMirror(true);
        localView.setZOrderMediaOverlay(true);

        remoteView.init(eglBase.getEglBaseContext(), null);
        remoteView.setMirror(false);

        PeerConnectionFactory.InitializationOptions initOpts =
            PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(false)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initOpts);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(), true, true))
            .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
            .createPeerConnectionFactory();

        // Video
        capturer = createCapturer();
        if (capturer != null) {
            surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource   = factory.createVideoSource(capturer.isScreencast());
            capturer.initialize(surfaceHelper, this, videoSource.getCapturerObserver());
            capturer.startCapture(640, 480, 30);
            localVideoTrack = factory.createVideoTrack("local_video", videoSource);
            localVideoTrack.setEnabled(true);
            localVideoTrack.addSink(localView);
        }

        // Audio
        MediaConstraints audioConstr = new MediaConstraints();
        audioConstr.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstr.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));
        audioSource      = factory.createAudioSource(audioConstr);
        localAudioTrack  = factory.createAudioTrack("local_audio", audioSource);
        localAudioTrack.setEnabled(true);

        createPeerConnection();
    }

    private VideoCapturer createCapturer() {
        CameraEnumerator en = new Camera1Enumerator(false);
        String[] names = en.getDeviceNames();
        // Передняя камера
        for (String n : names) {
            if (en.isFrontFacing(n)) {
                VideoCapturer c = en.createCapturer(n, null);
                if (c != null) return c;
            }
        }
        // Задняя
        for (String n : names) {
            if (!en.isFrontFacing(n)) {
                VideoCapturer c = en.createCapturer(n, null);
                if (c != null) return c;
            }
        }
        return null;
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = factory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                // Отправляем ICE кандидата через сокет
                try {
                    JSONObject d = new JSONObject();
                    d.put("to", withUser);
                    d.put("candidate", candidate.sdp);
                    d.put("sdpMid", candidate.sdpMid);
                    d.put("sdpMLineIndex", candidate.sdpMLineIndex);
                    // SocketManager.get().sendIce(d);
                } catch (Exception e) { /* ignore */ }
            }

            @Override
            public void onTrack(org.webrtc.RtpTransceiver transceiver) {
                org.webrtc.MediaStreamTrack track = transceiver.getReceiver().track();
                if (track instanceof VideoTrack) {
                    final VideoTrack vt = (VideoTrack) track;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vt.addSink(remoteView);
                            tvStatus.setText("Соединено ✅");
                        }
                    });
                }
            }

            @Override public void onSignalingChange(PeerConnection.SignalingState s) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s) {
                if (s == PeerConnection.IceConnectionState.DISCONNECTED ||
                    s == PeerConnection.IceConnectionState.FAILED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { tvStatus.setText("Соединение прервано"); }
                    });
                }
            }
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] c) {}
            @Override public void onAddStream(MediaStream s) {}
            @Override public void onRemoveStream(MediaStream s) {}
            @Override public void onDataChannel(org.webrtc.DataChannel c) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(org.webrtc.RtpReceiver r, MediaStream[] s) {}
        });

        if (peerConnection != null && localVideoTrack != null) {
            peerConnection.addTrack(localVideoTrack);
            peerConnection.addTrack(localAudioTrack);
        }
    }

    private void createOffer() {
        if (peerConnection == null) return;
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription s) {}
                    @Override public void onSetSuccess() {}
                    @Override public void onCreateFailure(String e) {}
                    @Override public void onSetFailure(String e) {}
                }, sdp);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { tvStatus.setText("Установка соединения..."); }
                });
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(VideoCallActivity.this, "Ошибка звонка", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onSetFailure(String e) {}
        }, constraints);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.get().removeCallListener(callListener);
        try {
            if (capturer != null) { capturer.stopCapture(); capturer.dispose(); }
            if (videoSource != null) videoSource.dispose();
            if (audioSource != null) audioSource.dispose();
            if (peerConnection != null) { peerConnection.close(); peerConnection.dispose(); }
            if (factory != null) factory.dispose();
            if (surfaceHelper != null) surfaceHelper.dispose();
            localView.release();
            remoteView.release();
            if (eglBase != null) eglBase.release();
        } catch (Exception e) { /* ignore */ }
    }
}
