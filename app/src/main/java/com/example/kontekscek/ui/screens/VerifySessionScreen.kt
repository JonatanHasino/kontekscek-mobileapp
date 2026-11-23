package com.aldointelligence.kontekscek.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.components.*
import com.aldointelligence.kontekscek.ui.theme.CyberColors
import com.aldointelligence.kontekscek.ui.viewmodel.VerifySessionState

@Composable
fun VerifySessionScreen(
    state: VerifySessionState,
    availableCards: List<ContextCardEntity>,
    onSelectCard: (ContextCardEntity) -> Unit,
    onSelectMode: (TaskMode) -> Unit,
    onOutputChange: (String) -> Unit,
    onChecklistChange: (String, CheckStatus) -> Unit,
    onStagesChange: (String?, String?, String?, String?) -> Unit,
    onToggleTimer: () -> Unit,
    onImportPdfUri: (Uri) -> Unit = {},
    onSaveAndExportPdf: () -> Unit
) {
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportPdfUri(it) }
    }

    val minutes = state.elapsedSeconds / 60
    val seconds = state.elapsedSeconds % 60
    val timerColor = if (state.elapsedSeconds <= 300) CyberColors.VerifiedEmerald else CyberColors.WarningAmber

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberColors.DeepSpace),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Live Stopwatch (Target <= 5 Menit)
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        if (state.editingLogId == 0L) "Ruang Verifikasi AI" else "Sunting Jejak #${state.editingLogId}",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberColors.TextPrimary
                    )
                    Text("Analisis 100% lokal • Target ≤ 5 menit", fontSize = 11.5.sp, color = CyberColors.TextSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = timerColor.copy(alpha = 0.16f),
                    onClick = onToggleTimer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (state.isTimerRunning) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                            contentDescription = "Timer",
                            tint = timerColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            String.format("%02d:%02d", minutes, seconds),
                            color = timerColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Langkah 1: Pilih Kartu Konteks & Mode
        item {
            NeonGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, tint = CyberColors.NeonCyan)
                    Text("1. Konteks & Mode Pemeriksaan", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskMode.entries.forEach { m ->
                        FilterChip(
                            selected = state.mode == m,
                            onClick = { onSelectMode(m) },
                            label = { Text(m.label) },
                            leadingIcon = { Icon(m.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (availableCards.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Pilih Kartu Konteks Tersimpan:", fontSize = 11.sp, color = CyberColors.TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCards.forEach { card ->
                            FilterChip(
                                selected = state.selectedCard?.id == card.id,
                                onClick = { onSelectCard(card) },
                                label = { Text(card.title, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // Langkah 2: Tempel / Impor Output AI + Scanner Rule-Based Lokal
        item {
            NeonGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = CyberColors.NeonCyan)
                        Text("2. Tempel / Impor Output AI", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                    }
                    Text(
                        "Skor Siap: ${state.analysis.readinessScore}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (state.analysis.readinessScore >= 80) CyberColors.VerifiedEmerald else CyberColors.WarningAmber
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = CyberColors.NeonCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("Unggah File PDF (Ekstrak Teks)", color = CyberColors.NeonCyan, fontSize = 12.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.aiOutputText,
                    onValueChange = onOutputChange,
                    placeholder = { Text("Tempel potongan jawaban/kode dari AI atau unggah file PDF di atas...") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.analysis.alerts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.analysis.alerts.forEach { alert ->
                            RuleAlertBanner(alert)
                        }
                    }
                }
            }
        }

        // Langkah 3: Daftar Periksa 5 Butir per Mode
        item {
            NeonGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = null, tint = CyberColors.VerifiedEmerald)
                    Text("3. Daftar Periksa Mode ${state.mode.label}", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.checklist.forEachIndexed { idx, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${idx + 1}. ${item.title}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CyberColors.TextPrimary)
                            if (item.status == CheckStatus.BELUM) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = CyberColors.NeonCyan, modifier = Modifier.size(14.dp))
                                    Text("Saran: ${item.suggestion}", fontSize = 11.sp, color = CyberColors.NeonCyan)
                                }
                            }
                            StatusTriToggle(
                                current = item.status,
                                onSelect = { newStatus -> onChecklistChange(item.id, newStatus) }
                            )
                        }
                        if (idx < state.checklist.lastIndex) {
                            HorizontalDivider(color = CyberColors.TextSecondary.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }

        // Langkah 4: Catatan Jejak 3 Tahap (Diminta -> Diperiksa -> Diubah)
        item {
            NeonGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.HistoryEdu, contentDescription = null, tint = CyberColors.ElectricBlue)
                    Text("4. Catatan Jejak 3 Tahap & Kesimpulan", fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.stageAsked,
                    onValueChange = { onStagesChange(it, null, null, null) },
                    label = { Text("A. Yang Diminta ke AI (ringkas)") },
                    leadingIcon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = CyberColors.NeonCyan) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.stageChecked,
                    onValueChange = { onStagesChange(null, it, null, null) },
                    label = { Text("B. Yang Diperiksa / Diuji Silang") },
                    leadingIcon = { Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = CyberColors.VerifiedEmerald) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.stageChanged,
                    onValueChange = { onStagesChange(null, null, it, null) },
                    label = { Text("C. Yang Diubah / Disunting Manusia") },
                    leadingIcon = { Icon(Icons.Rounded.AutoFixNormal, contentDescription = null, tint = CyberColors.WarningAmber) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.finalConclusion,
                    onValueChange = { onStagesChange(null, null, null, it) },
                    label = { Text("D. Kesimpulan Verifikator (Opsional)") },
                    leadingIcon = { Icon(Icons.Rounded.Gavel, contentDescription = null, tint = CyberColors.AlertCoral) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Langkah 5: Simpan CRUD & Ekspor PDF 1 Halaman
        item {
            Button(
                onClick = onSaveAndExportPdf,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberColors.VerifiedEmerald)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = CyberColors.DeepSpace)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Simpan & Ekspor PDF Jejak (1 Halaman)",
                    color = CyberColors.DeepSpace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}