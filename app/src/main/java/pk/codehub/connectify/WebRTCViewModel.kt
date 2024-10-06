package pk.codehub.connectify

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.*
import java.nio.ByteBuffer
import javax.inject.Inject

@Serializable
data class SDPMessage(val type: String, val sdp: String)

@HiltViewModel
class WebRTCViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var remotePeer: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _answer = MutableLiveData<String>()
    val answer: LiveData<String> = _answer

    private val _receivedMessages = MutableLiveData<List<String>>()
    val receivedMessages: LiveData<List<String>> = _receivedMessages

    private val messageList = mutableListOf<String>()

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
            PeerConnection.IceServer.builder("turn:myturn.codehub.pk:3478?transport=udp")
                .setUsername("myturnserveruser")
                .setPassword("PaswordOfSomethingScary69")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        // Initialize remotePeer for SDP exchange and Data Channel handling
        remotePeer = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}

            override fun onIceConnectionReceivingChange(p0: Boolean) {}

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.i("WebRTCViewModel", "New ICE candidate: $candidate")
                    if (remotePeer?.localDescription != null) {
                        Log.i("WebRTCViewModel", "SDP: ${Json.encodeToString(SDPMessage(remotePeer?.localDescription?.type?.name.toString(), remotePeer?.localDescription?.description.toString()))}")
                    }
                }
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
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
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    val data = ByteArray(it.data.remaining()) // Allocate a byte array with the remaining buffer size
                    it.data.get(data)  // Read buffer into byte array

                    val message = String(data)  // Convert the byte array to a string
                    messageList.add("Received: $message")
                    _receivedMessages.postValue(messageList)

                    Log.i("WebRTCViewModel", "Message received: $message")
                }
            }
        })
    }


    // Set the remote description from the offer received and create an answer
    fun setOfferFromJson(offerJson: String) {
        try {
            // Parse the JSON input to SDPMessage
            val offerMessage = Json.decodeFromString<SDPMessage>(offerJson)
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
                    val answerMessage = SDPMessage(type = "answer", sdp = it.description)
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

    // Send a message via DataChannel
    fun sendMessage(message: String) {
        if (dataChannel?.state() == DataChannel.State.OPEN) {
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray()), false)
            dataChannel?.send(buffer)
            messageList.add("You: $message")
            _receivedMessages.postValue(messageList)
            Log.i("WebRTCViewModel", "Message sent: $message")
        } else {
            Log.i("WebRTCViewModel", "DataChannel is not open")
        }
    }
}
