package pk.codehub.connectify.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import androidx.core.database.getLongOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pk.codehub.connectify.models.CallLogEntry
import pk.codehub.connectify.models.Contact
import pk.codehub.connectify.models.Gallery
import pk.codehub.connectify.models.Message

class Synchronizer {

    companion object{
        // Sync Function
        fun sync(viewModel: WebRTCViewModel, context: Context){
            // Launching in IO context to avoid blocking the main thread
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                    Log.i("Synchronizer", "Permissions granted, proceeding with sync")

                    // Permissions granted, proceed with syncing device state
                    val deviceState = DeviceStateUtils.getDeviceState(context)

                    viewModel.sendMessage(
                        Json.encodeToString(deviceState),
                        "DeviceStateInfo"
                    )

//                    val contactList = readAllContacts(context)
//                    viewModel.sendMessage(
//                        Json.encodeToString(contactList),
//                        "Contacts"
//                    )

//                    val callLogs = getCallLogs(context)
//                    viewModel.sendMessage(
//                        Json.encodeToString(callLogs),
//                        "CallLogs"
//                    )

//                    for (folder in getGalleryFolders(context)) {
//                        viewModel.sendMessage(
//                            Json.encodeToString(folder),
//                            "Gallery:Folder" // singular key to indicate one item
//                        )
//                        delay(10) // optional: 10ms delay between messages
//                    }

//                    val smsList = getAllSms(context)
//                    viewModel.sendMessage(
//                        Json.encodeToString(smsList),
//                        "Messages"
//                    )

                    // 🔥 Send broadcast to trigger NotificationService to send active notifications
                    val intent = Intent("SYNC_NOTIFICATIONS")
                    intent.setPackage(context.packageName)
                    context.sendBroadcast(intent)
                }
            }

        }

        private fun getAllSms(context: Context): List<Message> {
            val smsList = mutableListOf<Message>()

            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.DATE,
                Telephony.Sms.THREAD_ID
            )

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIdx = it.getColumnIndexOrThrow(Telephony.Sms.READ)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val threadIdIdx = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
//                val simSlotIdx = if (it.getColumnIndex("sim_id") != -1) it.getColumnIndex("sim_id") else -1

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val address = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val type = it.getInt(typeIdx)
                    val read = it.getInt(readIdx)
                    val date = it.getLong(dateIdx)
                    val threadId = it.getLong(threadIdIdx)
