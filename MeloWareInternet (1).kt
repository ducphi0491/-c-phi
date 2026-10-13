package duc_phi.music

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.nio.ByteBuffer
import java.security.SecureRandom
import livekit.org.webrtc.DataChannel
import livekit.org.webrtc.IceCandidate
import livekit.org.webrtc.MediaConstraints
import livekit.org.webrtc.MediaStream
import livekit.org.webrtc.PeerConnection
import livekit.org.webrtc.PeerConnectionFactory
import livekit.org.webrtc.RtpReceiver
import livekit.org.webrtc.SdpObserver
import livekit.org.webrtc.SessionDescription

/**
 * MeloWare Internet - Stage 1: proof-of-connection over the Internet.
 * Firebase only exchanges SDP / ICE; HELLO flows over WebRTC DataChannel.
 * NO songs, personal paths, or audio data are uploaded to Firebase.
 * STUN by default; optional TURN for mobile networks that block direct P2P.
 * TURN credentials, if provided, are kept in memory only for this dialog session.
 */
private const val MELOWARE_P2P_TAG = "MeloWareP2P"
private const val MELOWARE_P2P_DB =
    "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
private const val MELOWARE_P2P_PATH = "meloware_internet_rooms"
private const val MELOWARE_P2P_LIFETIME_MS = 15 * 60 * 1000L

private enum class InternetRole { NONE, HOST, GUEST }

private fun generateInternetRoomCode(): String {
    // 16 characters, 80 bits of entropy. Never use a short numerical PIN as a Firebase path.
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val rand = SecureRandom()
    return buildString {
        repeat(16) { append(alphabet[rand.nextInt(alphabet.length)]) }
    }
}

