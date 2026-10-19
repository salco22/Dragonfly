package com.salco.dragonfly.core.database.sync

import androidx.sqlite.db.SupportSQLiteDatabase

/** A table whose rows are synced, with the SQL expression giving a row's key in [SyncMetaEntity.localKey]. */
data class SyncedTable(val name: String, val keyColumns: List<String> = listOf("id")) {
    internal fun keyOf(row: String) = keyColumns.joinToString(" || ':' || ") { "CAST($row.$it AS TEXT)" }
}

/** Synced tables, parents before children so references can be resolved in this order. */
val SyncedTables = listOf(
    SyncedTable("accounts"),
    SyncedTable("categories"),
    SyncedTable("tags"),
    SyncedTable("budgets"),
    SyncedTable("bills"),
    SyncedTable("transaction_groups"),
    SyncedTable("transactions"),
    SyncedTable("transaction_tags", listOf("transaction_id", "tag_id")),
    SyncedTable("recurring_transactions"),
    SyncedTable("piggy_banks"),
    SyncedTable("piggy_bank_events"),
    SyncedTable("rules"),
    SyncedTable("rule_triggers"),
    SyncedTable("rule_actions"),
)

/**
 * Keeps [SyncMetaEntity] up to date with SQLite triggers (written without UPSERT, which SQLite
 * only supports from Android 11), so every write through any DAO is
 * tracked, cascades included. Installed each time the database opens (idempotent), and rows that
 * existed before tracking get their metadata once.
 */
object SyncTriggers {

    private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
    private const val NEW_UUID =
        "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
            "substr('89ab', 1 + (abs(random()) % 4), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"
    private const val NOT_APPLYING = "COALESCE((SELECT applying FROM sync_state WHERE id = 1), 0) = 0"

    fun install(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO sync_state (id, applying, last_pulled_at) VALUES (1, 0, NULL)")
        // A crash while applying remote changes must not leave tracking switched off.
        db.execSQL("UPDATE sync_state SET applying = 0 WHERE id = 1")
        SyncedTables.forEach { table ->
            val upsertMeta = { row: String ->
                """
                INSERT OR IGNORE INTO sync_meta (table_name, local_key, sync_id, modified_at, dirty, deleted)
                VALUES ('${table.name}', ${table.keyOf(row)}, $NEW_UUID, $NOW_MS, 1, 0);
                UPDATE sync_meta SET modified_at = $NOW_MS, dirty = 1, deleted = 0
                WHERE table_name = '${table.name}' AND local_key = ${table.keyOf(row)};
                """
            }
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS sync_${table.name}_insert AFTER INSERT ON ${table.name}
                WHEN $NOT_APPLYING BEGIN ${upsertMeta("NEW")} END
                """,
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS sync_${table.name}_update AFTER UPDATE ON ${table.name}
                WHEN $NOT_APPLYING BEGIN ${upsertMeta("NEW")} END
                """,
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS sync_${table.name}_delete AFTER DELETE ON ${table.name}
                WHEN $NOT_APPLYING BEGIN
                    UPDATE sync_meta SET deleted = 1, dirty = 1, modified_at = $NOW_MS
                    WHERE table_name = '${table.name}' AND local_key = ${table.keyOf("OLD")};
                END
                """,
            )
            // Rows written before tracking existed (or while it was off) still need an id.
            db.execSQL(
                """
                INSERT OR IGNORE INTO sync_meta (table_name, local_key, sync_id, modified_at, dirty, deleted)
                SELECT '${table.name}', ${table.keyOf(table.name)}, $NEW_UUID, $NOW_MS, 1, 0 FROM ${table.name}
                WHERE NOT EXISTS (
                    SELECT 1 FROM sync_meta m WHERE m.table_name = '${table.name}' AND m.local_key = ${table.keyOf(table.name)}
                )
                """,
            )
        }
    }
}
