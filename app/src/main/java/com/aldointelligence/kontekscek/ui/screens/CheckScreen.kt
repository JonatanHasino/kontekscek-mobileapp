package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aldointelligence.kontekscek.ui.components.HeroCard
import com.aldointelligence.kontekscek.ui.components.InfoBanner
import com.aldointelligence.kontekscek.ui.components.KonteksCard
import com.aldointelligence.kontekscek.ui.components.PrimaryButton
import com.aldointelligence.kontekscek.ui.components.SecondaryButton
import com.aldointelligence.kontekscek.ui.components.SectionHeader
import com.aldointelligence.kontekscek.ui.components.SegmentedControl
import com.aldointelligence.kontekscek.ui.components.SkeletonBlock
import com.aldointelligence.kontekscek.ui.components.TextBadge
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.components.ToggleRow
import com.aldointelligence.kontekscek.ui.components.konteksFieldColors
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import com.aldointelligence.kontekscek.ui.viewmodel.CheckDepth
import com.aldointelligence.kontekscek.ui.viewmodel.CheckPrivacyPreview
import com.aldointelligence.kontekscek.ui.viewmodel.CheckUiState
import com.aldointelligence.kontekscek.ui.viewmodel.SharedInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckScreen(
    state: CheckUiState,
    onPromptChange: (String) -> Unit,
    onOutputChange: (String) -> Unit,
    onBriefChange: (String) -> Unit,
    onToggleBrief: () -> Unit,
    onClearPrompt: () -> Unit,
    onClearOutput: () -> Unit,
    onSaveTextChange: (Boolean) -> Unit,
    onDepthChange: (CheckDepth) -> Unit,
    onAnalyze: () -> Unit,
    onRunLocal: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
    onConsentAccept: () -> Unit,
    onConsentDecline: () -> Unit,
    onPrivacyConfirm: () -> Unit,
    onPrivacyCancel: () -> Unit,
    onSharedAsPrompt: () -> Unit,
    onSharedAsOutput: () -> Unit,
    onSharedAuto: () -> Unit,
    onSharedDismiss: () -> Unit,
    onOpenLastAnalysis: () -> Unit,
    onOpenServerSettings: () -> Unit,
    onNotice: (String) -> Unit,
) {
    val colors = LocalKonteksColors.current
    val clipboard = LocalClipboardManager.current
    var clipboardHint by remember { mutableStateOf<String?>(null) }
    var showDeepInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state.prompt.isBlank()) {
            clipboardHint =
                runCatching { clipboard.getText()?.text?.trim() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() && it.length >= 10 }
        }
    }

    fun readClipboard(): String =
        runCatching { clipboard.getText()?.text.orEmpty() }.getOrDefault("")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(23.dp))
                    Text(
                        "Prompt-mu dicek. Dibenerin.",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tempel prompt dan jawaban AI. Dalam dua ketuk kamu dapat diagnosis, prompt perbaikan, dan cek kesesuaian jawaban.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextBadge(label = "Privacy Guard lokal", tone = Tone.SUCCESS)
                    TextBadge(label = "Lint lokal gratis", tone = Tone.PRIMARY)
                }
            }
        }

        if (!state.serverConfigured) {
            item {
                InfoBanner(
                    title = "Server AI belum diatur",
                    body = "Kamu tetap bisa memakai lint lokal. Untuk analisis AI, isi URL server di Pengaturan (atau jalankan backend Ktor lokal).",
                    tone = Tone.WARNING,
                    icon = Icons.Rounded.Settings,
                    actionLabel = "Atur server",
                    onAction = onOpenServerSettings,
                )
            }
        }

        clipboardHint?.let { hint ->
            if (state.prompt.isBlank()) {
                item {
                    InfoBanner(
                        title = "Ada teks di clipboard",
                        body = hint.replace("\n", " ").take(100) + if (hint.length > 100) "…" else "",
                        tone = Tone.INFO,
                        icon = Icons.Rounded.ContentPaste,
                        actionLabel = "Tempel sebagai prompt",
                        onAction = {
                            onPromptChange(hint.take(6_000))
                            clipboardHint = null
                            onNotice("Teks ditempel dari clipboard")
                        },
                    )
                }
            }
        }

        item {
            KonteksCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Prompt kamu",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextBadge(label = "Wajib", tone = Tone.PRIMARY)
                }
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = { if (it.length <= 6_000) onPromptChange(it) },
                    placeholder = { Text("Tempel prompt yang kamu pakai di ChatGPT/Gemini/Claude…", fontSize = 12.5.sp) },
                    minLines = 6,
                    maxLines = 12,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            val text = readClipboard()
                            if (text.isBlank()) {
                                onNotice("Clipboard kosong")
                            } else {
                                onPromptChange(text.take(6_000))
                                onNotice("Teks ditempel dari clipboard")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tempel", fontSize = 11.5.sp)
                    }
                    if (state.prompt.isNotEmpty()) {
                        TextButton(onClick = onClearPrompt, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.danger)
                            Spacer(Modifier.width(4.dp))
                            Text("Kosongkan", fontSize = 11.5.sp, color = colors.danger)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${state.prompt.length}/6000",
                        color = if (state.prompt.length >= 10) colors.inkMuted else colors.danger,
                        fontFamily = IbmPlexMono,
                        fontSize = 10.sp,
                    )
                }
                if (state.prompt.isNotEmpty() && state.prompt.length < 10) {
                    Text(
                        "Minimal 10 karakter agar bisa dianalisis.",
                        color = colors.danger,
                        fontFamily = PlusJakarta,
                        fontSize = 10.5.sp,
                    )
                }
            }
        }

        item {
            KonteksCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Jawaban AI",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextBadge(label = "Opsional", tone = Tone.NEUTRAL)
                }
                Text(
                    "Tempel jawaban yang kamu dapat untuk dicek kesesuaiannya.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = state.output,
                    onValueChange = { if (it.length <= 12_000) onOutputChange(it) },
                    placeholder = { Text("Tempel jawaban AI di sini (boleh kosong)…", fontSize = 12.5.sp) },
                    minLines = 5,
                    maxLines = 10,
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = konteksFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            val text = readClipboard()
                            if (text.isBlank()) {
                                onNotice("Clipboard kosong")
                            } else {
                                onOutputChange(text.take(12_000))
                                onNotice("Jawaban ditempel dari clipboard")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tempel", fontSize = 11.5.sp)
                    }
                    if (state.output.isNotEmpty()) {
                        TextButton(onClick = onClearOutput, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.danger)
                            Spacer(Modifier.width(4.dp))
                            Text("Kosongkan", fontSize = 11.5.sp, color = colors.danger)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${state.output.length}/12000",
                        color = colors.inkMuted,
                        fontFamily = IbmPlexMono,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            KonteksCard(contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleBrief() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Brief / persyaratan tugas",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Opsional. Setiap baris jadi persyaratan yang dicek.",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                        )
                    }
                    TextBadge(label = "Pro", tone = Tone.WARNING)
                    Icon(
                        if (state.showBrief) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = colors.inkMuted,
                    )
                }
                if (state.showBrief) {
                    HorizontalDivider(color = colors.line)
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = state.brief,
                            onValueChange = { if (it.length <= 20_000) onBriefChange(it) },
                            placeholder = { Text("Contoh:\n- Wajib memakai tabel harga\n- Sertakan sumber resmi untuk setiap angka", fontSize = 12.sp) },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(Dimens.radiusSm),
                            colors = konteksFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${state.brief.length}/20000 karakter • coverage butuh server AI",
                            color = colors.inkMuted,
                            fontFamily = IbmPlexMono,
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }

        item {
            KonteksCard(contentPadding = PaddingValues(0.dp)) {
                ToggleRow(
                    label = "Simpan teks di riwayat",
                    description = "Mati = prompt/jawaban tidak disimpan ke perangkat setelah sesi.",
                    checked = state.saveTextHistory,
                    onCheckedChange = onSaveTextChange,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Kedalaman analisis",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                SegmentedControl(
                    options = listOf(CheckDepth.FAST to "Cek Cepat (gratis)", CheckDepth.DEEP to "Mendalam (Pro)"),
                    selected = state.depth,
                    onSelect = { depth ->
                        if (depth == CheckDepth.DEEP) {
                            showDeepInfo = true
                        } else {
                            onDepthChange(depth)
                        }
                    },
                )
                Text(
                    "Cek Cepat memakai model ringkas. Cek Mendalam (file/brief + model kuat) direncanakan untuk Pro.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        item {
            PrimaryButton(
                text =
                    when {
                        state.loading -> "Menganalisis…"
                        !state.serverConfigured -> "Cek dengan lint lokal"
                        else -> "Cek & Perbaiki"
                    },
                onClick = onAnalyze,
                icon = Icons.Rounded.Bolt,
                enabled = state.prompt.trim().length >= 10 && !state.loading,
                loading = state.loading,
            )
        }

        if (state.loading) {
            item {
                KonteksCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                state.stage.ifBlank { "Menganalisis…" },
                                color = colors.ink,
                                fontFamily = PlusJakarta,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Batas waktu 45 detik. Teks sudah disensor sebelum dikirim.",
                                color = colors.inkMuted,
                                fontFamily = PlusJakarta,
                                fontSize = 10.5.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SkeletonBlock(height = 54.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(height = 54.dp)
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoBanner(
                        title =
                            when (state.errorCode) {
                                "QUOTA_EXCEEDED" -> "Kuota habis"
                                "PRO_REQUIRED" -> "Butuh Pro"
                                "SCHEMA_TOO_NEW" -> "Perbarui aplikasi"
                                "INPUT_TOO_LARGE" -> "Teks terlalu panjang"
                                else -> "Belum berhasil"
                            },
                        body = message,
                        tone = if (state.errorCode == "QUOTA_EXCEEDED") Tone.WARNING else Tone.DANGER,
                        actionLabel = "Tutup",
                        onAction = onDismissError,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = "Coba lagi",
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Lint lokal",
                            onClick = onRunLocal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (!state.loading && state.errorMessage == null) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onRunLocal, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cek cepat dengan lint lokal (offline)", fontSize = 11.sp)
                    }
                }
            }
        }

        state.lastAnalysis?.let { last ->
            item {
                KonteksCard(onClick = onOpenLastAnalysis) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Analisis terakhir",
                                color = colors.ink,
                                fontFamily = PlusJakarta,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${last.result.score.overall}/100 · ${formatDate(last.createdAt)}",
                                color = colors.inkMuted,
                                fontFamily = IbmPlexMono,
                                fontSize = 10.5.sp,
                            )
                        }
                        TextBadge(
                            label = if (last.result.source.name == "LOCAL") "Lokal" else "AI",
                            tone = if (last.result.source.name == "LOCAL") Tone.NEUTRAL else Tone.PRIMARY,
                        )
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Saran ini dibuat otomatis dan perlu penilaianmu. Bukan jaminan kebenaran.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 9.5.sp,
                )
            }
        }
    }

    if (state.showConsent) {
        ConsentDialog(onAccept = onConsentAccept, onDecline = onConsentDecline)
    }
    state.privacyPreview?.let { preview ->
        PrivacyPreviewDialog(preview = preview, onConfirm = onPrivacyConfirm, onCancel = onPrivacyCancel)
    }
    state.pendingShared?.let { shared ->
        SharedInputDialog(
            shared = shared,
            onAuto = onSharedAuto,
            onPrompt = onSharedAsPrompt,
            onOutput = onSharedAsOutput,
            onDismiss = onSharedDismiss,
        )
    }
    if (showDeepInfo) {
        AlertDialog(
            onDismissRequest = { showDeepInfo = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = colors.accent) },
            title = { Text("Cek Mendalam belum aktif", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Cek Mendalam (model lebih kuat, file/brief, coverage) adalah fitur Pro yang belum tersedia di versi ini. Cek Cepat tetap gratis dan berfungsi.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                Button(onClick = { showDeepInfo = false }, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                    Text("Pakai Cek Cepat", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun ConsentDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    val colors = LocalKonteksColors.current
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = colors.surface,
        shape = RoundedCornerShape(Dimens.radiusLg),
        icon = { Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.success) },
        title = { Text("Sebelum cek dengan AI", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    "Untuk analisis AI, prompt dan jawaban yang sudah disensor akan dikirim ke server KonteksCek lalu diteruskan ke penyedia model AI sebagai pemroses.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    "Server tidak menyimpan isi teks. Riwayat tetap di perangkat. Kamu bisa menarik persetujuan kapan saja di Pengaturan.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
                TextBadge(label = "Data sensitif disensor dulu", tone = Tone.SUCCESS)
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) {
                Text("Setuju & lanjut", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Nanti") }
        },
    )
}

@Composable
private fun PrivacyPreviewDialog(
    preview: CheckPrivacyPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
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
                        modifier = Modifier.size(34.dp).background(colors.successSoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pratinjau yang akan dikirim",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Isi asli tidak ditampilkan; data sensitif sudah diganti.",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 10.5.sp,
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = colors.inkMuted)
                    }
                }
                HorizontalDivider(color = colors.line)
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Data sensitif terdeteksi",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        preview.findings.forEach { finding ->
                            TextBadge(label = "${finding.type} ×${finding.count}", tone = Tone.WARNING)
                        }
                    }
                    Text(
                        "Isi temuan tidak dikirim dan tidak ditampilkan. Periksa versi tersensor di bawah.",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                    )
                    HorizontalDivider(color = colors.line)
                    SanitizedBlock("Prompt", preview.sanitizedPrompt)
                    if (preview.sanitizedOutput.isNotBlank()) {
                        SanitizedBlock("Jawaban AI", preview.sanitizedOutput)
                    }
                    if (preview.sanitizedBrief.isNotBlank()) {
                        SanitizedBlock("Brief", preview.sanitizedBrief)
                    }
                }
                HorizontalDivider(color = colors.line)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(text = "Batal", onClick = onCancel, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Kirim yang disensor",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SanitizedBlock(label: String, text: String) {
    val colors = LocalKonteksColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            color = colors.inkSecondary,
            fontFamily = PlusJakarta,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text.ifBlank { "-" },
            color = colors.ink,
            fontFamily = IbmPlexMono,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceMuted, RoundedCornerShape(Dimens.radiusXs))
                    .padding(9.dp),
        )
    }
}

