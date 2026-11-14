package com.nospam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nospam.data.model.BlockRule
import com.nospam.data.model.CallRecord

@Database(
    entities = [CallRecord::class, BlockRule::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NoSpamDatabase : RoomDatabase() {

    abstract fun callRecordDao(): CallRecordDao
    abstract fun blockRuleDao(): BlockRuleDao

    companion object {
        fun build(context: Context): NoSpamDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NoSpamDatabase::class.java,
                "nospam.db",
            ).build()
    }
}
