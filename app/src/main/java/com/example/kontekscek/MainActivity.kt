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
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aldointelligence.kontekscek.ui.components.KonteksTopBar
import com.aldointelligence.kontekscek.ui.components.Tone
import com.aldointelligence.kontekscek.ui.screens.CheckScreen
import com.aldointelligence.kontekscek.ui.screens.ContextCardsScreen
import com.aldointelligence.kontekscek.ui.screens.DashboardScreen
import com.aldointelligence.kontekscek.ui.screens.HistoryLogsScreen
import com.aldointelligence.kontekscek.ui.screens.ResultScreen
import com.aldointelligence.kontekscek.ui.screens.SettingsScreen
import com.aldointelligence.kontekscek.ui.screens.VerifySessionScreen
import com.aldointelligence.kontekscek.ui.theme.KonteksCekTheme
import com.aldointelligence.kontekscek.ui.theme.LocalKonteksColors
import com.aldointelligence.kontekscek.ui.theme.PlusJakarta
import com.aldointelligence.kontekscek.ui.viewmodel.CheckViewModel
import com.aldointelligence.kontekscek.ui.viewmodel.KonteksViewModel
import kotlinx.coroutines.launch
import java.io.File

private enum class BottomTab(val label: String, val icon: ImageVector, val inBar: Boolean = true) {
    CHECK("Cek", Icons.Rounded.AutoFixHigh),
    HISTORY("Riwayat", Icons.Rounded.History),
    MORE("Lainnya", Icons.Rounded.Tune),
    DASHBOARD("Beranda", Icons.Rounded.Home, inBar = false),
    CARDS("Preset", Icons.Rounded.CreditCard, inBar = false),
    VERIFY("Manual", Icons.Rounded.VerifiedUser, inBar = false),
}

class MainActivity : ComponentActivity() {

    private lateinit var checkVm: CheckViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        checkVm = ViewModelProvider(this)[CheckViewModel::class.java]
        if (savedInstanceState == null) {
            handleIncoming(intent)
        }

