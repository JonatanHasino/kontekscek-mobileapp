package com.aldointelligence.kontekscek.domain

import android.content.Context
import android.net.Uri
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Cadangan data lokal (JSON) untuk memindahkan Preset Konteks & arsip jejak.
 * Backup v1 tetap dapat dibaca; versi 2 menambahkan validasi format.
 */
object BackupManager {

    private const val FORMAT_VERSION = 2

    data class BackupPayload(
        val app: String = "KonteksCek",
        val version: Int = FORMAT_VERSION,
        val exportedAt: Long = System.currentTimeMillis(),
        val cards: List<ContextCardEntity> = emptyList(),
        val logs: List<VerificationLogEntity> = emptyList(),
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun export(
        context: Context,
        cards: List<ContextCardEntity>,
        logs: List<VerificationLogEntity>,
    ): File {
        val payload = BackupPayload(cards = cards, logs = logs)
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "KonteksCek_Backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { out ->
            out.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun read(context: Context, uri: Uri): BackupPayload {
        val raw =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readLimited(MAX_BACKUP_BYTES).toString(Charsets.UTF_8)
            } ?: error("File cadangan tidak dapat dibaca")
        val payload =
            runCatching { gson.fromJson(raw, BackupPayload::class.java) }
                .getOrElse { error("File cadangan bukan JSON yang valid.") }
                ?: error("Format file cadangan tidak dikenali")
        if (payload.app != "KonteksCek") {
            error("File ini bukan cadangan KonteksCek.")
        }
        if (payload.version !in 1..FORMAT_VERSION) {
            error("Cadangan berasal dari versi aplikasi yang lebih baru.")
        }
        return payload
    }

    private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024

    private fun InputStream.readLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) error("File cadangan terlalu besar (maksimal 20 MB).")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
