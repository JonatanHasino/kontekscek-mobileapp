package com.aldointelligence.kontekscek

import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.KonteksDefaults
import com.aldointelligence.kontekscek.data.model.TaskMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KonteksDefaultsTest {

    @Test
    fun setiapModePunyaLimaButirChecklistUnik() {
        TaskMode.entries.forEach { mode ->
            val checklist = KonteksDefaults.checklistForMode(mode)
            assertEquals("Mode ${mode.name} harus punya 5 butir", 5, checklist.size)
            assertEquals(
                "ID checklist mode ${mode.name} harus unik",
                checklist.size,
                checklist.map { it.id }.distinct().size,
            )
            assertTrue(checklist.all { it.title.isNotBlank() && it.suggestion.isNotBlank() })
        }
    }

    @Test
    fun templatCepatMencakupEmpatJenisKeluaran() {
        val taskTypes = KonteksDefaults.quickTemplates.map { it.taskType }
        assertEquals(listOf("Laporan", "Kode", "Desain", "Konten"), taskTypes)
        assertTrue(KonteksDefaults.quickTemplates.all { it.forbiddenData.isNotBlank() })
    }

    @Test
    fun headerSalinMemuatLimaIsianWajib() {
        val card =
            ContextCardEntity(
                title = "Uji",
                mode = TaskMode.KERJA.name,
                taskType = "Kode",
                objective = "Tujuan",
                audienceStandard = "Audiens",
                ethicsAndSources = "Etika",
                formatAndLanguage = "Format",
                forbiddenData = "API Key",
            )
        val header = card.toCopyablePromptHeader()
        listOf("1. Tujuan", "2. Audiens", "3. Batas Etika", "4. Format", "5. Data Terlarang")
            .forEach { section -> assertTrue("Header kehilangan bagian: $section", header.contains(section)) }
        assertTrue(header.contains("KERJA"))
    }
}
