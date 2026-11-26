package com.aldointelligence.kontekscek.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Token warna KonteksCek.
 *
 * Seluruh layar hanya membaca token ini lewat [LocalKonteksColors], sehingga
 * mode Terang/Gelap/Sistem bisa berganti tanpa menyentuh satu pun composable.
 * Token diadaptasi dari design system TokoJoJo Stock (apps/android).
 */
@Immutable
data class KonteksColors(
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceElevated: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val inkDisabled: Color,
    val onInverse: Color,
    val primary: Color,
    val primaryPressed: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val overlay: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
)

val LightKonteksColors =
    KonteksColors(
        canvas = Color(0xFFF4F7FB),
        surface = Color(0xFFFFFFFF),
        surfaceMuted = Color(0xFFEDF2F9),
        surfaceElevated = Color(0xFFFFFFFF),
        line = Color(0xFFE1E8F2),
        lineStrong = Color(0xFFCBD6E5),
        ink = Color(0xFF0B1220),
        inkSecondary = Color(0xFF475569),
        inkMuted = Color(0xFF71809A),
        inkDisabled = Color(0xFFA6B2C4),
        onInverse = Color(0xFFF8FAFC),
        primary = Color(0xFF0E7490),
        primaryPressed = Color(0xFF155E75),
        primarySoft = Color(0xFFE6F7FB),
        onPrimary = Color(0xFFFFFFFF),
        accent = Color(0xFF4F46E5),
        accentSoft = Color(0xFFEEF0FF),
        success = Color(0xFF047857),
        successSoft = Color(0xFFE7F8F0),
        warning = Color(0xFFB45309),
        warningSoft = Color(0xFFFEF4E4),
        danger = Color(0xFFB91C1C),
        dangerSoft = Color(0xFFFDECEC),
        info = Color(0xFF1D4ED8),
        infoSoft = Color(0xFFEAF1FF),
        overlay = Color(0x66101828),
        chart1 = Color(0xFF0E7490),
        chart2 = Color(0xFF4F46E5),
        chart3 = Color(0xFF047857),
        chart4 = Color(0xFFD97706),
        chart5 = Color(0xFF64748B),
    )

val DarkKonteksColors =
    KonteksColors(
        canvas = Color(0xFF070D19),
        surface = Color(0xFF0F1A2E),
        surfaceMuted = Color(0xFF0A1424),
        surfaceElevated = Color(0xFF162544),
        line = Color(0xFF1D2E4B),
        lineStrong = Color(0xFF2C4165),
        ink = Color(0xFFF8FAFC),
        inkSecondary = Color(0xFFB3C1D6),
        inkMuted = Color(0xFF8797B0),
        inkDisabled = Color(0xFF5A6A83),
        onInverse = Color(0xFF0B1220),
        primary = Color(0xFF22D3EE),
        primaryPressed = Color(0xFF67E8F9),
        primarySoft = Color(0xFF0A2A38),
        onPrimary = Color(0xFF04121C),
        accent = Color(0xFF8B93FF),
        accentSoft = Color(0xFF191F3F),
        success = Color(0xFF34D399),
        successSoft = Color(0xFF0B2B23),
        warning = Color(0xFFFBBF24),
        warningSoft = Color(0xFF33240A),
        danger = Color(0xFFFB7185),
        dangerSoft = Color(0xFF3A1220),
        info = Color(0xFF7CB0FF),
        infoSoft = Color(0xFF12233F),
        overlay = Color(0xB3000000),
        chart1 = Color(0xFF22D3EE),
        chart2 = Color(0xFF8B93FF),
        chart3 = Color(0xFF34D399),
        chart4 = Color(0xFFFBBF24),
        chart5 = Color(0xFF94A3B8),
    )
