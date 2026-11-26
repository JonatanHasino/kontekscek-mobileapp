package com.aldointelligence.kontekscek.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.components.*
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
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
    onSaveAndExportPdf: () -> Unit,
    onNewSession: () -> Unit,
    onResetChecklist: () -> Unit,
    onClearOutput: () -> Unit,
    onCopiedNotice: (String) -> Unit,
) {
    val colors = LocalKonteksColors.current
    val clipboard = LocalClipboardManager.current
    val pdfPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { onImportPdfUri(it) }
        }

    val minutes = state.elapsedSeconds / 60
    val seconds = state.elapsedSeconds % 60
    val timerTone =
        when {
            state.elapsedSeconds <= 300 -> Tone.SUCCESS
            state.elapsedSeconds <= 600 -> Tone.WARNING
            else -> Tone.DANGER
        }
    val completed = state.checklist.count { it.status != CheckStatus.BELUM }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            KonteksCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.editingLogId == 0L) "Sesi verifikasi baru" else "Sunting jejak #${state.editingLogId}",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Target selesai ≤ 5 menit • analisis lokal",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = toneBackground(timerTone),
                        onClick = onToggleTimer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                if (state.isTimerRunning) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                                contentDescription = "Timer",
                                tint = toneForeground(timerTone),
                                modifier = Modifier.size(17.dp),
                            )
                            Text(
                                String.format("%02d:%02d", minutes, seconds),
                                color = toneForeground(timerTone),
                                fontFamily = IbmPlexMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    IconButton(onClick = onNewSession, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Sesi baru", tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Langkah 1
        item {
            KonteksCard {
                StepHeader(step = 1, title = "Konteks & mode pemeriksaan", icon = Icons.Rounded.Layers)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskMode.entries.forEach { m ->
                        FilterChip(
                            selected = state.mode == m,
                            onClick = { onSelectMode(m) },
                            label = { Text(m.label) },
                            leadingIcon = { Icon(m.icon(), contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                    }
                }
                if (availableCards.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kartu Konteks tersimpan",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableCards.forEach { card ->
                            FilterChip(
                                selected = state.selectedCard?.id == card.id,
                                onClick = { onSelectCard(card) },
                                label = { Text(card.title, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            )
                        }
                    }
                }
                state.selectedCard?.let { card ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(colors.primarySoft, RoundedCornerShape(Dimens.radiusSm))
                                .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                card.title,
                                color = colors.ink,
                                fontFamily = PlusJakarta,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                "Tujuan: ${card.objective}",
                                color = colors.inkMuted,
                                fontFamily = PlusJakarta,
                                fontSize = 10.5.sp,
                                maxLines = 2,
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(card.toCopyablePromptHeader()))
                                onCopiedNotice("Kartu disalin! Tempel ke prompt AI.")
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin kartu", tint = colors.primary, modifier = Modifier.size(17.dp))
                        }
                    }
                } ?: run {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tanpa kartu: checklist mengikuti mode. Pilih kartu untuk mendeteksi data terlarang & penyimpangan brief.",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        // Langkah 2
        item {
            KonteksCard {
                StepHeader(
                    step = 2,
                    title = "Tempel / impor output AI",
                    icon = Icons.Rounded.DocumentScanner,
                ) {
                    ReadinessRing(score = state.analysis.readinessScore, size = 62.dp, stroke = 5.dp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        shape = RoundedCornerShape(Dimens.radiusXs),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Impor PDF", fontSize = 11.5.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) {
                                onCopiedNotice("Papan klip kosong")
                            } else {
                                val merged =
                                    if (state.aiOutputText.isBlank()) pasted else "${state.aiOutputText}\n$pasted"
                                onOutputChange(merged)
                                onCopiedNotice("Teks ditempel dari papan klip")
                            }
                        },
                        shape = RoundedCornerShape(Dimens.radiusXs),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Tempel", fontSize = 11.5.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onClearOutput,
                        shape = RoundedCornerShape(Dimens.radiusXs),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colors.danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Kosongkan", fontSize = 11.5.sp, color = colors.danger, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.aiOutputText,
                    onValueChange = onOutputChange,
                    placeholder = { Text("Tempel potongan jawaban/kode dari AI, atau impor file PDF di atas…", fontSize = 12.5.sp) },
                    minLines = 4,
                    maxLines = 9,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${state.aiOutputText.length} karakter",
                        color = colors.inkMuted,
                        fontFamily = IbmPlexMono,
                        fontSize = 10.sp,
                    )
                    Text(
                        "Skor siap: ${state.analysis.readinessScore}%",
                        color =
                            when {
                                state.analysis.readinessScore >= 80 -> colors.success
                                state.analysis.readinessScore >= 50 -> colors.warning
                                else -> colors.danger
                            },
                        fontFamily = PlusJakarta,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
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

        // Langkah 3
        item {
            KonteksCard {
                StepHeader(
                    step = 3,
                    title = "Daftar periksa mode ${state.mode.label}",
                    icon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                ) {
                    TextBadge(
                        label = "$completed/${state.checklist.size}",
                        tone = if (completed == state.checklist.size) Tone.SUCCESS else Tone.NEUTRAL,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(
                    progress =
                        if (state.checklist.isEmpty()) {
                            0f
                        } else {
                            completed.toFloat() / state.checklist.size
                        },
                    color = if (completed == state.checklist.size) colors.success else colors.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onResetChecklist, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset checklist", fontSize = 11.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.checklist.forEachIndexed { idx, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    "${idx + 1}.",
                                    color = colors.inkMuted,
                                    fontFamily = IbmPlexMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    item.title,
                                    color = colors.ink,
                                    fontFamily = PlusJakarta,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(start = 17.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = if (item.status == CheckStatus.BELUM) colors.info else colors.inkMuted,
                                    modifier = Modifier.size(13.dp).padding(top = 2.dp),
                                )
                                Text(
                                    item.suggestion,
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = colors.inkMuted,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            StatusTriToggle(
                                current = item.status,
                                onSelect = { newStatus -> onChecklistChange(item.id, newStatus) },
                            )
                        }
                        if (idx < state.checklist.lastIndex) {
                            HorizontalDivider(color = colors.line)
                        }
                    }
                }
            }
        }

        // Langkah 4
        item {
            KonteksCard {
                StepHeader(
                    step = 4,
                    title = "Catatan jejak 3 tahap",
                    icon = Icons.Rounded.HistoryEdu,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.stageAsked,
                    onValueChange = { onStagesChange(it, null, null, null) },
                    label = { Text("A. Yang diminta ke AI", fontSize = 12.sp) },
                    placeholder = { Text("Ringkas prompt / instruksi utama") },
                    leadingIcon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = colors.primary) },
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.stageChecked,
                    onValueChange = { onStagesChange(null, it, null, null) },
                    label = { Text("B. Yang diperiksa / diuji silang", fontSize = 12.sp) },
                    placeholder = { Text("Sumber, hitungan, atau unit test yang dicek") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null, tint = colors.success) },
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.stageChanged,
                    onValueChange = { onStagesChange(null, null, it, null) },
                    label = { Text("C. Yang diubah / disunting manusia", fontSize = 12.sp) },
                    placeholder = { Text("Bagian output yang Anda perbaiki") },
                    leadingIcon = { Icon(Icons.Rounded.AutoFixNormal, contentDescription = null, tint = colors.warning) },
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.finalConclusion,
                    onValueChange = { onStagesChange(null, null, null, it) },
                    label = { Text("D. Kesimpulan verifikator (opsional)", fontSize = 12.sp) },
                    placeholder = { Text("Contoh: layak dipakai setelah 2 sitasi diganti") },
                    leadingIcon = { Icon(Icons.Rounded.Gavel, contentDescription = null, tint = colors.danger) },
                    minLines = 2,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = if (state.editingLogId == 0L) "Simpan & Ekspor PDF Jejak" else "Perbarui & Ekspor PDF",
                    onClick = onSaveAndExportPdf,
                    icon = Icons.Rounded.PictureAsPdf,
                    container = colors.success,
                )
                if (state.editingLogId != 0L) {
                    SecondaryButton(
                        text = "Mulai sesi baru dari kartu ini",
                        onClick = onNewSession,
                        icon = Icons.Rounded.RestartAlt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "PDF dibuat di perangkat • tidak ada unggahan ke server",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
