package com.aldointelligence.kontekscek.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.RuleAlert
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.theme.CyberColors

fun TaskMode.icon(): ImageVector = when (this) {
    TaskMode.KULIAH -> Icons.Rounded.School
    TaskMode.KERJA -> Icons.Rounded.Work
    TaskMode.KLIEN -> Icons.Rounded.Handshake
}

fun TaskMode.accentColor(): Color = when (this) {
    TaskMode.KULIAH -> CyberColors.NeonCyan
    TaskMode.KERJA -> CyberColors.ElectricBlue
    TaskMode.KLIEN -> CyberColors.VerifiedEmerald
}

@Composable
fun NeonGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberColors.BorderGlow,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CyberColors.NavySurface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(CyberColors.NavyElevated.copy(alpha = 0.55f), CyberColors.NavySurface)
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ModeBadgeChip(mode: TaskMode, modifier: Modifier = Modifier) {
    val color = mode.accentColor()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(mode.icon(), contentDescription = mode.label, tint = color, modifier = Modifier.size(14.dp))
        Text(mode.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatGlowTile(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    NeonGlassCard(
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.35f)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CyberColors.TextPrimary)
        Text(title, fontSize = 11.sp, color = CyberColors.TextSecondary)
    }
}

@Composable
fun StatusTriToggle(
    current: CheckStatus,
    onSelect: (CheckStatus) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CheckStatus.entries.forEach { status ->
            val selected = current == status
            val color = when (status) {
                CheckStatus.SUDAH -> CyberColors.VerifiedEmerald
                CheckStatus.DIUBAH -> CyberColors.WarningAmber
                CheckStatus.BELUM -> CyberColors.AlertCoral
            }
            val icon = when (status) {
                CheckStatus.SUDAH -> Icons.Rounded.CheckCircle
                CheckStatus.DIUBAH -> Icons.Rounded.EditNote
                CheckStatus.BELUM -> Icons.Rounded.RadioButtonUnchecked
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) color.copy(alpha = 0.22f) else CyberColors.DeepSpace,
                border = BorderStroke(1.dp, if (selected) color else Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(status) }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = status.label, tint = if (selected) color else CyberColors.TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        status.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) color else CyberColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RuleAlertBanner(alert: RuleAlert) {
    val (color, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> CyberColors.AlertCoral to Icons.Rounded.GppBad
        AlertSeverity.WARNING -> CyberColors.WarningAmber to Icons.Rounded.WarningAmber
        AlertSeverity.INFO -> CyberColors.NeonCyan to Icons.Rounded.TipsAndUpdates
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = alert.title, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Text(alert.title, color = color, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Spacer(Modifier.height(2.dp))
                Text(alert.detail, color = CyberColors.TextPrimary.copy(alpha = 0.9f), fontSize = 11.5.sp, lineHeight = 15.sp)
            }
        }
    }
}