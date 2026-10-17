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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
