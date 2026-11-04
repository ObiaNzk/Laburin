package com.example.laburin.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun migration2To3AddsThePublishedOffersTable() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO applications (id, user_id, offer_id, offer_title, business_name, status, created_at) " +
                    "VALUES ('a-1', 'u-1', 'o-1', 'Mozo', '[COMERCIO]', 'SENT', 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true, LaburinDatabase.MIGRATION_2_3)

        db.query("SELECT status FROM applications WHERE id = 'a-1'").use { c ->
            c.moveToFirst()
            assertEquals("SENT", c.getString(0))
        }
        db.execSQL(
            "INSERT INTO published_offers (id, business_id, title, category, schedule, description, status, created_at) " +
                "VALUES ('p-1', 'b-1', 'Mozo', 'GASTRONOMY', 'Sábados', '[DESCRIPCION]', 'PENDING_PUBLISH', 0)"
        )
        db.close()
    }

    @Test
    fun migration3To4AddsTheApplicantsTable() {
        helper.createDatabase(DB, 3).close()

        val db = helper.runMigrationsAndValidate(DB, 4, true, LaburinDatabase.MIGRATION_3_4)

        db.execSQL(
            "INSERT INTO applicants (application_id, offer_id, candidate_name, cv_file_name, status, contact_email) " +
                "VALUES ('a-1', 'o-1', '[NOMBRE]', 'cv.pdf', 'PENDING_REVIEW', NULL)"
        )
        db.close()
    }

    @Test
    fun migration4To5AddsTheViewedOffersTable() {
        helper.createDatabase(DB, 4).close()

        val db = helper.runMigrationsAndValidate(DB, 5, true, LaburinDatabase.MIGRATION_4_5)

        db.execSQL("INSERT INTO viewed_offers (offer_id, viewed_at, source) VALUES ('o-1', 0, 'QR')")
        db.close()
    }

    @Test
    fun migration5To6KeepsOffersAndAddsTheOptionalSalary() {
        helper.createDatabase(DB, 5).apply {
            execSQL(
                "INSERT INTO offers (id, title, category, description, schedule, job_type, business_name, address, latitude, " +
                    "longitude, updated_at) VALUES ('o-1', 'Mozo', 'GASTRONOMY', '', '', '', '[COMERCIO]', '[DIRECCION]', 0, 0, 0)"
            )
            execSQL(
                "INSERT INTO published_offers (id, business_id, title, category, schedule, description, status, created_at) " +
                    "VALUES ('p-1', 'b-1', 'Mozo', 'GASTRONOMY', '', '', 'ACTIVE', 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 6, true, LaburinDatabase.MIGRATION_5_6)

        db.query("SELECT salary_min, salary_max, salary_period FROM offers WHERE id = 'o-1'").use {
            it.moveToFirst()
            assertTrue(it.isNull(0) && it.isNull(1) && it.isNull(2))
        }
        db.execSQL("UPDATE published_offers SET salary_min = 5000, salary_max = 7000, salary_period = 'HOUR' WHERE id = 'p-1'")
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
