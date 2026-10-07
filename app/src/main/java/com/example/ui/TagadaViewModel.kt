package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.model.*
import com.example.data.repository.TagadaRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    val currentUser = MutableStateFlow<FirebaseUser?>(AuthManager.currentUser)
    val toastMessage = MutableStateFlow<String?>(null)

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

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun setUser(user: FirebaseUser?) {
        currentUser.value = user
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
