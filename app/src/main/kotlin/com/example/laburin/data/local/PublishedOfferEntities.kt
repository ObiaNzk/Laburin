package com.example.laburin.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The business's own offers, including ones still waiting to be published. [id] is generated on the device. */
@Entity(tableName = "published_offers")
data class PublishedOfferEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "business_id") val businessId: String,
    val title: String,
    val category: String,
    val schedule: String,
    val description: String,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "salary_min") val salaryMin: Long? = null,
    @ColumnInfo(name = "salary_max") val salaryMax: Long? = null,
    @ColumnInfo(name = "salary_period") val salaryPeriod: String? = null,
)

@Dao
interface PublishedOfferDao {
    @Query("SELECT * FROM published_offers WHERE business_id = :businessId ORDER BY created_at DESC")
    fun observeMine(businessId: String): Flow<List<PublishedOfferEntity>>

    @Query("SELECT * FROM published_offers WHERE id = :id")
    fun observe(id: String): Flow<PublishedOfferEntity?>

    @Query("SELECT * FROM published_offers WHERE id = :id")
    suspend fun find(id: String): PublishedOfferEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(offer: PublishedOfferEntity)

    @Upsert
    suspend fun upsert(offer: PublishedOfferEntity)

    @Query("UPDATE published_offers SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
