package com.example.laburin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OfferEntity::class, SyncMetadataEntity::class, ApplicationEntity::class, PendingActionEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LaburinDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        const val NAME = "laburin.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `applications` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `offer_id` TEXT NOT NULL, `offer_title` TEXT NOT NULL, `business_name` TEXT NOT NULL, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_applications_user_id_offer_id` ON `applications` (`user_id`, `offer_id`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_actions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `target_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
