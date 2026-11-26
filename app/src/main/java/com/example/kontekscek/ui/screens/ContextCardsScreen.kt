package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.KonteksDefaults
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.components.*
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContextCardsScreen(
    cards: List<ContextCardEntity>,
    openCreateInitially: Boolean = false,
    createRequestKey: Int = 0,
    onSaveCard: (ContextCardEntity) -> Unit,
    onDeleteCard: (ContextCardEntity) -> Unit,
    onDuplicateCard: (ContextCardEntity) -> Unit,
    onUseForVerification: (ContextCardEntity) -> Unit,
    onCopiedNotice: (String) -> Unit,
    onCreateRequestHandled: () -> Unit = {},
) {
    val colors = LocalKonteksColors.current
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TaskMode?>(null) }
    var editingCard by remember { mutableStateOf<ContextCardEntity?>(null) }
    var templateForForm by remember { mutableStateOf<KonteksDefaults.QuickTemplate?>(null) }
    var showFormDialog by remember { mutableStateOf(openCreateInitially) }
    var deleteTarget by remember { mutableStateOf<ContextCardEntity?>(null) }

    LaunchedEffect(createRequestKey) {
        if (createRequestKey > 0) {
            editingCard = null
            templateForForm = null
            showFormDialog = true
            onCreateRequestHandled()
        }
    }

    val filteredCards =
        remember(cards, searchQuery, selectedFilter) {
            cards.filter { card ->
                val matchesMode = selectedFilter == null || card.getTaskMode() == selectedFilter
                val haystack =
                    "${card.title} ${card.objective} ${card.audienceStandard} ${card.taskType} ${card.forbiddenData}"
                val matchesText = searchQuery.isBlank() || haystack.contains(searchQuery, ignoreCase = true)
                matchesMode && matchesText
            }
        }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari judul, tujuan, atau data terlarang…", fontSize = 13.sp) },
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
                            label = { Text("Semua (${cards.size})") },
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        title = "Buat dari templat",
                        subtitle = "Isi 5 kolom otomatis, tinggal sesuaikan",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KonteksDefaults.quickTemplates.forEach { tpl ->
                            AssistChip(
                                onClick = {
                                    editingCard = null
                                    templateForForm = tpl
                                    showFormDialog = true
                                },
                                label = { Text("${tpl.taskType} · ${tpl.mode.label}", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )
                        }
                    }
                }
            }

            if (filteredCards.isEmpty()) {
                item {
                    KonteksCard {
                        EmptyState(
                            icon = if (cards.isEmpty()) Icons.Rounded.CreditCard else Icons.Rounded.SearchOff,
                            title = if (cards.isEmpty()) "Belum ada Kartu Konteks" else "Kartu tidak ditemukan",
                            body =
                                if (cards.isEmpty()) {
                                    "Mulai dari templat cepat atau buat kartu baru untuk menyimpan brief 5 isian."
                                } else {
                                    "Coba kata kunci lain atau hapus filter mode yang aktif."
                                },
                            actionLabel = if (cards.isEmpty()) "Buat kartu" else "Reset filter",
                            onAction = {
                                if (cards.isEmpty()) {
                                    editingCard = null
                                    templateForForm = null
                                    showFormDialog = true
                                } else {
                                    searchQuery = ""
                                    selectedFilter = null
                                }
                            },
                        )
                    }
                }
            } else {
                items(filteredCards, key = { it.id }) { card ->
                    ContextCardItem(
                        card = card,
                        onEdit = {
                            editingCard = card
                            templateForForm = null
                            showFormDialog = true
                        },
                        onDuplicate = { onDuplicateCard(card) },
                        onDelete = { deleteTarget = card },
                        onCopy = {
                            clipboard.setText(AnnotatedString(card.toCopyablePromptHeader()))
                            onCopiedNotice("Kartu disalin! Tempel ke prompt AI.")
                        },
                        onVerify = { onUseForVerification(card) },
                    )
                }
            }
        }

        AddFab(
            onClick = {
                editingCard = null
                templateForForm = null
                showFormDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
        )
    }

    if (showFormDialog) {
        ContextCardFormDialog(
            initial = editingCard,
            template = templateForForm,
            onDismiss = {
                showFormDialog = false
                templateForForm = null
            },
            onSubmit = {
                onSaveCard(it)
                showFormDialog = false
                templateForForm = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = colors.danger) },
            title = { Text("Hapus Kartu Konteks?", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${target.title}\" akan dihapus. Jejak verifikasi yang sudah dibuat tetap tersimpan.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(target)
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
private fun ContextCardItem(
    card: ContextCardEntity,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onVerify: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    var expanded by remember(card.id) { mutableStateOf(false) }

    KonteksCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ModeBadge(card.getTaskMode())
            TextBadge(label = card.taskType, tone = Tone.NEUTRAL)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDuplicate, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplikasi", tint = colors.inkMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Ubah", tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = colors.danger, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            card.title,
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Diperbarui ${formatter.format(Date(card.updatedAt))}",
            color = colors.inkMuted,
            fontFamily = IbmPlexMono,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(10.dp))

        CardFieldRow(Icons.Rounded.Flag, "Tujuan", card.objective)
        CardFieldRow(Icons.Rounded.Groups, "Audiens & mutu", card.audienceStandard)
        if (expanded) {
            CardFieldRow(Icons.Rounded.Gavel, "Etika & sumber", card.ethicsAndSources)
            CardFieldRow(Icons.Rounded.Description, "Format & bahasa", card.formatAndLanguage)
        }
        CardFieldRow(
            icon = Icons.Rounded.Shield,
            label = "Data terlarang",
            value = card.forbiddenData.ifBlank { "Belum diisi" },
            tint = colors.danger,
        )

        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
        ) {
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(if (expanded) "Ringkas isian" else "Lihat semua 5 isian", fontSize = 11.5.sp)
        }

        HorizontalDivider(color = colors.line)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = "Salin",
                onClick = onCopy,
                icon = Icons.Rounded.ContentCopy,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Verifikasi",
                onClick = onVerify,
                icon = Icons.Rounded.Verified,
                modifier = Modifier.weight(1f),
                container = colors.success,
            )
        }
    }
}