//                    val simSlot = if (simSlotIdx != -1) it.getInt(simSlotIdx) else null

                    val sender = if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) address else "Me"
                    val contactName = getContactNameFromNumber(context, address)

                    smsList.add(
                        Message(
                            id = id,
                            phoneNumber = address,
                            contactName = contactName,
                            content = body,
                            contentType = "text/plain",
                            sender = sender,
                            status = when (type) {
                                Telephony.Sms.MESSAGE_TYPE_INBOX -> "received"
                                Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
                                else -> "other"
                            },
                            isRead = read,
                            simSlot = 1,
                            threadId = threadId,
                            timestamp = date
                        )
                    )
                }
            }

            return smsList
        }

        private fun getContactNameFromNumber(context: Context, phoneNumber: String): String {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    return it.getString(nameIdx) ?: ""
                }
            }

            return "" // Return empty if contact not found
        }


        @SuppressLint("Range")
        fun getGalleryFolders(context: Context): List<Gallery> {
            val contentResolver = context.contentResolver
            val galleryMap = mutableMapOf<String, Gallery>()

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.Video.Media.DURATION
            )

            val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("image/%", "video/%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

            val uri = MediaStore.Files.getContentUri("external")

            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

            cursor?.use { it ->
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndex(MediaStore.MediaColumns._ID))
                    val fileName = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)) ?: ""
                    val filePath = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DATA)) ?: ""
                    val mimeType = it.getString(it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)) ?: ""
                    val size = it.getLong(it.getColumnIndex(MediaStore.MediaColumns.SIZE))
                    val width = it.getIntOrNull(it.getColumnIndex(MediaStore.MediaColumns.WIDTH))
                    val height = it.getIntOrNull(it.getColumnIndex(MediaStore.MediaColumns.HEIGHT))
                    val duration = it.getIntOrNull(it.getColumnIndex(MediaStore.Video.Media.DURATION))
                    val dateTaken = it.getLong(it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN))
                    val dateModified = it.getLongOrNull(it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED))
                    val bucketId = it.getString(it.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID))

                    val mediaType = when {
                        mimeType.startsWith("image") -> "image"
                        mimeType.startsWith("video") -> "video"
                        else -> continue
                    }

                    // Only keep the first (most recent) entry for each folder
                    if (!galleryMap.containsKey(bucketId)) {
                        val contentUri = when (mediaType) {
                            "image" -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            "video" -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            else -> null
                        }

                        val thumbnailBase64 = contentUri?.let { _ ->
                            try {
                                val bitmap = if (mediaType == "image") {
                                    MediaStore.Images.Thumbnails.getThumbnail(
                                        contentResolver, id,
                                        MediaStore.Images.Thumbnails.MINI_KIND,
                                        null
                                    )
                                } else {
                                    MediaStore.Video.Thumbnails.getThumbnail(
                                        contentResolver, id,
                                        MediaStore.Video.Thumbnails.MINI_KIND,
                                        null
                                    )
                                }

                                bitmap?.let { bitmap1 -> bitmapToBase64(bitmap1) }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val gallery = Gallery(
                            mediaId = id.toString(),
                            fileName = fileName,
                            filePath = filePath,
                            mediaType = mediaType,
                            mimeType = mimeType,
                            size = size,
                            width = width,
                            height = height,
                            duration = duration,
                            dateTaken = dateTaken,
                            dateModified = dateModified,
                            isFavorite = null,
                            synced = null,
                            thumbnailBase64 = thumbnailBase64
                        )
                        galleryMap[bucketId] = gallery

                    }
                }
            }

            return galleryMap.values.toList()
        }


        @SuppressLint("Range")
        fun getCallLogs(context: Context): List<CallLogEntry> {
            val callLogs = mutableListOf<CallLogEntry>()
            val resolver = context.contentResolver

            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                "subscription_id", // SIM slot
                CallLog.Calls.NEW,
                CallLog.Calls.IS_READ,
                CallLog.Calls.DATE
            )


            val cursor = resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)) ?: ""
                    val name = it.getString(it.getColumnIndex(CallLog.Calls.CACHED_NAME)) ?: ""
                    val typeCode = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                    val duration = it.getIntOrNull(it.getColumnIndex(CallLog.Calls.DURATION))
                    val simSlot = it.getIntOrNull(it.getColumnIndex("subscription_id"))
                    val isNew = it.getIntOrNull(it.getColumnIndex(CallLog.Calls.NEW))
                    val isRead = it.getIntOrNull(it.getColumnIndex(CallLog.Calls.IS_READ))
                    val timestamp = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))


                    val callType = when (typeCode) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                        CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                        else -> "UNKNOWN"
                    }

                    callLogs.add(
                        CallLogEntry(
                            phoneNumber = number,
                            contactName = name,
                            callType = callType,
                            duration = duration,
                            simSlot = simSlot,
                            isRead = isRead,
                            isNew = isNew,
                            timestamp = timestamp
                        )
                    )
                }
            }

            return callLogs
        }

        // Extension to handle nulls safely
        private fun Cursor.getIntOrNull(column: Int): Int? =
            if (isNull(column)) null else getInt(column)

        private suspend fun readAllContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
            Log.i("Synchronizer", "Reading all contacts from device")
            val contacts = mutableListOf<Contact>()
            val contentResolver: ContentResolver = context.contentResolver

            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val firstName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: continue
                    var phoneNumber: String? = null
                    var email: String? = null
                    var company: String? = null
                    var note: String? = null
                    var address: String? = null
//                    var photoBase64: String? = null

                    // Get phone number
                    val phones = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phones?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            phoneNumber = pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }

                    // Get email
                    val emails = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    emails?.use { eCursor ->
                        if (eCursor.moveToFirst()) {
                            email = eCursor.getString(eCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA))
                        }
                    }

                    // Get organization/company
                    val orgCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                        null
                    )
                    orgCursor?.use { oCursor ->
                        if (oCursor.moveToFirst()) {
                            company = oCursor.getString(oCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
                        }
                    }

                    // Get notes
                    val noteCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
                        null
                    )
                    noteCursor?.use { nCursor ->
                        if (nCursor.moveToFirst()) {
                            note = nCursor.getString(nCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
                        }
                    }

                    // Get postal address
                    val addrCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    addrCursor?.use { aCursor ->
                        if (aCursor.moveToFirst()) {
                            address = aCursor.getString(aCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
                        }
                    }

                    // Photo
//                    try {
//                        val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
//                            contentResolver,
//                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
//                        )
//                        inputStream?.use {
//                            val bitmap = BitmapFactory.decodeStream(it)
//                            photoBase64 = bitmapToBase64(bitmap)
//                        }
//                    } catch (_: Exception) {}

                    if (!phoneNumber.isNullOrBlank()) {
                        contacts.add(
                            Contact(
                                firstName = firstName,
                                phoneNumber = phoneNumber!!,
                                email = email,
                                company = company,
                                notes = note,
                                address = address,
//                                photoBase64 = photoBase64
                                // `dob` is not available via ContactsContract
                            )
                        )
                    }
                }
            }

            return@withContext contacts
        }

        private fun bitmapToBase64(bitmap: Bitmap?): String? {
            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }

}