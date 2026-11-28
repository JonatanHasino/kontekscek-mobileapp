package com.aldointelligence.kontekscek.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.domain.model.AnswersRequest
import com.aldointelligence.kontekscek.domain.model.CoverageStatus
import com.aldointelligence.kontekscek.domain.model.Issue
import com.aldointelligence.kontekscek.domain.model.RequirementCoverage
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta

@Composable
fun PromptScoreRing(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 118.dp,
    stroke: Dp = 10.dp,
) {
    val colors = LocalKonteksColors.current
    val accent =
        when {
            score >= 80 -> colors.success
            score >= 60 -> colors.primary
            score >= 40 -> colors.warning
            else -> colors.danger
        }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            drawArc(
                color = colors.line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - strokePx, this.size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - strokePx, this.size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                color = accent,
                fontFamily = PlusJakarta,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "/100",
                color = colors.inkMuted,
                fontFamily = IbmPlexMono,
                fontSize = 10.sp,
            )
            Text(
                text = label,
                color = colors.inkSecondary,
                fontFamily = PlusJakarta,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun IssueCard(
    issue: Issue,
    sourceText: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    val accent =
        when (issue.severity) {
            AlertSeverity.CRITICAL -> colors.danger
            AlertSeverity.WARNING -> colors.warning
            AlertSeverity.INFO -> colors.info
        }
    val icon =
        when (issue.severity) {
            AlertSeverity.CRITICAL -> Icons.Rounded.WarningAmber
            AlertSeverity.WARNING -> Icons.Rounded.WarningAmber
            AlertSeverity.INFO -> Icons.Rounded.WarningAmber
        }
    KonteksCard(
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.35f),
        contentPadding = PaddingValues(13.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(accent.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextBadge(
                        label = issue.category.label,
                        tone =
                            when (issue.severity) {
                                AlertSeverity.CRITICAL -> Tone.DANGER
                                AlertSeverity.WARNING -> Tone.WARNING
                                AlertSeverity.INFO -> Tone.INFO
                            },
                    )
                    TextBadge(
                        label =
                            when (issue.severity) {
                                AlertSeverity.CRITICAL -> "Kritis"
                                AlertSeverity.WARNING -> "Perhatian"
                                AlertSeverity.INFO -> "Info"
                            },
                        tone =
                            when (issue.severity) {
                                AlertSeverity.CRITICAL -> Tone.DANGER
                                AlertSeverity.WARNING -> Tone.WARNING
                                AlertSeverity.INFO -> Tone.NEUTRAL
                            },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    issue.title,
                    color = colors.ink,
                    fontFamily = PlusJakarta,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                )
                if (issue.why.isNotBlank()) {
                    Text(
                        issue.why,
                        color = colors.inkSecondary,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (!issue.evidenceQuote.isNullOrBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceMuted, RoundedCornerShape(Dimens.radiusXs))
                                .padding(8.dp),
                    ) {
                        Text(
                            text = highlightedEvidence(sourceText, issue.evidenceQuote, accent),
                            color = colors.inkSecondary,
                            fontFamily = IbmPlexMono,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
                if (issue.fix.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            "Saran",
                            color = accent,
                            fontFamily = PlusJakarta,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            issue.fix,
                            color = colors.inkSecondary,
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RewrittenPromptCard(
    rewrittenPrompt: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onUseAndRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    KonteksCard(
        modifier = modifier,
        borderColor = colors.primary.copy(alpha = 0.45f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TextBadge(label = "Prompt Perbaikan", tone = Tone.PRIMARY)
            Spacer(Modifier.weight(1f))
            Text(
                "${rewrittenPrompt.length} karakter",
                color = colors.inkMuted,
                fontFamily = IbmPlexMono,
                fontSize = 9.5.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            rewrittenPrompt.ifBlank { "Belum ada prompt perbaikan." },
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Salin",
                onClick = onCopy,
                icon = Icons.Rounded.ContentCopy,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Bagikan",
                onClick = onShare,
                icon = Icons.Rounded.IosShare,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Pakai & uji ulang",
            onClick = onUseAndRetry,
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AnswersRequestBadge(value: AnswersRequest) {
    val (label, tone) =
        when (value) {
            AnswersRequest.YES -> "Menjawab: Ya" to Tone.SUCCESS
            AnswersRequest.PARTIAL -> "Menjawab: Sebagian" to Tone.WARNING
            AnswersRequest.NO -> "Menjawab: Tidak" to Tone.DANGER
        }
    TextBadge(label = label, tone = tone)
}

@Composable
fun CoverageStatusBadge(status: CoverageStatus) {
    val (label, tone) =
        when (status) {
            CoverageStatus.MET -> "Terpenuhi" to Tone.SUCCESS
            CoverageStatus.PARTIAL -> "Sebagian" to Tone.WARNING
            CoverageStatus.MISSING -> "Belum" to Tone.DANGER
        }
    TextBadge(label = label, tone = tone)
}

@Composable
fun CoverageRow(item: RequirementCoverage) {
    val colors = LocalKonteksColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoverageStatusBadge(item.status)
        Text(
            item.requirement,
            color = colors.inkSecondary,
            fontFamily = PlusJakarta,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun QuotaChip(remainingToday: Int) {
    TextBadge(
        label = "Sisa hari ini: $remainingToday",
        tone = if (remainingToday <= 1) Tone.WARNING else Tone.NEUTRAL,
    )
}

private fun highlightedEvidence(
    source: String,
    quote: String,
    accent: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    val index = source.indexOf(quote, ignoreCase = true)
    if (index < 0) return AnnotatedString(quote)
    val start = (index - 70).coerceAtLeast(0)
    val end = (index + quote.length + 70).coerceAtMost(source.length)
    return buildAnnotatedString {
        if (start > 0) append("…")
        append(source.substring(start, index))
        withStyle(SpanStyle(background = accent.copy(alpha = 0.22f), color = accent, fontWeight = FontWeight.Bold)) {
            append(source.substring(index, index + quote.length))
        }
        append(source.substring(index + quote.length, end))
        if (end < source.length) append("…")
    }
}
