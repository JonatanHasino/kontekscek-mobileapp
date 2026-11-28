package com.aldointelligence.kontekscek.data.remote.mapper

import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeResponse
import com.aldointelligence.kontekscek.data.remote.dto.DetectedDto
import com.aldointelligence.kontekscek.data.remote.dto.DimensionDto
import com.aldointelligence.kontekscek.data.remote.dto.IssueDto
import com.aldointelligence.kontekscek.data.remote.dto.OutputCheckDto
import com.aldointelligence.kontekscek.data.remote.dto.PromptScoreDto
import com.aldointelligence.kontekscek.domain.model.AnswersRequest
import com.aldointelligence.kontekscek.domain.model.IssueCategory
import com.aldointelligence.kontekscek.domain.model.IssueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisMapperTest {

    @Test
    fun `overall dihitung ulang dari dimensi`() {
        val response =
            AnalyzeResponse(
                promptScore =
                    PromptScoreDto(
                        overall = 1,
                        dimensions =
                            listOf(
                                DimensionDto("goal", 10),
                                DimensionDto("format", 10),
                                DimensionDto("context", 10),
                            ),
                    ),
            )
        val domain = response.toDomain()
        assertEquals(100, domain.score.overall)
        assertEquals(3, domain.score.dimensions.size)
    }

    @Test
    fun `enum asing fallback aman`() {
        val response =
            AnalyzeResponse(
                issues =
                    listOf(
                        IssueDto(category = "TIDAK_DIKENAL", severity = "SUPER_KITIS", source = "apa"),
                    ),
            )
        val domain = response.toDomain()
        assertEquals(IssueCategory.OTHER, domain.issues.first().category)
        assertEquals(AlertSeverity.INFO, domain.issues.first().severity)
        assertEquals(IssueSource.PROMPT, domain.issues.first().source)
    }

    @Test
    fun `output check terpetakan`() {
        val response =
            AnalyzeResponse(
                outputCheck =
                    OutputCheckDto(
                        answersRequest = "no",
                        summary = "Melenceng",
                        followUpPrompt = "Perbaiki",
                    ),
            )
        val domain = response.toDomain()
        assertEquals(AnswersRequest.NO, domain.outputCheck!!.answersRequest)
        assertEquals("Perbaiki", domain.outputCheck!!.followUpPrompt)
    }

    @Test
    fun `jumlah isu dibatasi delapan`() {
        val response =
            AnalyzeResponse(
                issues = (1..12).map { IssueDto(id = "i$it", title = "Isu $it") },
                questionsToUser = (1..5).map { com.aldointelligence.kontekscek.data.remote.dto.QuestionDto("q$it", "Q$it", listOf("A")) },
            )
        val domain = response.toDomain()
        assertTrue(domain.issues.size == 8)
        assertTrue(domain.questionsToUser.size == 3)
    }

    @Test
    fun `detected dipakai`() {
        val domain = AnalyzeResponse(detected = DetectedDto("kode", "claude")).toDomain()
        assertEquals("kode", domain.taskType)
        assertEquals("claude", domain.targetAI)
    }
}
