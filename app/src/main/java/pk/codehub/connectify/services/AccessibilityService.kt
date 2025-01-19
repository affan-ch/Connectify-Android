package pk.codehub.connectify.services

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            val clipData = clipboardManager.primaryClip
            val clipboardText = clipData?.getItemAt(0)?.text.toString()
            Toast.makeText(this, "Clipboard: $clipboardText", Toast.LENGTH_SHORT).show()
            // Sync clipboard data to server or handle it as needed
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for clipboard monitoring
    }

    override fun onInterrupt() {
        // Handle service interruption
    }
}
