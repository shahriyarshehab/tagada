package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.util.DeviceDataManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TagadaRepository(private val context: Context) {

    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val auth get() = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _notes = MutableStateFlow<List<QuickNote>>(emptyList())
    val notes: StateFlow<List<QuickNote>> = _notes.asStateFlow()

    private val _payHistory = MutableStateFlow<List<PayHistoryItem>>(emptyList())
    val payHistory: StateFlow<List<PayHistoryItem>> = _payHistory.asStateFlow()

    private val _hisabTx = MutableStateFlow<List<HisabTx>>(emptyList())
    val hisabTx: StateFlow<List<HisabTx>> = _hisabTx.asStateFlow()

    private val _hisabReminders = MutableStateFlow<List<HisabReminder>>(emptyList())
    val hisabReminders: StateFlow<List<HisabReminder>> = _hisabReminders.asStateFlow()

    private val _hisabPhones = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val hisabPhones: StateFlow<Map<String, List<String>>> = _hisabPhones.asStateFlow()

    private val _hisabAcks = MutableStateFlow<Map<String, String>>(emptyMap())
    val hisabAcks: StateFlow<Map<String, String>> = _hisabAcks.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Device contacts and call logs
    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private val _deviceCallLogs = MutableStateFlow<List<DeviceCallLog>>(emptyList())
    val deviceCallLogs: StateFlow<List<DeviceCallLog>> = _deviceCallLogs.asStateFlow()

    private val _isLoadingDeviceData = MutableStateFlow(false)
    val isLoadingDeviceData: StateFlow<Boolean> = _isLoadingDeviceData.asStateFlow()

    private var contactsListener: ListenerRegistration? = null
    private var hisabTxListener: ListenerRegistration? = null
    private var remindersListener: ListenerRegistration? = null
    private var notesListener: ListenerRegistration? = null

    init {
        loadFromLocalStorage()
        performMonthlyResetIfNeeded()

        // Start observing Firestore if signed in
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                attachRealtimeListeners(user.uid)
                scope.launch {
                    syncWithFirestore()
                }
            } else {
                detachRealtimeListeners()
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google to perform Firestore sync")
    }

    private fun attachRealtimeListeners(uid: String) {
        detachRealtimeListeners()

        // 1. Live Contacts Snapshot Listener
        contactsListener = db.collection("users").document(uid).collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/contacts")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteContacts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            Contact(
                                id = doc.id,
                                userId = uid,
                                name = data["name"] as? String ?: "Unknown",
                                phone = data["phone"] as? String ?: "",
                                contactGroup = normalizeGroup(data["contactGroup"] as? String),
                                type = data["type"] as? String ?: "dialed",
                                status = data["status"] as? String ?: "unpaid",
                                paidDate = data["paidDate"] as? String,
                                date = data["date"] as? String ?: getTodayIso(),
                                monthCallCount = (data["monthCallCount"] as? Number)?.toInt() ?: 0,
                                callHistory = (data["callHistory"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                isFavorite = data["isFavorite"] as? Boolean ?: false,
                                leadStatus = data["leadStatus"] as? String ?: "none",
                                fin = (data["fin"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() }
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (remoteContacts.isNotEmpty()) {
                        val localMap = _contacts.value.associateBy { it.id }.toMutableMap()
                        remoteContacts.forEach { localMap[it.id] = it }
                        _contacts.value = localMap.values.toList().sortedByDescending { it.date }
                        saveToLocalStorage()
                    }
                }
            }

        // 2. Live Hisab Tx Snapshot Listener
        hisabTxListener = db.collection("users").document(uid).collection("hisabTx")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/hisabTx")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteTx = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            HisabTx(
                                id = doc.id,
                                userId = uid,
                                type = data["type"] as? String ?: "in",
                                name = data["name"] as? String ?: "",
                                amt = (data["amt"] as? Number)?.toDouble() ?: 0.0,
                                date = data["date"] as? String ?: getTodayDateOnly(),
                                note = data["note"] as? String ?: "",
                                src = data["src"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (remoteTx.isNotEmpty()) {
                        val localMap = _hisabTx.value.associateBy { it.id }.toMutableMap()
                        remoteTx.forEach { localMap[it.id] = it }
                        _hisabTx.value = localMap.values.toList().sortedByDescending { it.date }
                        saveToLocalStorage()
                    }
                }
            }

        // 3. Live Reminders Snapshot Listener
        remindersListener = db.collection("users").document(uid).collection("reminders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/reminders")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteRem = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            Reminder(
                                id = doc.id,
                                userId = uid,
                                callId = data["callId"] as? String,
                                name = data["name"] as? String ?: "",
                                phone = data["phone"] as? String ?: "",
                                group = normalizeGroup(data["group"] as? String),
                                datetime = data["datetime"] as? String ?: "",
                                note = data["note"] as? String ?: "",
                                done = data["done"] as? Boolean ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (remoteRem.isNotEmpty()) {
                        val localMap = _reminders.value.associateBy { it.id }.toMutableMap()
                        remoteRem.forEach { localMap[it.id] = it }
                        _reminders.value = localMap.values.toList()
                        saveToLocalStorage()
                    }
                }
            }

        // 4. Live Notes Snapshot Listener
        notesListener = db.collection("users").document(uid).collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users/$uid/notes")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteNotes = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            QuickNote(
                                id = doc.id,
                                userId = uid,
                                text = data["text"] as? String ?: "",
                                date = data["date"] as? String ?: getTodayIso()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (remoteNotes.isNotEmpty()) {
                        val localMap = _notes.value.associateBy { it.id }.toMutableMap()
                        remoteNotes.forEach { localMap[it.id] = it }
                        _notes.value = localMap.values.toList().sortedByDescending { it.date }
                        saveToLocalStorage()
                    }
                }
            }
    }

    private fun detachRealtimeListeners() {
        contactsListener?.remove()
        contactsListener = null
        hisabTxListener?.remove()
        hisabTxListener = null
        remindersListener?.remove()
        remindersListener = null
        notesListener?.remove()
        notesListener = null
    }

    // --- LOCAL STORAGE PERSISTENCE ---
    private fun getStorageFile(): File = File(context.filesDir, "tagada_data_v2.json")

    private fun loadFromLocalStorage() {
        try {
            val file = getStorageFile()
            if (!file.exists()) return
            val json = file.readText()
            parseAndPopulateData(json)
        } catch (e: Exception) {
            Log.e("TagadaRepo", "Failed to load local storage", e)
        }
    }

    private fun saveToLocalStorage() {
        try {
            val json = serializeCurrentData()
            getStorageFile().writeText(json)
        } catch (e: Exception) {
            Log.e("TagadaRepo", "Failed to save local storage", e)
        }
    }

    // --- MONTHLY RESET (1st of month auto-reset matching CRM rules) ---
    private fun performMonthlyResetIfNeeded() {
        val prefs = context.getSharedPreferences("tagada_prefs", Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        val currentMonthKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        val lastReset = prefs.getString("last_reset", null)
        if (lastReset != null && lastReset != currentMonthKey) {
            val parts = lastReset.split("-")
            if (parts.size == 2) {
                val lastYear = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
                val lastMonth = (parts[1].toIntOrNull() ?: 0) + 1
                val currentContacts = _contacts.value
                val newHistoryList = mutableListOf<PayHistoryItem>()
                val updatedContacts = currentContacts.map { c ->
                    if (c.contactGroup == "Active" || c.contactGroup == "Active Member") {
                        newHistoryList.add(
                            PayHistoryItem(
                                id = UUID.randomUUID().toString(),
                                userId = c.userId,
                                callId = c.id,
                                name = c.name,
                                month = lastMonth,
                                year = lastYear,
                                status = c.status,
                                paidDate = c.paidDate,
                                callsBeforePaid = c.monthCallCount
                            )
                        )
                        c.copy(status = "unpaid", paidDate = null, monthCallCount = 0)
                    } else {
                        c.copy(monthCallCount = 0)
                    }
                }
                _contacts.value = updatedContacts
                _payHistory.value = newHistoryList + _payHistory.value
                saveToLocalStorage()
            }
        }
        prefs.edit().putString("last_reset", currentMonthKey).apply()
    }

    // --- CONTACTS MUTATIONS ---
    fun addContact(name: String, phone: String, group: String, saveToDevice: Boolean = false) {
        val uid = auth.currentUser?.uid ?: ""
        val cleanPhone = DeviceDataManager.cleanPhoneNumber(phone)
        val newC = Contact(
            id = UUID.randomUUID().toString(),
            userId = uid,
            name = name.trim(),
            phone = cleanPhone,
            contactGroup = group,
            type = "dialed",
            status = "unpaid",
            date = getTodayIso(),
            monthCallCount = 0,
            isFavorite = false,
            leadStatus = "none"
        )
        _contacts.value = listOf(newC) + _contacts.value
        saveToLocalStorage()
        syncItemToFirestore("contacts", newC.id, contactToMap(newC))

        if (saveToDevice) {
            scope.launch {
                DeviceDataManager.saveContactToDevicePhonebook(context, name.trim(), cleanPhone)
            }
        }
    }

    fun toggleFavorite(contactId: String) {
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val updated = it.copy(isFavorite = !it.isFavorite)
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun recordCall(contactId: String, callType: String = "dialed") {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val newHist = it.callHistory + iso
                val updated = it.copy(
                    type = callType,
                    callHistory = newHist,
                    monthCallCount = it.monthCallCount + 1
                )
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun updateContactGroup(contactId: String, newGroup: String) {
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val updated = it.copy(contactGroup = newGroup)
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun updateContactLeadStatus(contactId: String, leadStatus: String) {
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val updated = it.copy(leadStatus = leadStatus)
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun updateContactStatus(contactId: String, status: String) {
        val paidDate = if (status == "paid") getTodayIso() else null
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val updated = it.copy(status = status, paidDate = paidDate)
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun updateContactFinance(contactId: String, finMap: Map<String, String>) {
        _contacts.value = _contacts.value.map {
            if (it.id == contactId) {
                val updated = it.copy(fin = finMap)
                syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun deleteContact(contactId: String) {
        _contacts.value = _contacts.value.filter { it.id != contactId }
        saveToLocalStorage()
        deleteItemFromFirestore("contacts", contactId)
    }

    // --- DEVICE CONTACTS & CALL LOG INTEGRATION ---
    fun loadDeviceData() {
        scope.launch {
            _isLoadingDeviceData.value = true
            try {
                val currentPhones = _contacts.value.map { it.phone }.toSet()
                val contacts = DeviceDataManager.loadDeviceContacts(context, currentPhones)
                val callLogs = DeviceDataManager.loadDeviceCallLogs(context, limit = 200)

                _deviceContacts.value = contacts
                _deviceCallLogs.value = callLogs
            } catch (e: Exception) {
                Log.e("TagadaRepo", "Failed to load device contacts / call log", e)
            } finally {
                _isLoadingDeviceData.value = false
            }
        }
    }

    fun importDeviceContacts(
        selectedContacts: List<DeviceContact>,
        targetGroup: String = "General"
    ): Int {
        val uid = auth.currentUser?.uid ?: ""
        val existingPhones = _contacts.value.map { it.phone }.toSet()
        val newOnes = mutableListOf<Contact>()

        selectedContacts.forEach { dc ->
            val cleanPhone = DeviceDataManager.cleanPhoneNumber(dc.phone)
            if (cleanPhone.isNotBlank() && existingPhones.none { DeviceDataManager.normalizeMatch(it, cleanPhone) }) {
                newOnes.add(
                    Contact(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        name = dc.name.ifBlank { "Contact" },
                        phone = cleanPhone,
                        contactGroup = targetGroup,
                        type = "dialed",
                        status = "unpaid",
                        date = getTodayIso(),
                        monthCallCount = 0,
                        isFavorite = false,
                        leadStatus = "none"
                    )
                )
            }
        }

        if (newOnes.isNotEmpty()) {
            _contacts.value = newOnes + _contacts.value
            saveToLocalStorage()
            scope.launch {
                newOnes.forEach { syncItemToFirestore("contacts", it.id, contactToMap(it)) }
            }
        }
        return newOnes.size
    }

    fun syncCallHistoryFromDeviceLogs(): Int {
        val logs = _deviceCallLogs.value
        if (logs.isEmpty()) return 0

        var matchedCount = 0
        val currentContacts = _contacts.value.toMutableList()

        currentContacts.forEachIndexed { index, contact ->
            val matchingLogs = logs.filter { DeviceDataManager.normalizeMatch(it.number, contact.phone) }
            if (matchingLogs.isNotEmpty()) {
                val existingHistory = contact.callHistory.toMutableSet()
                var addedCalls = 0
                var latestType = contact.type

                matchingLogs.forEach { log ->
                    val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(log.dateMillis))

                    if (existingHistory.add(isoDate)) {
                        addedCalls++
                        latestType = when (log.type) {
                            DeviceCallType.MISSED -> "missed"
                            DeviceCallType.INCOMING -> "received"
                            DeviceCallType.OUTGOING -> "dialed"
                            DeviceCallType.REJECTED -> "no_answer"
                            DeviceCallType.OTHER -> "dialed"
                        }
                    }
                }

                if (addedCalls > 0) {
                    val updated = contact.copy(
                        type = latestType,
                        monthCallCount = contact.monthCallCount + addedCalls,
                        callHistory = existingHistory.toList().sorted()
                    )
                    currentContacts[index] = updated
                    syncItemToFirestore("contacts", updated.id, contactToMap(updated))
                    matchedCount += addedCalls
                }
            }
        }

        if (matchedCount > 0) {
            _contacts.value = currentContacts
            saveToLocalStorage()
        }

        return matchedCount
    }

    // --- REMINDERS MUTATIONS ---
    fun addReminder(callId: String?, name: String, phone: String, group: String, datetimeIso: String, note: String) {
        val uid = auth.currentUser?.uid ?: ""
        val newR = Reminder(
            id = UUID.randomUUID().toString(),
            userId = uid,
            callId = callId,
            name = name,
            phone = phone,
            group = group,
            datetime = datetimeIso,
            note = note,
            done = false
        )
        _reminders.value = listOf(newR) + _reminders.value
        saveToLocalStorage()
        syncItemToFirestore("reminders", newR.id, reminderToMap(newR))
    }

    fun updateReminderNote(reminderId: String, note: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == reminderId) {
                val updated = it.copy(note = note)
                syncItemToFirestore("reminders", updated.id, reminderToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun deleteReminder(reminderId: String) {
        _reminders.value = _reminders.value.filter { it.id != reminderId }
        saveToLocalStorage()
        deleteItemFromFirestore("reminders", reminderId)
    }

    // --- NOTES MUTATIONS ---
    fun addNote(text: String) {
        if (text.isBlank()) return
        val uid = auth.currentUser?.uid ?: ""
        val newN = QuickNote(
            id = UUID.randomUUID().toString(),
            userId = uid,
            text = text.trim(),
            date = getTodayIso()
        )
        _notes.value = listOf(newN) + _notes.value
        saveToLocalStorage()
        syncItemToFirestore("notes", newN.id, noteToMap(newN))
    }

    fun updateNote(noteId: String, text: String) {
        _notes.value = _notes.value.map {
            if (it.id == noteId) {
                val updated = it.copy(text = text.trim())
                syncItemToFirestore("notes", updated.id, noteToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
        saveToLocalStorage()
        deleteItemFromFirestore("notes", noteId)
    }

    // --- HISAB MUTATIONS ---
    fun addHisabTx(type: String, name: String, amt: Double, date: String, phone: String?, note: String?, src: String?) {
        val uid = auth.currentUser?.uid ?: ""
        val newTx = HisabTx(
            id = UUID.randomUUID().toString(),
            userId = uid,
            type = type,
            name = name.trim(),
            amt = amt,
            date = date,
            note = note?.trim() ?: "",
            src = if (type == "dep") src?.trim() ?: "" else ""
        )
        _hisabTx.value = listOf(newTx) + _hisabTx.value
        if (!phone.isNullOrBlank()) {
            val existing = _hisabPhones.value[name.trim()] ?: emptyList()
            if (!existing.contains(phone.trim())) {
                _hisabPhones.value = _hisabPhones.value + (name.trim() to (existing + phone.trim()))
            }
        }
        saveToLocalStorage()
        syncItemToFirestore("hisabTx", newTx.id, hisabTxToMap(newTx))
    }

    fun updateHisabTx(tx: HisabTx) {
        _hisabTx.value = _hisabTx.value.map {
            if (it.id == tx.id) {
                syncItemToFirestore("hisabTx", tx.id, hisabTxToMap(tx))
                tx
            } else it
        }
        saveToLocalStorage()
    }

    fun deleteHisabTx(txId: String) {
        _hisabTx.value = _hisabTx.value.filter { it.id != txId }
        saveToLocalStorage()
        deleteItemFromFirestore("hisabTx", txId)
    }

    fun addHisabReminder(name: String, dueIso: String) {
        val uid = auth.currentUser?.uid ?: ""
        val newR = HisabReminder(
            id = UUID.randomUUID().toString(),
            userId = uid,
            name = name.trim(),
            due = dueIso,
            done = false
        )
        _hisabReminders.value = listOf(newR) + _hisabReminders.value
        saveToLocalStorage()
        syncItemToFirestore("hisabReminders", newR.id, hisabReminderToMap(newR))
    }

    fun markHisabReminderDone(remId: String) {
        _hisabReminders.value = _hisabReminders.value.map {
            if (it.id == remId) {
                val updated = it.copy(done = true)
                syncItemToFirestore("hisabReminders", updated.id, hisabReminderToMap(updated))
                updated
            } else it
        }
        saveToLocalStorage()
    }

    fun deleteHisabReminder(remId: String) {
        _hisabReminders.value = _hisabReminders.value.filter { it.id != remId }
        saveToLocalStorage()
        deleteItemFromFirestore("hisabReminders", remId)
    }

    fun acknowledgePerson(name: String) {
        val today = getTodayDateOnly()
        _hisabAcks.value = _hisabAcks.value + (name to today)
        saveToLocalStorage()
    }

    fun addPhoneForPerson(name: String, phone: String) {
        val existing = _hisabPhones.value[name] ?: emptyList()
        if (!existing.contains(phone)) {
            _hisabPhones.value = _hisabPhones.value + (name to (existing + phone))
            saveToLocalStorage()
        }
    }

    // --- VCF IMPORT ---
    fun importVcfText(vcfText: String, targetGroup: String): Int {
        val newContacts = mutableListOf<Contact>()
        val cards = vcfText.split(Regex("BEGIN:VCARD", RegexOption.IGNORE_CASE))
        val uid = auth.currentUser?.uid ?: ""
        for (card in cards) {
            if (card.isBlank()) continue
            var name = "Unknown"
            var phone = ""
            for (line in card.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("FN:", ignoreCase = true) || trimmed.startsWith("FN;", ignoreCase = true)) {
                    name = trimmed.substring(trimmed.indexOf(':') + 1).trim()
                } else if (trimmed.startsWith("TEL:", ignoreCase = true) || trimmed.startsWith("TEL;", ignoreCase = true)) {
                    phone = trimmed.substring(trimmed.indexOf(':') + 1).replace(Regex("[^0-9+]"), "").trim()
                }
            }
            if (phone.isNotBlank()) {
                newContacts.add(
                    Contact(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        name = name,
                        phone = phone,
                        contactGroup = targetGroup,
                        type = "dialed",
                        status = "unpaid",
                        date = getTodayIso(),
                        monthCallCount = 0,
                        isFavorite = false,
                        leadStatus = "none"
                    )
                )
            }
        }
        if (newContacts.isNotEmpty()) {
            _contacts.value = newContacts + _contacts.value
            saveToLocalStorage()
            scope.launch {
                newContacts.forEach {
                    syncItemToFirestore("contacts", it.id, contactToMap(it))
                }
            }
        }
        return newContacts.size
    }

    // --- GOOGLE DRIVE BACKUP & RESTORE SERIALIZATION ---
    fun createFullBackupJson(): String {
        val user = auth.currentUser
        val root = JSONObject()
        root.put("app", "Tagada")
        root.put("version", "2.2-android")
        root.put("timestamp", getTodayIso())
        if (user != null) {
            val userObj = JSONObject()
            userObj.put("name", user.displayName ?: "")
            userObj.put("email", user.email ?: "")
            userObj.put("uid", user.uid)
            root.put("user", userObj)
        }

        // Contacts
        val contactsArray = JSONArray()
        _contacts.value.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("contactGroup", c.contactGroup)
            obj.put("type", c.type)
            obj.put("status", c.status)
            obj.put("paidDate", c.paidDate ?: JSONObject.NULL)
            obj.put("date", c.date)
            obj.put("monthCallCount", c.monthCallCount)
            obj.put("isFavorite", c.isFavorite)
            obj.put("leadStatus", c.leadStatus)
            obj.put("callHistory", JSONArray(c.callHistory))
            if (c.fin != null) {
                obj.put("fin", JSONObject(c.fin as Map<*, *>))
            }
            contactsArray.put(obj)
        }
        root.put("calls", contactsArray)

        // Reminders
        val remindersArray = JSONArray()
        _reminders.value.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("callId", r.callId ?: "")
            obj.put("name", r.name)
            obj.put("phone", r.phone)
            obj.put("group", r.group)
            obj.put("datetime", r.datetime)
            obj.put("note", r.note)
            obj.put("done", r.done)
            remindersArray.put(obj)
        }
        root.put("reminders", remindersArray)

        // Notes
        val notesArray = JSONArray()
        _notes.value.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("text", n.text)
            obj.put("date", n.date)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // Pay History
        val payArray = JSONArray()
        _payHistory.value.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("callId", p.callId)
            obj.put("name", p.name)
            obj.put("month", p.month)
            obj.put("year", p.year)
            obj.put("status", p.status)
            obj.put("paidDate", p.paidDate ?: JSONObject.NULL)
            obj.put("callsBeforePaid", p.callsBeforePaid)
            payArray.put(obj)
        }
        root.put("payHistory", payArray)

        // Hisab Tx
        val hisabArray = JSONArray()
        _hisabTx.value.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type)
            obj.put("name", t.name)
            obj.put("amt", t.amt)
            obj.put("date", t.date)
            obj.put("note", t.note)
            obj.put("src", t.src)
            hisabArray.put(obj)
        }
        root.put("hisab1", hisabArray)

        // Hisab Reminders
        val hisabRemArray = JSONArray()
        _hisabReminders.value.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("due", r.due)
            obj.put("done", r.done)
            hisabRemArray.put(obj)
        }
        root.put("hisab_rem", hisabRemArray)

        // Hisab phones & acks
        val phObj = JSONObject()
        _hisabPhones.value.forEach { (k, v) -> phObj.put(k, JSONArray(v)) }
        root.put("hisab_ph", phObj)

        val ackObj = JSONObject()
        _hisabAcks.value.forEach { (k, v) -> ackObj.put(k, v) }
        root.put("hisab_ack", ackObj)

        return root.toString(2)
    }

    fun restoreFromBackupJson(jsonString: String): Boolean {
        return try {
            parseAndPopulateData(jsonString)
            saveToLocalStorage()
            scope.launch { syncWithFirestore() }
            true
        } catch (e: Exception) {
            Log.e("TagadaRepo", "Failed to parse backup JSON", e)
            false
        }
    }

    private fun parseAndPopulateData(json: String) {
        val root = JSONObject(json)
        val uid = auth.currentUser?.uid ?: ""

        // Contacts (calls)
        val callsArr = root.optJSONArray("calls") ?: root.optJSONArray("contacts")
        if (callsArr != null) {
            val list = mutableListOf<Contact>()
            for (i in 0 until callsArr.length()) {
                val obj = callsArr.getJSONObject(i)
                val histArr = obj.optJSONArray("callHistory")
                val histList = mutableListOf<String>()
                if (histArr != null) {
                    for (j in 0 until histArr.length()) histList.add(histArr.getString(j))
                }
                val finObj = obj.optJSONObject("fin")
                val finMap = mutableMapOf<String, String>()
                finObj?.keys()?.forEach { k -> finMap[k] = finObj.optString(k, "") }

                list.add(
                    Contact(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        name = obj.optString("name", "Unknown"),
                        phone = obj.optString("phone", ""),
                        contactGroup = normalizeGroup(obj.optString("contactGroup", "General")),
                        type = obj.optString("type", "dialed"),
                        status = obj.optString("status", "unpaid"),
                        paidDate = if (obj.has("paidDate") && !obj.isNull("paidDate")) obj.optString("paidDate") else null,
                        date = obj.optString("date", getTodayIso()),
                        monthCallCount = obj.optInt("monthCallCount", 0),
                        callHistory = histList,
                        isFavorite = obj.optBoolean("isFavorite", false),
                        leadStatus = obj.optString("leadStatus", "none"),
                        fin = if (finMap.isNotEmpty()) finMap else null
                    )
                )
            }
            _contacts.value = list
        }

        // Reminders
        val remArr = root.optJSONArray("reminders")
        if (remArr != null) {
            val list = mutableListOf<Reminder>()
            for (i in 0 until remArr.length()) {
                val obj = remArr.getJSONObject(i)
                list.add(
                    Reminder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        callId = obj.optString("callId", ""),
                        name = obj.optString("name", ""),
                        phone = obj.optString("phone", ""),
                        group = normalizeGroup(obj.optString("group", "General")),
                        datetime = obj.optString("datetime", ""),
                        note = obj.optString("note", ""),
                        done = obj.optBoolean("done", false)
                    )
                )
            }
            _reminders.value = list
        }

        // Notes
        val notesArr = root.optJSONArray("notes")
        if (notesArr != null) {
            val list = mutableListOf<QuickNote>()
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.getJSONObject(i)
                list.add(
                    QuickNote(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        text = obj.optString("text", ""),
                        date = obj.optString("date", getTodayIso())
                    )
                )
            }
            _notes.value = list
        }

        // Pay History
        val payArr = root.optJSONArray("payHistory")
        if (payArr != null) {
            val list = mutableListOf<PayHistoryItem>()
            for (i in 0 until payArr.length()) {
                val obj = payArr.getJSONObject(i)
                list.add(
                    PayHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        callId = obj.optString("callId", ""),
                        name = obj.optString("name", ""),
                        month = obj.optInt("month", 1),
                        year = obj.optInt("year", 2026),
                        status = obj.optString("status", "unpaid"),
                        paidDate = if (obj.has("paidDate") && !obj.isNull("paidDate")) obj.optString("paidDate") else null,
                        callsBeforePaid = obj.optInt("callsBeforePaid", 0)
                    )
                )
            }
            _payHistory.value = list
        }

        // Hisab Tx (hisab1 or tx)
        val hisabArr = root.optJSONArray("hisab1") ?: root.optJSONArray("tx")
        if (hisabArr != null) {
            val list = mutableListOf<HisabTx>()
            for (i in 0 until hisabArr.length()) {
                val obj = hisabArr.getJSONObject(i)
                list.add(
                    HisabTx(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        type = obj.optString("type", "in"),
                        name = obj.optString("name", ""),
                        amt = obj.optDouble("amt", 0.0),
                        date = obj.optString("date", getTodayDateOnly()),
                        note = obj.optString("note", ""),
                        src = obj.optString("src", "")
                    )
                )
            }
            _hisabTx.value = list
        }

        // Hisab Reminders (hisab_rem or rem)
        val hisabRemArr = root.optJSONArray("hisab_rem") ?: root.optJSONArray("rem")
        if (hisabRemArr != null) {
            val list = mutableListOf<HisabReminder>()
            for (i in 0 until hisabRemArr.length()) {
                val obj = hisabRemArr.getJSONObject(i)
                list.add(
                    HisabReminder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = uid,
                        name = obj.optString("name", ""),
                        due = obj.optString("due", ""),
                        done = obj.optBoolean("done", false)
                    )
                )
            }
            _hisabReminders.value = list
        }

        // Phones map
        val phObj = root.optJSONObject("hisab_ph") ?: root.optJSONObject("ph")
        if (phObj != null) {
            val map = mutableMapOf<String, List<String>>()
            phObj.keys().forEach { k ->
                val arr = phObj.optJSONArray(k)
                val plist = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) plist.add(arr.getString(i))
                }
                map[k] = plist
            }
            _hisabPhones.value = map
        }

        // Acks map
        val ackObj = root.optJSONObject("hisab_ack") ?: root.optJSONObject("ack")
        if (ackObj != null) {
            val map = mutableMapOf<String, String>()
            ackObj.keys().forEach { k -> map[k] = ackObj.optString(k, "") }
            _hisabAcks.value = map
        }
    }

    private fun serializeCurrentData(): String = createFullBackupJson()

    // --- FIRESTORE SYNC ---
    suspend fun syncWithFirestore() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val uid = user.uid
        _isSyncing.value = true
        try {
            // Save user profile
            val userProfile = mapOf(
                "userId" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userProfile, SetOptions.merge())

            // Fetch remote contacts and merge if needed
            db.collection("users").document(uid).collection("contacts").get()
                .addOnSuccessListener { contactsSnap ->
                    if (!contactsSnap.isEmpty) {
                        val remoteContacts = contactsSnap.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                Contact(
                                    id = doc.id,
                                    userId = uid,
                                    name = data["name"] as? String ?: "Unknown",
                                    phone = data["phone"] as? String ?: "",
                                    contactGroup = normalizeGroup(data["contactGroup"] as? String),
                                    type = data["type"] as? String ?: "dialed",
                                    status = data["status"] as? String ?: "unpaid",
                                    paidDate = data["paidDate"] as? String,
                                    date = data["date"] as? String ?: getTodayIso(),
                                    monthCallCount = (data["monthCallCount"] as? Number)?.toInt() ?: 0,
                                    callHistory = (data["callHistory"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    isFavorite = data["isFavorite"] as? Boolean ?: false,
                                    leadStatus = data["leadStatus"] as? String ?: "none",
                                    fin = (data["fin"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() }
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (remoteContacts.isNotEmpty()) {
                            val localMap = _contacts.value.associateBy { it.id }.toMutableMap()
                            remoteContacts.forEach { localMap[it.id] = it }
                            _contacts.value = localMap.values.toList().sortedByDescending { it.date }
                            saveToLocalStorage()
                        }
                    }
                }
                .addOnFailureListener {
                    handleFirestoreError(it, OperationType.LIST, "users/$uid/contacts")
                }

            // Push all local items with strict userId ownership & server timestamps
            _contacts.value.forEach {
                val cleanPayload = contactToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("contacts").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
            _reminders.value.forEach {
                val cleanPayload = reminderToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("reminders").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
            _notes.value.forEach {
                val cleanPayload = noteToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("notes").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
            _payHistory.value.forEach {
                val cleanPayload = payHistoryToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("payHistory").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
            _hisabTx.value.forEach {
                val cleanPayload = hisabTxToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("hisabTx").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
            _hisabReminders.value.forEach {
                val cleanPayload = hisabReminderToMap(it).filterValues { v -> v != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("hisabReminders").document(it.id)
                    .set(cleanPayload, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("TagadaRepo", "Sync with Firestore error", e)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun syncItemToFirestore(subcollection: String, docId: String, payload: Map<String, Any?>) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val cleanPayload = payload.filterValues { it != null } + mapOf(
                    "userId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection(subcollection).document(docId)
                    .set(cleanPayload, SetOptions.merge())
                    .addOnFailureListener { e ->
                        handleFirestoreError(e, OperationType.WRITE, "users/$uid/$subcollection/$docId")
                    }
            } catch (e: Exception) {
                Log.e("TagadaRepo", "Failed to sync to $subcollection", e)
            }
        }
    }

    private fun deleteItemFromFirestore(subcollection: String, docId: String) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                db.collection("users").document(uid).collection(subcollection).document(docId)
                    .delete()
                    .addOnFailureListener { e ->
                        handleFirestoreError(e, OperationType.DELETE, "users/$uid/$subcollection/$docId")
                    }
            } catch (e: Exception) {
                Log.e("TagadaRepo", "Failed to delete from $subcollection", e)
            }
        }
    }

    // Helper map converters
    private fun contactToMap(c: Contact): Map<String, Any?> = mapOf(
        "id" to c.id,
        "userId" to c.userId,
        "name" to c.name,
        "phone" to c.phone,
        "contactGroup" to c.contactGroup,
        "type" to c.type,
        "status" to c.status,
        "paidDate" to c.paidDate,
        "date" to c.date,
        "monthCallCount" to c.monthCallCount,
        "callHistory" to c.callHistory,
        "isFavorite" to c.isFavorite,
        "leadStatus" to c.leadStatus,
        "fin" to c.fin
    )

    private fun reminderToMap(r: Reminder): Map<String, Any?> = mapOf(
        "id" to r.id,
        "userId" to r.userId,
        "callId" to r.callId,
        "name" to r.name,
        "phone" to r.phone,
        "group" to r.group,
        "datetime" to r.datetime,
        "note" to r.note,
        "done" to r.done
    )

    private fun noteToMap(n: QuickNote): Map<String, Any?> = mapOf(
        "id" to n.id,
        "userId" to n.userId,
        "text" to n.text,
        "date" to n.date
    )

    private fun payHistoryToMap(p: PayHistoryItem): Map<String, Any?> = mapOf(
        "id" to p.id,
        "userId" to p.userId,
        "callId" to p.callId,
        "name" to p.name,
        "month" to p.month,
        "year" to p.year,
        "status" to p.status,
        "paidDate" to p.paidDate,
        "callsBeforePaid" to p.callsBeforePaid
    )

    private fun hisabTxToMap(t: HisabTx): Map<String, Any?> = mapOf(
        "id" to t.id,
        "userId" to t.userId,
        "type" to t.type,
        "name" to t.name,
        "amt" to t.amt,
        "date" to t.date,
        "note" to t.note,
        "src" to t.src
    )

    private fun hisabReminderToMap(r: HisabReminder): Map<String, Any?> = mapOf(
        "id" to r.id,
        "userId" to r.userId,
        "name" to r.name,
        "due" to r.due,
        "done" to r.done
    )

    companion object {
        fun normalizeGroup(g: String?): String {
            return when (g) {
                "General Member" -> "General"
                "Active Member" -> "Active"
                "Arrears Member" -> "Arrears"
                "Admission Member" -> "Admission"
                "Survey Target" -> "Survey"
                "Tpaid" -> "Paid"
                else -> g?.takeIf { it.isNotBlank() } ?: "General"
            }
        }

        fun getTodayIso(): String {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        }

        fun getTodayDateOnly(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}
