package com.example.laburin.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ApplicationDaoTest {

    private lateinit var db: LaburinDatabase
    private lateinit var applications: ApplicationDao
    private lateinit var pending: PendingActionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LaburinDatabase::class.java).build()
        applications = db.applicationDao()
        pending = db.pendingActionDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeMineReturnsOnlyThatUsersApplicationsNewestFirst() = runTest {
        applications.insert(app("a-old", user = "u-1", offer = "o-1", createdAt = 1))
        applications.insert(app("a-new", user = "u-1", offer = "o-2", createdAt = 2))
        applications.insert(app("a-other", user = "u-2", createdAt = 3))

        assertEquals(listOf("a-new", "a-old"), applications.observeMine("u-1").first().map { it.id })
    }

    @Test
    fun aSecondApplicationToTheSameOfferIsIgnored() = runTest {
        applications.insert(app("a-1", offer = "o-1"))

        applications.insert(app("a-2", offer = "o-1"))

        assertEquals(listOf("a-1"), applications.observeMine("u-1").first().map { it.id })
    }

    @Test
    fun observeForOfferFindsTheUsersApplication() = runTest {
        applications.insert(app("a-1", user = "u-1", offer = "o-1"))

        assertEquals("a-1", applications.observeForOffer("u-1", "o-1").first()?.id)
        assertNull(applications.observeForOffer("u-2", "o-1").first())
    }

    @Test
    fun updateStatusChangesOnlyThatApplication() = runTest {
        applications.insert(app("a-1"))
        applications.insert(app("a-2", offer = "o-2"))

        applications.updateStatus("a-1", "SENT")

        val byId = applications.observeMine("u-1").first().associate { it.id to it.status }
        assertEquals(mapOf("a-1" to "SENT", "a-2" to "PENDING_SEND"), byId)
    }

    @Test
    fun pendingActionsAreProcessedOldestFirstAndCanBeRemoved() = runTest {
        pending.insert(PendingActionEntity("p-2", "APPLY", "a-2", Instant.ofEpochMilli(2)))
        pending.insert(PendingActionEntity("p-1", "APPLY", "a-1", Instant.ofEpochMilli(1)))

        assertEquals(listOf("p-1", "p-2"), pending.all().map { it.id })

        pending.delete("p-1")

        assertEquals(listOf("p-2"), pending.all().map { it.id })
    }

    @Test
    fun actionsQueuedInTheSameMillisecondKeepTheirOrder() = runTest {
        // Ids chosen so that sorting by id would give the wrong order.
        pending.insert(PendingActionEntity("z", "PUBLISH", "o-1", Instant.EPOCH))
        pending.insert(PendingActionEntity("m", "EDIT", "o-1", Instant.EPOCH))
        pending.insert(PendingActionEntity("a", "CLOSE", "o-1", Instant.EPOCH))

        assertEquals(listOf("PUBLISH", "EDIT", "CLOSE"), pending.all().map { it.type })
    }

    @Test
    fun countsPendingActionsAsAFlow() = runTest {
        pending.insert(PendingActionEntity("p-1", "APPLY", "a-1", Instant.EPOCH))

        assertEquals(1, pending.observeCount().first())
    }

    private fun app(id: String, user: String = "u-1", offer: String = "o-1", createdAt: Long = 0) = ApplicationEntity(
        id = id, userId = user, offerId = offer, offerTitle = "Mozo", businessName = "[COMERCIO]",
        status = "PENDING_SEND", createdAt = Instant.ofEpochMilli(createdAt),
    )
}
