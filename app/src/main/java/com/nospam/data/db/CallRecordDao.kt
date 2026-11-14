package com.nospam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nospam.data.model.CallRecord
import com.nospam.data.model.Verdict
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {

    @Insert
    suspend fun insert(record: CallRecord): Long

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallRecord>>

    @Query("SELECT * FROM call_records ORDER BY id DESC LIMIT 1")
    suspend fun latest(): CallRecord?

    @Query("SELECT * FROM call_records ORDER BY id ASC")
    suspend fun allOrdered(): List<CallRecord>

    @Query("SELECT * FROM call_records WHERE e164Number = :e164 ORDER BY timestamp DESC")
    suspend fun byNumber(e164: String): List<CallRecord>

    @Query("SELECT COUNT(*) FROM call_records WHERE verdict = :verdict")
    fun countByVerdict(verdict: Verdict): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_records")
    fun total(): Flow<Int>

    /** Numeri più frequenti negli ultimi [sinceMillis], per la dashboard. */
    @Query(
        "SELECT e164Number FROM call_records " +
            "WHERE timestamp >= :sinceMillis AND e164Number IS NOT NULL " +
            "GROUP BY e164Number ORDER BY COUNT(*) DESC LIMIT :limit"
    )
    suspend fun topOffenders(sinceMillis: Long, limit: Int): List<String>
}
