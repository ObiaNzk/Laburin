package com.example.laburin.data.publish

import com.example.laburin.domain.publish.OfferLink
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeTest {

    @Test
    fun `the generated QR decodes back to the same link`() {
        val matrix = QrCode.encode("laburin://offer/o-1", sizePx = 300)

        assertEquals("laburin://offer/o-1", decode(matrix))
    }

    @Test
    fun `the printed web link with a real offer id is readable at the size shown on screen`() {
        val link = OfferLink.build("3f2b8c1e-9a4d-4e7b-8c21-5d6f7a8b9c0d")

        assertEquals(link, decode(QrCode.encode(link, sizePx = 220)))
    }

    @Test
    fun `the matrix has the requested size`() {
        val matrix = QrCode.encode("laburin://offer/o-1", sizePx = 200)

        assertEquals(200, matrix.size)
        assertEquals(200, matrix[0].size)
    }

    private fun decode(matrix: Array<BooleanArray>): String {
        val size = matrix.size
        val pixels = IntArray(size * size) { i -> if (matrix[i / size][i % size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        return QRCodeReader().decode(bitmap).text
    }
}
