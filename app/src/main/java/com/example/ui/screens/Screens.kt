package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.*
import com.example.ui.components.ContactCard
import com.example.ui.components.FirestoreSecurityBadge
import com.example.ui.components.TagadaBrandHeader
import com.example.ui.components.TagadaLogoBadge
import com.example.util.DeviceDataManager
import com.example.util.NotificationScheduler
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: TagadaViewModel,
    t: TagadaStrings,
    dashGroupFilter: String,
    dashTimeFilter: String,
    connectedList: List<Contact>,
    unpaidList: List<Contact>,
    paidList: List<Contact>,
    favoritesList: List<Contact>,
    hisabTotals: Map<String, Double>,
    contacts: List<Contact>,
    reminders: List<Reminder>,
    notes: List<QuickNote>,
    quickNoteInput: String,
    onQuickNoteChange: (String) -> Unit,
    onSaveQuickNote: () -> Unit,
    onDrillDown: (title: String, list: List<Contact>) -> Unit,
    onOpenSummary: () -> Unit,
    onAddReminderClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        // Group & Time filters
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1220), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1B263B), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group Pills
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = dashGroupFilter == "all",
                            onClick = { viewModel.dashGroupFilter.value = "all" },
                            label = { Text(t.allGroups, fontSize = 11.sp) }
                        )
                    }
                    items(CONTACT_GROUPS) { g ->
                        FilterChip(
                            selected = dashGroupFilter == g,
                            onClick = { viewModel.dashGroupFilter.value = g },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }

                // Time Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0F1D), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("all" to t.allTime, "1" to "1 ${t.days}", "7" to "7 ${t.days}", "30" to "30 ${t.days}").forEach { (valKey, label) ->
                        val selected = dashTimeFilter == valKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color(0xFF6366F1) else Color.Transparent)
                                .clickable { viewModel.dashTimeFilter.value = valKey }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 4 KPI Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = t.connected,
                        count = connectedList.size,
                        subtitle = "Completed Calls",
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onDrillDown(t.connected, connectedList) }
                    )
                    MetricCard(
                        title = "Unpaid (Active)",
                        count = unpaidList.size,
                        subtitle = "Follow-up Required",
                        accentColor = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f),
                        onClick = { onDrillDown(t.unpaidList, unpaidList) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Paid (Active)",
                        count = paidList.size,
                        subtitle = "Payment Verified",
                        accentColor = Color(0xFF14B8A6),
                        modifier = Modifier.weight(1f),
                        onClick = { onDrillDown(t.paidList, paidList) }
                    )
                    MetricCard(
                        title = t.favorites,
                        count = favoritesList.size,
                        subtitle = "Starred Contacts",
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onDrillDown(t.favorites, favoritesList) }
                    )
                }
            }
        }

        // Device Call & Contacts quick shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF312E81)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Device Call Logs & Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("View missed/received calls & sync contacts", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.activeTab.value = "calls"
                            viewModel.callsSubTab.value = "call_log"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("View Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hisab Khata Live Balance Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.activeTab.value = "hisab" },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6610B981))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x3310B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Hisab Khata Balance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Box(
                                    modifier = Modifier.background(Color(0x3310B981), RoundedCornerShape(8.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("LIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }
                            Text(
                                "Payable: ${moneyFmt(hisabTotals["sGive"] ?: 0.0)}   Receivable: ${moneyFmt(hisabTotals["sGet"] ?: 0.0)}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.activeTab.value = "hisab" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                    ) {
                        Text("Ledger →", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                    }
                }
            }
        }

        // Call Summary & Leaderboard Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenSummary,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162136))
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Summary", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                }
                Button(
                    onClick = onOpenSummary,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162136))
                ) {
                    Text("Top 10 Contacts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                }
            }
        }

        // Contact Group Breakdowns
        items(CONTACT_GROUPS) { group ->
            val groupList = contacts.filter { it.contactGroup == group }
            if (groupList.isNotEmpty()) {
                val accentColor = when (group) {
                    "Arrears" -> Color(0xFFF43F5E)
                    "Survey" -> Color(0xFFF59E0B)
                    "Active" -> Color(0xFF6366F1)
                    else -> Color(0xFF0EA5E9)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(14.dp).background(accentColor))
                                Text(group, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Total: ${groupList.size}", fontSize = 10.sp, color = Color.Gray)
                            }
                            TextButton(
                                onClick = { onDrillDown(group, groupList) },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                            ) {
                                Text("View All", fontSize = 10.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (group == "Arrears") {
                            val connected = groupList.count { it.callHistory.isNotEmpty() && it.type != "no_answer" }
                            val noAnswer = groupList.count { it.type == "no_answer" }
                            val notContacted = groupList.count { it.callHistory.isEmpty() }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BreakDownPill(
                                    label = t.connected,
                                    count = connected,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Arrears (Connected)", groupList.filter { it.callHistory.isNotEmpty() }) }
                                )
                                BreakDownPill(
                                    label = t.noAnswer,
                                    count = noAnswer,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Arrears (No Answer)", groupList.filter { it.type == "no_answer" }) }
                                )
                                BreakDownPill(
                                    label = t.notContacted,
                                    count = notContacted,
                                    color = Color(0xFFF43F5E),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Arrears (Not Contacted)", groupList.filter { it.callHistory.isEmpty() }) }
                                )
                            }
                        } else if (group == "Survey") {
                            val green = groupList.count { it.leadStatus == "green" }
                            val yellow = groupList.count { it.leadStatus == "yellow" }
                            val red = groupList.count { it.leadStatus == "red" }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BreakDownPill(
                                    label = "🔥 Hot",
                                    count = green,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Survey (Hot Leads)", groupList.filter { it.leadStatus == "green" }) }
                                )
                                BreakDownPill(
                                    label = "⚡ Warm",
                                    count = yellow,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Survey (Warm Leads)", groupList.filter { it.leadStatus == "yellow" }) }
                                )
                                BreakDownPill(
                                    label = "❌ Reject",
                                    count = red,
                                    color = Color(0xFFF43F5E),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDrillDown("Survey (Disqualified)", groupList.filter { it.leadStatus == "red" }) }
                                )
                            }
                        } else {
                            val contacted = groupList.count { it.callHistory.isNotEmpty() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0D1525), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contacted: $contacted", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                                Text("Remaining: ${groupList.size - contacted}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Reminders Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                            Text("Follow-up Reminders (${reminders.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = onAddReminderClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    for (r in reminders) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D1526), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${r.name} (${r.group})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(r.datetime.replace("T", " "), fontSize = 10.sp, color = Color(0xFF818CF8))
                                if (r.note.isNotBlank()) Text("- ${r.note}", fontSize = 9.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.repository.deleteReminder(r.id) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${r.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    if (reminders.isEmpty()) {
                        Text("No active reminders", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // Quick Notes Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.notes, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quickNoteInput,
                            onValueChange = onQuickNoteChange,
                            placeholder = { Text(t.typeNote, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = onSaveQuickNote,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text(t.save, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    for (note in notes.take(5)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x3378350F), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0x6692400E), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.date.take(10), fontSize = 9.sp, color = Color(0xFFFBBF24))
                                Text(note.text, fontSize = 11.sp, color = Color.White)
                            }
                            IconButton(onClick = { viewModel.repository.deleteNote(note.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            }
            Text("$count", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Text(subtitle, fontSize = 9.sp, color = Color(0x99FFFFFF))
        }
    }
}

@Composable
fun BreakDownPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text("$count", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun CallsScreen(
    viewModel: TagadaViewModel,
    contacts: List<Contact>,
    globalSearch: String,
    sortType: String,
    onToggleFavorite: (String) -> Unit,
    onCallClick: (Contact) -> Unit,
    onFinanceClick: (Contact) -> Unit,
    onChangeGroup: (String, String) -> Unit,
    onHistoryClick: (Contact) -> Unit,
    onStatusChange: (String, String) -> Unit,
    onReminderClick: (Contact) -> Unit,
    onAddContactDirectly: (name: String, phone: String) -> Unit,
    onOpenImportDeviceContacts: () -> Unit
) {
    val context = LocalContext.current
    val callsSubTab by viewModel.callsSubTab.collectAsState()
    val callLogFilter by viewModel.callLogFilter.collectAsState()
    val deviceCallLogs by viewModel.deviceCallLogs.collectAsState()
    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val isLoadingDeviceData by viewModel.isLoadingDeviceData.collectAsState()

    var hasContactsPermission by remember { mutableStateOf(DeviceDataManager.hasContactsPermission(context)) }
    var hasCallLogPermission by remember { mutableStateOf(DeviceDataManager.hasCallLogPermission(context)) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Contacts permission granted!")
            viewModel.loadDeviceData()
        }
    }

    val callLogPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Call log permission granted!")
            viewModel.loadDeviceData()
        }
    }

    // Auto load device data on first entry if permission exists
    LaunchedEffect(hasCallLogPermission, hasContactsPermission) {
        if (hasCallLogPermission || hasContactsPermission) {
            viewModel.loadDeviceData()
        }
    }

    val filteredContacts = remember(contacts, globalSearch, sortType) {
        contacts
            .filter { c ->
                globalSearch.isBlank() ||
                c.name.contains(globalSearch, ignoreCase = true) ||
                c.phone.contains(globalSearch)
            }
            .let { list ->
                when (sortType) {
                    "a-z" -> list.sortedBy { it.name.lowercase() }
                    "z-a" -> list.sortedByDescending { it.name.lowercase() }
                    "most-talked" -> list.sortedByDescending { it.monthCallCount }
                    else -> list.sortedByDescending { it.date }
                }
            }
    }

    val filteredCallLogs = remember(deviceCallLogs, callLogFilter, globalSearch) {
        deviceCallLogs.filter { log ->
            val matchesFilter = when (callLogFilter) {
                "missed" -> log.type == DeviceCallType.MISSED
                "received" -> log.type == DeviceCallType.INCOMING
                "dialed" -> log.type == DeviceCallType.OUTGOING
                else -> true
            }
            val matchesSearch = globalSearch.isBlank() ||
                    (log.name?.contains(globalSearch, ignoreCase = true) == true) ||
                    log.number.contains(globalSearch)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Sub-tabs: "CRM Contacts" vs "Device Call Log" vs "Device Contacts"
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = callsSubTab == "crm_contacts",
                onClick = { viewModel.callsSubTab.value = "crm_contacts" },
                label = { Text("CRM Contacts (${contacts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = callsSubTab == "call_log",
                onClick = {
                    viewModel.callsSubTab.value = "call_log"
                    if (hasCallLogPermission) viewModel.loadDeviceData()
                },
                label = { Text("Call History (${deviceCallLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = callsSubTab == "device_contacts",
                onClick = {
                    viewModel.callsSubTab.value = "device_contacts"
                    if (hasContactsPermission) viewModel.loadDeviceData()
                },
                label = { Text("Device (${deviceContacts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
        }

        when (callsSubTab) {
            "crm_contacts" -> {
                // Header with Count & Sort Menu
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${filteredContacts.size} Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    var expandedSort by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { expandedSort = true },
                            label = { Text("Sort: $sortType", fontSize = 10.sp) }
                        )
                        DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                            DropdownMenuItem(text = { Text("Newest First") }, onClick = { viewModel.sortType.value = "newest"; expandedSort = false })
                            DropdownMenuItem(text = { Text("Most Talked") }, onClick = { viewModel.sortType.value = "most-talked"; expandedSort = false })
                            DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { viewModel.sortType.value = "a-z"; expandedSort = false })
                            DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { viewModel.sortType.value = "z-a"; expandedSort = false })
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(filteredContacts, key = { it.id }) { call ->
                        ContactCard(
                            contact = call,
                            isCompact = true,
                            onToggleFavorite = { onToggleFavorite(call.id) },
                            onCallClick = { onCallClick(call) },
                            onFinanceClick = { onFinanceClick(call) },
                            onChangeGroup = { grp -> onChangeGroup(call.id, grp) },
                            onHistoryClick = { onHistoryClick(call) },
                            onStatusChange = { st -> onStatusChange(call.id, st) },
                            onReminderClick = { onReminderClick(call) }
                        )
                    }
                    if (filteredContacts.isEmpty()) {
                        item {
                            Text("No contacts found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(24.dp))
                        }
                    }
                }
            }

            "call_log" -> {
                // Call Log View
                if (!hasCallLogPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFFA5B4FC))
                                Text("Call History Permission Needed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(
                                "To view missed calls, received calls, and dialed call details directly in Tagada CRM, please grant the Call Log permission.",
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE),
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = { callLogPermLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Grant Call Log Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Filters & Sync action row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("all" to "All", "missed" to "Missed", "received" to "Received", "dialed" to "Dialed").forEach { (valKey, label) ->
                                val selected = callLogFilter == valKey
                                item {
                                    AssistChip(
                                        onClick = { viewModel.callLogFilter.value = valKey },
                                        label = { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (selected) Color(0xFF312E81) else Color.Transparent,
                                            labelColor = if (selected) Color(0xFFA5B4FC) else Color.Gray
                                        )
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.syncCallHistoryFromDevice() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync CRM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isLoadingDeviceData) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF6366F1))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(filteredCallLogs, key = { it.id }) { log ->
                                val (badgeColor, badgeText, badgeIcon) = when (log.type) {
                                    DeviceCallType.MISSED -> Triple(Color(0xFFEF4444), "MISSED", Icons.Default.CallMissed)
                                    DeviceCallType.INCOMING -> Triple(Color(0xFF38BDF8), "RECEIVED", Icons.Default.CallReceived)
                                    DeviceCallType.OUTGOING -> Triple(Color(0xFF10B981), "DIALED", Icons.Default.CallMade)
                                    DeviceCallType.REJECTED -> Triple(Color(0xFFF59E0B), "REJECTED", Icons.Default.CallEnd)
                                    DeviceCallType.OTHER -> Triple(Color.Gray, "CALL", Icons.Default.Phone)
                                }

                                val isAlreadyInCrm = contacts.any { DeviceDataManager.normalizeMatch(it.phone, log.number) }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(36.dp).clip(CircleShape).background(badgeColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                                            }

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        log.name ?: log.number,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Box(
                                                        modifier = Modifier.background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(badgeText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                                                    }
                                                }
                                                if (log.name != null) {
                                                    Text(log.number, fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(DeviceDataManager.formatCallDate(log.dateMillis), fontSize = 10.sp, color = Color.Gray)
                                                    if (log.type != DeviceCallType.MISSED && log.durationSeconds > 0) {
                                                        Text("• ${DeviceDataManager.formatDuration(log.durationSeconds)}", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                                                    }
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!isAlreadyInCrm) {
                                                Button(
                                                    onClick = {
                                                        onAddContactDirectly(log.name ?: "Contact", log.number)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                                                ) {
                                                    Text("+ CRM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.number}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF10B981))
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            if (filteredCallLogs.isEmpty()) {
                                item {
                                    Text("No call history records found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            "device_contacts" -> {
                // Device Contacts View
                if (!hasContactsPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Contacts, contentDescription = null, tint = Color(0xFFA5B4FC))
                                Text("Contacts Permission Needed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(
                                "To load device contacts into Tagada CRM, please grant the Contacts permission.",
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE),
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = { contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Grant Contacts Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val unimportedContacts = remember(deviceContacts) {
                        deviceContacts.filter { !it.isAlreadyInCrm }
                    }

                    if (unimportedContacts.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${unimportedContacts.size} Unimported Phone Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                                    Text("Auto-include all phone contacts into Tagada CRM", fontSize = 10.sp, color = Color(0xFFD1FAE5))
                                }
                                Button(
                                    onClick = {
                                        viewModel.autoIncludeAllDeviceContacts("General")
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto-Include All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${deviceContacts.size} Device Contacts (${unimportedContacts.size} new)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Button(
                            onClick = onOpenImportDeviceContacts,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batch Import", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isLoadingDeviceData) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF6366F1))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(deviceContacts, key = { it.id }) { dc ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(dc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                if (dc.isAlreadyInCrm) {
                                                    Box(
                                                        modifier = Modifier.background(Color(0xFF065F46), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("IN CRM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                                                    }
                                                }
                                            }
                                            Text(dc.phone, fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!dc.isAlreadyInCrm) {
                                                Button(
                                                    onClick = {
                                                        viewModel.repository.addContact(dc.name, dc.phone, "General")
                                                        viewModel.showToast("${dc.name} added to CRM")
                                                        viewModel.loadDeviceData()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                                ) {
                                                    Text("+ CRM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dc.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF10B981))
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            if (deviceContacts.isEmpty()) {
                                item {
                                    Text("No device contacts found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(24.dp))
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
fun HisabScreen(
    viewModel: TagadaViewModel,
    hisabSubTab: String,
    hisabSearch: String,
    hisabTotals: Map<String, Double>,
    hisabPeople: List<HisabPerson>,
    hisabTx: List<HisabTx>,
    hisabReminders: List<HisabReminder>,
    hisabAcks: Map<String, String>,
    adviceList: List<HisabAdvice>,
    onSelectPerson: (String) -> Unit,
    onEditTx: (HisabTx) -> Unit
) {
    var formName by remember { mutableStateOf("") }
    var formAmt by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var formPhone by remember { mutableStateOf("") }
    var formNote by remember { mutableStateOf("") }
    var formSrc by remember { mutableStateOf("") }

    val filteredPeople = remember(hisabPeople, hisabSearch) {
        hisabPeople.filter { hisabSearch.isBlank() || it.name.contains(hisabSearch, ignoreCase = true) }
    }

    val autoDues = remember(hisabPeople, hisabAcks, hisabTx) {
        val t0 = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        hisabPeople.filter { it.bal != 0.0 }.mapNotNull { p ->
            var last = hisabAcks[p.name] ?: ""
            hisabTx.filter { it.name == p.name }.forEach { t ->
                if (t.date > last) last = t.date
            }
            if (last.isNotBlank()) {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val d1 = parser.parse(t0)?.time ?: 0L
                val d2 = parser.parse(last)?.time ?: 0L
                val diffDays = ((d1 - d2) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays >= 10) Pair(p, diffDays) else null
            } else null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Sub-tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hisab Khata Ledger", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.hisabSubTab.value = "in" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3310B981))
                        ) {
                            Text("+ Received", fontSize = 10.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.hisabSubTab.value = "dep" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x336366F1))
                        ) {
                            Text("+ Deposit", fontSize = 10.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = hisabSubTab == "summary",
                            onClick = { viewModel.hisabSubTab.value = "summary" },
                            label = { Text("All Accounts (${hisabPeople.size})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hisabSubTab == "in",
                            onClick = { viewModel.hisabSubTab.value = "in" },
                            label = { Text("Received In (${hisabTx.count { it.type == "in" }})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hisabSubTab == "dep",
                            onClick = { viewModel.hisabSubTab.value = "dep" },
                            label = { Text("Deposited (${hisabTx.count { it.type == "dep" }})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hisabSubTab == "notes",
                            onClick = { viewModel.hisabSubTab.value = "notes" },
                            label = { Text("Smart Advice ✨", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = hisabSubTab == "reminders",
                            onClick = { viewModel.hisabSubTab.value = "reminders" },
                            label = { Text("Due Alerts ⏰", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 4 Financial Stat Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Received", fontSize = 9.sp, color = Color.Gray)
                            Text(moneyFmt(hisabTotals["sIn"] ?: 0.0), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Deposited", fontSize = 9.sp, color = Color.Gray)
                            Text(moneyFmt(hisabTotals["sDep"] ?: 0.0), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0x334C0519))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("They Get (Payable)", fontSize = 9.sp, color = Color(0xFFFDA4AF))
                            Text(moneyFmt(hisabTotals["sGive"] ?: 0.0), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF43F5E))
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0x33064E3B))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("I Get (Receivable)", fontSize = 9.sp, color = Color(0xFF6EE7B7))
                            Text(moneyFmt(hisabTotals["sGet"] ?: 0.0), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // Content by subtab
        when (hisabSubTab) {
            "summary" -> {
                item {
                    OutlinedTextField(
                        value = hisabSearch,
                        onValueChange = { viewModel.hisabSearch.value = it },
                        placeholder = { Text("Search accounts...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        singleLine = true
                    )
                }
                items(filteredPeople) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .clickable { onSelectPerson(p.name) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (p.phones.isNotEmpty()) {
                                    Text("📞 ${p.phones.first()}", fontSize = 9.sp, color = Color(0xFF34D399))
                                }
                            }
                            Text("Received ${moneyFmt(p.inn)} • Deposited ${moneyFmt(p.dep)}", fontSize = 10.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (p.bal > 0) Color(0x33F43F5E) else if (p.bal < 0) Color(0x3310B981) else Color(0xFF1E293B),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (p.bal > 0) "Payable ${moneyFmt(p.bal)}" else if (p.bal < 0) "Receivable ${moneyFmt(-p.bal)}" else "Settled",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (p.bal > 0) Color(0xFFF87171) else if (p.bal < 0) Color(0xFF34D399) else Color.Gray
                            )
                        }
                    }
                }
            }

            "in" -> {
                item {
                    // Form In
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Money Received From", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            OutlinedTextField(
                                value = formName,
                                onValueChange = { formName = it },
                                label = { Text("Customer Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = formAmt,
                                    onValueChange = { formAmt = it },
                                    label = { Text("Amount (৳)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = formDate,
                                    onValueChange = { formDate = it },
                                    label = { Text("Date") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = formPhone,
                                onValueChange = { formPhone = it },
                                label = { Text("Phone Number (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = formNote,
                                onValueChange = { formNote = it },
                                label = { Text("Note (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val amt = formAmt.toDoubleOrNull() ?: 0.0
                                    if (formName.isNotBlank() && amt > 0) {
                                        viewModel.repository.addHisabTx(
                                            type = "in",
                                            name = formName,
                                            amt = amt,
                                            date = formDate,
                                            phone = formPhone.takeIf { it.isNotBlank() },
                                            note = formNote.takeIf { it.isNotBlank() },
                                            src = null
                                        )
                                        formName = ""
                                        formAmt = ""
                                        formPhone = ""
                                        formNote = ""
                                        viewModel.showToast("Saved Received Entry")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Save Received Entry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(hisabTx.filter { it.type == "in" }.sortedByDescending { it.date }) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${dtFmt(t.date)} ${if (t.note.isNotBlank()) "• ${t.note}" else ""}", fontSize = 10.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Edit", fontSize = 10.sp, color = Color(0xFF818CF8), modifier = Modifier.clickable { onEditTx(t) })
                                Text("Delete", fontSize = 10.sp, color = Color(0xFFF87171), modifier = Modifier.clickable { viewModel.repository.deleteHisabTx(t.id) })
                            }
                        }
                        Text("+${moneyFmt(t.amt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                    }
                }
            }

            "dep" -> {
                item {
                    // Form Deposit
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Deposit Under Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                            OutlinedTextField(
                                value = formName,
                                onValueChange = { formName = it },
                                label = { Text("Account Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = formAmt,
                                    onValueChange = { formAmt = it },
                                    label = { Text("Amount (৳)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = formDate,
                                    onValueChange = { formDate = it },
                                    label = { Text("Date") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = formSrc,
                                onValueChange = { formSrc = it },
                                label = { Text("Source of Funds (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = formPhone,
                                onValueChange = { formPhone = it },
                                label = { Text("Phone Number (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = formNote,
                                onValueChange = { formNote = it },
                                label = { Text("Note (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val amt = formAmt.toDoubleOrNull() ?: 0.0
                                    if (formName.isNotBlank() && amt > 0) {
                                        viewModel.repository.addHisabTx(
                                            type = "dep",
                                            name = formName,
                                            amt = amt,
                                            date = formDate,
                                            phone = formPhone.takeIf { it.isNotBlank() },
                                            note = formNote.takeIf { it.isNotBlank() },
                                            src = formSrc.takeIf { it.isNotBlank() }
                                        )
                                        formName = ""
                                        formAmt = ""
                                        formSrc = ""
                                        formPhone = ""
                                        formNote = ""
                                        viewModel.showToast("Saved Deposit Entry")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Save Deposit Entry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(hisabTx.filter { it.type == "dep" }.sortedByDescending { it.date }) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${dtFmt(t.date)} ${if (t.src.isNotBlank()) "• Source: ${t.src}" else ""} ${if (t.note.isNotBlank()) "• ${t.note}" else ""}", fontSize = 10.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Edit", fontSize = 10.sp, color = Color(0xFF818CF8), modifier = Modifier.clickable { onEditTx(t) })
                                Text("Delete", fontSize = 10.sp, color = Color(0xFFF87171), modifier = Modifier.clickable { viewModel.repository.deleteHisabTx(t.id) })
                            }
                        }
                        Text("-${moneyFmt(t.amt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    }
                }
            }

            "notes" -> {
                item {
                    Text(
                        "💡 Smart Reconciliation Advice: Automatically calculates who has paid extra and who has a deficit, suggesting how to balance accounts:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
                items(adviceList) { adv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${adv.person} gave ${moneyFmt(adv.inn)}, but deposited ${moneyFmt(adv.dep)} → ${moneyFmt(adv.inn - adv.dep)} pending.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            for (sug in adv.suggestions) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0B1220), RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "👉 ${sug.sourcePerson} has unpaid dues. Collect from ${sug.sourcePerson} and deposit ${moneyFmt(sug.suggestAmt)} under ${sug.targetPerson}'s account.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.hisabSubTab.value = "dep"
                                            formName = sug.targetPerson
                                            formSrc = sug.sourcePerson
                                            formAmt = sug.suggestAmt.toInt().toString()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Text("Pre-fill Deposit Form", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "reminders" -> {
                if (autoDues.isNotEmpty()) {
                    item { Text("10+ Days Inactive Accounts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)) }
                    items(autoDues) { (p, days) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0x3378350F), RoundedCornerShape(10.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("$days days inactive (Bal: ${moneyFmt(p.bal)})", fontSize = 10.sp, color = Color(0xFFFBBF24))
                            }
                            Button(
                                onClick = { viewModel.repository.acknowledgePerson(p.name) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("Done ✓", fontSize = 10.sp) }
                        }
                    }
                }
                item { Text("Upcoming & Scheduled Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                items(hisabReminders) { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(10.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(r.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Due: ${r.due.replace("T", " ")}", fontSize = 10.sp, color = Color(0xFF818CF8))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!r.done) {
                                Button(
                                    onClick = { viewModel.repository.markHisabReminderDone(r.id) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) { Text("Done ✓", fontSize = 10.sp) }
                            }
                            IconButton(onClick = { viewModel.repository.deleteHisabReminder(r.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpScreen(
    viewModel: TagadaViewModel,
    contacts: List<Contact>,
    followUpGroupFilter: String,
    surveyLeadFilter: String,
    onToggleFavorite: (String) -> Unit,
    onCallClick: (Contact) -> Unit,
    onFinanceClick: (Contact) -> Unit,
    onChangeGroup: (String, String) -> Unit,
    onHistoryClick: (Contact) -> Unit,
    onStatusChange: (String, String) -> Unit,
    onReminderClick: (Contact) -> Unit,
    onLeadStatusChange: (String, String) -> Unit
) {
    val filtered = remember(contacts, followUpGroupFilter, surveyLeadFilter) {
        contacts
            .filter { it.contactGroup == followUpGroupFilter }
            .filter { if (followUpGroupFilter == "Survey" && surveyLeadFilter != "all") it.leadStatus == surveyLeadFilter else true }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Follow-up & Lead Pipeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CONTACT_GROUPS) { g ->
                    FilterChip(
                        selected = followUpGroupFilter == g,
                        onClick = {
                            viewModel.followUpGroupFilter.value = g
                            viewModel.surveyLeadFilter.value = "all"
                        },
                        label = { Text(g, fontSize = 11.sp) }
                    )
                }
            }
            if (followUpGroupFilter == "Survey") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All Leads", "green" to "🔥 Hot", "yellow" to "⚡ Warm", "red" to "❌ Disqualify").forEach { (valKey, lbl) ->
                        FilterChip(
                            selected = surveyLeadFilter == valKey,
                            onClick = { viewModel.surveyLeadFilter.value = valKey },
                            label = { Text(lbl, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filtered, key = { it.id }) { call ->
                ContactCard(
                    contact = call,
                    isCompact = false,
                    onToggleFavorite = { onToggleFavorite(call.id) },
                    onCallClick = { onCallClick(call) },
                    onFinanceClick = { onFinanceClick(call) },
                    onChangeGroup = { grp -> onChangeGroup(call.id, grp) },
                    onHistoryClick = { onHistoryClick(call) },
                    onStatusChange = { st -> onStatusChange(call.id, st) },
                    onReminderClick = { onReminderClick(call) },
                    onLeadStatusChange = { status -> onLeadStatusChange(call.id, status) }
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text("No contacts found in $followUpGroupFilter", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: TagadaViewModel,
    t: TagadaStrings,
    currentUser: FirebaseUser?,
    darkMode: Boolean,
    lang: String,
    settingsGroupTarget: String,
    onSettingsGroupTargetChange: (String) -> Unit,
    onGoogleClick: () -> Unit,
    onBackupToDrive: () -> Unit,
    onRestoreJson: () -> Unit,
    onUploadVcf: () -> Unit,
    onBatchImportContacts: () -> Unit
) {
    val context = LocalContext.current
    var hasNotifPermission by remember { mutableStateOf(NotificationScheduler.hasNotificationPermission(context)) }
    var hasContactsPermission by remember { mutableStateOf(DeviceDataManager.hasContactsPermission(context)) }
    var hasCallLogPermission by remember { mutableStateOf(DeviceDataManager.hasCallLogPermission(context)) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted
        if (isGranted) viewModel.showToast("Notification permission granted!")
    }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Contacts permission granted!")
            viewModel.loadDeviceData()
        }
    }

    val callLogPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Call log permission granted!")
            viewModel.loadDeviceData()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
    ) {
        // Tagada Official Brand & Cloud Security Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagadaBrandHeader(title = t.appName, subtitle = t.appSubtitle)
                    FirestoreSecurityBadge(isCloudSynced = currentUser != null)
                }
            }
        }

        item {
            Text(t.settings, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Google Account & Firestore Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF6366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUser?.displayName?.take(1)?.uppercase() ?: "G",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(currentUser?.displayName ?: t.googleLogin, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(currentUser?.email ?: "Sync backups & authenticate securely", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    if (currentUser != null) {
                        Button(
                            onClick = { viewModel.forceSecureFirestoreSync() },
                            modifier = Modifier.fillMaxWidth().testTag("force_firestore_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Force Secure Firestore Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onBackupToDrive,
                                modifier = Modifier.weight(1f).testTag("backup_to_drive_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.cloudSync, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onGoogleClick) {
                                Text(t.signout, fontSize = 11.sp, color = Color(0xFFF87171))
                            }
                        }
                    } else {
                        Button(
                            onClick = onGoogleClick,
                            modifier = Modifier.fillMaxWidth().testTag("settings_google_login_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A))
                        ) {
                            Text(t.googleLoginBtn, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Restore Data:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = onRestoreJson,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Select Backup JSON", fontSize = 10.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Device Permissions & Data Sync Card (Contacts & Call Logs)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                            Column {
                                Text("Device Permissions & Call Sync", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Required to load device contacts & call history", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Contacts Permission Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Contacts Permission", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (hasContactsPermission) "Granted • Can read device phonebook" else "Not Granted",
                                fontSize = 10.sp,
                                color = if (hasContactsPermission) Color(0xFF34D399) else Color(0xFFF87171)
                            )
                        }
                        if (!hasContactsPermission) {
                            Button(
                                onClick = { contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onBatchImportContacts,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                            ) {
                                Text("Import Contacts", fontSize = 10.sp, color = Color(0xFFC7D2FE))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Call History Permission Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Call History Permission", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (hasCallLogPermission) "Granted • Can read missed & received calls" else "Not Granted",
                                fontSize = 10.sp,
                                color = if (hasCallLogPermission) Color(0xFF34D399) else Color(0xFFF87171)
                            )
                        }
                        if (!hasCallLogPermission) {
                            Button(
                                onClick = { callLogPermLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.syncCallHistoryFromDevice()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Sync Call Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Auto-include toggle
                    val autoIncludeInDevice by viewModel.autoIncludeInDevice.collectAsState()
                    val autoIncludePhoneToApp by viewModel.autoIncludePhoneToApp.collectAsState()
                    val askConfirmationBeforeSave by viewModel.askConfirmationBeforeSave.collectAsState()
                    val deviceContacts by viewModel.deviceContacts.collectAsState()
                    val unimportedCount = remember(deviceContacts) { deviceContacts.count { !it.isAlreadyInCrm } }

                    val writeContactsLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) viewModel.setAutoIncludeInDevice(true)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-include in Phone Contacts", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Automatically save new CRM contacts into phonebook", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoIncludeInDevice,
                            onCheckedChange = { checked ->
                                if (checked && !DeviceDataManager.hasWriteContactsPermission(context)) {
                                    writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                                } else {
                                    viewModel.setAutoIncludeInDevice(checked)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-include Phone Contacts to CRM", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Prompt and auto-detect new phone contacts", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoIncludePhoneToApp,
                            onCheckedChange = { checked ->
                                viewModel.setAutoIncludePhoneToApp(checked)
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confirm Before Auto-Saving", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Display confirmation choice dialog when adding contacts", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = askConfirmationBeforeSave,
                            onCheckedChange = { checked ->
                                viewModel.setAskConfirmationBeforeSave(checked)
                            }
                        )
                    }

                    if (unimportedCount > 0) {
                        Button(
                            onClick = { viewModel.autoIncludeAllDeviceContacts("General") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Auto-Include $unimportedCount Phone Contacts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Notification Settings Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                            Column {
                                Text("Reminder Notifications", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    if (hasNotifPermission) "Active • Follow-up alerts enabled" else "Permission required to show alerts",
                                    fontSize = 10.sp,
                                    color = if (hasNotifPermission) Color(0xFF34D399) else Color(0xFFF87171)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.weight(1f).testTag("enable_notification_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Enable Notifications", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = {
                                NotificationScheduler.triggerImmediateNotification(
                                    context = context,
                                    name = "John Doe (VIP Lead)",
                                    phone = "01712345678",
                                    note = "Scheduled call regarding quarterly contract renewal"
                                )
                                viewModel.showToast("Test notification sent!")
                            },
                            modifier = Modifier.weight(1f).testTag("test_notification_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Send Test Alert", fontSize = 11.sp, color = Color(0xFFC7D2FE), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // VCF Import Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t.vcfUploadSettings, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(t.vcfUploadDesc, fontSize = 10.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        var expandedGroup by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { expandedGroup = true },
                                label = { Text(settingsGroupTarget, fontSize = 11.sp) }
                            )
                            DropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                                CONTACT_GROUPS.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            onSettingsGroupTargetChange(g)
                                            expandedGroup = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = onUploadVcf,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("Upload VCF File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Dark mode & Language Toggles
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t.darkMode, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { viewModel.darkMode.value = it }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t.lang, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        var expandedLang by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { expandedLang = true },
                                label = { Text(if (lang == "bn") "বাংলা (Bengali)" else "English (Default)", fontSize = 11.sp) }
                            )
                            DropdownMenu(expanded = expandedLang, onDismissRequest = { expandedLang = false }) {
                                DropdownMenuItem(text = { Text("English (Default)") }, onClick = { viewModel.lang.value = "en"; expandedLang = false })
                                DropdownMenuItem(text = { Text("বাংলা (Bengali)") }, onClick = { viewModel.lang.value = "bn"; expandedLang = false })
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Tagada v2.2 • Smart Call & Hisab Khata CRM with Device Sync",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
