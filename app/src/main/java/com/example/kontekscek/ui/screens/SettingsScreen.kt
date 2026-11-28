package com.aldointelligence.kontekscek.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.KonteksSettings
import com.aldointelligence.kontekscek.data.TextSize
import com.aldointelligence.kontekscek.data.ThemeMode
import com.aldointelligence.kontekscek.ui.components.InfoBanner
import com.aldointelligence.kontekscek.ui.components.KonteksCard
import com.aldointelligence.kontekscek.ui.components.SegmentedControl
import com.aldointelligence.kontekscek.ui.components.SettingsRow
import com.aldointelligence.kontekscek.ui.components.TextBadge
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.components.konteksFieldColors
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta

@Composable
fun SettingsScreen(
    settings: KonteksSettings,
    serverUrl: String,
    consentGiven: Boolean,
    totalCards: Int,
    totalLogs: Int,
    onTheme: (ThemeMode) -> Unit,
    onTextSize: (TextSize) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onWithdrawConsent: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenLegacyHistory: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (Uri) -> Unit,
    onPurgeAll: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    var confirmPurge by remember { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let(onImportBackup)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.canvas)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KonteksCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(46.dp).background(colors.primarySoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "KonteksCek",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Prompt Coach — cek & perbaiki prompt AI",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
                TextBadge(label = "Hybrid", tone = Tone.PRIMARY)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.line)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextBadge(label = "$totalCards preset", tone = Tone.PRIMARY)
                TextBadge(label = "$totalLogs arsip", tone = Tone.INFO)
                TextBadge(label = "v2.0.0-beta1", tone = Tone.NEUTRAL)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cek AI mengirim teks yang sudah disensor ke server untuk dianalisis. Server tidak menyimpan isinya. Mode Manual dan riwayat tetap di perangkat.",
                color = colors.inkSecondary,
                fontFamily = PlusJakarta,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }

        SettingsSection("Tampilan") {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tema aplikasi", color = colors.ink, fontFamily = PlusJakarta, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                SegmentedControl(
                    options =
                        listOf(
                            ThemeMode.LIGHT to "Terang",
                            ThemeMode.DARK to "Gelap",
                            ThemeMode.SYSTEM to "Sistem",
                        ),
                    selected = settings.themeMode,
                    onSelect = onTheme,
                )
                Text("Ukuran teks", color = colors.ink, fontFamily = PlusJakarta, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                SegmentedControl(
                    options = listOf(TextSize.NORMAL to "Normal", TextSize.LARGE to "Besar"),
                    selected = settings.textSize,
                    onSelect = onTextSize,
                )
            }
        }

        SettingsSection("Cek AI & privasi") {
            SettingsRow(
                label = "Server analisis",
                description = serverUrl.ifBlank { "Belum diatur — memakai lint lokal" },
                onClick = { showServerDialog = true },
                control = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.primary) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Persetujuan analisis AI",
                description =
                    if (consentGiven) {
                        "Aktif. Kamu bisa menariknya kapan saja."
                    } else {
                        "Akan diminta saat kamu menjalankan cek pertama."
                    },
                onClick = if (consentGiven) {
                    { confirmWithdraw = true }
                } else {
                    null
                },
                control = {
                    TextBadge(
                        label = if (consentGiven) "Aktif" else "Belum",
                        tone = if (consentGiven) Tone.SUCCESS else Tone.WARNING,
                    )
                },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Data sensitif",
                description = "API key, password, email, nomor HP, kartu/NIK disensor di perangkat sebelum dikirim.",
                control = { Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.success) },
            )
        }

        InfoBanner(
            title = "Privasi hybrid",
            body =
                "Server hanya menerima teks tersensor dan tidak menyimpan isinya. Penyedia model AI memproses teks tersebut sesuai kebijakan mereka. " +
                    "Kamu bisa memakai Mode Manual atau lint lokal tanpa mengirim apa pun.",
            tone = Tone.SUCCESS,
            icon = Icons.Rounded.Lock,
        )

        SettingsSection("Mode & data lama") {
            SettingsRow(
                label = "Mode Manual (lokal penuh)",
                description = "Checklist, stopwatch, dan PDF jejak 100% di perangkat.",
                onClick = onOpenManual,
                control = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = colors.primary) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Preset Konteks",
                description = "$totalCards kartu konteks untuk brief & data terlarang.",
                onClick = onOpenCards,
                control = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Beranda & statistik lama",
                description = "Dashboard jejak verifikasi mode manual.",
                onClick = onOpenDashboard,
                control = { Icon(Icons.Rounded.SpaceDashboard, contentDescription = null, tint = colors.inkMuted) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Arsip jejak lama",
                description = "$totalLogs sesi manual tersimpan.",
                onClick = onOpenLegacyHistory,
                control = { Icon(Icons.Rounded.History, contentDescription = null, tint = colors.inkMuted) },
            )
        }

        SettingsSection("Data & cadangan") {
            SettingsRow(
                label = "Ekspor cadangan JSON",
                description = "Kartu konteks & arsip jejak lama.",
                onClick = onExportBackup,
                control = { Icon(Icons.Rounded.IosShare, contentDescription = null, tint = colors.primary) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Impor cadangan JSON",
                description = "Pulihkan data dari file cadangan KonteksCek.",
                onClick = { importLauncher.launch("*/*") },
                control = { Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = colors.accent) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Hapus semua data lokal",
                description = "Kartu, arsip, analisis terakhir, draf, cache PDF/backup.",
                onClick = { confirmPurge = true },
                control = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colors.danger) },
            )
        }

        SettingsSection("Tentang") {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "KonteksCek 2.0.0-beta1",
                    color = colors.ink,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Dikembangkan oleh 3SEMA “AI” (Aldo Intelligence), Program Studi Teknologi Informasi, Universitas Internasional Batam.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
                Text(
                    "Tipografi Plus Jakarta Sans & IBM Plex Mono (SIL OFL). Ikon Material Symbols (Apache 2.0).",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
                Text(
                    "Saran dibuat otomatis dan bisa salah. Bukan pendeteksi tulisan AI dan bukan jaminan kebenaran.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        Text(
            "KonteksCek · prompt coach, bukan pendeteksi AI",
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 10.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (showServerDialog) {
        var draft by remember { mutableStateOf(serverUrl) }
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            title = { Text("Server analisis AI", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Contoh: https://api.domainmu.com atau http://10.0.2.2:8080 untuk backend Ktor di emulator (hanya build debug).",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        colors = konteksFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Kosongkan untuk mode lint lokal (tanpa server).",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 10.5.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onServerUrlChange(draft.trim())
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Batal") }
            },
        )
    }

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            title = { Text("Tarik persetujuan AI?", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Analisis AI akan berhenti sampai kamu memberi persetujuan lagi. Lint lokal dan Mode Manual tetap bisa dipakai.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWithdrawConsent()
                        confirmWithdraw = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = colors.onInverse),
                ) {
                    Text("Tarik persetujuan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWithdraw = false }) { Text("Batal") }
            },
        )
    }

    if (confirmPurge) {
        AlertDialog(
            onDismissRequest = { confirmPurge = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(Dimens.radiusLg),
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colors.danger) },
            title = { Text("Hapus Semua Data Lokal?", fontFamily = PlusJakarta, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Kartu konteks, arsip jejak, analisis terakhir, draf, serta cache PDF/backup akan dihapus permanen. " +
                        "Tema dan pengaturan tetap disimpan. Buat cadangan JSON dulu bila masih dibutuhkan.",
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
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalKonteksColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        KonteksCard(contentPadding = PaddingValues(0.dp)) { content() }
    }
}
