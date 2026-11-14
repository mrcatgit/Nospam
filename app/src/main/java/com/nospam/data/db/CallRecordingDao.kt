package com.nospam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nospam.data.model.CallRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {

    @Insert
    suspend fun insert(recording: CallRecording): Long

    @Delete
    suspend fun delete(recording: CallRecording)

    @Query("SELECT * FROM call_recordings ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<CallRecording>>

    @Query("SELECT * FROM call_recordings ORDER BY startedAt ASC")
    suspend fun allOrdered(): List<CallRecording>

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    suspend fun byId(id: Long): CallRecording?
}
