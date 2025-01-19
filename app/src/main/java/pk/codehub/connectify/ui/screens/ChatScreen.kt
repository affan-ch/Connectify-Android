package pk.codehub.connectify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pk.codehub.connectify.models.Packet
import pk.codehub.connectify.viewmodels.WebRTCViewModel

@Composable
fun ChatScreen(viewModel: WebRTCViewModel = hiltViewModel()) {
    val allMessages by viewModel.allMessages.observeAsState(emptyList())
    var inputMessage by remember { mutableStateOf("") }

    val chatMessages = allMessages.filter { it.type == "chat" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display messages in chronological order
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(), reverseLayout = true
        ) {
            items(chatMessages) { message ->
                MessageBubble(message = message)
            }
        }

        // Message input and send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            IconButton(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        viewModel.sendMessage(type = "chat", message = inputMessage)
                        inputMessage = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Packet) {
    val isUser = message.sender == "mobile"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = message.content,
            color = if (isUser) Color.White else Color.Black,
            modifier = Modifier
                .background(
                    color = if (isUser) Color.Blue else Color.Gray,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        )
    }
}

