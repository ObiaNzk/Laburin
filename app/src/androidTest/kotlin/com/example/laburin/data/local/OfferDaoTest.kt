package com.example.laburin.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class OfferDaoTest {

    private lateinit var db: LaburinDatabase
    private lateinit var dao: OfferDao

    // Box around the Obelisco; "outside" is ~10 km away.
    private val minLat = -34.62
    private val maxLat = -34.58
    private val minLng = -58.40
    private val maxLng = -58.36

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LaburinDatabase::class.java).build()
        dao = db.offerDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeInAreaFiltersByBoxCategoryAndTitle() = runTest {
        dao.upsertAll(
            listOf(
                entity("in-mech", "Ayudante de taller", Category.MECHANICS, -34.60, -58.38),
                entity("in-gastro", "Mozo", Category.GASTRONOMY, -34.60, -58.38),
                entity("outside", "Mozo", Category.GASTRONOMY, -34.70, -58.38),
            )
        )

        assertEquals(setOf("in-mech", "in-gastro"), ids(category = null, query = ""))
        assertEquals(setOf("in-gastro"), ids(category = Category.GASTRONOMY, query = ""))
        assertEquals(setOf("in-mech"), ids(category = null, query = "taller"))
    }

    @Test
    fun replaceInAreaRemovesWhatTheServerNoLongerReturnsOnlyInsideThatArea() = runTest {
        dao.upsertAll(
            listOf(
                entity("stale", "Viejo", Category.STOCK, -34.60, -58.38),
                entity("kept", "Sigue", Category.STOCK, -34.60, -58.38),
                entity("other-area", "Lejos", Category.STOCK, -34.70, -58.38),
            )
        )

        dao.replaceInArea(
            minLat, maxLat, minLng, maxLng, category = null,
            offers = listOf(entity("kept", "Sigue (editado)", Category.STOCK, -34.60, -58.38)),
        )

        assertEquals(setOf("kept"), ids(category = null, query = ""))
        val all = dao.observeInArea(-90.0, 90.0, -180.0, 180.0, null, "").first()
        assertEquals(setOf("kept", "other-area"), all.map { it.id }.toSet())
        assertEquals("Sigue (editado)", all.first { it.id == "kept" }.title)
    }

    @Test
    fun replaceInAreaWithCategoryOnlyTouchesThatCategory() = runTest {
        dao.upsertAll(
            listOf(
                entity("mech", "Taller", Category.MECHANICS, -34.60, -58.38),
                entity("gastro", "Mozo", Category.GASTRONOMY, -34.60, -58.38),
            )
        )

        dao.replaceInArea(minLat, maxLat, minLng, maxLng, Category.MECHANICS, offers = emptyList())

        assertEquals(setOf("gastro"), ids(category = null, query = ""))
    }

    private suspend fun ids(category: Category?, query: String) =
        dao.observeInArea(minLat, maxLat, minLng, maxLng, category, query).first().map { it.id }.toSet()

    private fun entity(id: String, title: String, category: Category, lat: Double, lng: Double) = OfferEntity(
        id = id, title = title, category = category, description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = lat, longitude = lng,
        updatedAt = Instant.parse("2026-11-01T12:00:00Z"),
    )
}
