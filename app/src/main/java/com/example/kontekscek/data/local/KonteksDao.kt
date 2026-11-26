package com.aldointelligence.kontekscek.data.local

import androidx.room.*
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KonteksDao {

    // ===== CRUD KARTU KONTEKS =====
    @Query("SELECT * FROM context_cards ORDER BY updatedAt DESC")
    fun observeAllCards(): Flow<List<ContextCardEntity>>

    @Query("SELECT * FROM context_cards ORDER BY updatedAt DESC")
    suspend fun getAllCards(): List<ContextCardEntity>

    @Query("SELECT * FROM context_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): ContextCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: ContextCardEntity): Long

    @Update
    suspend fun updateCard(card: ContextCardEntity)

    @Delete
    suspend fun deleteCard(card: ContextCardEntity)

    // ===== CRUD JEJAK VERIFIKASI =====
    @Query("SELECT * FROM verification_logs ORDER BY createdAt DESC")
    fun observeAllLogs(): Flow<List<VerificationLogEntity>>

    @Query("SELECT * FROM verification_logs ORDER BY createdAt DESC")
    suspend fun getAllLogs(): List<VerificationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VerificationLogEntity): Long

    @Update
    suspend fun updateLog(log: VerificationLogEntity)

    @Delete
    suspend fun deleteLog(log: VerificationLogEntity)

    @Query("DELETE FROM verification_logs")
    suspend fun purgeAllLogs()
}