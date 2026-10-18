package com.salco.dragonfly.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DragonflyDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsAccounts() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                """INSERT INTO accounts (name, type, currency_code, opening_balance, active, include_in_net_worth, created_at)
                   VALUES ('Conto', 'ASSET', 'EUR', 1000, 1, 1, 0)""",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 2, true).use { db ->
            db.query("SELECT COUNT(*) FROM accounts").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3_keepsTransactions() {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """INSERT INTO accounts (id, name, type, currency_code, opening_balance, active, include_in_net_worth, created_at)
                   VALUES (1, 'Conto', 'ASSET', 'EUR', 0, 1, 1, 0), (2, 'Bar', 'EXPENSE', 'EUR', 0, 1, 0, 0)""",
            )
            db.execSQL(
                """INSERT INTO transactions (type, description, date, amount, currency_code, source_account_id, destination_account_id)
                   VALUES ('WITHDRAWAL', 'Caffè', 20000, 120, 'EUR', 1, 2)""",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 3, true).use { db ->
            db.query("SELECT description, category_id FROM transactions").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Caffè", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate3To4_addsBudgets() {
        helper.createDatabase(DB_NAME, 3).close()
        helper.runMigrationsAndValidate(DB_NAME, 4, true).use { db ->
            db.query("SELECT COUNT(*) FROM budgets").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate4To5_addsRecurringTransactions() {
        helper.createDatabase(DB_NAME, 4).close()
        helper.runMigrationsAndValidate(DB_NAME, 5, true).use { db ->
            db.query("SELECT COUNT(*) FROM recurring_transactions").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate5To6_addsBills() {
        helper.createDatabase(DB_NAME, 5).close()
        helper.runMigrationsAndValidate(DB_NAME, 6, true).use { db ->
            db.query("SELECT COUNT(*) FROM bills").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
