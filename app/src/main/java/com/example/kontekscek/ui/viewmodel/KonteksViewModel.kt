package com.aldointelligence.kontekscek.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aldointelligence.kontekscek.data.local.KonteksDatabase
import com.aldointelligence.kontekscek.data.model.*
import com.aldointelligence.kontekscek.data.repository.KonteksRepository
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
import java.io.File

data class DashboardMetrics(
    val totalCards: Int = 0,
    val totalLogs: Int = 0,
    val avgDurationSeconds: Long = 0L,
    val totalIssuesCaught: Int = 0,
    val underFiveMinRate: Int = 100
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
    val finalConclusion: String = "", // FITUR BARU
    val elapsedSeconds: Long = 0L,
    val isTimerRunning: Boolean = false,
    val analysis: RuleAnalysisResult = RuleVerificationEngine.analyze(
        null, "", KonteksDefaults.checklistForMode(TaskMode.KULIAH)
    )
)

class KonteksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KonteksRepository
    private val gson = Gson()
    private var timerJob: Job? = null

    val cards: StateFlow<List<ContextCardEntity>>
    val logs: StateFlow<List<VerificationLogEntity>>
    val metrics: StateFlow<DashboardMetrics>

    private val _session = MutableStateFlow(VerifySessionState())
    val session: StateFlow<VerifySessionState> = _session.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        val db = KonteksDatabase.getInstance(application)
        repository = KonteksRepository(db.konteksDao())

        cards = repository.allCards.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )
        logs = repository.allLogs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )
        metrics = combine(cards, logs) { cardList, logList ->
            val avgSec = if (logList.isEmpty()) 0L else logList.map { it.durationSeconds }.average().toLong()
            val under5 = if (logList.isEmpty()) 100 else {
                (logList.count { it.durationSeconds <= 300 } * 100) / logList.size
            }
            DashboardMetrics(
                totalCards = cardList.size,
                totalLogs = logList.size,
                avgDurationSeconds = avgSec,
                totalIssuesCaught = logList.sumOf { it.issuesCaughtCount },
                underFiveMinRate = under5
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

        seedSampleCardIfEmpty()
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
                        forbiddenData = "NDA-Klien, omzet rahasia, nomor pribadi klien"
                    )
                )
            }
        }
    }

    // ===== CRUD KARTU KONTEKS =====
    fun saveContextCard(card: ContextCardEntity) {
        viewModelScope.launch {
            repository.saveCard(card)
            _toastMessage.emit(if (card.id == 0L) "Kartu Konteks dibuat" else "Kartu Konteks diperbarui")
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
        val mode = card?.getTaskMode() ?: TaskMode.KULIAH
        val defaultChecklist = KonteksDefaults.checklistForMode(mode)
        _session.value = VerifySessionState(
            selectedCard = card,
            mode = mode,
            checklist = defaultChecklist,
            analysis = RuleVerificationEngine.analyze(card, "", defaultChecklist)
        )
        startTimer()
    }

    fun loadLogForEditing(log: VerificationLogEntity) {
        viewModelScope.launch {
            val card = repository.getCardById(log.cardId)
            val parsedChecklist = log.parseChecklist().ifEmpty {
                KonteksDefaults.checklistForMode(log.getTaskMode())
            }
            _session.value = VerifySessionState(
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
                analysis = RuleVerificationEngine.analyze(card, log.aiOutputSnippet, parsedChecklist)
            )
        }
    }

    fun selectCardInSession(card: ContextCardEntity) {
        val mode = card.getTaskMode()
        val list = KonteksDefaults.checklistForMode(mode)
        updateSession {
            it.copy(selectedCard = card, mode = mode, checklist = list)
        }
        if (!_session.value.isTimerRunning) startTimer()
    }

    fun selectModeInSession(mode: TaskMode) {
        val list = KonteksDefaults.checklistForMode(mode)
        updateSession { it.copy(mode = mode, checklist = list) }
    }

    fun updateAiOutput(text: String) {
        if (!_session.value.isTimerRunning && text.isNotEmpty()) startTimer()
        updateSession { it.copy(aiOutputText = text) }
    }

    fun importPdfContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val extractedText = PdfExtractor.extractTextFromPdf(getApplication(), uri)
            if (extractedText.isNotBlank()) {
                val currentText = _session.value.aiOutputText
                val combined = if (currentText.isBlank()) extractedText else "$currentText\n\n[Teks dari PDF]:\n$extractedText"
                if (!_session.value.isTimerRunning) startTimer()
                updateSession { it.copy(aiOutputText = combined) }
                _toastMessage.emit("File PDF berhasil diimpor (${extractedText.length} karakter)")
            } else {
                _toastMessage.emit("Gagal membaca teks dari PDF ini (Mungkin berupa scan gambar/terenkripsi)")
            }
        }
    }

    fun updateChecklistStatus(itemId: String, status: CheckStatus) {
        updateSession { state ->
            val updated = state.checklist.map {
                if (it.id == itemId) it.copy(status = status) else it
            }
            state.copy(checklist = updated)
        }
    }

    fun updateStages(asked: String? = null, checked: String? = null, changed: String? = null, conclusion: String? = null) {
        updateSession {
            it.copy(
                stageAsked = asked ?: it.stageAsked,
                stageChecked = checked ?: it.stageChecked,
                stageChanged = changed ?: it.stageChanged,
                finalConclusion = conclusion ?: it.finalConclusion
            )
        }
    }

    fun toggleTimer() {
        if (_session.value.isTimerRunning) stopTimer() else startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _session.update { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
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
            val next = transform(current)
            next.copy(
                analysis = RuleVerificationEngine.analyze(
                    next.selectedCard,
                    next.aiOutputText,
                    next.checklist
                )
            )
        }
    }

    fun saveVerificationAndExportPdf(onPdfReady: (File) -> Unit) {
        stopTimer()
        val s = _session.value
        viewModelScope.launch {
            runCatching {
                val log = VerificationLogEntity(
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
                    issuesCaughtCount = s.analysis.detectedIssuesCount +
                        s.checklist.count { it.status == CheckStatus.DIUBAH },
                    durationSeconds = s.elapsedSeconds
                )
                val savedId = repository.saveLog(log)
                val finalLog = log.copy(id = savedId)
                val pdfFile = PdfExporter.generateVerificationPdf(
                    getApplication(),
                    finalLog,
                    s.selectedCard
                )
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
                val file = PdfExporter.generateVerificationPdf(getApplication(), log, card)
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
}