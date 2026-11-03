package com.example.laburin.data.cv

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.laburin.data.applications.ApplicationsRemoteDataSource
import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.domain.repository.CvRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/** Local record of each user's uploaded CV file name. */
interface CvCache {
    fun observe(userId: String): Flow<String?>
    suspend fun save(userId: String, fileName: String)
}

class DataStoreCvCache @Inject constructor(private val dataStore: DataStore<Preferences>) : CvCache {
    override fun observe(userId: String) = dataStore.data.map { it[key(userId)] }
    override suspend fun save(userId: String, fileName: String) {
        dataStore.edit { it[key(userId)] = fileName }
    }
    private fun key(userId: String) = stringPreferencesKey("cv_file_$userId")
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCvRepository @Inject constructor(
    private val remote: ApplicationsRemoteDataSource,
    private val session: SessionStore,
    private val cache: CvCache,
) : CvRepository {

    override fun observeCv(): Flow<CvInfo?> = session.stored.flatMapLatest { stored ->
        if (stored == null) flowOf(null) else cache.observe(stored.session.userId).map { name -> name?.let(::CvInfo) }
    }

    override suspend fun current(): CvInfo? {
        val stored = session.stored.first() ?: return null
        cache.observe(stored.session.userId).first()?.let { return CvInfo(it) }
        val fromServer = try {
            remote.getCv(stored.token)
        } catch (e: IOException) {
            return null
        } catch (e: ApiException) {
            return null
        } ?: return null
        cache.save(stored.session.userId, fromServer.fileName)
        return CvInfo(fromServer.fileName)
    }

    override suspend fun upload(fileName: String, mimeType: String?, bytes: ByteArray): CvUploadResult {
        if (bytes.size > MAX_BYTES) return CvUploadResult.TooLarge
        if (!looksLikePdf(fileName, mimeType, bytes)) return CvUploadResult.NotPdf
        val stored = session.stored.first() ?: return CvUploadResult.ServerError(HTTP_UNAUTHORIZED)
        try {
            remote.uploadCv(stored.token, fileName, bytes)
        } catch (e: IOException) {
            return CvUploadResult.NetworkError
        } catch (e: ApiException) {
            return CvUploadResult.ServerError(e.code)
        }
        cache.save(stored.session.userId, fileName)
        return CvUploadResult.Success
    }

    /** Type and extension can lie (a renamed file), so the content must also start with the PDF signature. */
    private fun looksLikePdf(fileName: String, mimeType: String?, bytes: ByteArray): Boolean {
        val declared = mimeType == PDF_MIME || (mimeType == null && fileName.endsWith(".pdf", ignoreCase = true))
        return declared && bytes.size >= PDF_SIGNATURE.size && bytes.copyOfRange(0, PDF_SIGNATURE.size).contentEquals(PDF_SIGNATURE)
    }

    private companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
        const val PDF_MIME = "application/pdf"
        const val HTTP_UNAUTHORIZED = 401
        val PDF_SIGNATURE = "%PDF".toByteArray()
    }
}
