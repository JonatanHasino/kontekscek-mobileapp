package com.aldointelligence.kontekscek.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aldointelligence.kontekscek.data.AppPreferences
import com.aldointelligence.kontekscek.data.KonteksSettings
import com.aldointelligence.kontekscek.data.SharedPreferencesAppPreferences
import com.aldointelligence.kontekscek.data.TextSize
import com.aldointelligence.kontekscek.data.ThemeMode
import com.aldointelligence.kontekscek.data.local.KonteksDatabase
import com.aldointelligence.kontekscek.data.model.*
import com.aldointelligence.kontekscek.data.repository.KonteksRepository
import com.aldointelligence.kontekscek.domain.BackupManager
import com.aldointelligence.kontekscek.domain.PdfExporter
import com.aldointelligence.kontekscek.domain.PdfExtractor
import com.aldointelligence.kontekscek.domain.RuleAnalysisResult
import com.aldointelligence.kontekscek.domain.RuleVerificationEngine
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class DailyCount(val label: String, val count: Int)

data class DashboardMetrics(
    val totalCards: Int = 0,
    val totalLogs: Int = 0,
    val todayLogs: Int = 0,
    val weekLogs: Int = 0,
    val streakDays: Int = 0,
    val avgDurationSeconds: Long = 0L,
    val totalIssuesCaught: Int = 0,
    val underFiveMinRate: Int = 100,
    val weeklyActivity: List<DailyCount> = emptyList(),
)

data class VerifySessionState(
    val editingLogId: Long = 0L,
    val selectedCard: ContextCardEntity? = null,
    val mode: TaskMode = TaskMode.KULIAH,
    val aiOutputText: String = "",
    val checklist: List<ChecklistItemState> = KonteksDefaults.checklistForMode(TaskMode.KULIAH),
    val stageAsked: String = "",
    val stageChecked: String = "",
    val stageChanged: String = "",
    val finalConclusion: String = "",
    val elapsedSeconds: Long = 0L,
    val isTimerRunning: Boolean = false,
    val analysis: RuleAnalysisResult =
        RuleVerificationEngine.analyze(
            null,
            "",
            KonteksDefaults.checklistForMode(TaskMode.KULIAH),
        ),
)

class KonteksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KonteksRepository
    private val preferences: AppPreferences
    private val gson = Gson()
    private var timerJob: Job? = null

    val cards: StateFlow<List<ContextCardEntity>>
    val logs: StateFlow<List<VerificationLogEntity>>
    val metrics: StateFlow<DashboardMetrics>

    private val _settings = MutableStateFlow(KonteksSettings())
    val settings: StateFlow<KonteksSettings> = _settings.asStateFlow()

    private val _session = MutableStateFlow(VerifySessionState())
    val session: StateFlow<VerifySessionState> = _session.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        val db = KonteksDatabase.getInstance(application)
        repository = KonteksRepository(db.konteksDao())
        preferences = SharedPreferencesAppPreferences(application)
        _settings.value = preferences.settings()
        _session.value =
            VerifySessionState(
                mode = _settings.value.defaultTaskMode,
                checklist = KonteksDefaults.checklistForMode(_settings.value.defaultTaskMode),
            ).withAnalysis()

        cards =
            repository.allCards.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )
        logs =
            repository.allLogs.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )
        metrics =
            combine(cards, logs) { cardList, logList ->
                buildMetrics(cardList, logList)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

        seedSampleCardIfEmpty()
    }

    private fun buildMetrics(
        cardList: List<ContextCardEntity>,
        logList: List<VerificationLogEntity>,
    ): DashboardMetrics {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun dayStart(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val todayStart = dayStart(today)
        val tomorrowStart = dayStart(today.plusDays(1))
        val weekStart = dayStart(today.minusDays(6))

        val todayLogs = logList.count { it.createdAt in todayStart until tomorrowStart }
        val weekLogs = logList.count { it.createdAt >= weekStart }

        val activity =
            (0..6).map { offset ->
                val day = today.minusDays((6 - offset).toLong())
                val start = dayStart(day)
                val end = dayStart(day.plusDays(1))
                DailyCount(
                    label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("id", "ID")),
                    count = logList.count { it.createdAt in start until end },
                )
            }

        var streak = 0
        var cursor = if (todayLogs > 0) today else today.minusDays(1)
        while (true) {
            val start = dayStart(cursor)
            val end = dayStart(cursor.plusDays(1))
            if (logList.any { it.createdAt in start until end }) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }

        val avgSec = if (logList.isEmpty()) 0L else logList.map { it.durationSeconds }.average().toLong()
        val under5 =
            if (logList.isEmpty()) {
                100
            } else {
                (logList.count { it.durationSeconds <= 300 } * 100) / logList.size
            }

        return DashboardMetrics(
            totalCards = cardList.size,
            totalLogs = logList.size,
            todayLogs = todayLogs,
            weekLogs = weekLogs,
            streakDays = streak,
            avgDurationSeconds = avgSec,
            totalIssuesCaught = logList.sumOf { it.issuesCaughtCount },
            underFiveMinRate = under5,
            weeklyActivity = activity,
        )
    }

    private fun seedSampleCardIfEmpty() {
        viewModelScope.launch {
            val existing = repository.allCards.first()
            if (existing.isEmpty()) {
                repository.saveCard(
                    ContextCardEntity(
                        title = "Redesign Landing Page Klien UMKM",
                        mode = TaskMode.KLIEN.name,
                        taskType = "Desain",
                        objective = "Menyusun struktur wireframe dan microcopy sesuai brief klien Batam",
                        audienceStandard = "Pemilik bisnis & pelanggan muda 18-30 tahun",
                        ethicsAndSources = "Gunakan AI untuk struktur awal saja, validasi data dan aset bebas hak cipta",
                        formatAndLanguage = "Bahasa Indonesia santai-profesional, poin terstruktur",
                        forbiddenData = "NDA-Klien, omzet rahasia, nomor pribadi klien",
                    ),
                )
            }
        }
    }

    // ===== PREFERENSI TAMPILAN =====

    fun setThemeMode(mode: ThemeMode) {
        updateSettings { it.copy(themeMode = mode) }
    }

    fun setTextSize(size: TextSize) {
        updateSettings { it.copy(textSize = size) }
    }

    fun setDefaultTaskMode(mode: TaskMode) {
        updateSettings { it.copy(defaultTaskMode = mode) }
        if (_session.value.selectedCard == null && _session.value.aiOutputText.isBlank()) {
            updateSession { state ->
                state.copy(
                    mode = mode,
                    checklist = KonteksDefaults.checklistForMode(mode),
                )
            }
        }
    }

    fun setAutoStartTimer(enabled: Boolean) {
        updateSettings { it.copy(autoStartTimer = enabled) }
    }

    private fun updateSettings(transform: (KonteksSettings) -> KonteksSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        preferences.save(next)
    }

    // ===== CRUD KARTU KONTEKS =====

    fun saveContextCard(card: ContextCardEntity) {
        viewModelScope.launch {
            repository.saveCard(card)
            _toastMessage.emit(if (card.id == 0L) "Kartu Konteks dibuat" else "Kartu Konteks diperbarui")
        }
    }

    fun duplicateContextCard(card: ContextCardEntity) {
        viewModelScope.launch {
            repository.saveCard(
                card.copy(
                    id = 0L,
                    title = "${card.title} (Salinan)",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            _toastMessage.emit("Kartu Konteks diduplikasi")
        }
    }

    fun deleteContextCard(card: ContextCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            _toastMessage.emit("Kartu Konteks dihapus")
        }
    }

    // ===== SESI VERIFIKASI (CREATE / UPDATE LOG) =====

    fun startNewSessionWithCard(card: ContextCardEntity?) {
        val mode = card?.getTaskMode() ?: _settings.value.defaultTaskMode
        val defaultChecklist = KonteksDefaults.checklistForMode(mode)
        timerJob?.cancel()
        _session.value =
            VerifySessionState(
                selectedCard = card,
                mode = mode,
                checklist = defaultChecklist,
                elapsedSeconds = 0L,
                isTimerRunning = false,
            ).withAnalysis()
        if (_settings.value.autoStartTimer) startTimer()
    }

    fun startFreshSession() {
        startNewSessionWithCard(_session.value.selectedCard)
    }

    fun reuseLogAsNewSession(log: VerificationLogEntity) {
        viewModelScope.launch {
            val card = repository.getCardById(log.cardId)
            startNewSessionWithCard(card)
            _toastMessage.emit(
                if (card != null) {
                    "Sesi baru dimulai dari kartu \"${card.title}\""
                } else {
                    "Sesi baru dimulai (kartu asal sudah dihapus)"
                },
            )
        }
    }

    fun loadLogForEditing(log: VerificationLogEntity) {
        viewModelScope.launch {
            val card = repository.getCardById(log.cardId)
            val parsedChecklist =
                log.parseChecklist().ifEmpty {
                    KonteksDefaults.checklistForMode(log.getTaskMode())
                }
            timerJob?.cancel()
            _session.value =
                VerifySessionState(
                    editingLogId = log.id,
                    selectedCard = card,
                    mode = log.getTaskMode(),
                    aiOutputText = log.aiOutputSnippet,
                    checklist = parsedChecklist,
                    stageAsked = log.stageAsked,
                    stageChecked = log.stageChecked,
                    stageChanged = log.stageChanged,
                    finalConclusion = log.finalConclusion,
                    elapsedSeconds = log.durationSeconds,
                    isTimerRunning = false,
                ).withAnalysis()
        }
    }

    fun selectCardInSession(card: ContextCardEntity) {
        val mode = card.getTaskMode()
        val list = KonteksDefaults.checklistForMode(mode)
        updateSession {
            it.copy(selectedCard = card, mode = mode, checklist = list)
        }
        if (!_session.value.isTimerRunning && _settings.value.autoStartTimer) startTimer()
    }

    fun selectModeInSession(mode: TaskMode) {
        val list = KonteksDefaults.checklistForMode(mode)
        updateSession { it.copy(mode = mode, checklist = list) }
    }

    fun resetChecklist() {
        updateSession { state ->
            state.copy(checklist = state.checklist.map { it.copy(status = CheckStatus.BELUM) })
        }
        viewModelScope.launch { _toastMessage.emit("Daftar periksa direset") }
    }

    fun clearAiOutput() {
        updateSession { it.copy(aiOutputText = "") }
    }

    fun updateAiOutput(text: String) {
        if (!_session.value.isTimerRunning && text.isNotEmpty() && _settings.value.autoStartTimer) startTimer()
        updateSession { it.copy(aiOutputText = text) }
    }

    fun importPdfContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val extractedText = PdfExtractor.extractTextFromPdf(getApplication(), uri)
            if (extractedText.isNotBlank()) {
                val currentText = _session.value.aiOutputText
                val combined =
                    if (currentText.isBlank()) extractedText else "$currentText\n\n[Teks dari PDF]:\n$extractedText"
                if (!_session.value.isTimerRunning && _settings.value.autoStartTimer) startTimer()
                updateSession { it.copy(aiOutputText = combined) }
                _toastMessage.emit("File PDF berhasil diimpor (${extractedText.length} karakter)")
            } else {
                _toastMessage.emit("Gagal membaca teks dari PDF ini (Mungkin berupa scan gambar/terenkripsi)")
            }
        }
    }

    fun updateChecklistStatus(itemId: String, status: CheckStatus) {
        updateSession { state ->
            val updated =
                state.checklist.map {
                    if (it.id == itemId) it.copy(status = status) else it
                }
            state.copy(checklist = updated)
        }
    }

    fun updateStages(
        asked: String? = null,
        checked: String? = null,
        changed: String? = null,
        conclusion: String? = null,
    ) {
        updateSession {
            it.copy(
                stageAsked = asked ?: it.stageAsked,
                stageChecked = checked ?: it.stageChecked,
                stageChanged = changed ?: it.stageChanged,
                finalConclusion = conclusion ?: it.finalConclusion,
            )
        }
    }

    fun toggleTimer() {
        if (_session.value.isTimerRunning) stopTimer() else startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _session.update { it.copy(isTimerRunning = true) }
        timerJob =
            viewModelScope.launch {
                while (true) {
                    delay(1_000L)
                    _session.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
                }
            }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _session.update { it.copy(isTimerRunning = false) }
    }

    private fun updateSession(transform: (VerifySessionState) -> VerifySessionState) {
        _session.update { current ->
            transform(current).withAnalysis()
        }
    }

    private fun VerifySessionState.withAnalysis(): VerifySessionState =
        copy(
            analysis =
                RuleVerificationEngine.analyze(
                    selectedCard,
                    aiOutputText,
                    checklist,
                ),
        )

    fun saveVerificationAndExportPdf(onPdfReady: (File) -> Unit) {
        stopTimer()
        val s = _session.value
        viewModelScope.launch {
            runCatching {
                val log =
                    VerificationLogEntity(
                        id = s.editingLogId,
                        cardId = s.selectedCard?.id ?: 0L,
                        cardTitle = s.selectedCard?.title ?: "Sesi Cepat (${s.mode.label})",
                        mode = s.mode.name,
                        aiOutputSnippet = s.aiOutputText.take(300),
                        checklistJson = gson.toJson(s.checklist),
                        stageAsked = s.stageAsked,
                        stageChecked = s.stageChecked,
                        stageChanged = s.stageChanged,
                        finalConclusion = s.finalConclusion,
                        issuesCaughtCount =
                            s.analysis.detectedIssuesCount +
                                s.checklist.count { it.status == CheckStatus.DIUBAH },
                        durationSeconds = s.elapsedSeconds,
                    )
                val savedId = repository.saveLog(log)
                val finalLog = log.copy(id = savedId)
                val pdfFile =
                    withContext(Dispatchers.IO) {
                        PdfExporter.generateVerificationPdf(
                            getApplication(),
                            finalLog,
                            s.selectedCard,
                        )
                    }
                _toastMessage.emit("Jejak tersimpan & PDF 1 halaman siap")
                onPdfReady(pdfFile)
            }.onFailure { err ->
                _toastMessage.emit("Gagal membuat PDF: ${err.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    // ===== CRUD RIWAYAT JEJAK =====

    fun exportExistingLogPdf(log: VerificationLogEntity, onPdfReady: (File) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val card = repository.getCardById(log.cardId)
                val file =
                    withContext(Dispatchers.IO) {
                        PdfExporter.generateVerificationPdf(getApplication(), log, card)
                    }
                onPdfReady(file)
            }.onFailure { err ->
                _toastMessage.emit("Gagal membagikan PDF: ${err.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun deleteLog(log: VerificationLogEntity) {
        viewModelScope.launch {
            repository.deleteLog(log)
            _toastMessage.emit("Jejak verifikasi dihapus permanen (UU PDP)")
        }
    }

    fun purgeAllData() {
        viewModelScope.launch {
            repository.purgeAllLogs()
            _toastMessage.emit("Seluruh riwayat jejak telah dibersihkan")
        }
    }

    // ===== CADANGAN DATA =====

    fun exportBackup(onReady: (File) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val allCards = repository.getAllCards()
                val allLogs = repository.getAllLogs()
                val file =
                    withContext(Dispatchers.IO) {
                        BackupManager.export(getApplication(), allCards, allLogs)
                    }
                _toastMessage.emit("Cadangan JSON siap (${allCards.size} kartu, ${allLogs.size} jejak)")
                onReady(file)
            }.onFailure { err ->
                _toastMessage.emit("Gagal membuat cadangan: ${err.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val payload = BackupManager.read(getApplication(), uri)
                val (cardCount, logCount) = repository.restoreBackup(payload.cards, payload.logs)
                _toastMessage.emit("Cadangan dipulihkan: $cardCount kartu & $logCount jejak")
            }.onFailure { err ->
                _toastMessage.emit("Gagal memulihkan cadangan: ${err.localizedMessage ?: "Format tidak valid"}")
            }
        }
    }
}
