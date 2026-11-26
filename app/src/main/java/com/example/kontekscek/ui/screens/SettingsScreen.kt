package com.aldointelligence.kontekscek.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.KonteksSettings
import com.aldointelligence.kontekscek.data.TextSize
import com.aldointelligence.kontekscek.data.ThemeMode
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.components.*
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta

@Composable
fun SettingsScreen(
    settings: KonteksSettings,
    totalCards: Int,
    totalLogs: Int,
    onTheme: (ThemeMode) -> Unit,
    onTextSize: (TextSize) -> Unit,
    onDefaultMode: (TaskMode) -> Unit,
    onAutoStartTimer: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (Uri) -> Unit,
    onPurgeAll: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    var confirmPurge by remember { mutableStateOf(false) }
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
                        "Pemeriksa konteks & pencatat jejak verifikasi AI",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
                TextBadge(label = "Lokal", tone = Tone.SUCCESS)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.line)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextBadge(label = "$totalCards kartu", tone = Tone.PRIMARY)
                TextBadge(label = "$totalLogs jejak", tone = Tone.INFO)
                TextBadge(label = "v1.1.0", tone = Tone.NEUTRAL)
            }
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
                Text(
                    "Pilihan “Besar” ikut memperbesar seluruh teks tanpa mengubah tata letak.",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        SettingsSection("Verifikasi") {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mode default sesi baru", color = colors.ink, fontFamily = PlusJakarta, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                SegmentedControl(
                    options = TaskMode.entries.map { it to it.label },
                    selected = settings.defaultTaskMode,
                    onSelect = onDefaultMode,
                )
            }
            HorizontalDivider(color = colors.line)
            ToggleRow(
                label = "Mulai stopwatch otomatis",
                description = "Timer berjalan saat sesi atau teks pertama masuk.",
                checked = settings.autoStartTimer,
                onCheckedChange = onAutoStartTimer,
            )
        }

        SettingsSection("Data & privasi") {
            SettingsRow(
                label = "Kelola Kartu Konteks",
                description = "$totalCards kartu tersimpan di perangkat",
                onClick = onOpenCards,
                control = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Riwayat jejak verifikasi",
                description = "$totalLogs jejak siap diekspor ke PDF",
                onClick = onOpenHistory,
                control = { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted) },
            )
            HorizontalDivider(color = colors.line)
            SettingsRow(
                label = "Ekspor cadangan JSON",
                description = "Bagikan semua kartu & jejak ke penyimpanan lain.",
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
                label = "Hapus semua riwayat",
                description = "Hak penghapusan data sesuai UU PDP.",
                onClick = { confirmPurge = true },
                control = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colors.danger) },
            )
        }

        InfoBanner(
            title = "Privasi sejak awal",
            body =
                "Analisis, penyimpanan, dan pembuatan PDF berjalan di perangkat. " +
                    "Tidak ada teks tugas, kartu, atau jejak yang dikirim ke server.",
            tone = Tone.SUCCESS,
            icon = Icons.Rounded.Lock,
        )

        SettingsSection("Tentang") {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "KonteksCek v1.1.0",
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tipografi Plus Jakarta Sans & IBM Plex Mono (SIL OFL). Ikon Material Symbols (Apache 2.0).",
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "PDF jejak mencatat proses pemeriksaan manusia, bukan jaminan kebenaran absolut atas keluaran AI.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        Text(
            "KonteksCek · local-first AI governance",
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 10.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
                    "Seluruh jejak verifikasi akan dihapus permanen. Kartu Konteks tetap tersimpan. " +
                        "Buat cadangan JSON terlebih dahulu bila masih dibutuhkan.",
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
