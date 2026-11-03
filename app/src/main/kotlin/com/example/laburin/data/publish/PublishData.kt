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

    /** @throws com.example.laburin.data.remote.ApiException 403 when the offer isn't the business's */
    suspend fun applicants(token: String, offerId: String): List<ApplicantDto>

    /** Idempotent. The candidate's contact is included only in an approval's response. */
    suspend fun review(token: String, applicationId: String, approve: Boolean): ApplicantDto
}

/** [status]: PENDING_REVIEW, APPROVED or REJECTED. [contactEmail] is sent only for approved applicants. */
@Serializable
data class ApplicantDto(
    val applicationId: String,
    val offerId: String,
    val candidateName: String,
    val cvFileName: String?,
    val status: String,
    val contactEmail: String?,
)

object QrCode {
    /** Square matrix of dark (true) / light (false) modules scaled to [sizePx]. */
    fun encode(text: String, sizePx: Int): Array<BooleanArray> {
        val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return Array(sizePx) { y -> BooleanArray(sizePx) { x -> bits[x, y] } }
    }
}
