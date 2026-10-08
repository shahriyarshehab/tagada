package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.Contact
import com.example.ui.CONTACT_GROUPS

@Composable
fun ContactCard(
    contact: Contact,
    isCompact: Boolean = false,
    onToggleFavorite: () -> Unit,
    onCallClick: () -> Unit,
    onFinanceClick: () -> Unit,
    onChangeGroup: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onReminderClick: () -> Unit,
    onLeadStatusChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val grp = contact.contactGroup

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top row: Star, Name, Phone, Call badge, and Call action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp).testTag("star_button_${contact.id}")
                    ) {
                        Icon(
                            if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (contact.isFavorite) Color(0xFFFBBF24) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            contact.name,
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact.phone, fontSize = 11.sp, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E1B4B), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("Calls: ${contact.monthCallCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                            }
                        }
                    }
                }

                // Lead qualification dots for Survey
                if (onLeadStatusChange != null && (grp == "Survey" || grp == "Survey Target")) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF0A0F1D), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (contact.leadStatus == "green") Color(0xFF10B981) else Color.Transparent)
                                .border(2.dp, Color(0xFF10B981), CircleShape)
                                .clickable { onLeadStatusChange("green") }
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (contact.leadStatus == "yellow") Color(0xFFF59E0B) else Color.Transparent)
                                .border(2.dp, Color(0xFFF59E0B), CircleShape)
                                .clickable { onLeadStatusChange("yellow") }
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (contact.leadStatus == "red") Color(0xFFEF4444) else Color.Transparent)
                                .border(2.dp, Color(0xFFEF4444), CircleShape)
                                .clickable { onLeadStatusChange("red") }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Phone Call button
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                        context.startActivity(intent)
                        onCallClick()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .testTag("call_button_${contact.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Arrears Finance Banner
            if (grp == "Arrears" || grp == "Arrears Member") {
                val fin = contact.fin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x334C0519), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x66881337), RoundedCornerShape(8.dp))
                        .clickable { onFinanceClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (fin?.get("bokeyaGiven") != null) "Given: ৳ ${fin["bokeyaGiven"]} (${fin["bokeyaGivenDate"] ?: ""})" else "+ Finance",
                        fontSize = 10.sp,
                        color = Color(0xFFFDA4AF)
                    )
                    if (fin?.get("bokeyaDue") != null) {
                        Text("Due: ৳ ${fin["bokeyaDue"]}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFECDD3))
                    }
                }
            }

            // Active Loan Finance Banner
            if (grp == "Active" || grp == "Active Member") {
                val fin = contact.fin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x331E1B4B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x66312E81), RoundedCornerShape(8.dp))
                        .clickable { onFinanceClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (fin?.get("loanAmount") != null) "Loan: ৳ ${fin["loanAmount"]} (${fin["loanDate"] ?: ""})" else "+ Finance",
                        fontSize = 10.sp,
                        color = Color(0xFFA5B4FC)
                    )
                    if (fin?.get("loanDuration") != null) {
                        Text("${fin["loanDuration"]} Months", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                    }
                }
            }

            // Bottom row: Group dropdown, History button, Paid/Unpaid for Active, Reminder
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expandedGroupMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expandedGroupMenu = true },
                        label = { Text(grp, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    DropdownMenu(
                        expanded = expandedGroupMenu,
                        onDismissRequest = { expandedGroupMenu = false }
                    ) {
                        CONTACT_GROUPS.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g, fontSize = 12.sp) },
                                onClick = {
                                    onChangeGroup(g)
                                    expandedGroupMenu = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = onHistoryClick,
                        label = { Text("Hist", fontSize = 10.sp, color = Color(0xFF38BDF8)) }
                    )
                    if (grp == "Active" || grp == "Active Member") {
                        Button(
                            onClick = { onStatusChange("paid") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (contact.status == "paid") Color(0xFF059669) else Color(0xFF1E293B)
                            )
                        ) {
                            Text("Paid", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onStatusChange("unpaid") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (contact.status == "unpaid") Color(0xFFE11D48) else Color(0xFF1E293B)
                            )
                        ) {
                            Text("Unpaid", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    AssistChip(
                        onClick = onReminderClick,
                        label = { Text("Rem", fontSize = 10.sp, color = Color(0xFFA5B4FC)) }
                    )
                }
            }
        }
    }
}
