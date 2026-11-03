package com.example.laburin.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A candidate who applied to one of the business's offers. [contactEmail] stays null until the server confirms
 * the match (RNF-05); it is never filled locally.
 */
@Entity(tableName = "applicants")
data class ApplicantEntity(
    @PrimaryKey @ColumnInfo(name = "application_id") val applicationId: String,
    @ColumnInfo(name = "offer_id") val offerId: String,
    @ColumnInfo(name = "candidate_name") val candidateName: String,
    @ColumnInfo(name = "cv_file_name") val cvFileName: String?,
    val status: String,
    @ColumnInfo(name = "contact_email") val contactEmail: String?,
)

@Dao
interface ApplicantDao {
    @Query("SELECT * FROM applicants WHERE offer_id = :offerId ORDER BY candidate_name")
    fun observeForOffer(offerId: String): Flow<List<ApplicantEntity>>

    @Query("SELECT * FROM applicants WHERE application_id = :applicationId")
    suspend fun find(applicationId: String): ApplicantEntity?

    @Upsert
    suspend fun upsert(applicant: ApplicantEntity)
}
