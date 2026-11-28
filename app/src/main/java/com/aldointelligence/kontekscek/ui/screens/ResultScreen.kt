package com.aldointelligence.kontekscek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.domain.model.AnalysisResult
import com.aldointelligence.kontekscek.domain.model.AnalysisSource
import com.aldointelligence.kontekscek.domain.model.IssueSource
import com.aldointelligence.kontekscek.ui.components.AnswersRequestBadge
import com.aldointelligence.kontekscek.ui.components.CoverageRow
import com.aldointelligence.kontekscek.ui.components.IssueCard
import com.aldointelligence.kontekscek.ui.components.InfoBanner
import com.aldointelligence.kontekscek.ui.components.KonteksCard
import com.aldointelligence.kontekscek.ui.components.PrimaryButton
import com.aldointelligence.kontekscek.ui.components.PromptScoreRing
import com.aldointelligence.kontekscek.ui.components.ProgressBar
import com.aldointelligence.kontekscek.ui.components.RewrittenPromptCard
import com.aldointelligence.kontekscek.ui.components.SecondaryButton
import com.aldointelligence.kontekscek.ui.components.SectionHeader
import com.aldointelligence.kontekscek.ui.components.TextBadge
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta

@Composable
fun ResultScreen(
    result: AnalysisResult,
    prompt: String,
    output: String,
    brief: String,
    previousScore: Int?,
    errorMessage: String?,
    onBack: () -> Unit,
    onUseRewritten: () -> Unit,
    onRerunWithAnswers: (Map<String, String>) -> Unit,
    onRunLocal: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onShareText: (String) -> Unit,
    onNotice: (String) -> Unit,
) {
    val colors = LocalKonteksColors.current
    val clipboard = LocalClipboardManager.current
    var showDimensions by remember(result.score.overall) { mutableStateOf(false) }
    val answers = remember(result.score.overall) { mutableStateMapOf<String, String>() }
    val delta = previousScore?.let { result.score.overall - it }

    fun copy(text: String, label: String) {
        if (text.isBlank()) return
        clipboard.setText(AnnotatedString(text))
        onNotice("$label disalin")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = colors.ink)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hasil analisis",
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Skor prompt, masalah, dan prompt perbaikan",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.sp,
                    )
                }
                TextBadge(
                    label = if (result.source == AnalysisSource.REMOTE) "Analisis AI" else "Lint lokal",
                    tone = if (result.source == AnalysisSource.REMOTE) Tone.PRIMARY else Tone.NEUTRAL,
                )
            }
        }

        if (result.source == AnalysisSource.LOCAL) {
            item {
                InfoBanner(
                    title = "Hasil lint lokal",
                    body = result.sourceNote ?: "Analisis AI butuh server. Hasil ini dibuat di perangkat.",
                    tone = Tone.WARNING,
                    icon = Icons.Rounded.Security,
                    actionLabel = "Coba server lagi",
                    onAction = onRetry,
                )
            }
        }

        item {
            KonteksCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PromptScoreRing(score = result.score.overall, label = result.scoreLabel, size = 116.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Skor prompt",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        delta?.let {
                            TextBadge(
                                label = if (it >= 0) "Naik +$it dari cek sebelumnya" else "Turun $it dari cek sebelumnya",
                                tone = if (it >= 0) Tone.SUCCESS else Tone.DANGER,
                            )
                        }
                        if (delta == null) {
                            Text(
                                "Skor 0–100 dari 10 dimensi rubrik.",
                                color = colors.inkMuted,
                                fontFamily = PlusJakarta,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextBadge(
                                label =
                                    when (result.confidence) {
                                        com.aldointelligence.kontekscek.domain.model.AnalysisConfidence.LOW -> "Keyakinan rendah"
                                        com.aldointelligence.kontekscek.domain.model.AnalysisConfidence.HIGH -> "Keyakinan tinggi"
                                        else -> "Keyakinan sedang"
                                    },
                                tone = Tone.NEUTRAL,
                            )
                            TextBadge(label = result.taskType.replaceFirstChar { it.uppercase() }, tone = Tone.INFO)
                        }
                        result.quota?.let { quota ->
                            Text(
                                "Sisa kuota hari ini: ${quota.remainingToday}",
                                color = colors.inkMuted,
                                fontFamily = IbmPlexMono,
                                fontSize = 9.5.sp,
                            )
                        }
                    }
                }
                TextButton(onClick = { showDimensions = !showDimensions }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(
                        if (showDimensions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showDimensions) "Sembunyikan rincian skor" else "Lihat rincian 10 dimensi", fontSize = 11.sp)
                }
                if (showDimensions) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.score.dimensions.forEach { dimension ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        dimension.label,
                                        color = colors.inkSecondary,
                                        fontFamily = PlusJakarta,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${dimension.score}/10",
                                        color = colors.inkMuted,
                                        fontFamily = IbmPlexMono,
                                        fontSize = 10.sp,
                                    )
                                }
                                ProgressBar(
                                    progress = dimension.score / 10f,
                                    color =
                                        when {
                                            dimension.score >= 8 -> colors.success
                                            dimension.score >= 5 -> colors.primary
                                            else -> colors.warning
                                        },
                                    height = 5.dp,
                                )
                                if (dimension.note.isNotBlank()) {
                                    Text(
                                        dimension.note,
                                        color = colors.inkMuted,
                                        fontFamily = PlusJakarta,
                                        fontSize = 9.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        errorMessage?.let {
            item {
                InfoBanner(
                    title = "Catatan",
                    body = it,
                    tone = Tone.DANGER,
                    actionLabel = "Tutup",
                    onAction = onDismissError,
                )
            }
        }

        item {
            RewrittenPromptCard(
                rewrittenPrompt = result.rewrittenPrompt,
                onCopy = { copy(result.rewrittenPrompt, "Prompt perbaikan") },
                onShare = { onShareText(result.rewrittenPrompt) },
                onUseAndRetry = onUseRewritten,
            )
        }

        item {
            SectionHeader(
                title = "Masalah ditemukan",
                subtitle = if (result.issues.isEmpty()) "Tidak ada masalah besar" else "${result.issues.size} temuan, diurutkan dari yang paling penting",
            )
        }
        if (result.issues.isEmpty()) {
            item {
                InfoBanner(
                    title = "Prompt sudah cukup rapi",
                    body = "Tetap cek fakta dan sumber sebelum memakai hasil AI.",
                    tone = Tone.SUCCESS,
                )
            }
        } else {
            result.issues.forEach { issue ->
                item {
                    IssueCard(
                        issue = issue,
                        sourceText =
                            when (issue.source) {
                                IssueSource.OUTPUT -> output
                                IssueSource.FILE -> brief
                                else -> prompt
                            },
                    )
                }
            }
        }

        result.outputCheck?.let { check ->
            item {
                SectionHeader(
                    title = "Cek jawaban AI",
                    subtitle = "Apakah jawaban benar-benar menjawab permintaan?",
                )
            }
            item {
                KonteksCard {
                    AnswersRequestBadge(check.answersRequest)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        check.summary,
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                    if (check.mismatches.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        check.mismatches.forEach { mismatch ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Text("•", color = colors.warning, fontFamily = PlusJakarta, fontSize = 12.sp)
                                Text(
                                    mismatch.text,
                                    color = colors.inkSecondary,
                                    fontFamily = PlusJakarta,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (check.followUpPrompt.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.line)
                        Spacer(Modifier.height(9.dp))
                        Text(
                            "Prompt tindak lanjut",
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            check.followUpPrompt,
                            color = colors.inkSecondary,
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .background(colors.surfaceMuted, androidx.compose.foundation.shape.RoundedCornerShape(Dimens.radiusXs))
                                    .padding(9.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton(
                                text = "Salin",
                                onClick = { copy(check.followUpPrompt, "Prompt tindak lanjut") },
                                icon = Icons.Rounded.ContentCopy,
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = "Bagikan",
                                onClick = { onShareText(check.followUpPrompt) },
                                icon = Icons.Rounded.IosShare,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (result.requirementCoverage.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Requirement coverage",
                    subtitle = "Kecocokan jawaban dengan brief/persyaratan",
                )
            }
            item {
                KonteksCard {
                    result.requirementCoverage.forEach { coverage ->
                        CoverageRow(coverage)
                    }
                }
            }
        }

        if (result.questionsToUser.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Biar prompt makin tepat",
                    subtitle = "Jawab cepat, lalu perbaiki lagi",
                )
            }
            item {
                KonteksCard {
                    result.questionsToUser.forEach { question ->
                        Text(
                            question.question,
                            color = colors.ink,
                            fontFamily = PlusJakarta,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            question.options.forEach { option ->
                                FilterChip(
                                    selected = answers[question.id] == option,
                                    onClick = {
                                        if (answers[question.id] == option) {
                                            answers.remove(question.id)
                                        } else {
                                            answers[question.id] = option
                                        }
                                    },
                                    label = { Text(option, fontSize = 10.5.sp) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Perbaiki lagi dengan jawabanku",
                        onClick = { onRerunWithAnswers(answers.toMap()) },
                        enabled = answers.isNotEmpty(),
                        icon = Icons.Rounded.PlayArrow,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = "Ubah input",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Lint lokal",
                    onClick = onRunLocal,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceMuted, androidx.compose.foundation.shape.RoundedCornerShape(Dimens.radiusSm))
                        .padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(15.dp))
                    Text(
                        "Saran dibuat otomatis dan bisa salah. Kamu tetap pengambil keputusan; cek sumber dan fakta sebelum dipakai. Bukan jaminan kebenaran absolut.",
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}
