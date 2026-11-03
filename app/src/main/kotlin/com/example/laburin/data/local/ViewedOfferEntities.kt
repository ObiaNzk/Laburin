package com.example.laburin.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Offers the user opened (from the map or a scanned QR): kept in the cache even outside the search area (RF-02). */
@Entity(tableName = "viewed_offers")
data class ViewedOfferEntity(
    @PrimaryKey @ColumnInfo(name = "offer_id") val offerId: String,
    @ColumnInfo(name = "viewed_at") val viewedAt: Instant,
    val source: String,
)

@Dao
interface ViewedOfferDao {
    @Upsert
    suspend fun upsert(viewed: ViewedOfferEntity)

    @Query(
        "SELECT offers.* FROM offers INNER JOIN viewed_offers ON offers.id = viewed_offers.offer_id " +
            "ORDER BY viewed_offers.viewed_at DESC"
    )
    fun observeRecentlyViewed(): Flow<List<OfferEntity>>

    @Query(
        "DELETE FROM viewed_offers WHERE offer_id NOT IN " +
            "(SELECT offer_id FROM viewed_offers ORDER BY viewed_at DESC LIMIT :keep)"
    )
    suspend fun keepMostRecent(keep: Int)

    /** RNF-08: the cache keeps only the current area and the recently viewed offers. */
    @Query(
        """
        DELETE FROM offers
        WHERE NOT (latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng)
          AND id NOT IN (SELECT offer_id FROM viewed_offers)
        """
    )
    suspend fun deleteOffersOutside(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double)
}
