package com.example.laburin.data.local

import androidx.room.withTransaction
import javax.inject.Inject

/** Lets repositories group writes atomically without depending on RoomDatabase (keeps them JVM-testable). */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(private val db: LaburinDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
