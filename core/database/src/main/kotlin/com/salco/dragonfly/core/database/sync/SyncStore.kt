package com.salco.dragonfly.core.database.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.salco.dragonfly.core.database.DragonflyDatabase
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** A row as exchanged with the server: its columns, with references written as sync ids. */
data class SyncRecord(
    val table: String,
    val syncId: String,
    /** Null for a deletion. */
    val data: JsonObject?,
    val modifiedAt: Long,
) {
    val deleted: Boolean get() = data == null
}

/**
 * Reads local changes and applies remote ones, working on raw SQLite rows so every synced table is
 * handled the same way. References between rows travel as sync ids: they are translated using
 * the foreign keys SQLite declares for each table, so new tables and columns need no extra code.
 */
@Singleton
class SyncStore @Inject constructor(private val database: DragonflyDatabase) {

    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase
    private val tables = SyncedTables.associateBy { it.name }
    private val order = SyncedTables.mapIndexed { i, table -> table.name to i }.toMap()

    /** Local changes not yet sent, parents first; rows referring to rows without a sync id yet wait. */
    suspend fun pendingChanges(): List<SyncRecord> = database.withTransaction {
        query("SELECT table_name, local_key, sync_id, modified_at, deleted FROM sync_meta WHERE dirty = 1") { c ->
            Pending(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4) == 1)
        }.filter { it.table in tables }
            .sortedBy { order.getValue(it.table) }
            .mapNotNull { pending ->
                if (pending.deleted) {
                    SyncRecord(pending.table, pending.syncId, null, pending.modifiedAt)
                } else {
                    readRow(pending.table, pending.localKey)?.let { SyncRecord(pending.table, pending.syncId, it, pending.modifiedAt) }
                }
            }
    }

    /** Marks [sent] as delivered, unless the row changed again meanwhile. */
    suspend fun markSent(sent: List<SyncRecord>) = database.withTransaction {
        sent.forEach { record ->
            if (record.deleted) {
                db.execSQL("DELETE FROM sync_meta WHERE sync_id = ? AND deleted = 1 AND modified_at = ?", arrayOf<Any?>(record.syncId, record.modifiedAt))
            } else {
                db.execSQL("UPDATE sync_meta SET dirty = 0 WHERE sync_id = ? AND modified_at = ?", arrayOf<Any?>(record.syncId, record.modifiedAt))
            }
        }
    }

    /**
     * Applies changes received from the server. The newest change wins: a local edit made after the
     * remote one is kept (and sent later). Returns how many records changed local data.
     */
    suspend fun applyRemote(records: List<SyncRecord>): Int = database.withTransaction {
        db.execSQL("UPDATE sync_state SET applying = 1 WHERE id = 1")
        try {
            var applied = 0
            var waiting = records.filter { it.table in tables }.sortedBy { order.getValue(it.table) }
            // A row can arrive before the row it refers to; retry until nothing more resolves.
            while (waiting.isNotEmpty()) {
                val next = waiting.filterNot { record ->
                    when (val result = apply(record)) {
                        ApplyResult.Applied -> { applied++; true }
                        ApplyResult.Skipped -> true
                        ApplyResult.MissingReference -> false
                    }
                }
                if (next.size == waiting.size) break
                waiting = next
            }
            applied
        } finally {
            db.execSQL("UPDATE sync_state SET applying = 0 WHERE id = 1")
        }
    }

    suspend fun lastPulledAt(): String? = database.withTransaction {
        query("SELECT last_pulled_at FROM sync_state WHERE id = 1") { if (it.isNull(0)) null else it.getString(0) }.firstOrNull()
    }

    suspend fun setLastPulledAt(value: String?) = database.withTransaction {
        db.execSQL("UPDATE sync_state SET last_pulled_at = ? WHERE id = 1", arrayOf(value))
    }

    /** Number of local changes waiting to be sent. */
    suspend fun pendingCount(): Int = database.withTransaction {
        query("SELECT COUNT(*) FROM sync_meta WHERE dirty = 1") { it.getInt(0) }.first()
    }

    /** Forgets what was synced, e.g. after signing out, so a new account starts from scratch. */
    suspend fun reset() = database.withTransaction {
        db.execSQL("UPDATE sync_meta SET dirty = 1")
        db.execSQL("DELETE FROM sync_meta WHERE deleted = 1")
        db.execSQL("UPDATE sync_state SET last_pulled_at = NULL WHERE id = 1")
    }

    private fun apply(record: SyncRecord): ApplyResult {
        val table = tables.getValue(record.table)
        val meta = query("SELECT local_key, modified_at, dirty, deleted FROM sync_meta WHERE sync_id = ?", record.syncId) { c ->
            Meta(c.getString(0), c.getLong(1), c.getInt(2) == 1, c.getInt(3) == 1)
        }.firstOrNull()
        if (meta != null && meta.dirty && meta.modifiedAt > record.modifiedAt) return ApplyResult.Skipped

        if (record.deleted) {
            if (meta == null) return ApplyResult.Skipped
            if (!meta.deleted) db.execSQL("DELETE FROM ${table.name} WHERE ${table.where()}", table.keyArgs(meta.localKey))
            db.execSQL("DELETE FROM sync_meta WHERE sync_id = ?", arrayOf(record.syncId))
            return ApplyResult.Applied
        }

        val values = toLocalValues(table, record.data!!) ?: return ApplyResult.MissingReference
        val localKey = if (meta != null && !meta.deleted &&
            db.update(table.name, 0, values, table.where(), table.keyArgs(meta.localKey)) > 0
        ) {
            meta.localKey
        } else {
            // New here, or deleted locally by an older change: (re)create the row.
            if (meta != null && table.keyColumns == listOf("id")) values.put("id", meta.localKey.toLong())
            // A cross-reference may already exist here under another sync id: keep one.
            val conflict = if (table.keyColumns == listOf("id")) SQLiteDatabase.CONFLICT_NONE else SQLiteDatabase.CONFLICT_IGNORE
            val rowId = db.insert(table.name, conflict, values)
            if (table.keyColumns == listOf("id")) rowId.toString() else table.keyColumns.joinToString(":") { values.getAsString(it) }
        }
        db.execSQL("DELETE FROM sync_meta WHERE table_name = ? AND local_key = ? AND sync_id <> ?", arrayOf(table.name, localKey, record.syncId))
        db.execSQL(
            "INSERT OR REPLACE INTO sync_meta (table_name, local_key, sync_id, modified_at, dirty, deleted) VALUES (?, ?, ?, ?, 0, 0)",
            arrayOf<Any?>(table.name, localKey, record.syncId, record.modifiedAt),
        )
        return ApplyResult.Applied
    }

    /** The row's columns, without its own id and with references replaced by sync ids. */
    private fun readRow(tableName: String, localKey: String): JsonObject? {
        val table = tables.getValue(tableName)
        val references = foreignKeys(tableName)
        val row = query("SELECT * FROM $tableName WHERE ${table.where()}", *table.keyArgs(localKey)) { c ->
            (0 until c.columnCount).associate { i -> c.getColumnName(i) to c.jsonAt(i) }
        }.firstOrNull() ?: return null
        val values = row.filterKeys { !(it == "id" && table.keyColumns == listOf("id")) }.mapValues { (column, value) ->
            val parent = references[column] ?: return@mapValues value
            if (value is JsonNull) return@mapValues value
            val parentId = (value as JsonPrimitive).content
            syncIdOf(parent, parentId)?.let(::JsonPrimitive) ?: return null
        }
        return JsonObject(values)
    }

    /** Remote columns as local values, or null when a referenced row is not here yet. */
    private fun toLocalValues(table: SyncedTable, data: JsonObject): ContentValues? {
        val columns = columns(table.name)
        val references = foreignKeys(table.name)
        val values = ContentValues()
        data.forEach { (column, value) ->
            // Columns this version of the app does not know are left out.
            if (column !in columns || (column == "id" && table.keyColumns == listOf("id"))) return@forEach
            val parent = references[column]
            if (parent != null && value !is JsonNull) {
                val localId = localKeyOf((value as JsonPrimitive).content) ?: return null
                values.put(column, localId.toLong())
            } else {
                values.put(column, value)
            }
        }
        return values
    }

    private fun syncIdOf(table: String, localKey: String): String? =
        query("SELECT sync_id FROM sync_meta WHERE table_name = ? AND local_key = ? AND deleted = 0", table, localKey) { it.getString(0) }.firstOrNull()

    private fun localKeyOf(syncId: String): String? =
        query("SELECT local_key FROM sync_meta WHERE sync_id = ? AND deleted = 0", syncId) { it.getString(0) }.firstOrNull()

    private val foreignKeyCache = mutableMapOf<String, Map<String, String>>()
    private val columnCache = mutableMapOf<String, Set<String>>()

    private fun foreignKeys(table: String) = foreignKeyCache.getOrPut(table) {
        query("PRAGMA foreign_key_list($table)") { c -> c.getString(c.getColumnIndexOrThrow("from")) to c.getString(c.getColumnIndexOrThrow("table")) }.toMap()
    }

    private fun columns(table: String) = columnCache.getOrPut(table) {
        query("PRAGMA table_info($table)") { c -> c.getString(c.getColumnIndexOrThrow("name")) }.toSet()
    }

    private fun <T> query(sql: String, vararg args: Any?, map: (Cursor) -> T): List<T> =
        db.query(sql, args).use { c -> buildList { while (c.moveToNext()) add(map(c)) } }

    private fun SyncedTable.where() = keyColumns.joinToString(" AND ") { "$it = ?" }
    private fun SyncedTable.keyArgs(localKey: String): Array<Any?> = localKey.split(":").map { it.toLong() }.toTypedArray()

    private data class Pending(val table: String, val localKey: String, val syncId: String, val modifiedAt: Long, val deleted: Boolean)
    private data class Meta(val localKey: String, val modifiedAt: Long, val dirty: Boolean, val deleted: Boolean)
    private enum class ApplyResult { Applied, Skipped, MissingReference }
}

private fun Cursor.jsonAt(i: Int): JsonElement = when (getType(i)) {
    Cursor.FIELD_TYPE_NULL -> JsonNull
    Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(i))
    Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(i))
    else -> JsonPrimitive(getString(i))
}

private fun ContentValues.put(column: String, value: JsonElement) {
    when {
        value is JsonNull -> putNull(column)
        value is JsonPrimitive && value.isString -> put(column, value.content)
        value is JsonPrimitive && value.longOrNull != null -> put(column, value.longOrNull)
        value is JsonPrimitive && value.doubleOrNull != null -> put(column, value.doubleOrNull)
        value is JsonPrimitive && value.booleanOrNull != null -> put(column, if (value.booleanOrNull == true) 1L else 0L)
        else -> put(column, (value as? JsonPrimitive)?.contentOrNull)
    }
}
