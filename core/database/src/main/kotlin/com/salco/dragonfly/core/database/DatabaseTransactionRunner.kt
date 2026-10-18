package com.salco.dragonfly.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs a block in a single database transaction, so callers outside this module stay atomic. */
interface DatabaseTransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(
    private val database: DragonflyDatabase,
) : DatabaseTransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = database.withTransaction { block() }
}
