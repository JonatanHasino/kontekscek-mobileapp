package com.aldointelligence.kontekscek.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.RuleAlert
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.ui.theme.Dimens
import com.aldointelligence.kontekscek.ui.theme.IbmPlexMono
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta

// =========================================================================
// Mode & status
// =========================================================================

fun TaskMode.icon(): ImageVector =
    when (this) {
        TaskMode.KULIAH -> Icons.Rounded.School
        TaskMode.KERJA -> Icons.Rounded.Work
        TaskMode.KLIEN -> Icons.Rounded.Handshake
    }

@Composable
fun TaskMode.accentColor(): Color {
    val colors = LocalKonteksColors.current
    return when (this) {
        TaskMode.KULIAH -> colors.primary
        TaskMode.KERJA -> colors.accent
        TaskMode.KLIEN -> colors.success
    }
}

enum class Tone { NEUTRAL, PRIMARY, SUCCESS, WARNING, DANGER, INFO }

@Composable
fun toneForeground(tone: Tone): Color {
    val colors = LocalKonteksColors.current
    return when (tone) {
        Tone.NEUTRAL -> colors.inkSecondary
        Tone.PRIMARY -> colors.primary
        Tone.SUCCESS -> colors.success
        Tone.WARNING -> colors.warning
        Tone.DANGER -> colors.danger
        Tone.INFO -> colors.info
    }
}

@Composable
fun toneBackground(tone: Tone): Color {
    val colors = LocalKonteksColors.current
    return when (tone) {
        Tone.NEUTRAL -> colors.surfaceMuted
        Tone.PRIMARY -> colors.primarySoft
        Tone.SUCCESS -> colors.successSoft
        Tone.WARNING -> colors.warningSoft
        Tone.DANGER -> colors.dangerSoft
        Tone.INFO -> colors.infoSoft
    }
}

@Composable
fun TextBadge(
    label: String,
    tone: Tone = Tone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = toneForeground(tone),
        fontFamily = PlusJakarta,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            modifier
                .background(toneBackground(tone), CircleShape)
                .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
fun ModeBadge(mode: TaskMode, modifier: Modifier = Modifier) {
    val accent = mode.accentColor()
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(mode.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Text(mode.label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChecklistStatusBadge(status: CheckStatus) {
    val (label, tone) =
        when (status) {
            CheckStatus.SUDAH -> "Sudah" to Tone.SUCCESS
            CheckStatus.DIUBAH -> "Diubah" to Tone.WARNING
            CheckStatus.BELUM -> "Belum" to Tone.DANGER
        }
    TextBadge(label = label, tone = tone)
}

@Composable
fun StatTile(
    title: String,
    value: String,
    hint: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    KonteksCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(30.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Text(
                text = title.uppercase(),
                color = colors.inkMuted,
                fontFamily = PlusJakarta,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            color = accent,
            fontFamily = PlusJakarta,
            fontSize = 21.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = hint,
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun StatusTriToggle(
    current: CheckStatus,
    onSelect: (CheckStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CheckStatus.entries.forEach { status ->
            val selected = current == status
            val accent = toneForeground(
                when (status) {
                    CheckStatus.SUDAH -> Tone.SUCCESS
                    CheckStatus.DIUBAH -> Tone.WARNING
                    CheckStatus.BELUM -> Tone.DANGER
                },
            )
            val icon =
                when (status) {
                    CheckStatus.SUDAH -> Icons.Rounded.CheckCircle
                    CheckStatus.DIUBAH -> Icons.Rounded.EditNote
                    CheckStatus.BELUM -> Icons.Rounded.RadioButtonUnchecked
                }
            Surface(
                shape = RoundedCornerShape(Dimens.radiusXs),
                color = if (selected) accent.copy(alpha = 0.14f) else colors.surfaceMuted,
                border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.55f) else colors.line),
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(status) },
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        contentDescription = status.label,
                        tint = if (selected) accent else colors.inkMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        status.label,
                        fontFamily = PlusJakarta,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) accent else colors.inkMuted,
                    )
                }
            }
        }
    }
}

// =========================================================================
// Container & layout
// =========================================================================

@Composable
fun KonteksCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalKonteksColors.current
    val shape = RoundedCornerShape(Dimens.radiusMd)
    val border = BorderStroke(1.dp, borderColor ?: colors.line)
    val cardColors = CardDefaults.cardColors(containerColor = colors.surface)
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, border = border, colors = cardColors) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            border = border,
            colors = cardColors,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Kartu gradien untuk hero/ringkasan, tetap membaca token tema. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalKonteksColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusXl),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.22f),
                                colors.accent.copy(alpha = 0.14f),
                                colors.surface,
                            ),
                        ),
                    )
                    .padding(contentPadding),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalKonteksColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = colors.primary,
                fontFamily = PlusJakarta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/** Judul langkah dengan nomor. */
