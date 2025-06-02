package pk.codehub.connectify.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.webrtc.*
import pk.codehub.connectify.models.Packet
import pk.codehub.connectify.models.Sdp
import pk.codehub.connectify.utils.Synchronizer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import android.os.Build
import pk.codehub.connectify.models.AppIcon
import android.content.ClipboardManager


@SuppressLint("StaticFieldLeak")
@HiltViewModel
class WebRTCViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var remotePeer: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    val context: Context = application

    private val _answer = MutableLiveData<String>()
    val answer: LiveData<String> = _answer

    private  val _state = MutableLiveData("disconnected")
    val state: LiveData<String> = _state

    private val _receivedPackets = MutableLiveData<List<Packet>>()

    private val _sentPackets = MutableLiveData<List<Packet>>()

    // Public method to update state from other classes
    fun updateState(newState: String) {
        _state.value = newState
    }

    // Reset Webrtc
    fun resetWebRTC() {
        remotePeer?.close()
        remotePeer?.dispose()
        remotePeer = null
        dataChannel?.dispose()
        dataChannel = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        _state.postValue("disconnected")
    }

    val allMessages: LiveData<List<Packet>> = MediatorLiveData<List<Packet>>().apply {
        addSource(_receivedPackets) { updateMessages() }
        addSource(_sentPackets) { updateMessages() }
    }

    private fun updateMessages() {
        val combinedMessages = (_receivedPackets.value ?: emptyList()) + (_sentPackets.value ?: emptyList())
        (allMessages as MediatorLiveData).value = combinedMessages.sortedBy { it.timestamp }
    }

    init {
        setupWebRTC()
    }

    // Initialize WebRTC PeerConnection
    fun setupWebRTC() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(application)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()

        // Setup ICE servers for connectivity
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:68.183.132.84:3478")
                .setUsername("myturnserveruser")
                .setPassword("PaswordOfSomethingScary69")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        // Initialize remotePeer for SDP exchange and Data Channel handling
        remotePeer = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Log.i("WebRTCViewModel", "Signaling state: $p0")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.i("WebRTCViewModel", "ICE connection state: $p0")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.i("WebRTCViewModel", "ICE connection receiving change: $p0")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Log.i("WebRTCViewModel", "ICE gathering state: $p0")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.i("WebRTCViewModel", "New ICE candidate: $candidate")
                    if (remotePeer?.localDescription != null) {
                        val sdp = Json.encodeToString(Sdp(remotePeer?.localDescription?.type?.name.toString(), remotePeer?.localDescription?.description.toString()))
                        Log.i("WebRTCViewModel", "SDP: $sdp")

                        // update the SDP answer with the ICE candidate
                        _answer.postValue(sdp)
                    }
                }
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                Log.i("WebRTCViewModel", "ICE candidates removed")
            }

            override fun onAddStream(p0: MediaStream?) {
                TODO("Not yet implemented")
            }

            override fun onRemoveStream(p0: MediaStream?) {
                TODO("Not yet implemented")
            }

            override fun onDataChannel(channel: DataChannel?) {
                dataChannel = channel
                setupDataChannel()  // Setup data channel listeners
            }

            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
    }

    // Setup the Data Channel for communication
    private fun setupDataChannel() {
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}

            override fun onStateChange() {
                Log.i("WebRTCViewModel", "DataChannel state changed: ${dataChannel?.state()}")
                if (dataChannel?.state() == DataChannel.State.OPEN) {
                    Log.i("WebRTCViewModel", "DataChannel is open and ready to send messages")
                    // update the state to notify the UI
                    _state.postValue("connected")

                    val applicationContext = application.applicationContext

                    // Call Sync function to sync everything
                    Synchronizer.sync(this@WebRTCViewModel, applicationContext)
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let { it ->
                    val data = ByteArray(it.data.remaining()) // Allocate a byte array with the remaining buffer size
                    it.data.get(data)  // Read buffer into byte array

                    val message = String(data)  // Convert the byte array to a string

                    // opt type, content, timestamp, sender
                    val messageJson = JSONObject(message)

                    val type = messageJson.optString("type")
                    val content = messageJson.optString("content")
                    val timestamp = messageJson.optString("timestamp")
                    val sender = messageJson.optString("sender")

                    if(type == "AppIcon:Request"){
                        Log.i("WebRTCViewModel", "Received AppIcon:Request")

                        if(content == "" || content == "null" || content == "[]") {
                            Log.i("WebRTCViewModel", "Empty App Icon List, Requesting all icons")
                            val pm = context.packageManager
                            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                            installedApps.forEach { appInfo ->
                                try {
                                    val packageName = appInfo.packageName
                                    val appLabel = pm.getApplicationLabel(appInfo).toString()

                                    val pkgInfo = pm.getPackageInfo(packageName, 0)
                                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        pkgInfo.longVersionCode.toString()
                                    } else {
                                        @Suppress("DEPRECATION")
                                        pkgInfo.versionCode.toLong().toString()
                                    }

                                    val icon = try {
                                        pm.getApplicationIcon(packageName)
                                    } catch (_: Exception) {
                                        null
                                    }

                                    val iconB64 = icon?.let { drawableToBase64(it) } ?: ""

                                    // Build your object to send
                                    val appData = AppIcon(
                                        appName = appLabel,
                                        packageName = packageName,
                                        packageVersion = versionCode,
                                        appIconBase64 = iconB64
                                    )

                                    // Send it as JSON via WebRTC
                                    sendMessage(
                                        Json.encodeToString(appData),
                                        "AppIcon:SinglePackage"
                                    )

                                } catch (e: Exception) {
                                    e.printStackTrace() // Log or handle error per app
                                }
                            }

                        }
                    }

                    if(type == "Clipboard"){
                        Log.i("WebRTCViewModel", "Received Clipboard data")
                        Log.i("WebRTCViewModel", "Clipboard content: $content")

                        copyToClipboard(context, "clipboard", content)
                    }
                    Log.d("WebRTCViewModel", "Received: type: $type, content: $content, timestamp: $timestamp, sender: $sender")

                    val messageFormatted = Packet(type = type, content = content, timestamp = timestamp, sender = sender)

                    _receivedPackets.postValue(_receivedPackets.value.orEmpty() + messageFormatted)
                }
            }
        })
    }

    fun copyToClipboard(context: Context, label: String = "clipboard", text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    // Set the remote description from the offer received and create an answer
    fun setOfferFromJson(offerJson: String) {
        try {
            // Parse the JSON input to SDPMessage
            val offerMessage = Json.decodeFromString<Sdp>(offerJson)
            Log.i("WebRTCViewModel", "Offer received: $offerMessage")

            if (offerMessage.type == "offer") {
                val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offerMessage.sdp)
                remotePeer?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        Log.i("WebRTCViewModel", "Remote description (offer) created successfully.")
                        createAnswer()  // Create an answer after the offer is set
                    }

                    override fun onSetSuccess() {
                        Log.i("WebRTCViewModel", "Remote description (offer) set successfully.")
                        createAnswer()  // Create an answer after the offer is set
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.i("WebRTCViewModel", "Error setting remote description: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.i("WebRTCViewModel", "Error setting remote description: $p0")
                    }
                }, sessionDescription)
            } else {
                Log.i("WebRTCViewModel", "Invalid SDP type. Expected offer.")
            }
        } catch (e: Exception) {
            Log.i("WebRTCViewModel", "Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Create an SDP answer and set it as the local description
    private fun createAnswer() {
        Log.i("WebRTCViewModel", "Creating answer...")

        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        remotePeer?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    remotePeer?.setLocalDescription(this, it)

                    // Create and send the SDP answer in JSON format
                    val answerMessage = Sdp(type = "answer", sdp = it.description)
                    Log.i("WebRTCViewModel", "Answer created: $answerMessage")
                    _answer.postValue(Json.encodeToString(answerMessage))
                }
            }

            override fun onSetSuccess() {
                Log.i("WebRTCViewModel", "Local description (answer) set successfully.")
            }

            override fun onCreateFailure(p0: String?) {
                Log.i("WebRTCViewModel", "Error creating local description: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.i("WebRTCViewModel", "Error setting local description: $p0")
            }
        }, mediaConstraints)
    }

    // Send a packet via DataChannel
    fun sendMessage(message: String, type: String) {
        if (dataChannel?.state() == DataChannel.State.OPEN) {

            val reply = Packet(type = type, content = message, timestamp = System.currentTimeMillis()
                .toString(), sender = "mobile")

            val buffer = DataChannel.Buffer(ByteBuffer.wrap(Json.encodeToString(reply).toByteArray()), false)
            dataChannel?.send(buffer)

            _sentPackets.postValue(_sentPackets.value.orEmpty() + reply)

            Log.i("WebRTCViewModel", "Message sent: $message")
        } else {
            Log.i("WebRTCViewModel", "DataChannel is not open")
        }
    }

    // Function to convert a Drawable to a Base64 string
    private fun drawableToBase64(drawable: Drawable): String? {
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
            createBitmap(width, height).apply {
                val canvas = android.graphics.Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
