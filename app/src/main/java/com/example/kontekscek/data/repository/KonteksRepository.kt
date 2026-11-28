package com.aldointelligence.kontekscek.data.repository

import com.aldointelligence.kontekscek.data.local.KonteksDao
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import kotlinx.coroutines.flow.Flow

class KonteksRepository(private val dao: KonteksDao) {

    val allCards: Flow<List<ContextCardEntity>> = dao.observeAllCards()
    val allLogs: Flow<List<VerificationLogEntity>> = dao.observeAllLogs()

    suspend fun getCardById(id: Long): ContextCardEntity? = dao.getCardById(id)

    suspend fun getAllCards(): List<ContextCardEntity> = dao.getAllCards()

    suspend fun getAllLogs(): List<VerificationLogEntity> = dao.getAllLogs()

    suspend fun saveCard(card: ContextCardEntity): Long {
        return if (card.id == 0L) {
            dao.insertCard(card)
        } else {
            dao.updateCard(card.copy(updatedAt = System.currentTimeMillis()))
            card.id
        }
    }

    suspend fun deleteCard(card: ContextCardEntity) = dao.deleteCard(card)

    suspend fun saveLog(log: VerificationLogEntity): Long {
        return if (log.id == 0L) {
            dao.insertLog(log)
        } else {
            dao.updateLog(log)
            log.id
        }
    }

    suspend fun deleteLog(log: VerificationLogEntity) = dao.deleteLog(log)

    suspend fun purgeAllLogs() = dao.purgeAllLogs()

    suspend fun purgeAllCards() = dao.purgeAllCards()

    /**
     * Impor cadangan JSON. ID lama dipetakan ulang agar tidak bentrok dengan
     * data yang sudah ada, dan relasi kartu -> jejak tetap terjaga.
     */
    suspend fun restoreBackup(
        cards: List<ContextCardEntity>,
        logs: List<VerificationLogEntity>,
    ): Pair<Int, Int> {
        val existingCards = dao.getAllCards()
        val cardKeyToId = existingCards.associate { (it.title.lowercase().trim() to it.createdAt) to it.id }.toMutableMap()
        val idMap = mutableMapOf<Long, Long>()
        var insertedCards = 0

        cards.forEach { card ->
            val key = card.title.lowercase().trim() to card.createdAt
            val existingId = cardKeyToId[key]
            if (existingId != null) {
                idMap[card.id] = existingId
            } else {
                val newId = dao.insertCard(card.copy(id = 0L))
                idMap[card.id] = newId
                cardKeyToId[key] = newId
                insertedCards++
            }
        }

        val existingLogKeys = dao.getAllLogs().map { it.cardTitle.lowercase().trim() to it.createdAt }.toMutableSet()
        var insertedLogs = 0
        logs.forEach { log ->
            val key = log.cardTitle.lowercase().trim() to log.createdAt
            if (existingLogKeys.add(key)) {
                dao.insertLog(
                    log.copy(
                        id = 0L,
                        cardId = idMap[log.cardId] ?: 0L,
                    ),
                )
                insertedLogs++
            }
        }
        return insertedCards to insertedLogs
    }
}