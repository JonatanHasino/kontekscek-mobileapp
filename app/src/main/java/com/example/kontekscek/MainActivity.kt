package com.aldointelligence.kontekscek

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aldointelligence.kontekscek.ui.components.KonteksTopBar
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.screens.ContextCardsScreen
import com.aldointelligence.kontekscek.ui.screens.DashboardScreen
import com.aldointelligence.kontekscek.ui.screens.HistoryLogsScreen
import com.aldointelligence.kontekscek.ui.screens.SettingsScreen
import com.aldointelligence.kontekscek.ui.screens.VerifySessionScreen
import com.aldointelligence.kontekscek.ui.theme.KonteksCekTheme
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import com.aldointelligence.kontekscek.ui.viewmodel.KonteksViewModel
import kotlinx.coroutines.launch
import java.io.File

private enum class BottomTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Beranda", Icons.Rounded.SpaceDashboard),
    CARDS("Kartu", Icons.Rounded.CreditCard),
    VERIFY("Cek AI", Icons.Rounded.VerifiedUser),
    HISTORY("Jejak", Icons.Rounded.History),
    MORE("Lainnya", Icons.Rounded.Tune),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: KonteksViewModel = viewModel()
            val settings by vm.settings.collectAsState()

            KonteksCekTheme(mode = settings.themeMode, textSize = settings.textSize) {
                KonteksCekRoot(
                    vm = vm,
                    onShareFile = ::shareFile,
                )
            }
        }
    }

    private fun shareFile(file: File, mime: String, subject: String) {
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    clipData = ClipData.newRawUri(subject, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val chooser =
                Intent.createChooser(shareIntent, subject).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(chooser)
        }.onFailure { err ->
            android.widget.Toast
                .makeText(this, "Gagal membagikan file: ${err.localizedMessage}", android.widget.Toast.LENGTH_LONG)
                .show()
        }
    }
}