@Composable
private fun SharedInputDialog(
    shared: SharedInput,
    onAuto: () -> Unit,
    onPrompt: () -> Unit,
    onOutput: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.93f).heightIn(max = 620.dp),
            shape = RoundedCornerShape(Dimens.radiusXl),
            color = colors.surface,
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    "Teks dari aplikasi lain",
                    color = colors.ink,
                    fontFamily = PlusJakarta,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Tentukan bagian ini. Kamu bisa membatalkan tanpa mengubah apa pun.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                )
                Text(
                    shared.rawText.take(400) + if (shared.rawText.length > 400) "…" else "",
                    color = colors.inkSecondary,
                    fontFamily = IbmPlexMono,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceMuted, RoundedCornerShape(Dimens.radiusXs))
                            .padding(10.dp),
                )
                if (shared.hasAutoSplit) {
                    Text(
                        "Pola chat terdeteksi.",
                        color = colors.success,
                        fontFamily = PlusJakarta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!shared.detectedPrompt.isNullOrBlank()) {
                        Text(
                            "Prompt: ${shared.detectedPrompt.take(120)}",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 10.5.sp,
                            maxLines = 2,
                        )
                    }
                    if (!shared.detectedOutput.isNullOrBlank()) {
                        Text(
                            "Jawaban: ${shared.detectedOutput.take(120)}",
                            color = colors.inkMuted,
                            fontFamily = PlusJakarta,
                            fontSize = 10.5.sp,
                            maxLines = 2,
                        )
                    }
                }
                if (shared.hasAutoSplit) {
                    PrimaryButton(text = "Gunakan hasil pisah", onClick = onAuto)
                } else {
                    PrimaryButton(text = "Ini prompt", onClick = onPrompt)
                }
                SecondaryButton(
                    text = if (shared.hasAutoSplit) "Ini prompt saja" else "Ini jawaban AI",
                    onClick = if (shared.hasAutoSplit) onPrompt else onOutput,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shared.hasAutoSplit) {
                    SecondaryButton(
                        text = "Ini jawaban AI",
                        onClick = onOutput,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Batal")
                }
            }
        }
    }
}

private fun formatDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(value))
