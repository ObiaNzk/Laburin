package com.example.laburin.data.local

import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

interface OfferLocalDataSource {
    fun observeInArea(area: BoundingBox, category: Category?, query: String): Flow<List<OfferEntity>>
    suspend fun replaceInArea(area: BoundingBox, category: Category?, offers: List<OfferEntity>)
    fun observeById(id: String): Flow<OfferEntity?>
    suspend fun upsert(offer: OfferEntity)
    suspend fun delete(id: String)
    fun observeLastSyncedAt(): Flow<Instant?>
    suspend fun setLastSyncedAt(instant: Instant)
}

class RoomOfferLocalDataSource @Inject constructor(
    private val offerDao: OfferDao,
    private val syncMetadataDao: SyncMetadataDao,
) : OfferLocalDataSource {

    override fun observeInArea(area: BoundingBox, category: Category?, query: String) =
        offerDao.observeInArea(
            area.minLatitude, area.maxLatitude, area.minLongitude, area.maxLongitude, category, query,
        )

    override suspend fun replaceInArea(area: BoundingBox, category: Category?, offers: List<OfferEntity>) =
        offerDao.replaceInArea(
            area.minLatitude, area.maxLatitude, area.minLongitude, area.maxLongitude, category, offers,
        )

    override fun observeById(id: String): Flow<OfferEntity?> = offerDao.observeById(id)

    override suspend fun upsert(offer: OfferEntity) = offerDao.upsert(offer)

    override suspend fun delete(id: String) = offerDao.deleteById(id)

    override fun observeLastSyncedAt() = syncMetadataDao.observeSyncedAt(OFFERS_SYNC_KEY)

    override suspend fun setLastSyncedAt(instant: Instant) =
        syncMetadataDao.upsert(SyncMetadataEntity(OFFERS_SYNC_KEY, instant))

    private companion object {
        const val OFFERS_SYNC_KEY = "offers"
    }
}
