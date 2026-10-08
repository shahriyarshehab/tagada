package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.auth.AuthManager
import com.example.data.model.*
import com.example.data.repository.TagadaRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TagadaViewModel(application: Application) : AndroidViewModel(application) {
    val repository = TagadaRepository(application)

    // Navigation and UI state
    val activeTab = MutableStateFlow("dashboard") // dashboard, calls, hisab, followup, settings
    val lang = MutableStateFlow("en")
    val darkMode = MutableStateFlow(true)
    val globalSearch = MutableStateFlow("")
    val sortType = MutableStateFlow("newest") // newest, most-talked, a-z, z-a
    val dashGroupFilter = MutableStateFlow("all")
    val dashTimeFilter = MutableStateFlow("all") // "all", "1", "7", "30"
    val followUpGroupFilter = MutableStateFlow("Survey")
    val surveyLeadFilter = MutableStateFlow("all") // all, green, yellow, red
    val hisabSubTab = MutableStateFlow("summary") // summary, in, dep, notes, reminders
    val hisabSearch = MutableStateFlow("")

    // Calls Screen sub-tab: "crm_contacts", "call_log", "device_contacts"
    val callsSubTab = MutableStateFlow("crm_contacts")
    val callLogFilter = MutableStateFlow("all") // "all", "missed", "received", "dialed"

    val currentUser = MutableStateFlow<FirebaseUser?>(AuthManager.currentUser)
    val toastMessage = MutableStateFlow<String?>(null)

    // AI High Thinking state
    val aiInsightText = MutableStateFlow<String?>(null)
    val isAiThinking = MutableStateFlow(false)

    private val prefs = application.getSharedPreferences("tagada_prefs", android.content.Context.MODE_PRIVATE)
    val autoIncludeInDevice = MutableStateFlow(prefs.getBoolean("auto_include_device_contacts", true))
    val askConfirmationBeforeSave = MutableStateFlow(prefs.getBoolean("ask_confirm_save_contact", true))
    val autoIncludePhoneToApp = MutableStateFlow(prefs.getBoolean("auto_include_phone_to_app", true))

    fun setAutoIncludeInDevice(enabled: Boolean) {
        autoIncludeInDevice.value = enabled
        prefs.edit().putBoolean("auto_include_device_contacts", enabled).apply()
    }

    fun setAskConfirmationBeforeSave(enabled: Boolean) {
        askConfirmationBeforeSave.value = enabled
        prefs.edit().putBoolean("ask_confirm_save_contact", enabled).apply()
    }

    fun setAutoIncludePhoneToApp(enabled: Boolean) {
        autoIncludePhoneToApp.value = enabled
        prefs.edit().putBoolean("auto_include_phone_to_app", enabled).apply()
    }

    fun saveContact(name: String, phone: String, group: String, alsoSaveToDevice: Boolean) {
        repository.addContact(name, phone, group, alsoSaveToDevice)
        if (alsoSaveToDevice) {
            showToast("Saved to CRM & Device Contacts!")
        } else {
            showToast("Saved to CRM Contacts")
        }
        loadDeviceData()
    }

    fun autoIncludeAllDeviceContacts(targetGroup: String = "General") {
        val unimported = repository.deviceContacts.value.filter { !it.isAlreadyInCrm }
        if (unimported.isEmpty()) {
            showToast("All device contacts are already in Tagada CRM!")
            return
        }
        val count = repository.importDeviceContacts(unimported, targetGroup)
        showToast("Auto-included $count device contacts into Tagada CRM!")
        loadDeviceData()
    }

    fun forceSecureFirestoreSync() {
        viewModelScope.launch {
            val user = AuthManager.currentUser
            if (user == null) {
                showToast("Sign in with Google to enable Firestore cloud sync")
            } else {
                repository.syncWithFirestore()
                showToast("Cloud sync complete: Data secured in Firestore")
            }
        }
    }

    val contacts: StateFlow<List<Contact>> = repository.contacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val reminders: StateFlow<List<Reminder>> = repository.reminders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val notes: StateFlow<List<QuickNote>> = repository.notes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val payHistory: StateFlow<List<PayHistoryItem>> = repository.payHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val hisabTx: StateFlow<List<HisabTx>> = repository.hisabTx.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val hisabReminders: StateFlow<List<HisabReminder>> = repository.hisabReminders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val hisabPhones: StateFlow<Map<String, List<String>>> = repository.hisabPhones.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )
    val hisabAcks: StateFlow<Map<String, String>> = repository.hisabAcks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )
    val isSyncing: StateFlow<Boolean> = repository.isSyncing.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val deviceContacts: StateFlow<List<DeviceContact>> = repository.deviceContacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val deviceCallLogs: StateFlow<List<DeviceCallLog>> = repository.deviceCallLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val isLoadingDeviceData: StateFlow<Boolean> = repository.isLoadingDeviceData.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun setUser(user: FirebaseUser?) {
        currentUser.value = user
    }

    fun loadDeviceData() {
        repository.loadDeviceData()
    }

    fun importSelectedDeviceContacts(selected: List<DeviceContact>, group: String) {
        val count = repository.importDeviceContacts(selected, group)
        showToast("$count Device Contacts imported to $group")
        repository.loadDeviceData() // Refresh device contacts list with updated status
    }

    fun syncCallHistoryFromDevice() {
        val updatedCount = repository.syncCallHistoryFromDeviceLogs()
        if (updatedCount > 0) {
            showToast("Synced $updatedCount calls from device history into CRM!")
        } else {
            showToast("No new matching device calls to sync.")
        }
    }

    // --- Computed Hisab People ---
    fun computeHisabPeople(txList: List<HisabTx>, phonesMap: Map<String, List<String>>): List<HisabPerson> {
        val map = mutableMapOf<String, Pair<Double, Double>>() // name -> (inn, dep)
        for (tx in txList) {
            val current = map[tx.name] ?: Pair(0.0, 0.0)
            map[tx.name] = if (tx.type == "in") {
                Pair(current.first + tx.amt, current.second)
            } else {
                Pair(current.first, current.second + tx.amt)
            }
        }
        return map.map { (name, totals) ->
            HisabPerson(
                name = name,
                inn = totals.first,
                dep = totals.second,
                bal = totals.first - totals.second,
                phones = phonesMap[name] ?: emptyList()
            )
        }.sortedByDescending { Math.abs(it.bal) }
    }

    // --- Computed Hisab Totals ---
    fun computeHisabTotals(people: List<HisabPerson>): Map<String, Double> {
        var sIn = 0.0
        var sDep = 0.0
        var sGive = 0.0
        var sGet = 0.0
        for (p in people) {
            sIn += p.inn
            sDep += p.dep
            if (p.bal > 0) sGive += p.bal
            else sGet += -p.bal
        }
        return mapOf("sIn" to sIn, "sDep" to sDep, "sGive" to sGive, "sGet" to sGet)
    }

    // --- Computed Smart Advice ---
    fun computeHisabAdvice(people: List<HisabPerson>): List<HisabAdvice> {
        val creditors = people.filter { it.bal > 0 }.map { it to it.bal }.sortedByDescending { it.second }.toMutableList()
        val debtors = people.filter { it.bal < 0 }.map { it to -it.bal }.sortedByDescending { it.second }.toMutableList()

        val adviceList = mutableListOf<HisabAdvice>()
        for (c in creditors) {
            var need = c.second
            val suggestions = mutableListOf<HisabSuggestion>()
            for (dIdx in debtors.indices) {
                val d = debtors[dIdx]
                if (need <= 0 || d.second <= 0) continue
                val x = Math.min(need, d.second)
                need -= x
                debtors[dIdx] = d.first to (d.second - x)
                suggestions.add(
                    HisabSuggestion(
                        targetPerson = c.first.name,
                        sourcePerson = d.first.name,
                        suggestAmt = x,
                        dueTotal = -d.first.bal
                    )
                )
            }
            adviceList.add(
                HisabAdvice(
                    person = c.first.name,
                    inn = c.first.inn,
                    dep = c.first.dep,
                    needRem = need,
                    suggestions = suggestions
                )
            )
        }
        return adviceList
    }

    // --- AI High Thinking Analysis (gemini-3.1-pro-preview with ThinkingLevel.HIGH) ---
    fun generateHighThinkingAudit(promptSummary: String) {
        viewModelScope.launch {
            isAiThinking.value = true
            aiInsightText.value = null
            try {
                val response = withContext(Dispatchers.IO) {
                    val apiKey = try {
                        val buildConfigClass = Class.forName("com.example.BuildConfig")
                        val field = buildConfigClass.getField("GEMINI_API_KEY")
                        field.get(null) as? String ?: ""
                    } catch (e: Exception) {
                        ""
                    }

                    if (apiKey.isBlank()) {
                        return@withContext "AI Analysis: Gemini API key is not configured in Secrets panel. Please provide GEMINI_API_KEY to enable AI High Thinking."
                    }

                    val systemInstruction = "You are Tagada CRM & Hisab Khata senior financial strategist. Analyze call performance, arrears recovery, and debtor-creditor reconciliation with deep reasoning."
                    val fullPrompt = "$systemInstruction\n\nData Summary:\n$promptSummary\n\nProvide deep strategic recommendations to optimize debt collection, call follow-ups, and ledger settlement."

                    val jsonBody = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                val partsArray = JSONArray().apply {
                                    put(JSONObject().apply { put("text", fullPrompt) })
                                }
                                put("parts", partsArray)
                            })
                        }
                        put("contents", contentsArray)
                        val config = JSONObject().apply {
                            val thinkingConfig = JSONObject().apply {
                                put("thinkingLevel", "HIGH")
                            }
                            put("thinkingConfig", thinkingConfig)
                        }
                        put("generationConfig", config)
                    }

                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey")
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val res = okHttpClient.newCall(request).execute()
                    val resString = res.body?.string() ?: ""
                    if (!res.isSuccessful) {
                        "AI error (${res.code}): ${JSONObject(resString).optJSONObject("error")?.optString("message") ?: res.message}"
                    } else {
                        val root = JSONObject(resString)
                        val candidates = root.optJSONArray("candidates")
                        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        parts?.optJSONObject(0)?.optString("text") ?: "No response generated."
                    }
                }
                aiInsightText.value = response
            } catch (e: Exception) {
                aiInsightText.value = "Analysis failed: ${e.localizedMessage}"
            } finally {
                isAiThinking.value = false
            }
        }
    }

    // Check time filter
    fun isWithinTime(dateStr: String, filter: String): Boolean {
        if (filter == "all" || dateStr.isBlank()) return true
        val days = filter.toIntOrNull() ?: return true
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = parser.parse(dateStr.take(10)) ?: return true
            val diffMs = Math.abs(System.currentTimeMillis() - d.time)
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            diffDays <= days
        } catch (e: Exception) {
            true
        }
    }
}
