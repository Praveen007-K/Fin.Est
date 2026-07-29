package com.pkoder.finest.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one irreversible change in this release: `fallbackToDestructiveMigration` was replaced with
 * [MIGRATION_3_4]. This builds a genuine v3 database with raw SQL, opens it with the current Room
 * schema, and checks nothing was lost.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration_test_db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrates_v3_to_v4_keeping_rows_and_backfilling_synced() {
        createV3DatabaseWithRows()

        val db = Room.databaseBuilder(context, FinanceDatabase::class.java, dbName)
            .addMigrations(MIGRATION_3_4)
            .build()

        runBlocking {
            val debits = db.debitEntryDao().observeDebits().first()
            assertEquals(2, debits.size)

            val cloudRow = debits.single { it.firestoreId == "remote123" }
            assertTrue("rows already in Firestore stay synced", cloudRow.synced)
            assertEquals("Food", cloudRow.category)
            assertEquals(250.0, cloudRow.amount, 0.001)

            val offlineRow = debits.single { it.firestoreId.startsWith("local_") }
            assertFalse("offline rows must be queued for upload", offlineRow.synced)

            val credits = db.creditEntryDao().observeCredits().first()
            assertEquals(1, credits.size)
            assertTrue(credits.first().synced)

            // Pending SMS rows are local-only and used to be wiped by the destructive fallback.
            assertEquals(1, db.pendingTransactionDao().observePending().first().size)
        }

        db.close()
    }

    /** The exact schema Room generated for version 3, before `synced` existed. */
    private fun createV3DatabaseWithRows() {
        val helper = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        helper.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS debit_entries (
                    firestoreId TEXT NOT NULL, category TEXT NOT NULL, paymentMethod TEXT NOT NULL,
                    bank TEXT NOT NULL, amount REAL NOT NULL, description TEXT,
                    timestamp INTEGER NOT NULL, PRIMARY KEY(firestoreId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS credit_entries (
                    firestoreId TEXT NOT NULL, source TEXT NOT NULL, amount REAL NOT NULL,
                    timestamp INTEGER NOT NULL, PRIMARY KEY(firestoreId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pending_transactions (
                    id TEXT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, bank TEXT NOT NULL,
                    paymentMethod TEXT NOT NULL, category TEXT NOT NULL, source TEXT NOT NULL,
                    description TEXT NOT NULL, timestamp INTEGER NOT NULL, rawSms TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                "INSERT INTO debit_entries VALUES ('remote123','Food','UPI','SBI',250.0,'lunch',1000)"
            )
            db.execSQL(
                "INSERT INTO debit_entries VALUES ('local_1755','Transport','Cash','BOB',60.0,NULL,2000)"
            )
            db.execSQL("INSERT INTO credit_entries VALUES ('remote456','Salary',50000.0,3000)")
            db.execSQL(
                "INSERT INTO pending_transactions VALUES ('abc','DEBIT',99.0,'HDFC','UPI'," +
                    "'Uncategorized','','note',4000,'raw sms')"
            )
            db.version = 3
        }
    }
}
