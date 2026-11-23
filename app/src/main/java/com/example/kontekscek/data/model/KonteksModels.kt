package com.aldointelligence.kontekscek.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class TaskMode(val label: String, val subtitle: String) {
    KULIAH("Kuliah", "Tugas akademik, makalah, riset & etika sitasi"),
    KERJA("Kerja", "Administrasi data, kode produksi, magang & SOP"),
    KLIEN("Klien", "Brief proyek freelance, UI/UX, konten & kerahasiaan")
}

enum class CheckStatus(val label: String) {
    BELUM("Belum"),
    SUDAH("Sudah"),
    DIUBAH("Diubah")
}

enum class AlertSeverity { CRITICAL, WARNING, INFO }

data class ChecklistItemState(
    val id: String,
    val title: String,
    val suggestion: String,
    val status: CheckStatus = CheckStatus.BELUM
)

data class RuleAlert(
    val title: String,
    val detail: String,
    val severity: AlertSeverity
)

/**
 * Tabel CRUD 1: Kartu Konteks (5 Isian Wajib sesuai S-07M)
 */
@Entity(tableName = "context_cards")
data class ContextCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val mode: String, // TaskMode.name
    val taskType: String, // Laporan, Kode, Desain, Konten
    val objective: String,           // Isian 1: Tujuan
    val audienceStandard: String,    // Isian 2: Audiens / Standar Mutu
    val ethicsAndSources: String,    // Isian 3: Batas Etika & Sumber
    val formatAndLanguage: String,   // Isian 4: Format / Bahasa
    val forbiddenData: String,       // Isian 5: Data Terlarang (dipisah koma)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getTaskMode(): TaskMode = runCatching { TaskMode.valueOf(mode) }.getOrDefault(TaskMode.KULIAH)

    /**
     * Format siap salin 1-klik untuk ditempel ke AI Chatbot (S-07S)
     */
    fun toCopyablePromptHeader(): String = """
        [KARTU KONTEKS - KONTEKSCEK | MODE: ${getTaskMode().label.uppercase()}]
        Judul Proyek: $title ($taskType)
        1. Tujuan: $objective
        2. Audiens & Standar Mutu: $audienceStandard
        3. Batas Etika & Sumber: $ethicsAndSources
        4. Format & Bahasa: $formatAndLanguage
        5. Data Terlarang (JANGAN DIPAKAI/DIMINTA): $forbiddenData
        ---
        Instruksi: Pastikan seluruh output mematuhi 5 poin konteks di atas.
    """.trimIndent()
}

/**
 * Tabel CRUD 2: Jejak Verifikasi 3 Tahap & Status Checklist (S-16M / S-01C)
 */
@Entity(tableName = "verification_logs")
data class VerificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val cardId: Long,
    val cardTitle: String,
    val mode: String,
    val aiOutputSnippet: String,
    val checklistJson: String,
    val stageAsked: String,
    val stageChecked: String,
    val stageChanged: String,
    val finalConclusion: String = "", // FITUR BARU: Kesimpulan Verifikator
    val issuesCaughtCount: Int,
    val durationSeconds: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTaskMode(): TaskMode = runCatching { TaskMode.valueOf(mode) }.getOrDefault(TaskMode.KULIAH)

    fun parseChecklist(): List<ChecklistItemState> {
        val type = object : TypeToken<List<ChecklistItemState>>() {}.type
        return runCatching {
            Gson().fromJson<List<ChecklistItemState>>(checklistJson, type)
        }.getOrDefault(emptyList())
    }
}

/**
 * Basis Data Daftar Periksa Berbasis Aturan (<= 5 Butir per Mode)
 * & Templat Cepat Kartu Konteks (S-07E)
 */
