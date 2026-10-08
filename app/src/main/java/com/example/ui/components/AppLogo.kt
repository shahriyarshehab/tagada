package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Reusable official brand logo for Tagada CRM & Hisab Khata.
 */
@Composable
fun TagadaLogoBadge(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF06B6D4))
                ),
                RoundedCornerShape(12.dp)
            )
            .testTag("tagada_app_logo_badge"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.tagada_app_logo_1791396714869),
            contentDescription = "Tagada Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TagadaBrandHeader(
    modifier: Modifier = Modifier,
    title: String = "Tagada",
    subtitle: String = "Smart Call & Hisab Khata CRM"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TagadaLogoBadge(size = 36.dp)
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x336366F1), RoundedCornerShape(8.dp))
                        .border(0.8.dp, Color(0x666366F1), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("PRO CRM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                }
            }
            Text(subtitle, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun FirestoreSecurityBadge(
    modifier: Modifier = Modifier,
    isCloudSynced: Boolean = false
) {
    Row(
        modifier = modifier
            .background(if (isCloudSynced) Color(0x2210B981) else Color(0x226366F1), RoundedCornerShape(12.dp))
            .border(0.8.dp, if (isCloudSynced) Color(0x6610B981) else Color(0x666366F1), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            Icons.Default.Shield,
            contentDescription = "Secured Flow",
            tint = if (isCloudSynced) Color(0xFF34D399) else Color(0xFFA5B4FC),
            modifier = Modifier.size(12.dp)
        )
        Text(
            if (isCloudSynced) "Firestore Secured • User Isolated" else "Local Storage • Cloud Ready",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCloudSynced) Color(0xFF6EE7B7) else Color(0xFFC7D2FE)
        )
    }
}
