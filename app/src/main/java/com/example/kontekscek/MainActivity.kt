package com.aldointelligence.kontekscek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aldointelligence.kontekscek.ui.screens.ContextCardsScreen
import com.aldointelligence.kontekscek.ui.screens.DashboardScreen
import com.aldointelligence.kontekscek.ui.screens.HistoryLogsScreen
import com.aldointelligence.kontekscek.ui.screens.VerifySessionScreen
import com.aldointelligence.kontekscek.ui.theme.CyberColors
import com.aldointelligence.kontekscek.ui.theme.KonteksCekTheme
import com.aldointelligence.kontekscek.ui.viewmodel.KonteksViewModel
import kotlinx.coroutines.launch
import java.io.File

private enum class BottomTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Beranda", Icons.Rounded.SpaceDashboard),
    CARDS("Kartu", Icons.Rounded.CreditCard),
    VERIFY("Cek AI", Icons.Rounded.VerifiedUser),
    HISTORY("Jejak", Icons.Rounded.History)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KonteksCekTheme {
                KonteksCekRoot(onSharePdf = ::sharePdfFile)
            }
        }
    }

    private fun sharePdfFile(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Jejak Verifikasi AI - KonteksCek")
                clipData = android.content.ClipData.newRawUri("PDF Jejak", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(shareIntent, "Bagikan PDF Jejak Verifikasi").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooserIntent)
        }.onFailure { err ->
            android.widget.Toast.makeText(this, "Gagal membagikan PDF: ${err.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun KonteksCekRoot(
    onSharePdf: (File) -> Unit,
    vm: KonteksViewModel = viewModel()
) {
    val cards by vm.cards.collectAsState()
    val logs by vm.logs.collectAsState()
    val metrics by vm.metrics.collectAsState()
    val session by vm.session.collectAsState()

    var activeTab by remember { mutableStateOf(BottomTab.DASHBOARD) }
    var triggerOpenCreateCard by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = CyberColors.DeepSpace,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = CyberColors.NavySurface) {
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = {
                            triggerOpenCreateCard = false
                            activeTab = tab
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberColors.DeepSpace,
                            selectedTextColor = CyberColors.NeonCyan,
                            indicatorColor = CyberColors.NeonCyan,
                            unselectedIconColor = CyberColors.TextSecondary,
                            unselectedTextColor = CyberColors.TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding), color = CyberColors.DeepSpace) {
            when (activeTab) {
                BottomTab.DASHBOARD -> DashboardScreen(
                    metrics = metrics,
                    recentLogs = logs,
                    onStartVerify = {
                        vm.startNewSessionWithCard(cards.firstOrNull())
                        activeTab = BottomTab.VERIFY
                    },
                    onCreateCard = {
                        triggerOpenCreateCard = true
                        activeTab = BottomTab.CARDS
                    },
                    onOpenLog = { log ->
                        vm.loadLogForEditing(log)
                        activeTab = BottomTab.VERIFY
                    }
                )

                BottomTab.CARDS -> ContextCardsScreen(
                    cards = cards,
                    openCreateInitially = triggerOpenCreateCard,
                    onSaveCard = vm::saveContextCard,
                    onDeleteCard = vm::deleteContextCard,
                    onUseForVerification = { card ->
                        vm.startNewSessionWithCard(card)
                        activeTab = BottomTab.VERIFY
                    },
                    onCopiedNotice = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )

                BottomTab.VERIFY -> VerifySessionScreen(
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
                        vm.saveVerificationAndExportPdf(onPdfReady = onSharePdf)
                    }
                )

                BottomTab.HISTORY -> HistoryLogsScreen(
                    logs = logs,
                    onEditLog = { log ->
                        vm.loadLogForEditing(log)
                        activeTab = BottomTab.VERIFY
                    },
                    onExportPdf = { log ->
                        vm.exportExistingLogPdf(log, onPdfReady = onSharePdf)
                    },
                    onDeleteLog = vm::deleteLog,
                    onPurgeAll = vm::purgeAllData
                )
            }
        }
    }
}