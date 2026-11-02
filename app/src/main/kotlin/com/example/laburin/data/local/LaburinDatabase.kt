package com.example.laburin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [OfferEntity::class, SyncMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LaburinDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val NAME = "laburin.db"
    }
}
