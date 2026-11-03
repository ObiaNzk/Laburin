package com.example.laburin.data.sync

import androidx.work.ListenableWorker.Result
import com.example.laburin.domain.model.SyncOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkResultTest {

    @Test
    fun `everything sent finishes the work`() {
        assertEquals(Result.success(), SyncOutcome.Done.toWorkResult())
    }

    @Test
    fun `network or server problems retry with backoff`() {
        assertEquals(Result.retry(), SyncOutcome.Retry.toWorkResult())
    }

    @Test
    fun `an expired session finishes without retrying, the actions stay queued for the next login`() {
        assertEquals(Result.success(), SyncOutcome.AuthRequired.toWorkResult())
    }
}
