package com.nospam.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Valida le migrazioni dello schema Room contro gli schemi esportati in app/schemas.
 * Test strumentato: richiede un device/emulatore (gira in CI).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NoSpamDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2To3() {
        // Crea il DB alla v1 e chiudilo.
        helper.createDatabase(dbName, 1).close()

        // Applica e valida ogni migrazione contro lo schema esportato.
        helper.runMigrationsAndValidate(dbName, 2, true, NoSpamDatabase.MIGRATION_1_2)
        helper.runMigrationsAndValidate(dbName, 3, true, NoSpamDatabase.MIGRATION_2_3)
    }

    @Test
    fun migratedDbOpensWithRoom() {
        helper.createDatabase(dbName, 1).close()
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NoSpamDatabase::class.java,
            dbName,
        )
            .addMigrations(NoSpamDatabase.MIGRATION_1_2, NoSpamDatabase.MIGRATION_2_3)
            .build()
        // Forza l'apertura: Room rivalida lo schema finale.
        assertTrue(db.openHelper.writableDatabase.isOpen)
        db.close()
    }
}
