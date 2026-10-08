package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import com.example.ui.dtFmt
import com.example.ui.moneyFmt
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HisabPersonLedgerDialog(
    personName: String,
    person: HisabPerson?,
    txList: List<HisabTx>,
    onDismiss: () -> Unit,
    onAddPhone: (phone: String) -> Unit,
    onSetAlertDays: (days: Int) -> Unit,
    onEditTx: (HisabTx) -> Unit
) {
    val context = LocalContext.current
    var showAddPhoneDialog by remember { mutableStateOf(false) }
    var newPhoneInput by remember { mutableStateOf("") }

    val ins = txList.filter { it.type == "in" && it.name == personName }
    val deps = txList.filter { it.type == "dep" && it.name == personName }
    val outs = txList.filter { it.type == "dep" && it.src == personName && it.name != personName }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1220),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).testTag("person_ledger_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(personName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Personal Account & Ledger Statement", fontSize = 10.sp, color = Color(0xFF34D399))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                // Phone Row
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(12.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Phone:", fontSize = 11.sp, color = Color.Gray)
                        person?.phones?.forEach { num ->
                            AssistChip(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                    context.startActivity(intent)
                                },
                                label = { Text("📞 $num", fontSize = 10.sp, color = Color(0xFF34D399)) }
                            )
                        }
                        if (person?.phones.isNullOrEmpty()) {
                            Text("No phone added", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Button(
                        onClick = { showAddPhoneDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("+ Phone", fontSize = 10.sp, color = Color.White)
                    }
                }

                // Mini stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gave", fontSize = 9.sp, color = Color.Gray)
                            Text(moneyFmt(person?.inn ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Deposited", fontSize = 9.sp, color = Color.Gray)
                            Text(moneyFmt(person?.dep ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Balance", fontSize = 9.sp, color = Color.Gray)
                            val bal = person?.bal ?: 0.0
                            Text(
                                if (bal > 0) "Payable ${moneyFmt(bal)}" else if (bal < 0) "Receivable ${moneyFmt(-bal)}" else "0",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bal > 0) Color(0xFFF87171) else if (bal < 0) Color(0xFF34D399) else Color.Gray
                            )
                        }
                    }
                }

                // Quick Alert Row
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(12.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set Follow-up Alert:", fontSize = 10.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 7).forEach { days ->
                            Button(
                                onClick = { onSetAlertDays(days) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                            ) {
                                Text("${days}d Later", fontSize = 10.sp, color = Color(0xFFC7D2FE))
                            }
                        }
                    }
                }

                // Transactions
                Text("Transaction History (${ins.size + deps.size + outs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ins) { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(10.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Received In", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                    Text(dtFmt(t.date), fontSize = 10.sp, color = Color.Gray)
                                }
                                if (t.note.isNotBlank()) Text(t.note, fontSize = 9.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("+${moneyFmt(t.amt)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                IconButton(onClick = { onEditTx(t) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    items(deps) { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(10.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Deposited", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                                    Text(dtFmt(t.date), fontSize = 10.sp, color = Color.Gray)
                                }
                                if (t.src.isNotBlank()) Text("Source: ${t.src}", fontSize = 9.sp, color = Color.Gray)
                                if (t.note.isNotBlank()) Text(t.note, fontSize = 9.sp, color = Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("-${moneyFmt(t.amt)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                                IconButton(onClick = { onEditTx(t) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    items(outs) { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF111827), RoundedCornerShape(10.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Deposited to Other", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text("${dtFmt(t.date)} (Under: ${t.name})", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text("-${moneyFmt(t.amt)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }
    }

    if (showAddPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showAddPhoneDialog = false },
            title = { Text("Add Phone Number") },
            text = {
                OutlinedTextField(
                    value = newPhoneInput,
                    onValueChange = { newPhoneInput = it },
                    placeholder = { Text("017xxxxxxxx") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPhoneInput.isNotBlank()) {
                        onAddPhone(newPhoneInput.trim())
                        newPhoneInput = ""
                        showAddPhoneDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhoneDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EditHisabTxDialog(
    tx: HisabTx,
    onDismiss: () -> Unit,
    onSave: (HisabTx) -> Unit
) {
    var name by remember { mutableStateOf(tx.name) }
    var amtStr by remember { mutableStateOf(tx.amt.toString()) }
    var date by remember { mutableStateOf(tx.date) }
    var note by remember { mutableStateOf(tx.note) }
    var src by remember { mutableStateOf(tx.src) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().testTag("edit_hisab_tx_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit ${if (tx.type == "in") "Received" else "Deposit"} Entry", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amtStr,
                        onValueChange = { amtStr = it },
                        label = { Text("Amount (৳)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (tx.type == "dep") {
                    OutlinedTextField(
                        value = src,
                        onValueChange = { src = it },
                        label = { Text("Source of Funds") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val parsedAmt = amtStr.toDoubleOrNull() ?: tx.amt
                            onSave(tx.copy(name = name, amt = parsedAmt, date = date, note = note, src = src))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Update")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleLoginDialog(
    user: FirebaseUser?,
    onDismiss: () -> Unit,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().testTag("google_login_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagadaLogoBadge(size = 32.dp)
                        Text("Cloud Firestore Account", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                FirestoreSecurityBadge(isCloudSynced = user != null)

                if (user != null) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF6366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.displayName?.take(1)?.uppercase() ?: "U",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(user.displayName ?: "Google User", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(user.email ?: "", fontSize = 12.sp, color = Color.Gray)
                    Text("✓ Secured End-to-End Firestore Sync Active", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)

                    Button(
                        onClick = onSignOutClicked,
                        modifier = Modifier.fillMaxWidth().testTag("google_sign_out_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Sign Out Account", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "Sign in with Google to securely store your Tagada CRM contacts, reminders, and Hisab Khata transactions in Firebase Firestore with strict user data isolation and cloud backup.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = onSignInClicked,
                        modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A))
                    ) {
                        Text("Sign in with Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryAnalysisDialog(
    totalContacts: Int,
    dialed: Int,
    received: Int,
    missedAndRejected: Int,
    top10List: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
    onRunAiAudit: (() -> Unit)? = null,
    aiResultText: String? = null,
    isAiThinking: Boolean = false
) {
    var selectedSubTab by remember { mutableStateOf("summary") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF080C16),
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { selectedSubTab = "summary" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSubTab == "summary") Color(0xFF6366F1) else Color(0xFF1E293B)
                        )
                    ) {
                        Text("Summary", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedSubTab = "analysis" },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSubTab == "analysis") Color(0xFF6366F1) else Color(0xFF1E293B)
                        )
                    ) {
                        Text("Top 10", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedSubTab = "ai" },
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSubTab == "ai") Color(0xFF7C3AED) else Color(0xFF1E293B)
                        )
                    ) {
                        Text("AI Audit ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                when (selectedSubTab) {
                    "summary" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11192D))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Contacts", fontSize = 10.sp, color = Color.Gray)
                                        Text("$totalContacts", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11192D))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Dialed Calls", fontSize = 10.sp, color = Color(0xFF34D399))
                                        Text("$dialed", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF34D399))
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11192D))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Received Calls", fontSize = 10.sp, color = Color(0xFF38BDF8))
                                        Text("$received", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11192D))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Missed / No Answer", fontSize = 10.sp, color = Color(0xFFF87171))
                                        Text("$missedAndRejected", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF87171))
                                    }
                                }
                            }
                        }
                    }
                    "analysis" -> {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(top10List) { (name, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1424), RoundedCornerShape(10.dp)).padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("$count calls", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                                }
                            }
                            if (top10List.isEmpty()) {
                                item {
                                    Text("No call history recorded yet", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(20.dp))
                                }
                            }
                        }
                    }
                    "ai" -> {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("High Thinking AI Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                                if (onRunAiAudit != null) {
                                    Button(
                                        onClick = onRunAiAudit,
                                        enabled = !isAiThinking,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                    ) {
                                        Text(if (isAiThinking) "Thinking..." else "Run Audit", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            if (isAiThinking) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFA78BFA))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Gemini 3.1 Pro Thinking Mode High...", fontSize = 11.sp, color = Color.LightGray)
                                }
                            } else if (!aiResultText.isNullOrBlank()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    item {
                                        Text(aiResultText, fontSize = 11.sp, color = Color(0xFFE2E8F0), lineHeight = 16.sp)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Tap 'Run Audit' to get deep AI reasoning on collection priorities, high-risk overdue arrears, and optimal follow-up schedule.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
