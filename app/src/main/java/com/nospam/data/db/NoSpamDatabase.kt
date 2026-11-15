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
    version = 3,
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
         * Gli statement rispecchiano lo schema generato da Room, esportato in
         * app/schemas (abilitato da room.schemaLocation in app/build.gradle.kts): se si
         * modificano i campi delle entità, rigenerare e validare con un MigrationTest
         * (androidx.room:room-testing) prima del rilascio.
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

        /** v2 → v3: indici su call_records per lo screening (historyFor, topOffenders). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_records_e164Number` ON `call_records` (`e164Number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_records_timestamp` ON `call_records` (`timestamp`)")
            }
        }

        fun build(context: Context): NoSpamDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NoSpamDatabase::class.java,
                "nospam.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
