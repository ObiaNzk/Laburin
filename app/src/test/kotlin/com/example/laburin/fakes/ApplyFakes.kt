package com.example.laburin.fakes

import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeCvRepository(initial: CvInfo? = CvInfo("cv.pdf")) : CvRepository {
    val cv = MutableStateFlow(initial)
    var nextUpload: CvUploadResult = CvUploadResult.Success
    val uploads = mutableListOf<String>()
    var currentCalls = 0
    override fun observeCv(): Flow<CvInfo?> = cv
    override suspend fun current(): CvInfo? { currentCalls++; return cv.value }
    override suspend fun upload(fileName: String, mimeType: String?, bytes: ByteArray): CvUploadResult {
        uploads += fileName
        if (nextUpload == CvUploadResult.Success) cv.value = CvInfo(fileName)
        return nextUpload
    }
}

class FakeSyncScheduler : SyncScheduler {
    var scheduled = 0
    override fun schedule() { scheduled++ }
}

class FakeApplicationRepository : ApplicationRepository {
    val store = MutableStateFlow<List<JobApplication>>(emptyList())
    val pendingCount = MutableStateFlow(0)
    var nextRefresh: RefreshResult = RefreshResult.Success
    var refreshCalls = 0
    val applied get() = store.value

    override fun observeMine() = store
    override fun observeForOffer(offerId: String) = store.map { list -> list.firstOrNull { it.offerId == offerId } }
    override fun observePendingCount(): Flow<Int> = pendingCount
    override suspend fun apply(offer: Offer): JobApplication {
        val app = testApplication("a-${store.value.size}", offer.id, offer.title)
        store.value = store.value + app
        pendingCount.value++
        return app
    }
    var onRefresh: () -> Unit = {}
    override suspend fun refreshMine(): RefreshResult { refreshCalls++; onRefresh(); return nextRefresh }
}

fun testApplication(
    id: String = "a-1",
    offerId: String = "offer-1",
    title: String = "Ayudante de taller",
    status: ApplicationStatus = ApplicationStatus.PENDING_SEND,
) = JobApplication(id, offerId, title, "[COMERCIO]", status, Instant.EPOCH)