/** This object is created only while the Internet dialog is open. */
private class MeloWareInternetSession(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(MELOWARE_P2P_DB)

    var roomCode by mutableStateOf("")
        private set
    var inputCode by mutableStateOf("")
    var isHost by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var guestRequested by mutableStateOf(false)
        private set
    var approved by mutableStateOf(false)
        private set
    var dataChannelOpen by mutableStateOf(false)
        private set
    var status by mutableStateOf("Sẵn sàng kết nối qua Internet")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastMessage by mutableStateOf("")
        private set
    var iceState by mutableStateOf("NEW")
        private set
    var diagnostics by mutableStateOf("")
        private set

    // Optional TURN test configuration: never saved to disk or Firebase.
    var turnUrl by mutableStateOf("")
    var turnUsername by mutableStateOf("")
    var turnPassword by mutableStateOf("")

    private val localIceTypes = linkedMapOf<String, Int>()
    private val remoteIceTypes = linkedMapOf<String, Int>()
    private var localIceGenerated = 0
    private var localIceSaved = 0
    private var localIceSaveFailed = 0
    private var remoteIceReceived = 0
    private var remoteIceAdded = 0
    private var remoteIceRejected = 0
    private var gatheringState = "NEW"
    private var localSdpType = "-"
    private var remoteSdpType = "-"

    @Volatile private var sequence = 0
    private var role = InternetRole.NONE
    private var room: DatabaseReference? = null
    private var roomListener: ValueEventListener? = null
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var channel: DataChannel? = null
    private var remoteSdpInProgress = false
    private var remoteSdpReady = false
    private var offerStarted = false
    private val pendingRemoteIce = mutableListOf<IceCandidate>()
    private val observedRemoteIceKeys = mutableSetOf<String>()

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    private fun candidateType(candidate: IceCandidate): String =
        Regex("(?:^|\\s)typ\\s+(host|srflx|prflx|relay)(?:\\s|$)")
            .find(candidate.sdp)?.groupValues?.get(1) ?: "unknown"

    private fun updateDiagnostics() {
        fun types(map: Map<String, Int>): String =
            if (map.isEmpty()) "-" else map.entries.joinToString(", ") { "${it.key}:${it.value}" }
        diagnostics = "Gathering: $gatheringState | ICE: $iceState\n" +
            "SDP: local=$localSdpType, remote=$remoteSdpType\n" +
            "Local: $localIceGenerated tạo / $localIceSaved lưu Firebase " +
            "(lỗi: $localIceSaveFailed); ${types(localIceTypes)}\n" +
            "Remote: $remoteIceReceived nhận / $remoteIceAdded thêm " +
            "(lỗi: $remoteIceRejected); ${types(remoteIceTypes)}"
    }

    fun diagnosticReport(): String = "MeloWareP2P ${if (isHost) "A" else "B"}\n$diagnostics\n" +
        "Status: $status\nError: ${error ?: "-"}"

    private fun validTurnConfig(): Boolean {
        val url = turnUrl.trim()
        // Allow STUN-only testing when all TURN inputs are empty.
        if (url.isEmpty() && turnUsername.isBlank() && turnPassword.isBlank()) return true
        // Do not silently ignore partial TURN credentials.
        if ((!url.startsWith("turn:") && !url.startsWith("turns:")) ||
            turnUsername.isBlank() || turnPassword.isBlank()) {
            fail("TURN: vui lòng nhập đủ URL, Username và Password (hoặc để trống cả 3 để dùng STUN)")
            return false
        }
        return true
    }

    private fun info(message: String) {
        Log.i(MELOWARE_P2P_TAG, message)
        onMain { status = message }
    }

    private fun fail(message: String, cause: Throwable? = null) {
        Log.e(MELOWARE_P2P_TAG, message, cause)
        onMain {
            busy = false
            error = "$message${cause?.localizedMessage?.let { ": $it" } ?: ""}"
            status = "Không thể hoàn tất kết nối"
        }
    }

    private fun authenticate(token: Int, callback: (String) -> Unit) {
        auth.currentUser?.uid?.let {
            if (sequence == token) callback(it)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                if (sequence == token) {
                    val uid = result.user?.uid
                    if (uid.isNullOrBlank()) fail("Firebase chưa trả UID") else callback(uid)
                }
            }
            .addOnFailureListener { if (sequence == token) fail("Firebase Anonymous Auth thất bại", it) }
    }

    fun createRoom() {
        if (!validTurnConfig()) return
        close()
        val token = sequence
        role = InternetRole.HOST
        isHost = true
        busy = true
        error = null
        info("Đang tạo phòng...")
        authenticate(token) { uid ->
            val code = generateInternetRoomCode()
            val reference = database.getReference(MELOWARE_P2P_PATH).child(code)
            room = reference
            val data = mapOf(
                "hostUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "expiresAt" to (System.currentTimeMillis() + MELOWARE_P2P_LIFETIME_MS)
            )
            reference.setValue(data).addOnSuccessListener {
                if (sequence != token) return@addOnSuccessListener
                roomCode = code
                busy = false
                // Firebase removes this room if the host loses its DB connection.
                reference.onDisconnect().removeValue()
                subscribeRoom(token)
                info("Đã tạo phòng. Gửi mã cho điện thoại B.")
            }.addOnFailureListener {
                if (sequence == token) fail("Không thể tạo phòng; kiểm tra Firebase Rules", it)
            }
        }
    }

    fun joinRoom() {
        if (!validTurnConfig()) return
        val code = inputCode.uppercase().replace(Regex("[^A-Z2-9]"), "")
        if (!Regex("[A-HJ-NP-Z2-9]{16}").matches(code)) {
            fail("Mã kết nối cần đủ 16 ký tự")
            return
        }
        close()
        val token = sequence
        role = InternetRole.GUEST
        isHost = false
        busy = true
        roomCode = code
        error = null
        info("Đang gửi yêu cầu đến điện thoại A...")
        authenticate(token) { uid ->
            val reference = database.getReference(MELOWARE_P2P_PATH).child(code)
            room = reference
            // Guest cannot read a room until this atomic claim has succeeded.
            reference.child("guestUid").setValue(uid)
                .addOnSuccessListener {
                    if (sequence == token) {
                        busy = false
                        subscribeRoom(token)
                        info("Đã gửi yêu cầu. Chờ máy A chấp nhận.")
                    }
                }
                .addOnFailureListener {
                    if (sequence == token) fail("Không tham gia được: mã sai, hết hạn hoặc phòng đã có khách", it)
                }
        }
    }

    fun approveGuest() {
        if (role != InternetRole.HOST || !guestRequested || approved) return
        val token = sequence
        busy = true
        room?.child("approved")?.setValue(true)
            ?.addOnSuccessListener {
                if (sequence == token) {
                    busy = false
                    info("Đã cho phép máy B. Đang tạo kết nối P2P...")
                }
            }
            ?.addOnFailureListener {
                if (sequence == token) fail("Không thể chấp nhận kết nối", it)
            }
    }

    private fun subscribeRoom(token: Int) {
        val reference = room ?: return
        val listener = object : ValueEventListener {
            override fun onCancelled(dbError: DatabaseError) {
                if (sequence == token) fail("Firebase từ chối đọc phòng: ${dbError.message}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (sequence != token) return
                if (!snapshot.exists()) {
                    fail("Phòng đã đóng hoặc bị ngắt kết nối")
                    return
                }
                val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: 0L
                if (expiresAt < System.currentTimeMillis()) {
                    fail("Mã ghép nối đã hết hạn. Tạo mã mới.")
                    return
                }
                val guestUid = snapshot.child("guestUid").getValue(String::class.java)
                val guestApproved = snapshot.child("approved").getValue(Boolean::class.java) == true
                if (role == InternetRole.HOST) {
                    if (!guestUid.isNullOrBlank() && !guestApproved) {
                        guestRequested = true
                        info("Máy B muốn kết nối. Nhấn Chấp nhận trên máy A.")
                    }
                    if (guestApproved && !offerStarted) {
                        approved = true
                        startOffer(token)
                    }
                    if (guestApproved) {
                        processRemoteSdp(snapshot.child("answer"), SessionDescription.Type.ANSWER, token)
                    }
                } else if (role == InternetRole.GUEST) {
                    if (guestApproved) {
                        approved = true
                        processRemoteSdp(snapshot.child("offer"), SessionDescription.Type.OFFER, token)
                    }
                }
                if (guestApproved) {
                    val candidates = if (role == InternetRole.HOST) "guestIce" else "hostIce"
                    processIce(snapshot.child(candidates))
                }
            }
        }
        roomListener = listener
        reference.addValueEventListener(listener)
    }

    private fun setupPeer(token: Int): PeerConnection? {
        peer?.let { return it }
        return runCatching {
            initializeWebRtc(context.applicationContext)
            val newFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            factory = newFactory
            // Redundant public STUN endpoints reduce single-server failures, but do not
            // bypass restrictive 5G CGNAT. TURN is the reliable relay fallback.
            val iceServers = mutableListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
            if (turnUrl.isNotBlank()) {
                iceServers += PeerConnection.IceServer.builder(turnUrl.trim())
                    .setUsername(turnUsername.trim())
                    .setPassword(turnPassword)
                    .createIceServer()
                Log.i(MELOWARE_P2P_TAG, "TURN configured (credentials not logged)")
            } else {
                Log.i(MELOWARE_P2P_TAG, "STUN-only connection; TURN not configured")
            }
            val config = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
            val newPeer = newFactory.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(MELOWARE_P2P_TAG, "Signaling: $state")
                }
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    onMain {
                        if (sequence != token) return@onMain
                        iceState = state.toString()
                        status = "ICE: $state"
                        Log.i(MELOWARE_P2P_TAG, "ICE connection: $state")
                        if (state == PeerConnection.IceConnectionState.CONNECTED ||
                            state == PeerConnection.IceConnectionState.COMPLETED) {
                            error = null
                        }
                        if (state == PeerConnection.IceConnectionState.FAILED) {
                            error = if ((localIceTypes["srflx"] ?: 0) == 0 &&
                                (localIceTypes["relay"] ?: 0) == 0) {
                                "ICE FAILED: Không có candidate công khai/relay ở máy này. " +
                                    "Kiểm tra STUN trên mạng hiện tại; có thể cần TURN."
                            } else {
                                "ICE FAILED: Hai mạng không tìm được đường truyền chung. " +
                                    "Nếu Firebase đã trao đổi ICE đủ, hãy cấu hình TURN."
                            }
                        }
                        updateDiagnostics()
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    onMain {
                        if (sequence != token) return@onMain
                        gatheringState = state.toString()
                        Log.i(MELOWARE_P2P_TAG, "ICE gathering: $state")
                        updateDiagnostics()
                    }
                }
                override fun onIceCandidate(candidate: IceCandidate) {
                    // Never print raw ICE SDP: it can contain local/public IP addresses.
                    onMain {
                        if (sequence != token) return@onMain
                        val kind = candidateType(candidate)
                        localIceGenerated++
                        localIceTypes[kind] = (localIceTypes[kind] ?: 0) + 1
                        Log.d(MELOWARE_P2P_TAG, "Local candidate: type=$kind")
                        updateDiagnostics()
                        val branch = if (role == InternetRole.HOST) "hostIce" else "guestIce"
                        val target = room?.child(branch)?.push()
                        if (target == null) {
                            localIceSaveFailed++
                            Log.e(MELOWARE_P2P_TAG, "Cannot queue ICE: room missing")
                            updateDiagnostics()
                            return@onMain
                        }
                        target.setValue(
                            mapOf("mid" to (candidate.sdpMid ?: ""),
                                "line" to candidate.sdpMLineIndex, "sdp" to candidate.sdp)
                        ).addOnSuccessListener {
                            onMain {
                                if (sequence == token) {
                                    localIceSaved++
                                    updateDiagnostics()
                                }
                            }
                        }.addOnFailureListener { cause ->
                            onMain {
                                if (sequence == token) {
                                    localIceSaveFailed++
                                    Log.e(MELOWARE_P2P_TAG, "Firebase ICE write rejected", cause)
                                    error = "Firebase không ghi được ICE Candidate: ${cause.localizedMessage}"
                                    updateDiagnostics()
                                }
                            }
                        }
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
                override fun onAddStream(stream: MediaStream) = Unit
                override fun onRemoveStream(stream: MediaStream) = Unit
                override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) = Unit
                override fun onDataChannel(incoming: DataChannel) {
                    onMain { if (sequence == token) observeDataChannel(incoming, token) }
                }
                override fun onRenegotiationNeeded() = Unit
            }) ?: throw IllegalStateException("Không tạo được PeerConnection")
            peer = newPeer
            newPeer
        }.onFailure { fail("Không khởi tạo được WebRTC", it) }.getOrNull()
    }

    private fun observeDataChannel(newChannel: DataChannel, token: Int) {
        channel = newChannel
        newChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) = Unit
            override fun onStateChange() {
                onMain {
                    if (sequence != token) return@onMain
                    dataChannelOpen = newChannel.state() == DataChannel.State.OPEN
                    if (dataChannelOpen) info("WebRTC DataChannel đã kết nối! Có thể gửi HELLO.")
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                val text = bytes.toString(Charsets.UTF_8)
                onMain {
                    if (sequence == token) {
                        lastMessage = text.take(300)
                        info("Đã nhận dữ liệu WebRTC từ thiết bị còn lại")
                    }
                }
            }
        })
        dataChannelOpen = newChannel.state() == DataChannel.State.OPEN
    }

    private fun startOffer(token: Int) {
        offerStarted = true
        val pc = setupPeer(token) ?: return
        val newChannel = pc.createDataChannel("meloware-test", DataChannel.Init())
        if (newChannel == null) {
            fail("Không tạo được WebRTC DataChannel")
            return
        }
        observeDataChannel(newChannel, token)
        pc.createOffer(object : BaseInternetSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                onMain { if (sequence == token) applyLocalSdp(sdp, token, "offer") }
            }
            override fun onCreateFailure(reason: String) {
                if (sequence == token) fail("Tạo SDP offer thất bại: $reason")
            }
        }, MediaConstraints())
    }

    private fun applyLocalSdp(sdp: SessionDescription, token: Int, key: String) {
        val pc = peer ?: return
        pc.setLocalDescription(object : BaseInternetSdpObserver() {
            override fun onSetSuccess() {
                onMain {
                    if (sequence == token) {
                        localSdpType = key
                        Log.i(MELOWARE_P2P_TAG, "Local SDP set: $key")
                        updateDiagnostics()
                        room?.child(key)?.setValue(mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description))
                            ?.addOnFailureListener { fail("Không gửi được $key", it) }
                    }
                }
            }
            override fun onSetFailure(reason: String) { if (sequence == token) fail("Lỗi local SDP: $reason") }
        }, sdp)
    }

    private fun processRemoteSdp(node: DataSnapshot, kind: SessionDescription.Type, token: Int) {
        if (!node.exists() || remoteSdpInProgress || remoteSdpReady) return
        val sdpText = node.child("sdp").getValue(String::class.java).orEmpty()
        val sdpType = node.child("type").getValue(String::class.java).orEmpty()
        if (sdpText.isBlank() || sdpType != kind.canonicalForm()) return
        remoteSdpInProgress = true
        val pc = setupPeer(token) ?: return
        pc.setRemoteDescription(object : BaseInternetSdpObserver() {
            override fun onSetSuccess() {
                onMain {
                    if (sequence != token) return@onMain
                    remoteSdpReady = true
                    remoteSdpInProgress = false
                    remoteSdpType = kind.canonicalForm()
                    Log.i(MELOWARE_P2P_TAG, "Remote SDP set: $remoteSdpType")
                    flushRemoteIce()
                    updateDiagnostics()
                    if (kind == SessionDescription.Type.OFFER) {
                        pc.createAnswer(object : BaseInternetSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription) {
                                onMain { if (sequence == token) applyLocalSdp(sdp, token, "answer") }
                            }
                            override fun onCreateFailure(reason: String) {
                                if (sequence == token) fail("Tạo SDP answer thất bại: $reason")
                            }
                        }, MediaConstraints())
                    }
                }
            }
            override fun onSetFailure(reason: String) {
                onMain {
                    remoteSdpInProgress = false
                    if (sequence == token) fail("Remote SDP thất bại: $reason")
                }
            }
        }, SessionDescription(kind, sdpText))
    }

    private fun processIce(node: DataSnapshot) {
        node.children.forEach { child ->
            val key = child.key ?: return@forEach
            if (key in observedRemoteIceKeys) return@forEach
            val sdp = child.child("sdp").getValue(String::class.java).orEmpty()
            if (sdp.isBlank()) return@forEach
            observedRemoteIceKeys.add(key)
            val candidate = IceCandidate(
                child.child("mid").getValue(String::class.java).orEmpty(),
                child.child("line").getValue(Int::class.java) ?: 0,
                sdp
            )
            val kind = candidateType(candidate)
            remoteIceReceived++
            remoteIceTypes[kind] = (remoteIceTypes[kind] ?: 0) + 1
            Log.d(MELOWARE_P2P_TAG, "Remote candidate received: type=$kind")
            if (remoteSdpReady) addRemoteIce(candidate) else pendingRemoteIce.add(candidate)
        }
        updateDiagnostics()
    }

    private fun addRemoteIce(candidate: IceCandidate) {
        val accepted = peer?.addIceCandidate(candidate) == true
        if (accepted) {
            remoteIceAdded++
        } else {
            remoteIceRejected++
            Log.w(MELOWARE_P2P_TAG, "WebRTC rejected remote candidate: type=${candidateType(candidate)}")
        }
        updateDiagnostics()
    }

    private fun flushRemoteIce() {
        pendingRemoteIce.forEach { addRemoteIce(it) }
        pendingRemoteIce.clear()
    }

    fun sendHello() {
        val dc = channel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            fail("WebRTC chưa kết nối; chưa thể gửi HELLO")
            return
        }
        val message = "HELLO từ MeloWare ${if (isHost) "máy A" else "máy B"}"
        if (dc.send(DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false))) {
            info("Đã gửi HELLO trực tiếp qua WebRTC")
        } else {
            fail("Không gửi được HELLO")
        }
    }

    fun close() {
        sequence++
        val oldRoom = room
        val oldListener = roomListener
        val host = role == InternetRole.HOST
        room = null
        roomListener = null
        if (oldRoom != null && oldListener != null) oldRoom.removeEventListener(oldListener)
        if (host) {
            oldRoom?.onDisconnect()?.cancel()
            oldRoom?.removeValue()
        }
        runCatching { channel?.unregisterObserver() }
        runCatching { channel?.dispose() }
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        runCatching { factory?.dispose() }
        channel = null
        peer = null
        factory = null
        observedRemoteIceKeys.clear()
        pendingRemoteIce.clear()
        localIceTypes.clear()
        remoteIceTypes.clear()
        localIceGenerated = 0
        localIceSaved = 0
        localIceSaveFailed = 0
        remoteIceReceived = 0
        remoteIceAdded = 0
        remoteIceRejected = 0
        gatheringState = "NEW"
        iceState = "NEW"
        localSdpType = "-"
        remoteSdpType = "-"
        diagnostics = ""
        remoteSdpReady = false
        remoteSdpInProgress = false
        offerStarted = false
        role = InternetRole.NONE
        roomCode = ""
        isHost = false
        busy = false
        guestRequested = false
        approved = false
        dataChannelOpen = false
        lastMessage = ""
        status = "Sẵn sàng kết nối qua Internet"
        error = null
    }
}