@Composable
private fun KonteksCekRoot(
    vm: KonteksViewModel,
    onShareFile: (File, String, String) -> Unit,
) {
    val colors = LocalKonteksColors.current
    val cards by vm.cards.collectAsState()
    val logs by vm.logs.collectAsState()
    val metrics by vm.metrics.collectAsState()
    val session by vm.session.collectAsState()
    val settings by vm.settings.collectAsState()

    var activeTab by remember { mutableStateOf(BottomTab.DASHBOARD) }
    var createRequestKey by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val (title, subtitle) =
        when (activeTab) {
            BottomTab.DASHBOARD -> "KonteksCek" to "Pemeriksa konteks & jejak verifikasi AI"
            BottomTab.CARDS -> "Kartu Konteks" to "${cards.size} kartu tersimpan di perangkat"
            BottomTab.VERIFY -> "Ruang Verifikasi" to "Analisis lokal • target selesai ≤ 5 menit"
            BottomTab.HISTORY -> "Jejak Verifikasi" to "${logs.size} sesi tersimpan"
            BottomTab.MORE -> "Pengaturan" to "Tampilan, data, dan privasi"
        }

    val actionIcon: ImageVector? =
        when (activeTab) {
            BottomTab.DASHBOARD -> Icons.Rounded.AddCard
            BottomTab.CARDS -> Icons.Rounded.Add
            BottomTab.VERIFY -> Icons.Rounded.RestartAlt
            else -> null
        }

    Scaffold(
        containerColor = colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            KonteksTopBar(
                title = title,
                subtitle = subtitle,
                statusLabel = "Lokal",
                statusTone = Tone.SUCCESS,
                onStatusClick = { activeTab = BottomTab.MORE },
                actionIcon = actionIcon,
                actionDescription =
                    when (activeTab) {
                        BottomTab.DASHBOARD -> "Buat kartu"
                        BottomTab.CARDS -> "Tambah kartu"
                        BottomTab.VERIFY -> "Mulai sesi baru"
                        else -> null
                    },
                onAction =
                    when (activeTab) {
                        BottomTab.DASHBOARD -> {
                            {
                                createRequestKey++
                                activeTab = BottomTab.CARDS
                            }
                        }

                        BottomTab.CARDS -> {
                            { createRequestKey++ }
                        }

                        BottomTab.VERIFY -> {
                            { vm.startFreshSession() }
                        }

                        else -> null
                    },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                tab.label,
                                fontFamily = PlusJakarta,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primarySoft,
                                unselectedIconColor = colors.inkMuted,
                                unselectedTextColor = colors.inkMuted,
                            ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding), color = colors.canvas) {
            when (activeTab) {
                BottomTab.DASHBOARD -> {
                    DashboardScreen(
                        metrics = metrics,
                        recentLogs = logs,
                        onStartVerify = {
                            vm.startNewSessionWithCard(cards.firstOrNull())
                            activeTab = BottomTab.VERIFY
                        },
                        onCreateCard = {
                            createRequestKey++
                            activeTab = BottomTab.CARDS
                        },
                        onOpenLog = { log ->
                            vm.loadLogForEditing(log)
                            activeTab = BottomTab.VERIFY
                        },
                        onOpenHistory = { activeTab = BottomTab.HISTORY },
                        onOpenCards = { activeTab = BottomTab.CARDS },
                    )
                }

                BottomTab.CARDS -> {
                    ContextCardsScreen(
                        cards = cards,
                        createRequestKey = createRequestKey,
                        onSaveCard = vm::saveContextCard,
                        onDeleteCard = vm::deleteContextCard,
                        onDuplicateCard = vm::duplicateContextCard,
                        onUseForVerification = { card ->
                            vm.startNewSessionWithCard(card)
                            activeTab = BottomTab.VERIFY
                        },
                        onCopiedNotice = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        onCreateRequestHandled = { createRequestKey = 0 },
                    )
                }

                BottomTab.VERIFY -> {
                    VerifySessionScreen(
                        state = session,
                        availableCards = cards,
                        onSelectCard = vm::selectCardInSession,
                        onSelectMode = vm::selectModeInSession,
                        onOutputChange = vm::updateAiOutput,
                        onChecklistChange = vm::updateChecklistStatus,
                        onStagesChange = vm::updateStages,
                        onToggleTimer = vm::toggleTimer,
                        onImportPdfUri = vm::importPdfContent,
                        onSaveAndExportPdf = {
                            vm.saveVerificationAndExportPdf(
                                onPdfReady = { file ->
                                    onShareFile(file, "application/pdf", "Jejak Verifikasi AI - KonteksCek")
                                },
                            )
                        },
                        onNewSession = vm::startFreshSession,
                        onResetChecklist = vm::resetChecklist,
                        onClearOutput = vm::clearAiOutput,
                        onCopiedNotice = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                    )
                }

                BottomTab.HISTORY -> {
                    HistoryLogsScreen(
                        logs = logs,
                        onEditLog = { log ->
                            vm.loadLogForEditing(log)
                            activeTab = BottomTab.VERIFY
                        },
                        onExportPdf = { log ->
                            vm.exportExistingLogPdf(
                                log,
                                onPdfReady = { file ->
                                    onShareFile(file, "application/pdf", "Jejak Verifikasi AI - KonteksCek")
                                },
                            )
                        },
                        onDeleteLog = vm::deleteLog,
                        onPurgeAll = vm::purgeAllData,
                        onReuseLog = { log ->
                            vm.reuseLogAsNewSession(log)
                            activeTab = BottomTab.VERIFY
                        },
                    )
                }

                BottomTab.MORE -> {
                    SettingsScreen(
                        settings = settings,
                        totalCards = cards.size,
                        totalLogs = logs.size,
                        onTheme = vm::setThemeMode,
                        onTextSize = vm::setTextSize,
                        onDefaultMode = vm::setDefaultTaskMode,
                        onAutoStartTimer = vm::setAutoStartTimer,
                        onExportBackup = {
                            vm.exportBackup { file ->
                                onShareFile(file, "application/json", "Cadangan Data KonteksCek")
                            }
                        },
                        onImportBackup = { uri: Uri -> vm.importBackup(uri) },
                        onPurgeAll = vm::purgeAllData,
                        onOpenCards = { activeTab = BottomTab.CARDS },
                        onOpenHistory = { activeTab = BottomTab.HISTORY },
                    )
                }
            }
        }
    }
}
