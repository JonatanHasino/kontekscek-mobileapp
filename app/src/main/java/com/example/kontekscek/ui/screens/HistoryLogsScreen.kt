package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.aldointelligence.kontekscek.ui.components.ModeBadgeChip
import com.aldointelligence.kontekscek.ui.components.NeonGlassCard
import com.aldointelligence.kontekscek.ui.theme.CyberColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryLogsScreen(
    logs: List<VerificationLogEntity>,
    onEditLog: (VerificationLogEntity) -> Unit,
    onExportPdf: (VerificationLogEntity) -> Unit,
    onDeleteLog: (VerificationLogEntity) -> Unit,
    onPurgeAll: () -> Unit
) {
    var confirmPurge by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberColors.DeepSpace),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Riwayat Jejak Verifikasi", fontSize = 21.sp, fontWeight = FontWeight.Black, color = CyberColors.TextPrimary)
                    Text("Kelola, ekspor ulang PDF, atau hapus data (Hak UU PDP)", fontSize = 11.5.sp, color = CyberColors.TextSecondary)
                }
                if (logs.isNotEmpty()) {
                    IconButton(onClick = { confirmPurge = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Hapus Semua", tint = CyberColors.AlertCoral)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                NeonGlassCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = CyberColors.NeonCyan, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Belum ada riwayat tersimpan", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                        Text("Setiap sesi verifikasi yang selesai akan tersimpan di sini.", fontSize = 12.sp, color = CyberColors.TextSecondary)
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                val checks = log.parseChecklist()
                val doneCount = checks.count { it.status != CheckStatus.BELUM }

                NeonGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        ModeBadgeChip(log.getTaskMode())
                        Text(formatter.format(Date(log.createdAt)), fontSize = 11.sp, color = CyberColors.TextSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(log.cardTitle, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = CyberColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Checklist, contentDescription = null, tint = CyberColors.VerifiedEmerald, modifier = Modifier.size(15.dp))
                            Text("$doneCount/${checks.size} butir", fontSize = 11.5.sp, color = CyberColors.TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = CyberColors.NeonCyan, modifier = Modifier.size(15.dp))
                            Text("${log.durationSeconds / 60}m ${log.durationSeconds % 60}d", fontSize = 11.5.sp, color = CyberColors.TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.BugReport, contentDescription = null, tint = CyberColors.WarningAmber, modifier = Modifier.size(15.dp))
                            Text("${log.issuesCaughtCount} isu", fontSize = 11.5.sp, color = CyberColors.TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Diminta: ${log.stageAsked.ifBlank { "-" }}", fontSize = 12.sp, color = CyberColors.TextSecondary, maxLines = 1)
                    Text("Diperiksa: ${log.stageChecked.ifBlank { "-" }}", fontSize = 12.sp, color = CyberColors.TextSecondary, maxLines = 1)
                    Text("Diubah: ${log.stageChanged.ifBlank { "-" }}", fontSize = 12.sp, color = CyberColors.TextSecondary, maxLines = 1)

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onExportPdf(log) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonCyan)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = CyberColors.DeepSpace, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("PDF", color = CyberColors.DeepSpace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onEditLog(log) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Ubah", fontSize = 12.sp)
                        }
                        IconButton(onClick = { onDeleteLog(log) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = CyberColors.AlertCoral)
                        }
                    }
                }
            }
        }
    }

    if (confirmPurge) {
        AlertDialog(
            onDismissRequest = { confirmPurge = false },
            containerColor = CyberColors.NavySurface,
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Seluruh catatan jejak verifikasi di perangkat ini akan dihapus permanen sesuai hak penghapusan data UU PDP.") },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeAll()
                        confirmPurge = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColors.AlertCoral)
                ) { Text("Hapus Permanen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmPurge = false }) { Text("Batal") }
            }
        )
    }
}