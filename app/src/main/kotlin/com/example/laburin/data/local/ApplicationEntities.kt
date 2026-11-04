package com.example.laburin.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** [id] is generated on the device so a retried send is idempotent on the server. */
@Entity(
    tableName = "applications",
    indices = [Index(value = ["user_id", "offer_id"], unique = true)],
)
data class ApplicationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "offer_id") val offerId: String,
    @ColumnInfo(name = "offer_title") val offerTitle: String,
    @ColumnInfo(name = "business_name") val businessName: String,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/** Outbox of user actions confirmed while offline (RNF-03). Never evicted by cache limits. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String,
    val type: String,
    @ColumnInfo(name = "target_id") val targetId: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeMine(userId: String): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE user_id = :userId AND offer_id = :offerId")
    fun observeForOffer(userId: String, offerId: String): Flow<ApplicationEntity?>

    @Query("SELECT * FROM applications WHERE id = :id")
    suspend fun find(id: String): ApplicationEntity?

    // An offer can be applied to once per user: a second insert is ignored, never duplicated.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(application: ApplicationEntity): Long

    @Query("UPDATE applications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface PendingActionDao {
    // rowid breaks ties: actions queued in the same millisecond keep their order (publish before edit before close).
    @Query("SELECT * FROM pending_actions ORDER BY created_at ASC, rowid ASC")
    suspend fun all(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions WHERE type = :type AND target_id = :targetId")
    suspend fun countFor(type: String, targetId: String): Int

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: String)
}
