package com.example.laburin.ui.screens.cv

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Thin adapter over Android's [PdfRenderer] (API 21+, no extra library). The renderer allows one open page at a time,
 * so every access goes through [lock].
 */
internal class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    private val lock = Mutex()
    private val aspectRatios: List<Float> = List(renderer.pageCount) { index ->
        renderer.openPage(index).use { page -> page.width.toFloat() / page.height }
    }

    val pageCount: Int get() = aspectRatios.size

    fun aspectRatio(index: Int): Float = aspectRatios[index]

    /** The page as a bitmap [widthPx] wide, or null if the renderer fails on it. */
    suspend fun render(index: Int, widthPx: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        lock.withLock {
            try {
                renderer.openPage(index).use { page ->
                    val heightPx = (widthPx / aspectRatios[index]).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE) // PDFs are transparent where nothing is drawn
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            } catch (e: IllegalStateException) {
                null
            }
        }
    }

    fun close() {
        renderer.close()
        descriptor.close()
    }

    companion object {
        /**
         * Null when the file is missing or isn't a PDF the phone can read.
         *
         * On Android 8 and 8.1, once PdfRenderer fails on a file, it fails on every later file in the process, valid
         * or not. So anything without the PDF signature is rejected here and never reaches the renderer. A damaged
         * file that does start with the signature can still trigger it; the viewer then asks to upload the CV again.
         */
        suspend fun open(file: File): PdfDocument? = withContext(Dispatchers.IO) {
            if (!file.startsWithPdfSignature()) return@withContext null
            var descriptor: ParcelFileDescriptor? = null
            try {
                descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    descriptor.close()
                    null
                } else {
                    PdfDocument(descriptor, renderer)
                }
            } catch (e: IOException) {
                descriptor?.close()
                null
            } catch (e: SecurityException) {
                descriptor?.close() // password-protected PDF
                null
            }
        }
    }
}

private val PDF_SIGNATURE = "%PDF-".toByteArray(Charsets.US_ASCII)

private fun File.startsWithPdfSignature(): Boolean = try {
    val head = ByteArray(PDF_SIGNATURE.size)
    inputStream().use { it.read(head) } == head.size && head.contentEquals(PDF_SIGNATURE)
} catch (e: IOException) {
    false
}
