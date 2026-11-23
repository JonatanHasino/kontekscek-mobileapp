package com.aldointelligence.kontekscek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CyberColors {
    val DeepSpace = Color(0xFF070D19)      // Background utama futuristik
    val NavySurface = Color(0xFF0F1A2E)    // Kartu glassmorphic
    val NavyElevated = Color(0xFF162544)   // Permukaan elevasi
    val NeonCyan = Color(0xFF06B6D4)       // Aksen utama futuristik
    val ElectricBlue = Color(0xFF3B82F6)   // Identitas kepercayaan KonteksCek
    val VerifiedEmerald = Color(0xFF10B981)// Status Lolos / Centang
    val WarningAmber = Color(0xFFF59E0B)   // Status Diubah / Peringatan
    val AlertCoral = Color(0xFFF43F5E)     // Status Kritis / Data Terlarang
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val BorderGlow = Color(0x4406B6D4)
}

private val KonteksCekColorScheme = darkColorScheme(
    primary = CyberColors.NeonCyan,
    onPrimary = CyberColors.DeepSpace,
    secondary = CyberColors.VerifiedEmerald,
    onSecondary = CyberColors.DeepSpace,
    tertiary = CyberColors.ElectricBlue,
    background = CyberColors.DeepSpace,
    onBackground = CyberColors.TextPrimary,
    surface = CyberColors.NavySurface,
    onSurface = CyberColors.TextPrimary,
    surfaceVariant = CyberColors.NavyElevated,
    onSurfaceVariant = CyberColors.TextSecondary,
    error = CyberColors.AlertCoral
)

@Composable
fun KonteksCekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KonteksCekColorScheme,
        content = content
    )
}