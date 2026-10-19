package com.salco.dragonfly.core.database.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sync bookkeeping for one row of a synced table, kept apart from the row itself so entities and
 * their DAOs stay unchanged. Written by SQLite triggers on every local change (see [SyncTriggers])
 * and by the sync engine when it applies remote changes.
 */
@Entity(
    tableName = "sync_meta",
    primaryKeys = ["table_name", "local_key"],
    indices = [Index(value = ["sync_id"], unique = true), Index("dirty")],
)
data class SyncMetaEntity(
    @ColumnInfo(name = "table_name") val tableName: String,
    /** The row's primary key as text: "12", or "12:5" for a cross-reference. */
    @ColumnInfo(name = "local_key") val localKey: String,
    /** Id shared by every device, used by the remote copy. */
    @ColumnInfo(name = "sync_id") val syncId: String,
    /** When the row last changed, in epoch milliseconds; the newest change wins. */
    @ColumnInfo(name = "modified_at") val modifiedAt: Long,
    /** Changed locally and not yet sent. */
    val dirty: Boolean,
    /** The local row was deleted; the deletion still has to be sent. */
    val deleted: Boolean,
)

/** Single row (id 1) with the sync engine's state. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    /** 1 while remote changes are being applied, so the triggers do not mark them as local edits. */
    val applying: Boolean = false,
    /** Server time of the newest remote change already received, ISO-8601; null before the first sync. */
    @ColumnInfo(name = "last_pulled_at") val lastPulledAt: String? = null,
)