@Composable
private fun CardFieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color = LocalKonteksColors.current.primary,
) {
    val colors = LocalKonteksColors.current
    Row(
        modifier = Modifier.padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp).padding(top = 2.dp))
        Text(
            "$label: ",
            fontFamily = PlusJakarta,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = colors.ink,
        )
        Text(
            value.ifBlank { "-" },
            fontFamily = PlusJakarta,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = colors.inkSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContextCardFormDialog(
    initial: ContextCardEntity?,
    template: KonteksDefaults.QuickTemplate?,
    onDismiss: () -> Unit,
    onSubmit: (ContextCardEntity) -> Unit,
) {
    val colors = LocalKonteksColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var mode by remember { mutableStateOf(initial?.getTaskMode() ?: template?.mode ?: TaskMode.KULIAH) }
    var taskType by remember { mutableStateOf(initial?.taskType ?: template?.taskType ?: "Laporan") }
    var objective by remember { mutableStateOf(initial?.objective ?: template?.objective ?: "") }
    var audience by remember { mutableStateOf(initial?.audienceStandard ?: template?.audienceStandard ?: "") }
    var ethics by remember { mutableStateOf(initial?.ethicsAndSources ?: template?.ethicsAndSources ?: "") }
    var format by remember { mutableStateOf(initial?.formatAndLanguage ?: template?.formatAndLanguage ?: "") }
    var forbidden by remember { mutableStateOf(initial?.forbiddenData ?: template?.forbiddenData ?: "") }

    val canSave = title.isNotBlank() && objective.isNotBlank() && audience.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(Dimens.radiusXl),
            color = colors.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).background(colors.primarySoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (initial == null) "Buat Kartu Konteks" else "Ubah Kartu Konteks",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "5 isian wajib · tersimpan lokal",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = colors.inkMuted)
                    }
                }
                HorizontalDivider(color = colors.line)

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Templat cepat", color = colors.inkMuted, fontFamily = PlusJakarta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )
                        }
                    }

                    Text("Mode", color = colors.inkMuted, fontFamily = PlusJakarta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskMode.entries.forEach { m ->
                            FilterChip(
                                selected = mode == m,
                                onClick = { mode = m },
                                label = { Text(m.label, fontSize = 11.sp) },
                                leadingIcon = { Icon(m.icon(), contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )
                        }
                    }

                    FieldLabel("Judul tugas / proyek", required = true)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Contoh: Laporan Praktikum Basis Data") },
                        singleLine = true,
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FieldLabel("Jenis keluaran")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("Laporan", "Kode", "Desain", "Konten", "Data").forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
                                label = { Text(type, fontSize = 11.sp) },
                            )
                        }
                    }

                    FieldLabel("1. Tujuan utama", required = true)
                    OutlinedTextField(
                        value = objective,
                        onValueChange = { objective = it },
                        placeholder = { Text("Apa hasil akhir yang ingin dicapai?") },
                        minLines = 2,
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FieldLabel("2. Audiens & standar mutu", required = true)
                    OutlinedTextField(
                        value = audience,
                        onValueChange = { audience = it },
                        placeholder = { Text("Siapa pembacanya dan standar apa yang dipakai?") },
                        minLines = 2,
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FieldLabel("3. Batas etika & sumber")
                    OutlinedTextField(
                        value = ethics,
                        onValueChange = { ethics = it },
                        placeholder = { Text("Bagian mana yang wajib dicek ke sumber asli?") },
                        minLines = 2,
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FieldLabel("4. Format & bahasa")
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        placeholder = { Text("Contoh: Bahasa Indonesia formal, Markdown") },
                        minLines = 2,
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FieldLabel("5. Data terlarang")
                    OutlinedTextField(
                        value = forbidden,
                        onValueChange = { forbidden = it },
                        placeholder = { Text("Pisahkan dengan koma: NIM, password, data klien") },
                        supportingText = { Text("Rule engine memberi peringatan bila kata ini muncul di output AI.", fontSize = 10.5.sp) },
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider(color = colors.line)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = if (initial == null) "Simpan Kartu" else "Perbarui Kartu",
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
                                    createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }
        }
    }
}