private open class BaseInternetSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(reason: String) = Unit
    override fun onSetFailure(reason: String) = Unit
}

private val WEBRTC_INIT_LOCK = Any()
@Volatile private var webRtcInitialized = false
private fun initializeWebRtc(context: Context) {
    if (webRtcInitialized) return
    synchronized(WEBRTC_INIT_LOCK) {
        if (!webRtcInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
            )
            webRtcInitialized = true
        }
    }
}

@Composable
fun MeloWareInternetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val session = remember(context) { MeloWareInternetSession(context.applicationContext) }
    DisposableEffect(session) { onDispose { session.close() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 680.dp),
            color = colors.card,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kết nối qua Internet", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = colors.textPrimary)
                    TextButton(onClick = onDismiss) { Text("Đóng", color = colors.purpleBright) }
                }
                Text(
                    "Thử nghiệm WebRTC P2P: tạo mã, chấp nhận kết nối và gửi HELLO. " +
                        "Chưa bật chia sẻ file nhạc qua Internet ở bước này.",
                    color = colors.textSecondary, fontSize = 12.sp
                )
                // TURN credentials are displayed before connection controls on purpose.
                // Older versions hid these fields at the bottom of a collapsed section,
                // making them very hard to discover on compact Android screens.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.backgroundSoft,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "Cấu hình TURN (5G ↔ Wi-Fi)",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Điền trên cả hai điện thoại trước khi tạo / tham gia phòng. " +
                                "Để trống cả 3 ô nếu muốn thử STUN không dùng TURN.",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = session.turnUrl,
                            onValueChange = { session.turnUrl = it.take(240) },
                            label = { Text("TURN URL") },
                            placeholder = { Text("turn:global.relay.metered.ca:80") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            session.turnUrl = "turn:global.relay.metered.ca:80"
                        }) {
                            Text("Điền nhanh URL Metered", color = colors.purpleBright, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = session.turnUsername,
                            onValueChange = { session.turnUsername = it.take(200) },
                            label = { Text("TURN Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = session.turnPassword,
                            onValueChange = { session.turnPassword = it.take(300) },
                            label = { Text("TURN Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val anyTurnFieldEntered = session.turnUrl.isNotBlank() ||
                            session.turnUsername.isNotBlank() || session.turnPassword.isNotBlank()
                        val completeTurnFields = session.turnUrl.isNotBlank() &&
                            session.turnUsername.isNotBlank() && session.turnPassword.isNotBlank()
                        Text(
                            when {
                                completeTurnFields -> "TURN đã nhập đủ. Hãy tạo phiên mới để áp dụng."
                                anyTurnFieldEntered -> "Bạn cần nhập đủ 3 ô trước khi kết nối."
                                else -> "Chưa nhập TURN: ứng dụng sẽ chỉ dùng STUN."
                            },
                            color = if (anyTurnFieldEntered && !completeTurnFields) colors.red else colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = session::createRoom,
                    enabled = !session.busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)
                ) { Text("Máy A · Tạo mã kết nối") }

                if (session.roomCode.isNotBlank() && session.isHost) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(session.roomCode.chunked(4).joinToString("-"),
                            color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare Internet", session.roomCode))
                        }) { Text("Sao chép", color = colors.purpleBright) }
                    }
                }
                OutlinedTextField(
                    value = session.inputCode,
                    onValueChange = { session.inputCode = it.take(23) },
                    label = { Text("Máy B · Nhập mã từ máy A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = session::joinRoom, enabled = !session.busy,
                    modifier = Modifier.fillMaxWidth()) { Text("Máy B · Tham gia phòng") }

                if (session.isHost && session.guestRequested && !session.approved) {
                    Button(onClick = session::approveGuest, enabled = !session.busy,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Chấp nhận cho máy B kết nối")
                    }
                }
                if (session.dataChannelOpen) {
                    Button(onClick = session::sendHello, modifier = Modifier.fillMaxWidth()) {
                        Text("Gửi HELLO để thử P2P")
                    }
                }
                Text(session.status, color = colors.textPrimary, fontSize = 13.sp)
                if (session.diagnostics.isNotBlank()) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(12.dp)) {
                        Text(session.diagnostics, color = colors.textSecondary, fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp))
                    }
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("MeloWare ICE", session.diagnosticReport())
                        )
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sao chép chẩn đoán ICE")
                    }
                }
                session.error?.let { Text(it, color = colors.red, fontSize = 12.sp) }
                if (session.lastMessage.isNotBlank()) {
                    Text("Tin nhắn nhận được: ${session.lastMessage}",
                        color = colors.purpleBright, fontSize = 13.sp)
                }
                OutlinedButton(onClick = session::close, modifier = Modifier.fillMaxWidth()) {
                    Text("Ngắt / Tạo phiên mới")
                }
                Text("Dữ liệu TURN chỉ giữ trong bộ nhớ trong lúc mở cửa sổ này; không ghi vào Firebase.",
                    color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
}
