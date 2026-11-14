package com.nospam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nospam.data.model.BlockRule
import com.nospam.data.model.CallRecord
import com.nospam.data.model.CallRecording

@Database(
    entities = [CallRecord::class, BlockRule::class, CallRecording::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NoSpamDatabase : RoomDatabase() {

    abstract fun callRecordDao(): CallRecordDao
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun callRecordingDao(): CallRecordingDao

    companion object {
        /**
         * v1 → v2: aggiunge la tabella delle registrazioni audio.
         * Lo statement rispecchia lo schema generato da Room (vedi schema JSON esportato
         * in app/schemas): se si modificano i campi di [CallRecording], rigenerare e
         * validare con un MigrationTest prima del rilascio.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `call_recordings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`number` TEXT NOT NULL, " +
                        "`e164Number` TEXT, " +
                        "`startedAt` INTEGER NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, " +
                        "`filePath` TEXT NOT NULL, " +
                        "`fileSha256` TEXT NOT NULL, " +
                        "`note` TEXT, " +
                        "`linkedCallRecordId` INTEGER)"
                )
            }
        }

        fun build(context: Context): NoSpamDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NoSpamDatabase::class.java,
                "nospam.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
