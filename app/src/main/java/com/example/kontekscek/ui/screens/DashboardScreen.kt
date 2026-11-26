package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.aldointelligence.kontekscek.ui.components.DayActivity
import com.aldointelligence.kontekscek.ui.components.EmptyState
import com.aldointelligence.kontekscek.ui.components.HeroCard
import com.aldointelligence.kontekscek.ui.components.InfoBanner
import com.aldointelligence.kontekscek.ui.components.KonteksCard
import com.aldointelligence.kontekscek.ui.components.ModeBadge
import com.aldointelligence.kontekscek.ui.components.PrimaryButton
import com.aldointelligence.kontekscek.ui.components.QuickActionCard
import com.aldointelligence.kontekscek.ui.components.SecondaryButton
import com.aldointelligence.kontekscek.ui.components.SectionHeader
import com.aldointelligence.kontekscek.ui.components.StatTile
import com.aldointelligence.kontekscek.ui.components.TextBadge
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.components.WeeklyActivityChart
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import com.aldointelligence.kontekscek.ui.viewmodel.DashboardMetrics
import java.time.LocalTime

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    recentLogs: List<VerificationLogEntity>,
    onStartVerify: () -> Unit,
    onCreateCard: () -> Unit,
    onOpenLog: (VerificationLogEntity) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCards: () -> Unit,
) {
    val colors = LocalKonteksColors.current
    val greeting =
        when (LocalTime.now().hour) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    val avgMinutes = metrics.avgDurationSeconds / 60
    val avgRemSec = metrics.avgDurationSeconds % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = PaddingValues(start = Dimens.pagePadding, end = Dimens.pagePadding, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Text(
                        text = greeting,
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextBadge(label = "100% lokal", tone = Tone.SUCCESS)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pakai AI boleh. Pastikan cocok konteks, lalu simpan jejak verifikasinya dalam ≤ 5 menit.",
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.success, modifier = Modifier.size(13.dp))
                    Text(
                        "Tidak ada teks tugas yang dikirim keluar perangkat",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 10.5.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        text = "Mulai Cek AI",
                        onClick = onStartVerify,
                        icon = Icons.Rounded.Bolt,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Kartu",
                        onClick = onCreateCard,
                        icon = Icons.Rounded.AddCard,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (metrics.totalCards == 0) {
            item {
                InfoBanner(
                    title = "Belum ada Kartu Konteks",
                    body = "Buat kartu berisi 5 isian agar AI tahu tujuan, standar, dan data yang dilarang.",
                    tone = Tone.WARNING,
                    actionLabel = "Buat kartu pertama",
                    onAction = onCreateCard,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Ringkasan verifikasi", subtitle = "Diperbarui otomatis dari data lokal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        title = "Hari ini",
                        value = "${metrics.todayLogs}",
                        hint = "jejak verifikasi",
                        icon = Icons.AutoMirrored.Rounded.FactCheck,
                        accent = colors.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        title = "Total jejak",
                        value = "${metrics.totalLogs}",
                        hint = "${metrics.weekLogs} dalam 7 hari",
                        icon = Icons.Rounded.History,
                        accent = colors.accent,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        title = "Rata-rata durasi",
                        value = "${avgMinutes}m ${avgRemSec}d",
                        hint = "target ≤ 5 menit",
                        icon = Icons.Rounded.Timer,
                        accent = colors.info,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        title = "Isu tertangkap",
                        value = "${metrics.totalIssuesCaught}",
                        hint = "dari rule engine",
                        icon = Icons.Rounded.BugReport,
                        accent = colors.warning,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            KonteksCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Text(
                        "Aktivitas 7 hari",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextBadge(
                        label = if (metrics.streakDays > 0) "🔥 ${metrics.streakDays} hari beruntun" else "Belum ada streak",
                        tone = if (metrics.streakDays > 0) Tone.PRIMARY else Tone.NEUTRAL,
                    )
                }
                Spacer(Modifier.height(12.dp))
                WeeklyActivityChart(
                    days = metrics.weeklyActivity.map { DayActivity(it.label, it.count) },
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${metrics.weekLogs} sesi minggu ini",
                        color = colors.inkSecondary,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${metrics.underFiveMinRate}% selesai ≤ 5 menit",
                        color = colors.success,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Aksi cepat")
                QuickActionCard(
                    icon = Icons.Rounded.PlayArrow,
                    title = "Verifikasi output AI",
                    subtitle = "Tempel teks, cek 5 butir, ekspor PDF",
                    onClick = onStartVerify,
                )
                QuickActionCard(
                    icon = Icons.Rounded.CreditCard,
                    title = "Kelola Kartu Konteks",
                    subtitle = "Simpan brief, salin ke prompt dalam 1 klik",
                    onClick = onOpenCards,
                )
            }
        }

        item {
            SectionHeader(
                title = "Jejak terbaru",
                subtitle = "Ketuk kartu untuk membuka & menyunting",
                actionLabel = if (recentLogs.isNotEmpty()) "Lihat semua" else null,
                onAction = onOpenHistory,
            )
        }

        if (recentLogs.isEmpty()) {
            item {
                KonteksCard {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = "Belum ada jejak verifikasi",
                        body = "Selesaikan sesi cek pertama Anda; hasilnya otomatis tersimpan di sini.",
                        actionLabel = "Mulai sekarang",
                        onAction = onStartVerify,
                    )
                }
            }
        } else {
            items(recentLogs.take(3), key = { it.id }) { log ->
                RecentLogCard(log = log, onClick = { onOpenLog(log) })
            }
        }
    }
}

@Composable
private fun RecentLogCard(log: VerificationLogEntity, onClick: () -> Unit) {
    val colors = LocalKonteksColors.current
    KonteksCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeBadge(log.getTaskMode())
            Spacer(Modifier.weight(1f))
            Text(
                "${log.durationSeconds / 60}m ${log.durationSeconds % 60}d",
                color = colors.inkMuted,
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            log.cardTitle,
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Diubah: ${log.stageChanged.ifBlank { "belum ada catatan perubahan" }}",
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (log.issuesCaughtCount > 0) {
                TextBadge(label = "${log.issuesCaughtCount} isu tertangkap", tone = Tone.WARNING)
            } else {
                TextBadge(label = "Tanpa isu kritis", tone = Tone.SUCCESS)
            }
            if (log.finalConclusion.isNotBlank()) {
                TextBadge(label = "Ada kesimpulan", tone = Tone.INFO)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(26.dp).background(colors.surfaceMuted, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("→", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
