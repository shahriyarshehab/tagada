package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Contact(
    var id: String = "",
    var userId: String = "",
    var name: String = "",
    var phone: String = "",
    var contactGroup: String = "General",
    var type: String = "dialed",
    var status: String = "unpaid",
    var paidDate: String? = null,
    var date: String = "",
    var monthCallCount: Int = 0,
    var callHistory: List<String> = emptyList(),
    var isFavorite: Boolean = false,
    var leadStatus: String = "none", // none, green, yellow, red
    var fin: Map<String, String>? = null
)

@IgnoreExtraProperties
data class Reminder(
    var id: String = "",
    var userId: String = "",
    var callId: String? = null,
    var name: String = "",
    var phone: String = "",
    var group: String = "General",
    var datetime: String = "",
    var note: String = "",
    var done: Boolean = false
)

@IgnoreExtraProperties
data class QuickNote(
    var id: String = "",
    var userId: String = "",
    var text: String = "",
    var date: String = ""
)

@IgnoreExtraProperties
data class PayHistoryItem(
    var id: String = "",
    var userId: String = "",
    var callId: String = "",
    var name: String = "",
    var month: Int = 1,
    var year: Int = 2026,
    var status: String = "unpaid",
    var paidDate: String? = null,
    var callsBeforePaid: Int = 0
)

@IgnoreExtraProperties
data class HisabTx(
    var id: String = "",
    var userId: String = "",
    var type: String = "in", // "in" or "dep"
    var name: String = "",
    var amt: Double = 0.0,
    var date: String = "",
    var note: String = "",
    var src: String = ""
)

@IgnoreExtraProperties
data class HisabReminder(
    var id: String = "",
    var userId: String = "",
    var name: String = "",
    var due: String = "",
    var done: Boolean = false
)

data class HisabPerson(
    val name: String,
    val inn: Double,
    val dep: Double,
    val bal: Double, // inn - dep (>0: Payable; <0: Receivable)
    val phones: List<String> = emptyList()
)

data class HisabSuggestion(
    val targetPerson: String,
    val sourcePerson: String,
    val suggestAmt: Double,
    val dueTotal: Double
)

data class HisabAdvice(
    val person: String,
    val inn: Double,
    val dep: Double,
    val needRem: Double,
    val suggestions: List<HisabSuggestion>
)