@Composable
fun StepHeader(
    step: Int,
    title: String,
    icon: ImageVector,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalKonteksColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).background(colors.primarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$step",
                color = colors.primary,
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
        Text(
            text = title,
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(LocalKonteksColors.current.line))
}

// =========================================================================
// Tombol
// =========================================================================

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    container: Color? = null,
) {
    val colors = LocalKonteksColors.current
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
        shape = RoundedCornerShape(Dimens.radiusSm),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container ?: colors.primary,
                contentColor = colors.onPrimary,
            ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = colors.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, fontFamily = PlusJakarta, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = LocalKonteksColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Dimens.controlHeight),
        shape = RoundedCornerShape(Dimens.radiusSm),
        border = BorderStroke(1.dp, colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = colors.primary)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontFamily = PlusJakarta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SoftDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalKonteksColors.current
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Dimens.controlHeight),
        shape = RoundedCornerShape(Dimens.radiusSm),
        colors = ButtonDefaults.buttonColors(containerColor = colors.dangerSoft, contentColor = colors.danger),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontFamily = PlusJakarta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// =========================================================================
// Form
// =========================================================================

@Composable
fun FieldLabel(text: String, required: Boolean = false) {
    val colors = LocalKonteksColors.current
    Row(modifier = Modifier.padding(bottom = 5.dp)) {
        Text(
            text = text,
            color = colors.inkSecondary,
            fontFamily = PlusJakarta,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        if (required) {
            Text(" *", color = colors.danger, fontFamily = PlusJakarta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun konteksFieldColors(): TextFieldColors {
    val colors = LocalKonteksColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.lineStrong,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        disabledContainerColor = colors.surfaceMuted,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.inkMuted,
        focusedTextColor = colors.ink,
        unfocusedTextColor = colors.ink,
        cursorColor = colors.primary,
        focusedPlaceholderColor = colors.inkMuted,
        unfocusedPlaceholderColor = colors.inkMuted,
        errorBorderColor = colors.danger,
        errorLabelColor = colors.danger,
    )
}

@Composable
fun SettingsRow(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    control: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalKonteksColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = colors.inkMuted,
                fontFamily = PlusJakarta,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
        }
        control()
    }
}

@Composable
fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalKonteksColors.current
    SettingsRow(label = label, description = description) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.inkMuted,
                    uncheckedTrackColor = colors.surfaceMuted,
                    uncheckedBorderColor = colors.lineStrong,
                ),
        )
    }
}

@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surfaceMuted, RoundedCornerShape(Dimens.radiusSm))
                .border(1.dp, colors.line, RoundedCornerShape(Dimens.radiusSm))
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = selected == value
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            if (active) colors.surface else Color.Transparent,
                            RoundedCornerShape(Dimens.radiusXs),
                        ).clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) colors.primary else colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

// =========================================================================
// Feedback
// =========================================================================

