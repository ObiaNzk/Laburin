package com.example.laburin.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LaburinDatabase::class.java)

    @Test
    fun migration1To2KeepsCachedOffersAndAddsTheApplicationTables() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO offers (id, title, category, description, schedule, job_type, business_name, address, " +
                    "latitude, longitude, updated_at) VALUES ('o-1', 'Mozo', 'GASTRONOMY', '', '', '', '[COMERCIO]', " +
                    "'[DIRECCION]', -34.6, -58.38, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, LaburinDatabase.MIGRATION_1_2)

        db.query("SELECT title FROM offers WHERE id = 'o-1'").use { c ->
            c.moveToFirst()
            assertEquals("Mozo", c.getString(0))
        }
        db.execSQL(
            "INSERT INTO applications (id, user_id, offer_id, offer_title, business_name, status, created_at) " +
                "VALUES ('a-1', 'u-1', 'o-1', 'Mozo', '[COMERCIO]', 'PENDING_SEND', 0)"
        )
        db.execSQL("INSERT INTO pending_actions (id, type, target_id, created_at) VALUES ('p-1', 'APPLY', 'a-1', 0)")
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
