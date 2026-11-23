package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.KonteksDefaults
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.components.ModeBadgeChip
import com.aldointelligence.kontekscek.ui.components.NeonGlassCard
import com.aldointelligence.kontekscek.ui.components.icon
import com.aldointelligence.kontekscek.ui.theme.CyberColors

@Composable
fun ContextCardsScreen(
    cards: List<ContextCardEntity>,
    openCreateInitially: Boolean = false,
    onSaveCard: (ContextCardEntity) -> Unit,
    onDeleteCard: (ContextCardEntity) -> Unit,
    onUseForVerification: (ContextCardEntity) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TaskMode?>(null) }
    var editingCard by remember { mutableStateOf<ContextCardEntity?>(null) }
    var showFormDialog by remember { mutableStateOf(openCreateInitially) }

    val filteredCards = remember(cards, searchQuery, selectedFilter) {
        cards.filter { card ->
            val matchesMode = selectedFilter == null || card.getTaskMode() == selectedFilter
            val matchesText = searchQuery.isBlank() ||
                card.title.contains(searchQuery, ignoreCase = true) ||
                card.objective.contains(searchQuery, ignoreCase = true)
            matchesMode && matchesText
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CyberColors.DeepSpace)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Kartu Konteks (5 Isian)", fontSize = 22.sp, fontWeight = FontWeight.Black, color = CyberColors.TextPrimary)
                Text("Simpan brief sekali, salin ke AI 1 klik, dan pakai ulang untuk verifikasi.", fontSize = 12.sp, color = CyberColors.TextSecondary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul atau tujuan kartu...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CyberColors.NeonCyan) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Semua (${cards.size})") },
                        leadingIcon = { Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    TaskMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedFilter == mode,
                            onClick = { selectedFilter = if (selectedFilter == mode) null else mode },
                            label = { Text(mode.label) },
                            leadingIcon = { Icon(mode.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            items(filteredCards, key = { it.id }) { card ->
                NeonGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ModeBadgeChip(card.getTaskMode())
                            Text("• ${card.taskType}", fontSize = 11.sp, color = CyberColors.TextSecondary)
                        }
                        Row {
                            IconButton(onClick = {
                                editingCard = card
                                showFormDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Ubah", tint = CyberColors.NeonCyan, modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = { onDeleteCard(card) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = CyberColors.AlertCoral, modifier = Modifier.size(19.dp))
                            }
                        }
                    }

                    Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))

                    CardFieldRow(Icons.Rounded.Flag, "Tujuan", card.objective)
                    CardFieldRow(Icons.Rounded.Groups, "Audiens/Mutu", card.audienceStandard)
                    CardFieldRow(Icons.Rounded.Gavel, "Etika & Sumber", card.ethicsAndSources)
                    CardFieldRow(Icons.Rounded.Description, "Format", card.formatAndLanguage)
                    CardFieldRow(Icons.Rounded.Shield, "Data Terlarang", card.forbiddenData, tint = CyberColors.AlertCoral)

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(card.toCopyablePromptHeader()))
                                onCopiedNotice("Kartu Konteks disalin! Tempel ke prompt AI.")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Salin Kartu", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onUseForVerification(card) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberColors.VerifiedEmerald)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = CyberColors.DeepSpace, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Verifikasi AI", color = CyberColors.DeepSpace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingCard = null
                showFormDialog = true
            },
            containerColor = CyberColors.NeonCyan,
            contentColor = CyberColors.DeepSpace,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah Kartu Konteks")
        }
    }

    if (showFormDialog) {
        ContextCardFormDialog(
            initial = editingCard,
            onDismiss = { showFormDialog = false },
            onSubmit = {
                onSaveCard(it)
                showFormDialog = false
            }
        )
    }
}

@Composable
private fun CardFieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color = CyberColors.NeonCyan
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp).padding(top = 2.dp))
        Text("$label: ", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CyberColors.TextPrimary)
        Text(value, fontSize = 12.sp, color = CyberColors.TextSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ContextCardFormDialog(
    initial: ContextCardEntity?,
    onDismiss: () -> Unit,
    onSubmit: (ContextCardEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var mode by remember { mutableStateOf(initial?.getTaskMode() ?: TaskMode.KULIAH) }
    var taskType by remember { mutableStateOf(initial?.taskType ?: "Laporan") }
    var objective by remember { mutableStateOf(initial?.objective ?: "") }
    var audience by remember { mutableStateOf(initial?.audienceStandard ?: "") }
    var ethics by remember { mutableStateOf(initial?.ethicsAndSources ?: "") }
    var format by remember { mutableStateOf(initial?.formatAndLanguage ?: "") }
    var forbidden by remember { mutableStateOf(initial?.forbiddenData ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberColors.NavySurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = CyberColors.NeonCyan)
                Text(if (initial == null) "Buat Kartu Konteks" else "Ubah Kartu Konteks", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Templat Cepat (S-07E):", fontSize = 11.sp, color = CyberColors.TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KonteksDefaults.quickTemplates.forEach { tpl ->
                        AssistChip(
                            onClick = {
                                mode = tpl.mode
                                taskType = tpl.taskType
                                objective = tpl.objective
                                audience = tpl.audienceStandard
                                ethics = tpl.ethicsAndSources
                                format = tpl.formatAndLanguage
                                forbidden = tpl.forbiddenData
                                if (title.isBlank()) title = "Tugas ${tpl.taskType} (${tpl.mode.label})"
                            },
                            label = { Text(tpl.taskType, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Tugas / Proyek") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("1. Tujuan Utama") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("2. Audiens & Standar Mutu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ethics, onValueChange = { ethics = it }, label = { Text("3. Batas Etika & Sumber") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = format, onValueChange = { format = it }, label = { Text("4. Format & Bahasa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = forbidden, onValueChange = { forbidden = it }, label = { Text("5. Data Terlarang (pisah koma)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && objective.isNotBlank(),
                onClick = {
                    onSubmit(
                        ContextCardEntity(
                            id = initial?.id ?: 0L,
                            title = title.trim(),
                            mode = mode.name,
                            taskType = taskType,
                            objective = objective.trim(),
                            audienceStandard = audience.trim(),
                            ethicsAndSources = ethics.trim(),
                            formatAndLanguage = format.trim(),
                            forbiddenData = forbidden.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonCyan)
            ) {
                Text("Simpan Kartu", color = CyberColors.DeepSpace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}