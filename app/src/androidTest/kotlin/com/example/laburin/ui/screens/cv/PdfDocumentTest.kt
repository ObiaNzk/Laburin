package com.example.laburin.ui.screens.cv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.data.fakebackend.FakeBackend
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfDocumentTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun file(name: String, bytes: ByteArray) = File(context.cacheDir, name).apply { writeBytes(bytes) }

    private val demoPdf = runBlocking {
        val backend = FakeBackend()
        backend.downloadCv(backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD).token)!!
    }

    /**
     * On Android 8 and 8.1 a failed PdfRenderer leaves the whole process unable to open any PDF, valid or not.
     * A file that isn't a PDF must be rejected before it reaches the renderer.
     */
    @Test
    fun aFileThatIsNotAPdfDoesNotBreakTheNextOne(): Unit = runBlocking {
        assertNull(PdfDocument.open(file("no-es-pdf.pdf", "no es un pdf".toByteArray())))

        val document = PdfDocument.open(file("cv.pdf", demoPdf))

        assertEquals(1, document?.pageCount)
        document?.close()
    }
}
