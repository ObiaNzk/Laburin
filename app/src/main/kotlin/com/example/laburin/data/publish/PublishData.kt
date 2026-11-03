package com.example.laburin.data.publish

import com.example.laburin.data.remote.OfferDto
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.serialization.Serializable

/** [id] is generated on the device so a retried publish is idempotent. Location comes from the business profile. */
@Serializable
data class PublishOfferDto(val id: String, val title: String, val category: String, val schedule: String, val description: String)

/**
 * @throws java.io.IOException on network failure
 * @throws com.example.laburin.data.remote.ApiException 401 session expired, 403 not a business
 */
interface BusinessRemoteDataSource {
    suspend fun publishOffer(token: String, offer: PublishOfferDto)
    suspend fun myOffers(token: String): List<OfferDto>
}

object QrCode {
    /** Square matrix of dark (true) / light (false) modules scaled to [sizePx]. */
    fun encode(text: String, sizePx: Int): Array<BooleanArray> {
        val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return Array(sizePx) { y -> BooleanArray(sizePx) { x -> bits[x, y] } }
    }
}
