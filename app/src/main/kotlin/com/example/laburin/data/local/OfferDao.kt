package com.example.laburin.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.laburin.domain.model.Category
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class OfferDao {

    @Query(
        """
        SELECT * FROM offers
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
          AND (:category IS NULL OR category = :category)
          AND title LIKE '%' || :query || '%'
        """
    )
    abstract fun observeInArea(
        minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
        category: Category?, query: String,
    ): Flow<List<OfferEntity>>

    @Upsert
    abstract suspend fun upsertAll(offers: List<OfferEntity>)

    @Query(
        """
        DELETE FROM offers
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
          AND (:category IS NULL OR category = :category)
          AND id NOT IN (:keepIds)
        """
    )
    abstract suspend fun deleteInAreaExcept(
        minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
        category: Category?, keepIds: List<String>,
    )

    /** Server wins: offers of this area that the server no longer returns are removed. */
    @Transaction
    open suspend fun replaceInArea(
        minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
        category: Category?, offers: List<OfferEntity>,
    ) {
        deleteInAreaExcept(minLat, maxLat, minLng, maxLng, category, offers.map { it.id })
        upsertAll(offers)
    }
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT synced_at FROM sync_metadata WHERE `key` = :key")
    fun observeSyncedAt(key: String): Flow<Instant?>

    @Upsert
    suspend fun upsert(entity: SyncMetadataEntity)
}
