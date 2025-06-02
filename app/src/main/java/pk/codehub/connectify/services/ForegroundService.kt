package pk.codehub.connectify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.asFlow
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.json.JSONObject
import pk.codehub.connectify.models.Offer
import pk.codehub.connectify.utils.ApiRoutes
import pk.codehub.connectify.utils.DataStoreManager
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import java.net.URISyntaxException
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundService : Service() {

    @Inject
    lateinit var webRTCViewModel: WebRTCViewModel

    private var currentNotification: Notification? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Observe WebRTC state changes on the main thread
        Handler(Looper.getMainLooper()).post {
            webRTCViewModel.state.observeForever { newState ->
                updateNotification(newState)
            }
        }

        // Initial notification
        currentNotification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Connectify")
            .setContentText("Waiting for Desktop device to come online.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        val appContext = this.applicationContext

        WebRTCManager(appContext, webRTCViewModel, serviceScope)

        startForeground(NOTIFICATION_ID, currentNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service logic
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove observer to avoid memory leaks
        webRTCViewModel.state.removeObserver { updateNotification(it) }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun updateNotification(state: String) {
        val notificationText = when (state) {
            "connected" -> "Desktop Device is online and connected."
            "disconnected" -> "Waiting for Desktop device to come online."
            else -> "Connection state: $state"
        }

        currentNotification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Connectify")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, currentNotification)
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}



@OptIn(FlowPreview::class)
class WebRTCManager(
    private val appContext: Context,
    private val viewModel: WebRTCViewModel,
    private val coroutineScope: CoroutineScope
) {
    private var socket: Socket? = null
    private var receivedOffer: Offer? = null

    init {
        try {
            socket = IO.socket(ApiRoutes.BASE_URL)
            Log.i("Socket.io", "Initialized")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        coroutineScope.launch {
            val loginToken = DataStoreManager.getValue(appContext, "token", "").first()
            val deviceToken = DataStoreManager.getValue(appContext, "deviceToken", "").first()

            if (loginToken.isEmpty() || deviceToken.isEmpty()) {
                return@launch
            }

            // Connect to the server
            socket?.connect()

            // Register the device with the server
            socket?.emit("register", JSONObject().apply {
                put("loginToken", loginToken)
                put("deviceToken", deviceToken)
            })

            // Observe WebRTC answer on the main thread
            Handler(Looper.getMainLooper()).post {
                coroutineScope.launch(Dispatchers.IO) {
                    viewModel.answer.asFlow().debounce(2000L) // Waits for 2 seconds and collects only the latest value
                        .distinctUntilChanged()
                        .flowOn(Dispatchers.IO)
                        .collectLatest { createdAnswer ->
                            receivedOffer?.let {
                                socket?.emit("webrtc_answer", JSONObject().apply {
                                    put("loginToken", loginToken)
                                    put("deviceToken", deviceToken)
                                    put("deviceId", it.deviceId)
                                    put("answer", createdAnswer)
                                })
                                Log.d("WebRTC", "Sent Answer to Device ${it.deviceId}: $createdAnswer")
                            }
                        }
                }
            }
        }

        // Listen for WebRTC offers from the server
        socket?.on("webrtc_offer") { args ->
            Handler(Looper.getMainLooper()).post {
                viewModel.resetWebRTC()

                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.setupWebRTC()

                    // After setup, process the offer
                    coroutineScope.launch(Dispatchers.Main) {
                        Log.d("WebRTC", "Received Offer")
                        val offerJson = args[0] as JSONObject
                        val offer = offerJson.getString("offer")
                        val deviceId = offerJson.getString("callbackDeviceId")

                        Log.d("WebRTC", "Received offer from Device $deviceId: $offer")

                        viewModel.setOfferFromJson(offer)
                        receivedOffer = Offer(offer, deviceId)
                    }
                }, 300)
            }
        }

        socket?.on("desktop_connected") { args ->
            val obj = args[0] as JSONObject
            val deviceId = obj.getString("deviceId")

            Log.i("Foreground Service", "Desktop Device Connected: ID:$deviceId")

            coroutineScope.launch {
                val loginToken = DataStoreManager.getValue(appContext, "token", "").first()
                val deviceToken = DataStoreManager.getValue(appContext, "deviceToken", "").first()

                socket?.emit("mobile_connected", JSONObject().apply {
                    put("desktopDeviceId", deviceId)
                    put("loginToken", loginToken)
                    put("deviceToken", deviceToken)
                })
            }
        }


        socket?.on("desktop_disconnected"){ args ->
            val obj = args[0] as JSONObject

            val deviceId = obj.getString("deviceId")

            Log.i("Foreground Service", "Desktop Device Disconnected having ID:${deviceId}")

            viewModel.updateState("disconnected")
        }
    }
}
