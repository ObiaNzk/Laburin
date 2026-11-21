package com.example.laburin.domain.repository

import com.example.laburin.domain.model.CvFileResult
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

/** The current user's applications. Empty while logged out. */
interface ApplicationRepository {
    fun observeMine(): Flow<List<JobApplication>>
    fun observeForOffer(offerId: String): Flow<JobApplication?>
    fun observePendingCount(): Flow<Int>

    /** Stored locally as pending and queued; never lost on app close or network loss (RNF-03). */
    suspend fun apply(offer: Offer): JobApplication

    suspend fun refreshMine(): RefreshResult
}

interface CvRepository {
    fun observeCv(): Flow<CvInfo?>

    /** Cached CV, or the server's when nothing is cached yet (null if unknown or offline). */
    suspend fun current(): CvInfo?

    suspend fun upload(fileName: String, mimeType: String?, bytes: ByteArray): CvUploadResult

    /** Downloads the uploaded PDF so the user can see exactly what businesses get. */
    suspend fun openFile(): CvFileResult

    /** Deletes the copy made by [openFile]; a CV must not stay around once it is no longer shown. */
    fun closeFile()
}

/** Sends queued actions as soon as there is a connection, even if the app is closed. */
fun interface SyncScheduler {
    fun schedule()
}
