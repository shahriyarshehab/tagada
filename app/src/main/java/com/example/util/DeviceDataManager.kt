package com.example.util

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.DeviceCallLog
import com.example.data.model.DeviceCallType
import com.example.data.model.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object DeviceDataManager {
    private const val TAG = "DeviceDataManager"

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun saveContactToDevicePhonebook(
        context: Context,
        name: String,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission(context)) {
            Log.w(TAG, "Cannot save contact to device: WRITE_CONTACTS permission not granted")
            return@withContext false
        }

        try {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. Insert RawContact
            val rawContactInsertIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Insert Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 3. Insert Phone
            val cleanPhone = cleanPhoneNumber(phone)
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cleanPhone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert contact into device phonebook", e)
            false
        }
    }

    fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun loadDeviceContacts(
        context: Context,
        existingPhones: Set<String> = emptySet()
    ): List<DeviceContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission(context)) {
            Log.w(TAG, "Cannot load contacts: READ_CONTACTS permission not granted")
            return@withContext emptyList()
        }

        val contactsList = mutableListOf<DeviceContact>()
        val seenPhones = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            if (cursor != null) {
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null)?.trim() ?: "Unnamed"
                    val rawPhone = (if (numIdx >= 0) cursor.getString(numIdx) else null) ?: ""
                    val cleanPhone = cleanPhoneNumber(rawPhone)

                    if (cleanPhone.isNotBlank() && seenPhones.add(cleanPhone)) {
                        val isAlreadyIn = existingPhones.any { normalizeMatch(it, cleanPhone) }
                        contactsList.add(
                            DeviceContact(
                                id = id,
                                name = name,
                                phone = cleanPhone,
                                isAlreadyInCrm = isAlreadyIn
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        } finally {
            cursor?.close()
        }

        contactsList
    }

    suspend fun loadDeviceCallLogs(
        context: Context,
        limit: Int = 150
    ): List<DeviceCallLog> = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission(context)) {
            Log.w(TAG, "Cannot load call log: READ_CALL_LOG permission not granted")
            return@withContext emptyList()
        }

        val callLogs = mutableListOf<DeviceCallLog>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            if (cursor != null) {
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "$count" else "$count"
                    val rawNumber = (if (numIdx >= 0) cursor.getString(numIdx) else null) ?: ""
                    val callerName = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim() else null
                    val callTypeInt = if (typeIdx >= 0) cursor.getInt(typeIdx) else 0
                    val dateMillis = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val durationSeconds = if (durIdx >= 0) cursor.getLong(durIdx) else 0L

                    val type = when (callTypeInt) {
                        CallLog.Calls.MISSED_TYPE -> DeviceCallType.MISSED
                        CallLog.Calls.INCOMING_TYPE -> DeviceCallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> DeviceCallType.OUTGOING
                        CallLog.Calls.REJECTED_TYPE -> DeviceCallType.REJECTED
                        else -> DeviceCallType.OTHER
                    }

                    val cleanNumber = cleanPhoneNumber(rawNumber)
                    if (cleanNumber.isNotBlank()) {
                        callLogs.add(
                            DeviceCallLog(
                                id = id,
                                number = cleanNumber,
                                name = callerName?.takeIf { it.isNotBlank() },
                                type = type,
                                dateMillis = dateMillis,
                                durationSeconds = durationSeconds
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying call log", e)
        } finally {
            cursor?.close()
        }

        callLogs
    }

    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").trim()
    }

    fun normalizeMatch(p1: String, p2: String): Boolean {
        val s1 = cleanPhoneNumber(p1)
        val s2 = cleanPhoneNumber(p2)
        if (s1 == s2) return true
        // Strip country code if e.g. +88017 vs 017
        val suffix1 = s1.takeLast(10)
        val suffix2 = s2.takeLast(10)
        return suffix1.length >= 8 && suffix1 == suffix2
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0s"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    fun formatCallDate(timeMillis: Long): String {
        if (timeMillis <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val todayCal = Calendar.getInstance()

        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US).format(Date(timeMillis))

        return if (isToday) {
            "Today at $timeFormat"
        } else if (diff < 48 * 3600 * 1000L) {
            "Yesterday at $timeFormat"
        } else {
            SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US).format(Date(timeMillis))
        }
    }
}
