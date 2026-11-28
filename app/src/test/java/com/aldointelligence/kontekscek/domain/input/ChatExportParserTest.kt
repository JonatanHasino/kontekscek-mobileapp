package com.aldointelligence.kontekscek.domain.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatExportParserTest {

    @Test
    fun `format You said dan ChatGPT said`() {
        val parsed =
            ChatExportParser.parse(
                """
                You said:
                Buat ringkasan laporan dalam 5 poin.

                ChatGPT said:
                Berikut ringkasannya:
                1. Pendahuluan
                2. Temuan
                """.trimIndent(),
            )
        assertNotNull(parsed)
        assertEquals("Buat ringkasan laporan dalam 5 poin.", parsed!!.prompt)
        assertTrue(parsed.output!!.contains("Berikut ringkasannya"))
    }

    @Test
    fun `format User dan Assistant inline`() {
        val parsed = ChatExportParser.parse("User: tolong buat caption\nAssistant: Ini captionnya")
        assertNotNull(parsed)
        assertEquals("tolong buat caption", parsed!!.prompt)
        assertEquals("Ini captionnya", parsed.output)
    }

    @Test
    fun `format Human dan AI`() {
        val parsed = ChatExportParser.parse("Human: pertanyaan saya\nAI: jawaban saya")
        assertEquals("pertanyaan saya", parsed!!.prompt)
        assertEquals("jawaban saya", parsed.output)
    }

    @Test
    fun `hanya assistant`() {
        val parsed = ChatExportParser.parse("ChatGPT said:\nIni jawaban tanpa prompt.")
        assertNull(parsed!!.prompt)
        assertEquals("Ini jawaban tanpa prompt.", parsed.output)
    }

    @Test
    fun `tanpa penanda mengembalikan null`() {
        assertNull(ChatExportParser.parse("Teks biasa tanpa format chat sama sekali."))
    }

    @Test
    fun `beberapa pesan assistant digabung`() {
        val parsed = ChatExportParser.parse("User: halo\nAssistant: bagian satu\nAssistant: bagian dua")
        assertEquals("halo", parsed!!.prompt)
        assertTrue(parsed.output!!.contains("bagian satu"))
        assertTrue(parsed.output!!.contains("bagian dua"))
    }

    @Test
    fun `marker tidak peka huruf besar kecil`() {
        val parsed = ChatExportParser.parse("USER: tulis puisi\nASSISTANT: ini puisinya")
        assertEquals("tulis puisi", parsed!!.prompt)
        assertEquals("ini puisinya", parsed.output)
    }
}
