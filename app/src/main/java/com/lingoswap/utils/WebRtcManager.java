package com.lingoswap.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WebRtcManager — quản lý toàn bộ vòng đời WebRTC cho một cuộc gọi video.
 */
public class WebRtcManager {

    private static final String TAG = "WebRtcManager";

    // STUN cho trường hợp có thể P2P trực tiếp; TURN (relay) bắt buộc khi 2 máy
    // sau NAT không gọi được nhau (vd 2 emulator cùng PC) → nếu không có TURN, ICE sẽ FAILED.
    private static final String TURN_USERNAME = com.lingoswap.BuildConfig.TURN_USERNAME;
    private static final String TURN_CREDENTIAL = com.lingoswap.BuildConfig.TURN_CREDENTIAL;

    private static final List<PeerConnection.IceServer> ICE_SERVERS = new ArrayList<PeerConnection.IceServer>() {{
        add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        add(PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer());
        // TURN (relay) — bắt buộc khi 2 máy sau NAT/CGNAT không P2P trực tiếp được.
        add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                .setUsername(TURN_USERNAME).setPassword(TURN_CREDENTIAL).createIceServer());
        add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                .setUsername(TURN_USERNAME).setPassword(TURN_CREDENTIAL).createIceServer());
        add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                .setUsername(TURN_USERNAME).setPassword(TURN_CREDENTIAL).createIceServer());
        add(PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                .setUsername(TURN_USERNAME).setPassword(TURN_CREDENTIAL).createIceServer());
    }};

    public interface Callback {
        void onLocalSdpReady(SessionDescription sdp);
        void onIceCandidateReady(IceCandidate candidate);
        void onRemoteVideoTrackReceived(VideoTrack videoTrack);
        void onConnectionFailed(String reason);
    }

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private EglBase eglBase;

    private final Callback callback;
    private boolean isInitialized = false;
    private boolean isReleased = false;
    private boolean remoteDescriptionSet = false;
    private final List<IceCandidate> pendingRemoteIceCandidates = new ArrayList<>();

    public WebRtcManager(Callback callback) {
        this.callback = callback;
    }

    public void init(Context context, EglBase eglBase) {
        this.eglBase = eglBase;
        isReleased = false;

        PeerConnectionFactory.InitializationOptions initOptions =
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(), true, true))
            .setVideoDecoderFactory(new DefaultVideoDecoderFactory(
                eglBase.getEglBaseContext()))
            .createPeerConnectionFactory();

        isInitialized = true;
        Log.d(TAG, "PeerConnectionFactory initialized");
    }

    public void startLocalStream(SurfaceViewRenderer localView) {
        if (!isInitialized) throw new IllegalStateException("Gọi init() trước");

        localView.init(eglBase.getEglBaseContext(), null);
        localView.setMirror(true);

        videoCapturer = createVideoCapturer();
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, localView.getContext(), videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);

            localVideoTrack = factory.createVideoTrack("local_video", videoSource);
            localVideoTrack.addSink(localView);
        }

        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("local_audio", audioSource);

        Log.d(TAG, "Local stream started");
    }

    private VideoCapturer createVideoCapturer() {
        CameraEnumerator enumerator = new Camera1Enumerator(false);
        for (String name : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null);
            }
        }
        for (String name : enumerator.getDeviceNames()) {
            return enumerator.createCapturer(name, null);
        }
        return null;
    }

    private void createPeerConnection() {
        PeerConnection.RTCConfiguration config =
            new PeerConnection.RTCConfiguration(ICE_SERVERS);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = factory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                // candidate.sdp chứa "typ host|srflx|relay" — để biết TURN có gom được relay không.
                String typ = "?";
                if (candidate.sdp != null && candidate.sdp.contains("typ ")) {
                    String[] parts = candidate.sdp.split("typ ");
                    if (parts.length > 1) typ = parts[1].split(" ")[0];
                }
                Log.d(TAG, "Local ICE candidate type=" + typ);
                callback.onIceCandidateReady(candidate);
            }

            @Override
            public void onAddStream(MediaStream stream) {
                // Plan-B fallback; với UNIFIED_PLAN remote track về qua onAddTrack().
                if (stream.videoTracks.size() > 0) {
                    Log.d(TAG, "Remote video stream received (onAddStream)");
                    callback.onRemoteVideoTrackReceived(stream.videoTracks.get(0));
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                Log.d(TAG, "Connection state: " + newState);
                if (newState == PeerConnection.PeerConnectionState.FAILED) {
                    callback.onConnectionFailed("Kết nối WebRTC thất bại");
                }
            }

            @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
                handleRemoteTrack(receiver.track(), "onAddTrack");
            }
            @Override public void onTrack(RtpTransceiver transceiver) {
                if (transceiver != null && transceiver.getReceiver() != null) {
                    handleRemoteTrack(transceiver.getReceiver().track(), "onTrack");
                }
            }
            @Override public void onSignalingChange(PeerConnection.SignalingState s) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s) {
                Log.d(TAG, "ICE connection state: " + s);
            }
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) {
                Log.d(TAG, "ICE gathering state: " + s);
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] c) {}
            @Override public void onRemoveStream(MediaStream s) {}
            @Override public void onDataChannel(DataChannel d) {}
            @Override public void onRenegotiationNeeded() {}
        });

        if (localVideoTrack != null) {
            peerConnection.addTrack(localVideoTrack, Collections.singletonList("local_stream"));
        }
        if (localAudioTrack != null) {
            peerConnection.addTrack(localAudioTrack, Collections.singletonList("local_stream"));
        }
    }

    public void createOffer() {
        if (peerConnection == null) createPeerConnection();

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        if (!isReleased) callback.onLocalSdpReady(sdp);
                        Log.d(TAG, "Offer created and set as local description");
                    }
                }, sdp);
            }
        }, constraints);
    }

    private void handleRemoteTrack(org.webrtc.MediaStreamTrack track, String source) {
        if (track instanceof VideoTrack) {
            Log.d(TAG, "Remote video track received (" + source + ")");
            callback.onRemoteVideoTrackReceived((VideoTrack) track);
        }
    }

    public void setRemoteOffer(String sdpJson) {
        setRemoteOffer(sdpJson, null);
    }

    public void setRemoteOffer(String sdpJson, Runnable onSetSuccess) {
        if (peerConnection == null) createPeerConnection();
        try {
            JSONObject obj  = new JSONObject(sdpJson);
            String sdp      = obj.getString("sdp");
            SessionDescription remoteSdp = new SessionDescription(
                SessionDescription.Type.OFFER, sdp);
            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                @Override
                public void onSetSuccess() {
                    remoteDescriptionSet = true;
                    flushPendingRemoteIceCandidates();
                    if (onSetSuccess != null && !isReleased) onSetSuccess.run();
                    Log.d(TAG, "Remote offer set");
                }
            }, remoteSdp);
        } catch (JSONException e) {
            Log.e(TAG, "setRemoteOffer JSON error: " + e.getMessage());
        }
    }

    public void createAnswer() {
        if (peerConnection == null) createPeerConnection();
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        if (!isReleased) callback.onLocalSdpReady(sdp);
                        Log.d(TAG, "Answer created and set as local description");
                    }
                }, sdp);
            }
        }, constraints);
    }

    public void setRemoteAnswer(String sdpJson) {
        setRemoteAnswer(sdpJson, null);
    }

    public void setRemoteAnswer(String sdpJson, Runnable onSetSuccess) {
        if (peerConnection == null) {
            Log.w(TAG, "setRemoteAnswer: peerConnection null, bỏ qua");
            return;
        }
        try {
            JSONObject obj = new JSONObject(sdpJson);
            String sdp     = obj.getString("sdp");
            SessionDescription remoteSdp = new SessionDescription(
                SessionDescription.Type.ANSWER, sdp);
            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                @Override
                public void onSetSuccess() {
                    remoteDescriptionSet = true;
                    flushPendingRemoteIceCandidates();
                    if (onSetSuccess != null && !isReleased) onSetSuccess.run();
                    Log.d(TAG, "Remote answer set");
                }
            }, remoteSdp);
        } catch (JSONException e) {
            Log.e(TAG, "setRemoteAnswer JSON error: " + e.getMessage());
        }
    }

    public void addRemoteIceCandidate(String candidateJson) {
        try {
            JSONObject obj = new JSONObject(candidateJson);
            String candidateSdp = obj.optString("candidate", "");
            if (candidateSdp.isEmpty()) {
                Log.w(TAG, "Ignoring empty ICE candidate");
                return;
            }
            IceCandidate candidate = new IceCandidate(
                obj.optString("sdpMid", null),
                obj.optInt("sdpMLineIndex", -1),
                candidateSdp
            );
            if (peerConnection == null || !remoteDescriptionSet) {
                pendingRemoteIceCandidates.add(candidate);
                Log.d(TAG, "Queued remote ICE candidate");
                return;
            }
            if (peerConnection.addIceCandidate(candidate)) {
                Log.d(TAG, "Remote ICE candidate added");
            } else {
                Log.w(TAG, "Remote ICE candidate rejected");
            }
        } catch (JSONException e) {
            Log.e(TAG, "addRemoteIceCandidate JSON error: " + e.getMessage());
        }
    }

    private void flushPendingRemoteIceCandidates() {
        if (peerConnection == null || !remoteDescriptionSet || pendingRemoteIceCandidates.isEmpty()) {
            return;
        }
        Log.d(TAG, "Flushing " + pendingRemoteIceCandidates.size() + " queued ICE candidates");
        List<IceCandidate> candidates = new ArrayList<>(pendingRemoteIceCandidates);
        pendingRemoteIceCandidates.clear();
        for (IceCandidate candidate : candidates) {
            if (!peerConnection.addIceCandidate(candidate)) {
                Log.w(TAG, "Queued ICE candidate rejected");
            }
        }
    }

    public void setMicEnabled(boolean enabled) {
        if (localAudioTrack != null) localAudioTrack.setEnabled(enabled);
    }

    public void setCameraEnabled(boolean enabled) {
        if (localVideoTrack != null) localVideoTrack.setEnabled(enabled);
    }

    public void attachRemoteView(VideoTrack remoteTrack, SurfaceViewRenderer remoteView) {
        remoteView.init(eglBase.getEglBaseContext(), null);
        remoteView.setMirror(false);
        remoteTrack.addSink(remoteView);
    }

    public void release() {
        isReleased = true;
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            }
            if (surfaceTextureHelper != null) surfaceTextureHelper.dispose();
            if (videoSource != null)  videoSource.dispose();
            if (audioSource != null)  audioSource.dispose();
            if (peerConnection != null) {
                peerConnection.close();
                peerConnection.dispose();
            }
            if (factory != null) factory.dispose();
            if (eglBase != null) eglBase.release();
            pendingRemoteIceCandidates.clear();
            remoteDescriptionSet = false;
            peerConnection = null;
            factory = null;
        } catch (Exception e) {
            Log.e(TAG, "release error: " + e.getMessage());
        }
        Log.d(TAG, "WebRtcManager released");
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sdp) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) { Log.e("SdpObserver", "Create fail: " + s); }
        @Override public void onSetFailure(String s)    { Log.e("SdpObserver", "Set fail: " + s); }
    }
}
