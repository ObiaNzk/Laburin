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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomOfferLocalDataSourceTest {

    private lateinit var db: LaburinDatabase
    private lateinit var dataSource: RoomOfferLocalDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LaburinDatabase::class.java).build()
        dataSource = RoomOfferLocalDataSource(db.offerDao(), db.syncMetadataDao(), db.viewedOfferDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeByIdEmitsNullForAnUnknownOffer() = runTest {
        assertNull(dataSource.observeById("missing").first())
    }

    @Test
    fun upsertStoresTheOfferAndObserveByIdReturnsIt() = runTest {
        dataSource.upsert(entity("a", "Mozo"))

        assertEquals("Mozo", dataSource.observeById("a").first()?.title)
    }

    @Test
    fun upsertReplacesAnExistingOffer() = runTest {
        dataSource.upsert(entity("a", "Mozo"))

        dataSource.upsert(entity("a", "Mozo (editado)"))

        assertEquals("Mozo (editado)", dataSource.observeById("a").first()?.title)
    }

    @Test
    fun deleteRemovesOnlyThatOffer() = runTest {
        dataSource.upsert(entity("a", "Mozo"))
        dataSource.upsert(entity("b", "Cajero"))

        dataSource.delete("a")

        assertNull(dataSource.observeById("a").first())
        assertEquals("Cajero", dataSource.observeById("b").first()?.title)
    }

    private fun entity(id: String, title: String) = OfferEntity(
        id = id, title = title, category = Category.GASTRONOMY, description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = -34.60, longitude = -58.38,
        updatedAt = Instant.parse("2026-11-01T12:00:00Z"),
    )
}
