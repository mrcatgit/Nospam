package com.nospam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nospam.data.model.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: BlockRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<BlockRule>)

    @Update suspend fun update(rule: BlockRule)
    @Delete suspend fun delete(rule: BlockRule)

    @Query("SELECT * FROM block_rules WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun activeRules(): List<BlockRule>

    @Query("SELECT * FROM block_rules ORDER BY priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<BlockRule>>

    @Query("SELECT COUNT(*) FROM block_rules WHERE enabled = 1")
    fun activeCount(): Flow<Int>
}