object KonteksDefaults {
    fun checklistForMode(mode: TaskMode): List<ChecklistItemState> = when (mode) {
        TaskMode.KULIAH -> listOf(
            ChecklistItemState("k1", "Kesesuaian dengan rubrik & tujuan tugas", "Bandingkan poin jawaban AI dengan instruksi dosen di LMS."),
            ChecklistItemState("k2", "Keabsahan sitasi, angka & referensi ilmiah", "Buka langsung jurnal/sumber asli; jangan percaya sitasi mentah AI."),
            ChecklistItemState("k3", "Pemahaman pribadi atas isi jawaban", "Coba jelaskan ulang 2-3 poin inti dengan bahasa sendiri."),
            ChecklistItemState("k4", "Batas plagiarisme & atribusi penggunaan AI", "Parafrase kalimat mentah dan catat bagian bantuan AI sesuai panduan kampus."),
            ChecklistItemState("k5", "Format, bahasa & struktur penulisan", "Pastikan gaya bahasa akademik dan format file sesuai ketentuan.")
        )
        TaskMode.KERJA -> listOf(
            ChecklistItemState("w1", "Akurasi data, rumus, atau pengujian kode", "Jalankan unit test/debugging atau hitung ulang formula secara manual."),
            ChecklistItemState("w2", "Kesesuaian istilah teknis & SOP perusahaan", "Cocokkan istilah dengan dokumentasi internal atau konfirmasi mentor."),
            ChecklistItemState("w3", "Kejelasan jejak sumber bagi atasan", "Sertakan tautan dokumen/referensi agar hasil bisa dipertanggungjawabkan."),
            ChecklistItemState("w4", "Bebas kebocoran data rahasia perusahaan", "Pastikan tidak ada kredensial, data gaji, atau data pelanggan terpapar."),
            ChecklistItemState("w5", "Konsistensi dengan proyek/struktur eksisting", "Cek nama variabel, skema tabel, atau format laporan sebelumnya.")
        )
        TaskMode.KLIEN -> listOf(
            ChecklistItemState("c1", "Kecocokan 100% dengan brief & ekspektasi klien", "Cocokkan tiap deliverable AI dengan dokumen brief dan revisi terakhir."),
            ChecklistItemState("c2", "Tidak memakai hasil mentah (sudah dikurasi)", "Sesuaikan alur UX/copywriting dengan brand voice klien."),
            ChecklistItemState("c3", "Keamanan & kerahasiaan data klien (NDA)", "Pastikan data privat klien tidak pernah masuk prompt maupun output."),
            ChecklistItemState("c4", "Kejelasan hak cipta & validitas klaim visual/data", "Verifikasi fakta, harga, atau lisensi aset sebelum dipublikasi."),
            ChecklistItemState("c5", "Siap serah & memiliki catatan revisi", "Simpan ringkasan perubahan agar mudah dijelaskan saat presentasi klien.")
        )
    }

    data class QuickTemplate(
        val taskType: String,
        val mode: TaskMode,
        val objective: String,
        val audienceStandard: String,
        val ethicsAndSources: String,
        val formatAndLanguage: String,
        val forbiddenData: String
    )

    val quickTemplates = listOf(
        QuickTemplate(
            taskType = "Laporan",
            mode = TaskMode.KULIAH,
            objective = "Menyusun kerangka & analisis laporan kuliah berbasis data valid",
            audienceStandard = "Dosen pengampu, wajib argumen logis dan referensi 5 tahun terakhir",
            ethicsAndSources = "AI hanya untuk draf kerangka; seluruh sitasi dicek ke jurnal asli",
            formatAndLanguage = "Bahasa Indonesia formal akademik, struktur Markdown/DOCX",
            forbiddenData = "NIM mahasiswa lain, data wawancara mentah tanpa anonimisasi"
        ),
        QuickTemplate(
            taskType = "Kode",
            mode = TaskMode.KERJA,
            objective = "Debugging dan optimasi fungsi tanpa merusak arsitektur eksisting",
            audienceStandard = "Production Engineer / Tim Dev, lulus uji kompilasi & edge-case",
            ethicsAndSources = "Tulis ulang logika kritis dan uji lokal sebelum commit",
            formatAndLanguage = "Kotlin/TypeScript bersih, sesuai konvensi penamaan proyek",
            forbiddenData = "API Key, password database, token produksi, IP internal"
        ),
        QuickTemplate(
            taskType = "Desain",
            mode = TaskMode.KLIEN,
            objective = "Menyusun user flow dan struktur wireframe sesuai brief klien",
            audienceStandard = "Klien UMKM/Startup, fokus kemudahan navigasi & konsistensi brand",
            ethicsAndSources = "Eksplorasi struktur saja; visual akhir dikurasi manual tanpa plagiat",
            formatAndLanguage = "Daftar layar terstruktur & microcopy Bahasa Indonesia santai-profesional",
            forbiddenData = "Nama asli klien NDA, angka omzet rahasia, kontrak harga"
        ),
        QuickTemplate(
            taskType = "Konten",
            mode = TaskMode.KERJA,
            objective = "Membuat draf caption & ide konten edukatif yang akurat",
            audienceStandard = "Audiens Gen Z (18-25 th) & atasan divisi konten",
            ethicsAndSources = "Setiap angka statistik wajib memiliki sumber resmi & tahun akses",
            formatAndLanguage = "Kalimat ringkas, CTA jelas, tidak menakut-nakuti",
            forbiddenData = "Data kontak pelanggan, testimoni tanpa izin tertulis"
        )
    )
}