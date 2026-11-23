package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.aldointelligence.kontekscek.ui.components.ModeBadgeChip
import com.aldointelligence.kontekscek.ui.components.NeonGlassCard
import com.aldointelligence.kontekscek.ui.components.StatGlowTile
import com.aldointelligence.kontekscek.ui.theme.CyberColors
import com.aldointelligence.kontekscek.ui.viewmodel.DashboardMetrics

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    recentLogs: List<VerificationLogEntity>,
    onStartVerify: () -> Unit,
    onCreateCard: () -> Unit,
    onOpenLog: (VerificationLogEntity) -> Unit
) {
    val avgMinutes = metrics.avgDurationSeconds / 60
    val avgRemSec = metrics.avgDurationSeconds % 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberColors.DeepSpace),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner Futuristik
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CyberColors.ElectricBlue.copy(alpha = 0.35f),
                                CyberColors.NeonCyan.copy(alpha = 0.22f),
                                CyberColors.NavySurface
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = CyberColors.VerifiedEmerald, modifier = Modifier.size(28.dp))
                        Text("KonteksCek", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CyberColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CyberColors.VerifiedEmerald.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = CyberColors.VerifiedEmerald, modifier = Modifier.size(12.dp))
                                Text("Lokal • Privasi", fontSize = 10.sp, color = CyberColors.VerifiedEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        "Pakai AI boleh. Pastikan cocok konteks, dan simpan jejak verifikasinya dalam ≤5 menit.",
                        fontSize = 13.sp,
                        color = CyberColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onStartVerify,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonCyan)
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = CyberColors.DeepSpace, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mulai Cek AI", color = CyberColors.DeepSpace, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCreateCard,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.AddCard, contentDescription = null, tint = CyberColors.NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kartu Baru", color = CyberColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // KPI Grid 2x2
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatGlowTile(
                    title = "Kartu Konteks",
                    value = "${metrics.totalCards}",
                    icon = Icons.Rounded.CreditCard,
                    accent = CyberColors.NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatGlowTile(
                    title = "Jejak Verifikasi",
                    value = "${metrics.totalLogs}",
                    icon = Icons.Rounded.FactCheck,
                    accent = CyberColors.VerifiedEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatGlowTile(
                    title = "Rata-Rata Durasi (Target ≤5m)",
                    value = "${avgMinutes}m ${avgRemSec}d",
                    icon = Icons.Rounded.Timer,
                    accent = CyberColors.ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatGlowTile(
                    title = "Isu Tertangkap",
                    value = "${metrics.totalIssuesCaught}",
                    icon = Icons.Rounded.BugReport,
                    accent = CyberColors.WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Riwayat Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jejak Verifikasi Terbaru", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                Text("${metrics.underFiveMinRate}% lolos ≤5 menit", fontSize = 11.sp, color = CyberColors.VerifiedEmerald, fontWeight = FontWeight.SemiBold)
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                NeonGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyberColors.NeonCyan, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Belum ada jejak verifikasi", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                            Text("Pilih Kartu Konteks dan jalankan sesi cek pertama Anda.", fontSize = 12.sp, color = CyberColors.TextSecondary)
                        }
                    }
                }
            }
        } else {
            items(recentLogs.take(4)) { log ->
                NeonGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        ModeBadgeChip(log.getTaskMode())
                        Text(
                            "${log.durationSeconds / 60}m ${log.durationSeconds % 60}d",
                            fontSize = 11.sp,
                            color = CyberColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(log.cardTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyberColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Diubah: ${log.stageChanged.ifBlank { "Tidak ada catatan perubahan" }}",
                        fontSize = 12.sp,
                        color = CyberColors.TextSecondary,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onOpenLog(log) }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buka / Sunting Jejak", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}