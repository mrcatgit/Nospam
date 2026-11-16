package com.nospam.data.db

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Valida la catena di migrazioni v1 → v2 → v3.
 *
 * Crea un database alla v1 con lo schema originale (SQL grezzo), poi lo apre con Room
 * applicando MIGRATION_1_2 e MIGRATION_2_3: Room esegue le migrazioni e **rivalida lo
 * schema finale** contro quello atteso (v3), lanciando se una migrazione è errata. Questo
 * approccio non dipende dagli schemi storici esportati (KSP esporta solo il corrente).
 *
 * Test strumentato: richiede un device/emulatore (gira in CI).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before @After fun cleanUp() {
        context.deleteDatabase(dbName)
    }

    /** Crea il database alla versione 1 con lo schema originale (CallRecord + BlockRule). */
    private fun createV1Database() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `call_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`rawNumber` TEXT NOT NULL, `e164Number` TEXT, `regionOrCarrier` TEXT, " +
                        "`timestamp` INTEGER NOT NULL, `verdict` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `riskScore` INTEGER NOT NULL, " +
                        "`reasons` TEXT NOT NULL, `knownContact` INTEGER NOT NULL, " +
                        "`userNote` TEXT, `prevHash` TEXT NOT NULL, `hash` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `block_rules` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `value` TEXT NOT NULL, `action` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `priority` INTEGER NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close() // forza onCreate (crea lo schema v1, user_version=1)
        helper.close()
    }

    @Test
    fun migratesV1ToV3AndRoomValidatesSchema() {
        createV1Database()
        val db = Room.databaseBuilder(context, NoSpamDatabase::class.java, dbName)
            .addMigrations(NoSpamDatabase.MIGRATION_1_2, NoSpamDatabase.MIGRATION_2_3)
            .build()
        // Aprire esegue le migrazioni e fa rivalidare a Room lo schema finale (v3).
        assertTrue(db.openHelper.writableDatabase.isOpen)
        db.close()
    }
}
