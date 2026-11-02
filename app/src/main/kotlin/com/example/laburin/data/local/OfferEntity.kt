package com.example.laburin.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.laburin.domain.model.Category
import java.time.Instant

@Entity(
    tableName = "offers",
    indices = [Index("latitude", "longitude"), Index("category")],
)
data class OfferEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: Category,
    val description: String,
    val schedule: String,
    @ColumnInfo(name = "job_type") val jobType: String,
    @ColumnInfo(name = "business_name") val businessName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Instant,
)
