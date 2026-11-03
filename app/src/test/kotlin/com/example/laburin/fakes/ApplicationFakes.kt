package com.example.laburin.fakes

import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.auth.StoredSession
import com.example.laburin.data.local.ApplicationDao
import com.example.laburin.data.local.ApplicationEntity
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionStore(initial: StoredSession? = null) : SessionStore {
    val saved = MutableStateFlow(initial)
    override val stored: Flow<StoredSession?> = saved
    override suspend fun save(session: StoredSession) { saved.value = session }
    override suspend fun clear() { saved.value = null }
}

val CANDIDATE_STORED = StoredSession("token-candidate", CANDIDATE_SESSION)

class FakeApplicationDao : ApplicationDao {
    val rows = MutableStateFlow<List<ApplicationEntity>>(emptyList())

    override fun observeMine(userId: String) =
        rows.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun observeForOffer(userId: String, offerId: String) =
        rows.map { list -> list.firstOrNull { it.userId == userId && it.offerId == offerId } }

    override suspend fun find(id: String) = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(application: ApplicationEntity): Long {
        if (rows.value.any { it.id == application.id || (it.userId == application.userId && it.offerId == application.offerId) }) return -1
        rows.value = rows.value + application
        return 1
    }

    override suspend fun updateStatus(id: String, status: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    fun statusOf(id: String) = rows.value.first { it.id == id }.status
}

class FakePendingActionDao : PendingActionDao {
    val rows = MutableStateFlow<List<PendingActionEntity>>(emptyList())
    override suspend fun all() = rows.value.sortedBy { it.createdAt }
    override fun observeCount() = rows.map { it.size }
    override suspend fun insert(action: PendingActionEntity) {
        if (rows.value.none { it.id == action.id }) rows.value = rows.value + action
    }
    override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
}

object ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
