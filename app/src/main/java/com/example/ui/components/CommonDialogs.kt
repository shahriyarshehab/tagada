package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.CONTACT_GROUPS
import com.example.ui.dtFmt
import com.example.ui.moneyFmt
import com.example.util.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, group: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("General") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("add_contact_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add New Contact", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input"),
                    singleLine = true
                )

                Text("Contact Group", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CONTACT_GROUPS.take(3).forEach { g ->
                        FilterChip(
                            selected = group == g,
                            onClick = { group = g },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CONTACT_GROUPS.drop(3).forEach { g ->
                        FilterChip(
                            selected = group == g,
                            onClick = { group = g },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onConfirm(name, phone, group)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_contact_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Save Contact", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FinanceDialog(
    call: Contact,
    groupType: String,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val isArrears = groupType == "Arrears" || groupType == "বকেয়া"
    var amount by remember { mutableStateOf(if (isArrears) call.fin?.get("bokeyaGiven") ?: "" else call.fin?.get("loanAmount") ?: "") }
    var date by remember { mutableStateOf(if (isArrears) call.fin?.get("bokeyaGivenDate") ?: "" else call.fin?.get("loanDate") ?: "") }
    var dueOrDuration by remember { mutableStateOf(if (isArrears) call.fin?.get("bokeyaDue") ?: "" else call.fin?.get("loanDuration") ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().testTag("finance_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isArrears) "Arrears & Recovery Details" else "Loan Account Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isArrears) "Given Amount (৳)" else "Loan Amount (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueOrDuration,
                    onValueChange = { dueOrDuration = it },
                    label = { Text(if (isArrears) "Remaining Due (৳)" else "Tenure (Months)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val map = if (isArrears) {
                            mapOf("bokeyaGiven" to amount, "bokeyaGivenDate" to date, "bokeyaDue" to dueOrDuration)
                        } else {
                            mapOf("loanAmount" to amount, "loanDate" to date, "loanDuration" to dueOrDuration)
                        }
                        onSave(map)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_finance_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isArrears) Color(0xFFE11D48) else Color(0xFF6366F1)
                    )
                ) {
                    Text("Save Finance Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SurveyNoteDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSave: (note: String, newGroup: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(contact.contactGroup) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().testTag("survey_note_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Survey Call Follow-up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                Text("${contact.name} (${contact.phone})", fontSize = 12.sp, color = Color.LightGray)

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("What was discussed during the call?") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                Text("Move to another group?", fontSize = 11.sp, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CONTACT_GROUPS.take(3).forEach { g ->
                        FilterChip(
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = g },
                            label = { Text(g, fontSize = 10.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CONTACT_GROUPS.drop(3).forEach { g ->
                        FilterChip(
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = g },
                            label = { Text(g, fontSize = 10.sp) }
                        )
                    }
                }

                Button(
                    onClick = { onSave(note, selectedGroup) },
                    modifier = Modifier.fillMaxWidth().testTag("save_survey_note_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReminderDialog(
    contacts: List<Contact>,
    preselectedContact: Contact?,
    onDismiss: () -> Unit,
    onSave: (callId: String?, name: String, phone: String, group: String, datetime: String, note: String) -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(NotificationScheduler.hasNotificationPermission(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var selectedContact by remember { mutableStateOf(preselectedContact ?: contacts.firstOrNull()) }
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var date by remember { mutableStateOf(todayDate) }
    var time by remember { mutableStateOf("10:00") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().testTag("reminder_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set Follow-up Reminder", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                // Notification Permission Banner
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🔔 Enable Notifications", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                                Text("Allow permission so Tagada can alert you when follow-up is due.", fontSize = 9.sp, color = Color(0xFFC7D2FE))
                            }
                            Button(
                                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Allow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (preselectedContact == null && contacts.isNotEmpty()) {
                    Text("Select Contact", fontSize = 11.sp, color = Color.Gray)
                    LazyColumn(modifier = Modifier.height(100.dp)) {
                        items(contacts) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedContact = c }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    c.name,
                                    fontSize = 12.sp,
                                    color = if (selectedContact?.id == c.id) Color(0xFF818CF8) else Color.White
                                )
                                Text(c.phone, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else if (selectedContact != null) {
                    Text(
                        "${selectedContact?.name} (${selectedContact?.phone})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Reminder note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val c = selectedContact
                        if (c != null) {
                            onSave(c.id, c.name, c.phone, c.contactGroup, "${date}T$time", note)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_reminder_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Save Reminder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MonthlyPaymentHistoryDialog(
    contact: Contact,
    historyList: List<PayHistoryItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment History: ${contact.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                val items = historyList.filter { it.callId == contact.id }
                if (items.isEmpty()) {
                    Text("No payment history recorded yet", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { h ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Month: ${h.month}/${h.year}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            h.status.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (h.status == "paid") Color(0xFF34D399) else Color(0xFFF87171)
                                        )
                                        h.paidDate?.let {
                                            Text("Paid on: ${it.take(10)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    Text("Calls before payment: ${h.callsBeforePaid}", fontSize = 10.sp, color = Color(0xFF818CF8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
