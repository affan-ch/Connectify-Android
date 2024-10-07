package pk.codehub.connectify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun MaterialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = null.toString()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        modifier = modifier
            .padding(0.dp)
            .padding(start = 16.dp),
        label = { Text(label) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = "Clear text",
                    modifier = Modifier
                        .clickable { onValueChange("") }
                        .size(22.dp)
                        .graphicsLayer(alpha = 0.8f)
                )
            }
        },
        singleLine = true
    )
}