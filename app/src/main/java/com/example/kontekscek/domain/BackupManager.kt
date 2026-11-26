package com.aldointelligence.kontekscek.domain

import android.content.Context
import android.net.Uri
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream

/**
 * Cadangan data lokal (JSON) supaya pengguna bisa memindahkan Kartu Konteks &
 * Jejak Verifikasi tanpa server. Tetap 100% lokal & bisa dihapus kapan saja.
 */
object BackupManager {

    private const val FORMAT_VERSION = 1

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
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: error("File cadangan tidak dapat dibaca")
        return gson.fromJson(raw, BackupPayload::class.java)
            ?: error("Format file cadangan tidak dikenali")
    }
}
