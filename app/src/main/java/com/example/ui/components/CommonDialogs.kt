package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.util.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddContactDialog(
    initialPhone: String = "",
    initialName: String = "",
    defaultAlsoSaveToDevice: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, group: String, alsoSaveToDevice: Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var group by remember { mutableStateOf("General") }
    var alsoSaveToDevice by remember { mutableStateOf(defaultAlsoSaveToDevice) }

    val writeContactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        alsoSaveToDevice = isGranted
    }

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagadaLogoBadge(size = 28.dp)
                        Text("Add New Contact", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
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

                // Confirmation / Auto-include toggle card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!alsoSaveToDevice) {
                                    writeContactsPermLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                                }
                                alsoSaveToDevice = !alsoSaveToDevice
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-include in Device Phonebook", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                            Text("Also creates this contact in your phone's native Contacts app", fontSize = 9.sp, color = Color(0xFFC7D2FE))
                        }
                        Checkbox(
                            checked = alsoSaveToDevice,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    writeContactsPermLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                                }
                                alsoSaveToDevice = checked
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onConfirm(name, phone, group, true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_and_include_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Auto-Include in Phone Contacts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onConfirm(name, phone, group, false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_crm_only_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save to Tagada CRM Only", fontSize = 12.sp, color = Color(0xFFC7D2FE))
                    }
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
    val isArrears = groupType == "Arrears" || groupType == "Arrears Member"
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
                                Text("Allow notification permission so Tagada can alert you when follow-up is due.", fontSize = 9.sp, color = Color(0xFFC7D2FE))
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

@Composable
fun DeviceContactsImportDialog(
    deviceContacts: List<DeviceContact>,
    onDismiss: () -> Unit,
    onImport: (selected: List<DeviceContact>, targetGroup: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("General") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val filteredContacts = remember(deviceContacts, searchQuery) {
        deviceContacts.filter { dc ->
            searchQuery.isBlank() || dc.name.contains(searchQuery, ignoreCase = true) || dc.phone.contains(searchQuery)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Import Device Contacts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${filteredContacts.size} contacts found on device", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter device contacts...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                )

                // Target Group selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Target Group:", fontSize = 11.sp, color = Color.LightGray)
                    var expandedGrp by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { expandedGrp = true },
                            label = { Text(selectedGroup, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        DropdownMenu(expanded = expandedGrp, onDismissRequest = { expandedGrp = false }) {
                            CONTACT_GROUPS.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g, fontSize = 11.sp) },
                                    onClick = {
                                        selectedGroup = g
                                        expandedGrp = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Select All / Deselect All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedIds.size} selected", fontSize = 11.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                selectedIds = filteredContacts.filter { !it.isAlreadyInCrm }.map { it.id }.toSet()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Select New", fontSize = 11.sp, color = Color(0xFF34D399))
                        }
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredContacts) { dc ->
                        val isChecked = selectedIds.contains(dc.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChecked) Color(0xFF1E1B4B) else Color(0xFF111827))
                                .border(1.dp, if (isChecked) Color(0xFF6366F1) else Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - dc.id else selectedIds + dc.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(dc.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (dc.isAlreadyInCrm) {
                                        Box(
                                            modifier = Modifier.background(Color(0xFF065F46), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("IN CRM", fontSize = 8.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(dc.phone, fontSize = 10.sp, color = Color.Gray)
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + dc.id else selectedIds - dc.id
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val toImport = deviceContacts.filter { selectedIds.contains(it.id) }
                        if (toImport.isNotEmpty()) {
                            onImport(toImport, selectedGroup)
                            onDismiss()
                        }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Import ${selectedIds.size} Contacts to $selectedGroup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Confirmation dialog shown when saving or auto-including a contact.
 */
@Composable
fun ConfirmContactSaveDialog(
    name: String,
    phone: String,
    group: String,
    onDismiss: () -> Unit,
    onConfirm: (alsoSaveToDevice: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("confirm_contact_save_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagadaLogoBadge(size = 28.dp)
                        Text("Confirm Contact Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(phone, fontSize = 13.sp, color = Color(0xFFA5B4FC))
                        Text("Group: $group", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Text(
                    "Would you like to auto-include and save this contact into your device's native contacts as well?",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 17.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yes, Save to CRM & Phonebook", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onConfirm(false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("No, Keep in Tagada CRM Only", fontSize = 12.sp, color = Color(0xFFC7D2FE))
                    }
                }
            }
        }
    }
}

/**
 * Prompt banner or dialog when new contacts on device are detected.
 */
@Composable
fun AutoIncludeDeviceContactsPromptDialog(
    newContactsCount: Int,
    onDismiss: () -> Unit,
    onConfirmAutoIncludeAll: () -> Unit,
    onReviewClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("auto_include_device_prompt_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagadaLogoBadge(size = 30.dp)
                        Text("Auto-Include Device Contacts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    "Detected $newContactsCount contact(s) on your phone that are not yet in Tagada CRM. Would you like to automatically include them?",
                    fontSize = 12.5.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirmAutoIncludeAll,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Include All ($newContactsCount) Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReviewClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Review Contacts First", fontSize = 12.sp, color = Color(0xFFA5B4FC))
                    }
                }
            }
        }
    }
}
