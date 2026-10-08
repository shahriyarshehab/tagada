package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AuthManager
import com.example.data.model.*
import com.example.ui.components.*
import com.example.util.NotificationScheduler
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TagadaApp(
    viewModel: TagadaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // States from ViewModel
    val activeTab by viewModel.activeTab.collectAsState()
    val lang by viewModel.lang.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val globalSearch by viewModel.globalSearch.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val dashGroupFilter by viewModel.dashGroupFilter.collectAsState()
    val dashTimeFilter by viewModel.dashTimeFilter.collectAsState()
    val followUpGroupFilter by viewModel.followUpGroupFilter.collectAsState()
    val surveyLeadFilter by viewModel.surveyLeadFilter.collectAsState()
    val hisabSubTab by viewModel.hisabSubTab.collectAsState()
    val hisabSearch by viewModel.hisabSearch.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val contacts by viewModel.contacts.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val payHistory by viewModel.payHistory.collectAsState()
    val hisabTx by viewModel.hisabTx.collectAsState()
    val hisabReminders by viewModel.hisabReminders.collectAsState()
    val hisabPhones by viewModel.hisabPhones.collectAsState()
    val hisabAcks by viewModel.hisabAcks.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val aiInsightText by viewModel.aiInsightText.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    val t = getStrings(lang)

    // Modals
    var showFabMenu by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var addContactInitialPhone by remember { mutableStateOf("") }
    var addContactInitialName by remember { mutableStateOf("") }

    var financeModalContact by remember { mutableStateOf<Contact?>(null) }
    var surveyNoteModalContact by remember { mutableStateOf<Contact?>(null) }
    var reminderModalContact by remember { mutableStateOf<Contact?>(null) }
    var showReminderModal by remember { mutableStateOf(false) }
    var historyModalContact by remember { mutableStateOf<Contact?>(null) }
    var selectedPersonName by remember { mutableStateOf<String?>(null) }
    var editingHisabTx by remember { mutableStateOf<HisabTx?>(null) }
    var showGoogleModal by remember { mutableStateOf(false) }
    var showSummaryAnalysisModal by remember { mutableStateOf(false) }
    var showImportDeviceContactsModal by remember { mutableStateOf(false) }

    var drillDownListTitle by remember { mutableStateOf<String?>(null) }
    var drillDownListData by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var quickNoteInput by remember { mutableStateOf("") }
    var settingsGroupTarget by remember { mutableStateOf("General") }

    // Google Drive / JSON Restore Launcher
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val success = viewModel.repository.restoreFromBackupJson(jsonString)
                if (success) {
                    viewModel.showToast("Data Restored Successfully!")
                } else {
                    viewModel.showToast("Invalid backup file!")
                }
            } catch (e: Exception) {
                viewModel.showToast("Restore failed: ${e.message}")
            }
        }
    }

    // VCF Import Launcher
    val vcfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val count = viewModel.repository.importVcfText(text, settingsGroupTarget)
                viewModel.showToast("$count Contacts Imported to $settingsGroupTarget!")
            } catch (e: Exception) {
                viewModel.showToast("Import error: ${e.message}")
            }
        }
    }

    // Helper: Share / Backup JSON to Google Drive
    fun backupToGoogleDrive() {
        try {
            val json = viewModel.repository.createFullBackupJson()
            val fileName = "Tagada_Backup_${currentUser?.email?.substringBefore('@') ?: "local"}_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(json)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Tagada Backup ($fileName)")
                putExtra(Intent.EXTRA_TEXT, "Here is your Tagada CRM & Hisab Khata backup file. Save it directly to your Google Drive.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Backup to Google Drive or Share"))
            viewModel.showToast("Complete Tagada & Hisab Backup Ready!")
        } catch (e: Exception) {
            viewModel.showToast("Backup created in local storage")
        }
    }

    // Auto-switch to calls tab on global search
    LaunchedEffect(globalSearch) {
        if (globalSearch.isNotBlank() && activeTab != "calls") {
            viewModel.activeTab.value = "calls"
        }
    }

    // Computed Hisab items
    val hisabPeople = remember(hisabTx, hisabPhones) {
        viewModel.computeHisabPeople(hisabTx, hisabPhones)
    }
    val hisabTotals = remember(hisabPeople) {
        viewModel.computeHisabTotals(hisabPeople)
    }
    val hisabAdviceList = remember(hisabPeople) {
        viewModel.computeHisabAdvice(hisabPeople)
    }

    // Dashboard filtered stats
    val filteredContacts = remember(contacts, dashTimeFilter, dashGroupFilter) {
        contacts.filter { c ->
            val matchesTime = viewModel.isWithinTime(c.date, dashTimeFilter)
            val matchesGroup = if (dashGroupFilter == "all") true else (c.contactGroup == dashGroupFilter)
            matchesTime && matchesGroup
        }
    }

    val connectedList = remember(filteredContacts) {
        filteredContacts.filter { it.type == "dialed" || it.type == "received" || it.callHistory.isNotEmpty() }
    }
    val unpaidList = remember(filteredContacts) {
        filteredContacts.filter { it.status == "unpaid" && (it.contactGroup == "Active" || it.contactGroup == "Active Member") }
    }
    val paidList = remember(filteredContacts) {
        filteredContacts.filter { it.status == "paid" && (it.contactGroup == "Active" || it.contactGroup == "Active Member") }
    }
    val favoritesList = remember(contacts) {
        contacts.filter { it.isFavorite }
    }

    // Analysis data
    val analysisData = remember(contacts) {
        var dialed = 0
        var received = 0
        var missed = 0
        var noAnswer = 0
        val map = mutableMapOf<String, Pair<String, Int>>()
        contacts.forEach { c ->
            when (c.type) {
                "dialed" -> dialed++
                "received" -> received++
                "missed" -> missed++
                "no_answer" -> noAnswer++
            }
            val count = c.callHistory.size
            if (count > 0) {
                map[c.phone] = Pair(c.name, count)
            }
        }
        val top10 = map.values.sortedByDescending { it.second }.take(10).map { it.first to it.second }
        object {
            val total = contacts.size
            val d = dialed
            val r = received
            val m = missed + noAnswer
            val top = top10
        }
    }

    val bg = if (darkMode) Color(0xFF040711) else Color(0xFFF8FAFC)

    Box(modifier = modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. TOP BRAND HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0D1424), Color(0xFF101B33), Color(0xFF0D1424))
                        )
                    )
                    .border(width = 0.5.dp, color = Color(0xFF1F293D))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagadaBrandHeader(title = t.appName, subtitle = t.appSubtitle)

                    // Google Login / User Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6366F1)
                            )
                        }
                        if (currentUser != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF172238))
                                    .border(1.dp, Color(0xFF263757), RoundedCornerShape(20.dp))
                                    .clickable { showGoogleModal = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF6366F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        currentUser?.displayName?.take(1)?.uppercase() ?: "U",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    currentUser?.displayName?.substringBefore(' ') ?: "User",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                            }
                        } else {
                            Button(
                                onClick = { showGoogleModal = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))
                            ) {
                                Text("Login", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FirestoreSecurityBadge(isCloudSynced = currentUser != null)
                    Text(
                        if (currentUser != null) "Cloud Firestore Active" else "Offline / Local Mode",
                        fontSize = 9.5.sp,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Global Search Bar
                OutlinedTextField(
                    value = globalSearch,
                    onValueChange = { viewModel.globalSearch.value = it },
                    placeholder = { Text(t.search, fontSize = 12.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (globalSearch.isNotBlank()) {
                            IconButton(onClick = { viewModel.globalSearch.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("global_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0A0F1D),
                        unfocusedContainerColor = Color(0xFF0A0F1D),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // --- 2. TAB CONTENT ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    "dashboard" -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            t = t,
                            dashGroupFilter = dashGroupFilter,
                            dashTimeFilter = dashTimeFilter,
                            connectedList = connectedList,
                            unpaidList = unpaidList,
                            paidList = paidList,
                            favoritesList = favoritesList,
                            hisabTotals = hisabTotals,
                            contacts = contacts,
                            reminders = reminders,
                            notes = notes,
                            quickNoteInput = quickNoteInput,
                            onQuickNoteChange = { quickNoteInput = it },
                            onSaveQuickNote = {
                                if (quickNoteInput.isNotBlank()) {
                                    viewModel.repository.addNote(quickNoteInput)
                                    quickNoteInput = ""
                                    viewModel.showToast("Note Saved!")
                                }
                            },
                            onDrillDown = { title, list ->
                                drillDownListTitle = title
                                drillDownListData = list
                            },
                            onOpenSummary = { showSummaryAnalysisModal = true },
                            onAddReminderClick = {
                                reminderModalContact = null
                                showReminderModal = true
                            }
                        )
                    }
                    "calls" -> {
                        CallsScreen(
                            viewModel = viewModel,
                            contacts = contacts,
                            globalSearch = globalSearch,
                            sortType = sortType,
                            onToggleFavorite = { viewModel.repository.toggleFavorite(it) },
                            onCallClick = { contact ->
                                viewModel.repository.recordCall(contact.id, "dialed")
                                if (contact.contactGroup == "Survey" || contact.contactGroup == "Survey Target") {
                                    surveyNoteModalContact = contact
                                }
                            },
                            onFinanceClick = { financeModalContact = it },
                            onChangeGroup = { id, grp -> viewModel.repository.updateContactGroup(id, grp) },
                            onHistoryClick = { historyModalContact = it },
                            onStatusChange = { id, st -> viewModel.repository.updateContactStatus(id, st) },
                            onReminderClick = {
                                reminderModalContact = it
                                showReminderModal = true
                            },
                            onAddContactDirectly = { name, phone ->
                                addContactInitialName = name
                                addContactInitialPhone = phone
                                showAddContactDialog = true
                            },
                            onOpenImportDeviceContacts = {
                                showImportDeviceContactsModal = true
                            }
                        )
                    }
                    "hisab" -> {
                        HisabScreen(
                            viewModel = viewModel,
                            hisabSubTab = hisabSubTab,
                            hisabSearch = hisabSearch,
                            hisabTotals = hisabTotals,
                            hisabPeople = hisabPeople,
                            hisabTx = hisabTx,
                            hisabReminders = hisabReminders,
                            hisabAcks = hisabAcks,
                            adviceList = hisabAdviceList,
                            onSelectPerson = { selectedPersonName = it },
                            onEditTx = { editingHisabTx = it }
                        )
                    }
                    "followup" -> {
                        FollowUpScreen(
                            viewModel = viewModel,
                            contacts = contacts,
                            followUpGroupFilter = followUpGroupFilter,
                            surveyLeadFilter = surveyLeadFilter,
                            onToggleFavorite = { viewModel.repository.toggleFavorite(it) },
                            onCallClick = { contact ->
                                viewModel.repository.recordCall(contact.id, "dialed")
                                if (contact.contactGroup == "Survey" || contact.contactGroup == "Survey Target") {
                                    surveyNoteModalContact = contact
                                }
                            },
                            onFinanceClick = { financeModalContact = it },
                            onChangeGroup = { id, grp -> viewModel.repository.updateContactGroup(id, grp) },
                            onHistoryClick = { historyModalContact = it },
                            onStatusChange = { id, st -> viewModel.repository.updateContactStatus(id, st) },
                            onReminderClick = {
                                reminderModalContact = it
                                showReminderModal = true
                            },
                            onLeadStatusChange = { id, status -> viewModel.repository.updateContactLeadStatus(id, status) }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            t = t,
                            currentUser = currentUser,
                            darkMode = darkMode,
                            lang = lang,
                            settingsGroupTarget = settingsGroupTarget,
                            onSettingsGroupTargetChange = { settingsGroupTarget = it },
                            onGoogleClick = { showGoogleModal = true },
                            onBackupToDrive = { backupToGoogleDrive() },
                            onRestoreJson = { jsonPickerLauncher.launch("application/json") },
                            onUploadVcf = { vcfPickerLauncher.launch("*/*") },
                            onBatchImportContacts = { showImportDeviceContactsModal = true }
                        )
                    }
                }
            }

            // --- 3. BOTTOM NAVIGATION DOCK (5 TABS) ---
            NavigationBar(
                containerColor = Color(0xFF080C16),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(60.dp).border(width = 0.5.dp, color = Color(0xFF1A253A))
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { viewModel.activeTab.value = "dashboard" },
                    icon = { Icon(Icons.Default.Home, contentDescription = t.dash, modifier = Modifier.size(20.dp)) },
                    label = { Text(t.dash, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF818CF8),
                        selectedTextColor = Color(0xFF818CF8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "calls",
                    onClick = { viewModel.activeTab.value = "calls" },
                    icon = { Icon(Icons.Default.PhoneCallback, contentDescription = t.list, modifier = Modifier.size(20.dp)) },
                    label = { Text("Calls", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF818CF8),
                        selectedTextColor = Color(0xFF818CF8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "hisab",
                    onClick = { viewModel.activeTab.value = "hisab" },
                    icon = { Icon(Icons.Default.Book, contentDescription = t.hisab, modifier = Modifier.size(20.dp)) },
                    label = { Text(t.hisab, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF34D399),
                        selectedTextColor = Color(0xFF34D399),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "followup",
                    onClick = { viewModel.activeTab.value = "followup" },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = t.followup, modifier = Modifier.size(20.dp)) },
                    label = { Text(t.followup, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF818CF8),
                        selectedTextColor = Color(0xFF818CF8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { viewModel.activeTab.value = "settings" },
                    icon = { Icon(Icons.Default.Person, contentDescription = t.settings, modifier = Modifier.size(20.dp)) },
                    label = { Text(t.settings, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF818CF8),
                        selectedTextColor = Color(0xFF818CF8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }

        // --- FLOATING ACTION BUTTON (FAB) ---
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 72.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedVisibility(visible = showFabMenu, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showFabMenu = false
                                addContactInitialName = ""
                                addContactInitialPhone = ""
                                showAddContactDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Contact", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                showImportDeviceContactsModal = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
                        ) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Phonebook", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                viewModel.activeTab.value = "hisab"
                                viewModel.hisabSubTab.value = "in"
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Ledger Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                reminderModalContact = null
                                showReminderModal = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = if (showFabMenu) Color(0xFFE11D48) else Color(0xFF6366F1),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).testTag("fab_menu_button")
                ) {
                    Icon(
                        if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- TOAST BANNER ---
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                    Text(toastMessage ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            LaunchedEffect(toastMessage) {
                kotlinx.coroutines.delay(2600)
                viewModel.clearToast()
            }
        }
    }

    val autoIncludeInDevice by viewModel.autoIncludeInDevice.collectAsState()
    val unimportedCount = remember(deviceContacts) { deviceContacts.count { !it.isAlreadyInCrm } }
    var showAutoIncludePrompt by remember { mutableStateOf(false) }
    var pendingConfirmSaveContact by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    // --- DIALOGS ---
    if (pendingConfirmSaveContact != null) {
        val (cName, cPhone, cGroup) = pendingConfirmSaveContact!!
        ConfirmContactSaveDialog(
            name = cName,
            phone = cPhone,
            group = cGroup,
            onDismiss = { pendingConfirmSaveContact = null },
            onConfirm = { alsoSave ->
                viewModel.saveContact(cName, cPhone, cGroup, alsoSave)
                pendingConfirmSaveContact = null
            }
        )
    }

    if (showAutoIncludePrompt && unimportedCount > 0) {
        AutoIncludeDeviceContactsPromptDialog(
            newContactsCount = unimportedCount,
            onDismiss = { showAutoIncludePrompt = false },
            onConfirmAutoIncludeAll = {
                viewModel.autoIncludeAllDeviceContacts("General")
                showAutoIncludePrompt = false
            },
            onReviewClick = {
                showAutoIncludePrompt = false
                showImportDeviceContactsModal = true
            }
        )
    }
    if (showAddContactDialog) {
        AddContactDialog(
            initialName = addContactInitialName,
            initialPhone = addContactInitialPhone,
            defaultAlsoSaveToDevice = autoIncludeInDevice,
            onDismiss = {
                showAddContactDialog = false
                addContactInitialName = ""
                addContactInitialPhone = ""
            },
            onConfirm = { name, phone, grp, alsoSaveToDevice ->
                viewModel.saveContact(name, phone, grp, alsoSaveToDevice)
                showAddContactDialog = false
                addContactInitialName = ""
                addContactInitialPhone = ""
            }
        )
    }

    if (showImportDeviceContactsModal) {
        DeviceContactsImportDialog(
            deviceContacts = deviceContacts,
            onDismiss = { showImportDeviceContactsModal = false },
            onImport = { selected, targetGroup ->
                viewModel.importSelectedDeviceContacts(selected, targetGroup)
            }
        )
    }

    financeModalContact?.let { contact ->
        FinanceDialog(
            call = contact,
            groupType = contact.contactGroup,
            onDismiss = { financeModalContact = null },
            onSave = { map ->
                viewModel.repository.updateContactFinance(contact.id, map)
                financeModalContact = null
                viewModel.showToast("Finance Details Saved")
            }
        )
    }

    surveyNoteModalContact?.let { contact ->
        SurveyNoteDialog(
            contact = contact,
            onDismiss = { surveyNoteModalContact = null },
            onSave = { note, newGroup ->
                if (note.isNotBlank()) {
                    viewModel.repository.addNote("[${contact.name}] - $note")
                }
                if (newGroup != contact.contactGroup) {
                    viewModel.repository.updateContactGroup(contact.id, newGroup)
                }
                surveyNoteModalContact = null
                viewModel.showToast("Survey Follow-up Logged")
            }
        )
    }

    if (showReminderModal) {
        ReminderDialog(
            contacts = contacts,
            preselectedContact = reminderModalContact,
            onDismiss = { showReminderModal = false },
            onSave = { callId, name, phone, grp, dt, note ->
                viewModel.repository.addReminder(callId, name, phone, grp, dt, note)
                val targetMillis = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(dt)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                NotificationScheduler.scheduleReminder(
                    context = context,
                    reminderId = UUID.randomUUID().toString(),
                    name = name,
                    phone = phone,
                    note = note,
                    timeMillis = targetMillis
                )
                showReminderModal = false
                viewModel.showToast("Reminder Saved & Notification Scheduled")
            }
        )
    }

    historyModalContact?.let { contact ->
        MonthlyPaymentHistoryDialog(
            contact = contact,
            historyList = payHistory,
            onDismiss = { historyModalContact = null }
        )
    }

    selectedPersonName?.let { personName ->
        val person = hisabPeople.find { it.name == personName }
        HisabPersonLedgerDialog(
            personName = personName,
            person = person,
            txList = hisabTx,
            onDismiss = { selectedPersonName = null },
            onAddPhone = { p -> viewModel.repository.addPhoneForPerson(personName, p) },
            onSetAlertDays = { days ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                val dueIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(cal.time)
                viewModel.repository.addHisabReminder(personName, dueIso)
                NotificationScheduler.scheduleReminder(
                    context = context,
                    reminderId = UUID.randomUUID().toString(),
                    name = personName,
                    phone = person?.phones?.firstOrNull() ?: "",
                    note = "Hisab follow-up alert ($days days)",
                    timeMillis = cal.timeInMillis
                )
                viewModel.showToast("Alert set for $days days later with notification!")
            },
            onEditTx = { tx -> editingHisabTx = tx }
        )
    }

    editingHisabTx?.let { tx ->
        EditHisabTxDialog(
            tx = tx,
            onDismiss = { editingHisabTx = null },
            onSave = { updated ->
                viewModel.repository.updateHisabTx(updated)
                editingHisabTx = null
                viewModel.showToast("Transaction updated")
            }
        )
    }

    if (showGoogleModal) {
        GoogleLoginDialog(
            user = currentUser,
            onDismiss = { showGoogleModal = false },
            onSignInClicked = {
                (context as? Activity)?.let { act ->
                    AuthManager.signInWithGoogle(
                        activity = act,
                        credentialManager = credentialManager,
                        onAuthSuccess = { u ->
                            viewModel.setUser(u)
                            showGoogleModal = false
                            viewModel.showToast("Signed in as ${u.displayName}")
                        },
                        onAuthError = { err -> viewModel.showToast("Auth error: $err") },
                        onCancelled = { viewModel.showToast("Sign in cancelled") },
                        scope = coroutineScope
                    )
                }
            },
            onSignOutClicked = {
                AuthManager.signOut(
                    credentialManager = credentialManager,
                    onComplete = {
                        viewModel.setUser(null)
                        showGoogleModal = false
                        viewModel.showToast("Signed out of Google account")
                    },
                    scope = coroutineScope
                )
            }
        )
    }

    if (showSummaryAnalysisModal) {
        SummaryAnalysisDialog(
            totalContacts = analysisData.total,
            dialed = analysisData.d,
            received = analysisData.r,
            missedAndRejected = analysisData.m,
            top10List = analysisData.top,
            onDismiss = { showSummaryAnalysisModal = false },
            onRunAiAudit = {
                val summary = "Total Contacts: ${analysisData.total}, Dialed: ${analysisData.d}, Received: ${analysisData.r}, Missed: ${analysisData.m}. Unpaid Accounts: ${unpaidList.size}, Total Receivable: ${moneyFmt(hisabTotals["sGet"] ?: 0.0)}, Total Payable: ${moneyFmt(hisabTotals["sGive"] ?: 0.0)}."
                viewModel.generateHighThinkingAudit(summary)
            },
            aiResultText = aiInsightText,
            isAiThinking = isAiThinking
        )
    }

    if (drillDownListTitle != null) {
        Dialog(onDismissRequest = { drillDownListTitle = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0D1526),
                modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(drillDownListTitle ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Box(
                                modifier = Modifier.background(Color(0xFF6366F1), RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${drillDownListData.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        IconButton(onClick = { drillDownListTitle = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(drillDownListData) { call ->
                            ContactCard(
                                contact = call,
                                isCompact = true,
                                onToggleFavorite = { viewModel.repository.toggleFavorite(call.id) },
                                onCallClick = { viewModel.repository.recordCall(call.id, "dialed") },
                                onFinanceClick = { financeModalContact = call },
                                onChangeGroup = { grp -> viewModel.repository.updateContactGroup(call.id, grp) },
                                onHistoryClick = { historyModalContact = call },
                                onStatusChange = { st -> viewModel.repository.updateContactStatus(call.id, st) },
                                onReminderClick = {
                                    reminderModalContact = call
                                    showReminderModal = true
                                }
                            )
                        }
                        if (drillDownListData.isEmpty()) {
                            item {
                                Text("No records found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