@Composable
fun InfoBanner(
    title: String,
    body: String,
    tone: Tone = Tone.INFO,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalKonteksColors.current
    val foreground = toneForeground(tone)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(toneBackground(tone), RoundedCornerShape(Dimens.radiusSm))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(19.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = foreground,
                fontFamily = PlusJakarta,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                color = colors.inkSecondary,
                fontFamily = PlusJakarta,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    color = foreground,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onAction).padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun RuleAlertBanner(alert: RuleAlert) {
    val colors = LocalKonteksColors.current
    val (foreground, background, icon) =
        when (alert.severity) {
            AlertSeverity.CRITICAL -> Triple(colors.danger, colors.dangerSoft, Icons.Rounded.ErrorOutline)
            AlertSeverity.WARNING -> Triple(colors.warning, colors.warningSoft, Icons.Rounded.ErrorOutline)
            AlertSeverity.INFO -> Triple(colors.info, colors.infoSoft, Icons.Rounded.Info)
        }
    Surface(
        shape = RoundedCornerShape(Dimens.radiusSm),
        color = background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
            Column {
                Text(
                    alert.title,
                    color = foreground,
                    fontFamily = PlusJakarta,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    alert.detail,
                    color = colors.inkSecondary,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalKonteksColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(colors.surfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(21.dp))
        }
        Text(
            text = title,
            color = colors.ink,
            fontFamily = PlusJakarta,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            color = colors.inkMuted,
            fontFamily = PlusJakarta,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
        )
        if (actionLabel != null && onAction != null) {
            SecondaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = 6.dp).width(200.dp),
            )
        }
    }
}

@Composable
fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier, width: Dp? = null) {
    val colors = LocalKonteksColors.current
    Box(
        modifier =
            modifier
                .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
                .height(height)
                .background(colors.surfaceMuted, RoundedCornerShape(Dimens.radiusXs)),
    )
}

// =========================================================================
// Data visualisation
// =========================================================================

@Composable
fun ReadinessRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    stroke: Dp = 8.dp,
) {
    val colors = LocalKonteksColors.current
    val accent =
        when {
            score >= 80 -> colors.success
            score >= 50 -> colors.warning
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
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "% siap",
                color = colors.inkMuted,
                fontFamily = PlusJakarta,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

data class DayActivity(val label: String, val count: Int)

@Composable
fun WeeklyActivityChart(
    days: List<DayActivity>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val fraction = day.count.toFloat() / max
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (day.count > 0) {
                        Text(
                            text = "${day.count}",
                            color = colors.inkSecondary,
                            fontFamily = IbmPlexMono,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height((6 + 56 * fraction).dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (day.count == 0) colors.surfaceMuted else colors.primary.copy(alpha = 0.85f),
                                ),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Dp = 7.dp,
) {
    val colors = LocalKonteksColors.current
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height).clip(CircleShape),
        color = color ?: colors.primary,
        trackColor = colors.surfaceMuted,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun NumericText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalKonteksColors.current.ink,
    size: Int = 14,
    weight: FontWeight = FontWeight.SemiBold,
) {
    Text(
        text = text,
        color = color,
        fontFamily = IbmPlexMono,
        fontSize = size.sp,
        fontWeight = weight,
        modifier = modifier,
    )
}

// =========================================================================
// Top bar & bottom bar helpers
// =========================================================================

@Composable
fun KonteksTopBar(
    title: String,
    subtitle: String,
    statusLabel: String,
    statusTone: Tone = Tone.SUCCESS,
    onStatusClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalKonteksColors.current
    Surface(color = colors.surface) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(66.dp)
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = colors.ink)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.ink,
                        fontFamily = PlusJakarta,
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text = subtitle,
                        color = colors.inkMuted,
                        fontFamily = PlusJakarta,
                        fontSize = 11.5.sp,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = toneBackground(statusTone),
                    modifier = Modifier.then(
                        if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).background(toneForeground(statusTone), CircleShape),
                        )
                        Text(
                            text = statusLabel,
                            color = toneForeground(statusTone),
                            fontFamily = PlusJakarta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (actionIcon != null && onAction != null) {
                    IconButton(onClick = onAction) {
                        Icon(
                            actionIcon,
                            contentDescription = actionDescription,
                            tint = colors.ink,
                        )
                    }
                }
            }
            DividerLine()
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKonteksColors.current
    KonteksCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(colors.primarySoft, RoundedCornerShape(Dimens.radiusSm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.ink,
                    fontFamily = PlusJakarta,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    subtitle,
                    color = colors.inkMuted,
                    fontFamily = PlusJakarta,
                    fontSize = 11.5.sp,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKonteksColors.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusLg),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "Kartu Baru",
                fontFamily = PlusJakarta,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
