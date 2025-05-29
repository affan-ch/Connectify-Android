package pk.codehub.connectify.viewmodels

import android.app.Application
import android.util.Log
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
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class WebRTCViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var remotePeer: PeerConnection? = null
    private var dataChannel: DataChannel? = null

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
        remotePeer?.dispose()
        dataChannel?.dispose()
        peerConnectionFactory?.dispose()
        remotePeer = null
        dataChannel = null
        peerConnectionFactory = null
        _state.value = "disconnected"
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
    private fun setupWebRTC() {
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
                buffer?.let {
                    val data = ByteArray(it.data.remaining()) // Allocate a byte array with the remaining buffer size
                    it.data.get(data)  // Read buffer into byte array

                    val message = String(data)  // Convert the byte array to a string

                    // opt type, content, timestamp, sender
                    val messageJson = JSONObject(message)

                    val type = messageJson.optString("type")
                    val content = messageJson.optString("content")
                    val timestamp = messageJson.optString("timestamp")
                    val sender = messageJson.optString("sender")

                    Log.d("WebRTCViewModel", "Received: type: $type, content: $content, timestamp: $timestamp, sender: $sender")

                    val messageFormatted = Packet(type = type, content = content, timestamp = timestamp, sender = sender)

                    _receivedPackets.postValue(_receivedPackets.value.orEmpty() + messageFormatted)
                }
            }
        })
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
}
