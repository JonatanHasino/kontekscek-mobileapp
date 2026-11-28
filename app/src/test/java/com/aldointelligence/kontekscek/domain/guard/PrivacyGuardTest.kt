package com.aldointelligence.kontekscek.domain.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyGuardTest {

    @Test
    fun `animasi tidak dianggap NIM`() {
        val result = PrivacyGuard.sanitize("Buat animasi sederhana untuk presentasi.", listOf("NIM"))
        assertFalse(result.hasFindings)
        assertEquals("Buat animasi sederhana untuk presentasi.", result.sanitized)
    }

    @Test
    fun `NIM utuh sebagai kata tetap disensor`() {
        val result = PrivacyGuard.sanitize("NIM 2532035 milik mahasiswa.", listOf("NIM"))
        assertTrue(result.findings.any { it.type == "FORBIDDEN" })
        assertTrue(result.sanitized.contains("[REDACTED:FORBIDDEN]"))
        assertFalse(result.sanitized.contains("NIM 2532035"))
    }

    @Test
    fun `forbidden case-insensitive`() {
        val result = PrivacyGuard.sanitize("Data klien rahasia", listOf("Klien"))
        assertTrue(result.findings.any { it.type == "FORBIDDEN" })
    }

    @Test
    fun `JWT disensor`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature123456"
        val result = PrivacyGuard.sanitize("Authorization: $jwt")
        assertTrue(result.findings.any { it.type == "JWT" })
        assertTrue(result.sanitized.contains("[REDACTED:JWT]"))
        assertFalse(result.sanitized.contains(jwt))
    }

    @Test
    fun `GitHub token disensor`() {
        val token = "ghp_" + "a".repeat(36)
        val result = PrivacyGuard.sanitize("token $token")
        assertTrue(result.findings.any { it.type == "API_KEY" })
        assertFalse(result.sanitized.contains(token))
    }

    @Test
    fun `AWS access key disensor`() {
        val result = PrivacyGuard.sanitize("akun AKIAIOSFODNN7EXAMPLE")
        assertTrue(result.findings.any { it.type == "API_KEY" })
        assertFalse(result.sanitized.contains("AKIAIOSFODNN7EXAMPLE"))
    }

    @Test
    fun `Google API key disensor`() {
        val key = "AIza" + "B".repeat(35)
        val result = PrivacyGuard.sanitize("key=$key")
        assertTrue(result.findings.any { it.type == "API_KEY" })
        assertFalse(result.sanitized.contains(key))
    }

    @Test
    fun `private key disensor`() {
        val key = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"
        val result = PrivacyGuard.sanitize("Ini kunci:\n$key")
        assertTrue(result.findings.any { it.type == "PRIVATE_KEY" })
        assertTrue(result.sanitized.contains("[REDACTED:PRIVATE_KEY]"))
    }

    @Test
    fun `email disensor`() {
        val result = PrivacyGuard.sanitize("Hubungi jonatan@example.com besok.")
        assertTrue(result.findings.any { it.type == "EMAIL" })
        assertTrue(result.sanitized.contains("[REDACTED:EMAIL]"))
    }

    @Test
    fun `nomor HP 0812 disensor`() {
        val result = PrivacyGuard.sanitize("WA saya 081234567890 ya")
        assertTrue(result.findings.any { it.type == "PHONE_ID" })
        assertFalse(result.sanitized.contains("081234567890"))
    }

    @Test
    fun `nomor HP plus 62 disensor`() {
        val result = PrivacyGuard.sanitize("Hubungi +6281234567890")
        assertTrue(result.findings.any { it.type == "PHONE_ID" })
        assertFalse(result.sanitized.contains("+6281234567890"))
    }

    @Test
    fun `nomor kartu valid Luhn disensor`() {
        val result = PrivacyGuard.sanitize("Kartu 4111 1111 1111 1111")
        assertTrue(result.findings.any { it.type == "CARD" })
        assertFalse(result.sanitized.contains("4111"))
    }

    @Test
    fun `nomor kartu tidak valid Luhn tidak disensor`() {
        val result = PrivacyGuard.sanitize("Nomor transaksi 4111 1111 1111 1112")
        assertFalse(result.findings.any { it.type == "CARD" })
    }

    @Test
    fun `NIK dengan provinsi valid disensor`() {
        val result = PrivacyGuard.sanitize("NIK 3171234567890123")
        assertTrue(result.findings.any { it.type == "NIK" || it.type == "CARD" })
        assertTrue(result.sanitized.contains("[REDACTED:"))
    }

    @Test
    fun `16 digit dengan provinsi tidak valid bukan NIK`() {
        val result = PrivacyGuard.sanitize("ID 9912345678901234")
        assertFalse(result.findings.any { it.type == "NIK" })
        assertFalse(result.findings.any { it.type == "CARD" })
    }

    @Test
    fun `password assignment disensor`() {
        val result = PrivacyGuard.sanitize("password=rahasia123")
        assertTrue(result.findings.any { it.type == "SECRET_ASSIGNMENT" })
        assertFalse(result.sanitized.contains("rahasia123"))
    }

    @Test
    fun `api key assignment disensor`() {
        val result = PrivacyGuard.sanitize("api_key: abcdef123456")
        assertTrue(result.findings.any { it.type == "SECRET_ASSIGNMENT" })
        assertFalse(result.sanitized.contains("abcdef123456"))
    }

    @Test
    fun `beberapa email dihitung`() {
        val result = PrivacyGuard.sanitize("a@example.com dan b@example.com")
        val email = result.findings.first { it.type == "EMAIL" }
        assertEquals(2, email.count)
    }

    @Test
    fun `teks normal tanpa temuan`() {
        val result = PrivacyGuard.sanitize("Tolong buatkan ringkasan laporan mingguan dalam bentuk poin.")
        assertFalse(result.hasFindings)
        assertEquals("Tolong buatkan ringkasan laporan mingguan dalam bentuk poin.", result.sanitized)
    }

    @Test
    fun `findings tidak membocorkan isi`() {
        val secret = "ghp_" + "c".repeat(36)
        val result = PrivacyGuard.sanitize("token $secret", listOf("rahasia"))
        assertTrue(result.findings.isNotEmpty())
        assertFalse(result.findings.joinToString { it.type }.contains(secret))
    }

    @Test
    fun `kata terlarang multi kata`() {
        val result = PrivacyGuard.sanitize("Jangan pakai data gaji karyawan.", listOf("data gaji"))
        assertTrue(result.findings.any { it.type == "FORBIDDEN" })
        assertFalse(result.sanitized.contains("data gaji", ignoreCase = true))
    }
}
