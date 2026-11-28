package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.aldointelligence.kontekscek.data.LastAnalysis
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.aldointelligence.kontekscek.domain.model.AnalysisSource
import com.aldointelligence.kontekscek.ui.components.*
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryLogsScreen(
    logs: List<VerificationLogEntity>,
    lastAnalysis: LastAnalysis? = null,
    onOpenLastAnalysis: (() -> Unit)? = null,
    onEditLog: (VerificationLogEntity) -> Unit,
    onExportPdf: (VerificationLogEntity) -> Unit,
    onDeleteLog: (VerificationLogEntity) -> Unit,
    onPurgeAll: () -> Unit,
    onReuseLog: (VerificationLogEntity) -> Unit,
) {
    val colors = LocalKonteksColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TaskMode?>(null) }
    var confirmPurge by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<VerificationLogEntity?>(null) }
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    val filtered =
        remember(logs, searchQuery, selectedFilter) {
            logs.filter { log ->
                val matchesMode = selectedFilter == null || log.getTaskMode() == selectedFilter
                val haystack =
                    "${log.cardTitle} ${log.stageAsked} ${log.stageChecked} ${log.stageChanged} ${log.finalConclusion}"
                matchesMode && (searchQuery.isBlank() || haystack.contains(searchQuery, ignoreCase = true))
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul, catatan tahap, atau kesimpulan…", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Bersihkan", tint = colors.inkMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Semua (${logs.size})") },
                        leadingIcon = { Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    )
                    TaskMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedFilter == mode,
                            onClick = { selectedFilter = if (selectedFilter == mode) null else mode },
                            label = { Text(mode.label) },
                            leadingIcon = { Icon(mode.icon(), contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            item {
                KonteksCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Arsip jejak lama",
                                color = colors.ink,
                                fontFamily = PlusJakarta,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${logs.sumOf { it.issuesCaughtCount }} isu tertangkap · " +
                                    "${logs.count { it.durationSeconds <= 300 }} dari ${logs.size} sesi ≤ 5 menit",
                                color = colors.inkMuted,
                                fontFamily = PlusJakarta,
                                fontSize = 11.sp,
                            )
                        }
                        IconButton(onClick = { confirmPurge = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Hapus semua", tint = colors.danger)
                        }
                    }
                }
            }
        }

        if (lastAnalysis != null && onOpenLastAnalysis != null) {
            item {
                KonteksCard(onClick = onOpenLastAnalysis) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).background(colors.primarySoft, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${lastAnalysis.result.score.overall}",
                                color = colors.primary,
                                fontFamily = IbmPlexMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Analisis terakhir",
                                color = colors.ink,
                                fontFamily = PlusJakarta,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${lastAnalysis.result.scoreLabel} · ${formatter.format(Date(lastAnalysis.createdAt))}",
                                color = colors.inkMuted,
                                fontFamily = PlusJakarta,
                                fontSize = 11.sp,
                            )
                            if (!lastAnalysis.prompt.isNullOrBlank()) {
                                Text(
                                    lastAnalysis.prompt.replace("\n", " ").take(90),
                                    color = colors.inkSecondary,
                                    fontFamily = PlusJakarta,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                )
                            } else {
                                Text(
                                    "Teks tidak disimpan (opsi privasi aktif)",
                                    color = colors.inkMuted,
                                    fontFamily = PlusJakarta,
                                    fontSize = 10.5.sp,
                                )
                            }
                        }
                        TextBadge(
                            label = if (lastAnalysis.result.source == AnalysisSource.REMOTE) "AI" else "Lokal",
                            tone = if (lastAnalysis.result.source == AnalysisSource.REMOTE) Tone.PRIMARY else Tone.NEUTRAL,
                        )
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                KonteksCard {
                    EmptyState(
                        icon = if (logs.isEmpty()) Icons.Rounded.History else Icons.Rounded.SearchOff,
                        title = if (logs.isEmpty()) "Belum ada arsip jejak lama" else "Jejak tidak ditemukan",
                        body =
                            if (logs.isEmpty()) {
                                "Arsip ini menyimpan sesi Mode Manual lama. Hasil Cek AI terbaru tampil di atas."
                            } else {
                                "Coba kata kunci lain atau reset filter mode."
                            },
                        actionLabel = if (logs.isNotEmpty()) "Reset filter" else null,
                        onAction =
                            if (logs.isNotEmpty()) {
                                {
                                    searchQuery = ""
                                    selectedFilter = null
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { log ->
                HistoryLogCard(
                    log = log,
                    formatter = formatter,
                    onEdit = { onEditLog(log) },
                    onExportPdf = { onExportPdf(log) },
                    onReuse = { onReuseLog(log) },
                    onDelete = { deleteTarget = log },
                )
            }
        }
    }

    if (confirmPurge) {
        AlertDialog(
            onDismissRequest = { confirmPurge = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colors.danger) },
            title = { Text("Hapus Semua Riwayat?", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Seluruh catatan jejak lama, kartu konteks, analisis terakhir, dan cache PDF/backup di perangkat ini akan dihapus permanen sesuai hak penghapusan data UU PDP.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeAll()
                        confirmPurge = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = colors.onInverse),
                ) {
                    Text("Hapus Permanen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPurge = false }) { Text("Batal") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            title = { Text("Hapus Jejak Ini?", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${target.cardTitle}\" akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLog(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = colors.onInverse),
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun HistoryLogCard(
    log: VerificationLogEntity,
    formatter: SimpleDateFormat,
    onEdit: () -> Unit,
    onExportPdf: () -> Unit,
    onReuse: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    val checks = log.parseChecklist()
    val doneCount = checks.count { it.status != CheckStatus.BELUM }
    var expanded by remember(log.id) { mutableStateOf(false) }

    KonteksCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeBadge(log.getTaskMode())
            Spacer(Modifier.weight(1f))
            Text(
                formatter.format(Date(log.createdAt)),
                color = colors.inkMuted,
                fontFamily = IbmPlexMono,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            log.cardTitle,
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Checklist, contentDescription = null, tint = colors.success, modifier = Modifier.size(14.dp))
                Text("$doneCount/${checks.size}", fontSize = 11.sp, color = colors.inkSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                Text("${log.durationSeconds / 60}m ${log.durationSeconds % 60}d", fontSize = 11.sp, color = colors.inkSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.BugReport, contentDescription = null, tint = colors.warning, modifier = Modifier.size(14.dp))
                Text("${log.issuesCaughtCount} isu", fontSize = 11.sp, color = colors.inkSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(
            progress = if (checks.isEmpty()) 0f else doneCount.toFloat() / checks.size,
            color = if (doneCount == checks.size && checks.isNotEmpty()) colors.success else colors.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Diubah: ${log.stageChanged.ifBlank { "belum ada catatan perubahan" }}",
            color = colors.inkSecondary,
            fontFamily = PlusJakarta,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            maxLines = if (expanded) 8 else 2,
        )

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                DetailLine("Diminta", log.stageAsked, colors.primary)
                DetailLine("Diperiksa", log.stageChecked, colors.success)
                DetailLine("Kesimpulan", log.finalConclusion, colors.danger)
            }
        }

        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
        ) {
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(if (expanded) "Sembunyikan detail" else "Lihat detail 3 tahap", fontSize = 11.sp)
        }

        HorizontalDivider(color = colors.line)
        Spacer(Modifier.height(9.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                text = "PDF",
                onClick = onExportPdf,
                icon = Icons.Rounded.PictureAsPdf,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Ubah",
                onClick = onEdit,
                icon = Icons.Rounded.Edit,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onReuse, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = "Sesi baru", tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = colors.danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    val colors = LocalKonteksColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 5.dp).size(6.dp).background(accent, CircleShape),
        )
        Text(
            "$label: ",
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            value.ifBlank { "-" },
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
