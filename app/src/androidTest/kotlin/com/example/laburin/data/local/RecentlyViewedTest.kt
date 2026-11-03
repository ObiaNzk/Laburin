package com.example.laburin.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RecentlyViewedTest {

    private lateinit var db: LaburinDatabase
    private lateinit var local: RoomOfferLocalDataSource

    private val area = BoundingBox(-34.62, -34.58, -58.40, -58.36)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LaburinDatabase::class.java).build()
        local = RoomOfferLocalDataSource(db.offerDao(), db.syncMetadataDao(), db.viewedOfferDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun recentlyViewedListsViewedOffersNewestFirst() = runTest {
        local.upsert(offer("a"))
        local.upsert(offer("b"))
        local.markViewed("a", Instant.ofEpochMilli(1), "MAP")
        local.markViewed("b", Instant.ofEpochMilli(2), "QR")

        assertEquals(listOf("b", "a"), local.observeRecentlyViewed().first().map { it.id })
    }

    @Test
    fun viewingAgainMovesTheOfferToTheTop() = runTest {
        local.upsert(offer("a"))
        local.upsert(offer("b"))
        local.markViewed("a", Instant.ofEpochMilli(1), "MAP")
        local.markViewed("b", Instant.ofEpochMilli(2), "MAP")

        local.markViewed("a", Instant.ofEpochMilli(3), "MAP")

        assertEquals(listOf("a", "b"), local.observeRecentlyViewed().first().map { it.id })
    }

    @Test
    fun aViewedOfferThatIsNoLongerCachedIsNotListed() = runTest {
        local.markViewed("gone", Instant.EPOCH, "MAP")

        assertEquals(emptyList<String>(), local.observeRecentlyViewed().first().map { it.id })
    }

    @Test
    fun pruningKeepsTheAreaAndViewedOffersAndDropsTheRest() = runTest {
        local.upsert(offer("in-area"))
        local.upsert(offer("far-viewed", lat = -34.70))
        local.upsert(offer("far-not-viewed", lat = -34.70))
        local.markViewed("far-viewed", Instant.EPOCH, "MAP")

        local.prune(area, maxViewed = 30)

        val all = db.offerDao().observeInArea(-90.0, 90.0, -180.0, 180.0, null, "").first().map { it.id }.toSet()
        assertEquals(setOf("in-area", "far-viewed"), all)
    }

    @Test
    fun pruningKeepsOnlyTheMostRecentViewedOffers() = runTest {
        (1..3).forEach { i ->
            local.upsert(offer("v$i", lat = -34.70))
            local.markViewed("v$i", Instant.ofEpochMilli(i.toLong()), "MAP")
        }

        local.prune(area, maxViewed = 2)

        assertEquals(listOf("v3", "v2"), local.observeRecentlyViewed().first().map { it.id })
    }

    private fun offer(id: String, lat: Double = -34.60) = OfferEntity(
        id = id, title = "Puesto $id", category = Category.STOCK, description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = lat, longitude = -58.38,
        updatedAt = Instant.EPOCH,
    )
}