        setContent {
            val legacyVm: KonteksViewModel = viewModel()
            val settings by legacyVm.settings.collectAsState()

            KonteksCekTheme(mode = settings.themeMode, textSize = settings.textSize) {
                KonteksCekRoot(
                    legacyVm = legacyVm,
                    checkVm = checkVm,
                    onShareFile = ::shareFile,
                    onShareText = ::shareText,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(incoming: Intent?) {
        when (incoming?.action) {
            Intent.ACTION_SEND -> {
                val text =
                    incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                        ?: incoming.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    checkVm.onSharedText(text)
                }
                setIntent(Intent())
            }

            Intent.ACTION_PROCESS_TEXT -> {
                val text = incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!text.isNullOrBlank()) {
                    checkVm.onSharedText(text)
                }
                setIntent(Intent())
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

    private fun shareText(text: String) {
        if (text.isBlank()) return
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        startActivity(Intent.createChooser(shareIntent, "Bagikan prompt"))
    }
}

@Composable
private fun KonteksCekRoot(
    legacyVm: KonteksViewModel,
    checkVm: CheckViewModel,
    onShareFile: (File, String, String) -> Unit,
    onShareText: (String) -> Unit,
) {
    val colors = LocalKonteksColors.current
    val cards by legacyVm.cards.collectAsState()
    val logs by legacyVm.logs.collectAsState()
    val metrics by legacyVm.metrics.collectAsState()
    val session by legacyVm.session.collectAsState()
    val settings by legacyVm.settings.collectAsState()
    val checkState by checkVm.state.collectAsState()

    var activeTab by remember { mutableStateOf(BottomTab.CHECK) }
    var createRequestKey by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        legacyVm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(checkState.pendingShared) {
        if (checkState.pendingShared != null) {
            checkVm.clearResult()
            activeTab = BottomTab.CHECK
        }
    }

    val (title, subtitle) =
        when (activeTab) {
            BottomTab.CHECK -> "KonteksCek" to "Cek & perbaiki prompt-mu"
            BottomTab.HISTORY -> "Riwayat" to "Analisis terakhir & arsip jejak lama"
            BottomTab.MORE -> "Pengaturan" to "Akun, privasi, dan data"
            BottomTab.DASHBOARD -> "Beranda (lama)" to "Statistik verifikasi mode manual"
            BottomTab.CARDS -> "Preset Konteks" to "${cards.size} kartu tersimpan"
            BottomTab.VERIFY -> "Mode Manual" to "Lokal penuh • checklist & PDF"
        }

    Scaffold(
        containerColor = colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            KonteksTopBar(
                title = title,
                subtitle = subtitle,
                statusLabel = "Privasi",
                statusTone = Tone.SUCCESS,
                onStatusClick = { activeTab = BottomTab.MORE },
                onBack = if (activeTab.inBar) null else ({ activeTab = BottomTab.MORE }),
            )
        },
        bottomBar = {
            if (activeTab.inBar) {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    BottomTab.entries.filter { it.inBar }.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    fontFamily = PlusJakarta,
                                    fontSize = 12.sp,
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
            }
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding), color = colors.canvas) {
            when (activeTab) {
                BottomTab.CHECK -> {
                    val result = checkState.result
                    if (result != null) {
                        ResultScreen(
                            result = result,
                            prompt = checkState.prompt,
                            output = checkState.output,
                            brief = checkState.brief,
                            previousScore = checkState.previousScore,
                            errorMessage = checkState.errorMessage,
                            onBack = checkVm::clearResult,
                            onUseRewritten = checkVm::useRewrittenPrompt,
                            onRerunWithAnswers = checkVm::reanalyzeWithAnswers,
                            onRunLocal = checkVm::runLocalLintNow,
                            onRetry = checkVm::retry,
                            onDismissError = checkVm::dismissError,
                            onShareText = onShareText,
                            onNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                        )
                    } else {
                        CheckScreen(
                            state = checkState,
                            onPromptChange = checkVm::updatePrompt,
                            onOutputChange = checkVm::updateOutput,
                            onBriefChange = checkVm::updateBrief,
                            onToggleBrief = checkVm::toggleBrief,
                            onClearPrompt = checkVm::clearPrompt,
                            onClearOutput = checkVm::clearOutput,
                            onSaveTextChange = checkVm::setSaveTextHistory,
                            onDepthChange = checkVm::setDepth,
                            onAnalyze = checkVm::requestAnalyze,
                            onRunLocal = checkVm::runLocalLintNow,
                            onDismissError = checkVm::dismissError,
                            onRetry = checkVm::retry,
                            onConsentAccept = checkVm::giveConsent,
                            onConsentDecline = checkVm::declineConsent,
                            onPrivacyConfirm = checkVm::confirmPrivacyPreviewAndAnalyze,
                            onPrivacyCancel = checkVm::cancelPrivacyPreview,
                            onSharedAsPrompt = checkVm::applySharedAsPrompt,
                            onSharedAsOutput = checkVm::applySharedAsOutput,
                            onSharedAuto = checkVm::applySharedAutoSplit,
                            onSharedDismiss = checkVm::dismissShared,
                            onOpenLastAnalysis = {
                                checkVm.openLastAnalysis()
                                activeTab = BottomTab.CHECK
                            },
                            onOpenServerSettings = { activeTab = BottomTab.MORE },
                            onNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                        )
                    }
                }

                BottomTab.HISTORY -> {
                    HistoryLogsScreen(
                        logs = logs,
                        lastAnalysis = checkState.lastAnalysis,
                        onOpenLastAnalysis = {
                            checkVm.openLastAnalysis()
                            activeTab = BottomTab.CHECK
                        },
                        onEditLog = { log ->
                            legacyVm.loadLogForEditing(log)
                            activeTab = BottomTab.VERIFY
                        },
                        onExportPdf = { log ->
                            legacyVm.exportExistingLogPdf(
                                log,
                                onPdfReady = { file ->
                                    onShareFile(file, "application/pdf", "Jejak Verifikasi AI - KonteksCek")
                                },
                            )
                        },
                        onDeleteLog = legacyVm::deleteLog,
                        onPurgeAll = {
                            legacyVm.purgeAllData()
                            checkVm.clearAnalysisData()
                        },
                        onReuseLog = { log ->
                            legacyVm.reuseLogAsNewSession(log)
                            activeTab = BottomTab.VERIFY
                        },
                    )
                }

                BottomTab.MORE -> {
                    SettingsScreen(
                        settings = settings,
                        serverUrl = checkState.serverUrl,
                        consentGiven = checkState.consentGiven,
                        totalCards = cards.size,
                        totalLogs = logs.size,
                        onTheme = legacyVm::setThemeMode,
                        onTextSize = legacyVm::setTextSize,
                        onServerUrlChange = checkVm::setServerUrl,
                        onWithdrawConsent = checkVm::withdrawConsent,
                        onOpenManual = { activeTab = BottomTab.VERIFY },
                        onOpenCards = { activeTab = BottomTab.CARDS },
                        onOpenDashboard = { activeTab = BottomTab.DASHBOARD },
                        onOpenLegacyHistory = { activeTab = BottomTab.HISTORY },
                        onExportBackup = {
                            legacyVm.exportBackup { file ->
                                onShareFile(file, "application/json", "Cadangan Data KonteksCek")
                            }
                        },
                        onImportBackup = { uri: Uri -> legacyVm.importBackup(uri) },
                        onPurgeAll = {
                            legacyVm.purgeAllData()
                            checkVm.clearAnalysisData()
                            scope.launch { snackbarHostState.showSnackbar("Data lokal dibersihkan") }
                        },
                    )
                }

                BottomTab.DASHBOARD -> {
                    DashboardScreen(
                        metrics = metrics,
                        recentLogs = logs,
                        onStartVerify = {
                            legacyVm.startNewSessionWithCard(cards.firstOrNull())
                            activeTab = BottomTab.VERIFY
                        },
                        onCreateCard = {
                            createRequestKey++
                            activeTab = BottomTab.CARDS
                        },
                        onOpenLog = { log ->
                            legacyVm.loadLogForEditing(log)
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
                        onSaveCard = legacyVm::saveContextCard,
                        onDeleteCard = legacyVm::deleteContextCard,
                        onDuplicateCard = legacyVm::duplicateContextCard,
                        onUseForVerification = { card ->
                            legacyVm.startNewSessionWithCard(card)
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
                        onSelectCard = legacyVm::selectCardInSession,
                        onSelectMode = legacyVm::selectModeInSession,
                        onOutputChange = legacyVm::updateAiOutput,
                        onChecklistChange = legacyVm::updateChecklistStatus,
                        onStagesChange = legacyVm::updateStages,
                        onToggleTimer = legacyVm::toggleTimer,
                        onImportPdfUri = legacyVm::importPdfContent,
                        onSaveAndExportPdf = {
                            legacyVm.saveVerificationAndExportPdf(
                                onPdfReady = { file ->
                                    onShareFile(file, "application/pdf", "Jejak Verifikasi AI - KonteksCek")
                                },
                            )
                        },
                        onNewSession = legacyVm::startFreshSession,
                        onResetChecklist = legacyVm::resetChecklist,
                        onClearOutput = legacyVm::clearAiOutput,
                        onCopiedNotice = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                    )
                }
            }
        }
    }
}
