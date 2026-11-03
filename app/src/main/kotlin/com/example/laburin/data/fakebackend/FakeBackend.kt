package com.example.laburin.data.fakebackend

import com.example.laburin.data.applications.ApplicationDto
import com.example.laburin.data.applications.ApplicationsRemoteDataSource
import com.example.laburin.data.applications.CvDto
import com.example.laburin.data.auth.AuthRemoteDataSource
import com.example.laburin.data.auth.AuthResponseDto
import com.example.laburin.data.auth.UserDto
import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.OfferDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the Go API, shared by every fake remote so flows connect end to end
 * (a candidate applies, the business reviews, the candidate sees the result). State lives while the process does.
 * All accounts and data are fictional.
 */
@Singleton
class FakeBackend @Inject constructor() : AuthRemoteDataSource, ApplicationsRemoteDataSource {

    private data class Account(val user: UserDto, val password: String)

    private data class ApplicationRecord(
        val id: String,
        val candidateId: String,
        val offerId: String,
        val status: String,
        val createdAt: Instant,
    )

    private val mutex = Mutex()
    private val accounts = mutableMapOf<String, Account>()
    private val tokens = mutableMapOf<String, String>()
    private val offers = SEED_OFFERS.toMutableList()
    private val offerOwners = SEED_OFFERS.associate { it.id to DEMO_BUSINESS_ID }.toMutableMap()
    private val applications = mutableMapOf<String, ApplicationRecord>()
    private val cvs = mutableMapOf(DEMO_CANDIDATE_ID to CvDto("cv-postulante-demo.pdf"))

    init {
        addAccount(DEMO_CANDIDATE_ID, "CANDIDATE", "Postulante Demo", DEMO_CANDIDATE_EMAIL)
        addAccount(DEMO_BUSINESS_ID, "BUSINESS", "Comercio Demo", DEMO_BUSINESS_EMAIL)
    }

    // --- Auth ---

    override suspend fun login(email: String, password: String): AuthResponseDto = call {
        val account = accounts[email.lowercase()]?.takeIf { it.password == password } ?: throw ApiException(401)
        AuthResponseDto(token = newToken(account.user.id), user = account.user)
    }

    override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) = call {
        if (accounts.containsKey(email.lowercase())) throw ApiException(409)
        val user = addAccount(UUID.randomUUID().toString(), "CANDIDATE", "$firstName $lastName", email, password)
        AuthResponseDto(token = newToken(user.id), user = user)
    }

    // --- Offers (used by FakeOfferRemoteDataSource) ---

    suspend fun allOffers(): List<OfferDto> = mutex.withLock { offers.toList() }

    // --- Applications and CV ---

    override suspend fun apply(token: String, applicationId: String, offerId: String) = call {
        val candidateId = userFor(token)
        if (applications.containsKey(applicationId)) return@call
        if (offers.none { it.id == offerId && it.status == ACTIVE }) throw ApiException(409)
        applications[applicationId] = ApplicationRecord(applicationId, candidateId, offerId, "SENT", Instant.now())
    }

    override suspend fun myApplications(token: String): List<ApplicationDto> = call {
        val candidateId = userFor(token)
        applications.values.filter { it.candidateId == candidateId }.map { record ->
            val offer = offers.first { it.id == record.offerId }
            ApplicationDto(record.id, offer.id, offer.title, offer.businessName, record.status, record.createdAt.toString())
        }
    }

    override suspend fun uploadCv(token: String, fileName: String, bytes: ByteArray) = call {
        cvs[userFor(token)] = CvDto(fileName)
    }

    override suspend fun getCv(token: String): CvDto? = call { cvs[userFor(token)] }

    // --- Internals ---

    private fun userFor(token: String): String = tokens[token] ?: throw ApiException(401)

    private fun addAccount(id: String, role: String, name: String, email: String, password: String = DEMO_PASSWORD): UserDto {
        val user = UserDto(id = id, role = role, displayName = name, email = email.lowercase())
        accounts[user.email] = Account(user, password)
        return user
    }

    private fun newToken(userId: String) = ("fake-" + UUID.randomUUID()).also { tokens[it] = userId }

    /** Simulated latency; every call is serialized like a single server would. */
    private suspend fun <T> call(block: () -> T): T {
        delay(LATENCY_MS)
        return mutex.withLock { block() }
    }

    companion object {
        const val DEMO_CANDIDATE_ID = "demo-candidate"
        const val DEMO_BUSINESS_ID = "demo-business"
        const val DEMO_CANDIDATE_EMAIL = "postulante@demo.laburin"
        const val DEMO_BUSINESS_EMAIL = "comercio@demo.laburin"
        const val DEMO_PASSWORD = "demo1234"
        const val ACTIVE = "ACTIVE"
        private const val LATENCY_MS = 400L
        private const val UPDATED = "2026-11-01T12:00:00Z"

        private fun seed(
            id: String, title: String, category: String, lat: Double, lng: Double,
            business: String, address: String, status: String = ACTIVE,
        ) = OfferDto(
            id = id, title = title, category = category,
            description = "Buscamos una persona responsable y con ganas de aprender. No se requiere experiencia previa.",
            schedule = "Lunes a viernes, 9 a 17 h", jobType = "Jornada completa",
            businessName = business, address = address,
            latitude = lat, longitude = lng, status = status, updatedAt = UPDATED,
        )

        // Around the default map center, at different distances, so the radius filter has something to do.
        private val SEED_OFFERS = listOf(
            seed("f-1", "Ayudante de taller", "MECHANICS", -34.6080, -58.3712, "Taller Ejemplo", "Av. Ejemplo 100"),
            seed("f-2", "Mozo/a fin de semana", "GASTRONOMY", -34.5997, -58.3819, "Bodegón Ejemplo", "Calle Ficticia 200"),
            seed("f-3", "Repositor/a", "STOCK", -34.6120, -58.3900, "Autoservicio Ejemplo", "Av. Ejemplo 300"),
            seed("f-4", "Vendedor/a", "CLOTHING", -34.6010, -58.3770, "Tienda Ejemplo", "Calle Ficticia 400"),
            seed("f-5", "Cocinero/a", "GASTRONOMY", -34.5850, -58.3950, "Parrilla Ejemplo", "Av. Ejemplo 500"),
            seed("f-6", "Mecánico/a", "MECHANICS", -34.6400, -58.4200, "Lubricentro Ejemplo", "Calle Ficticia 600"),
            seed("f-7", "Cajero/a", "STOCK", -34.6050, -58.3800, "Kiosco Ejemplo", "Av. Ejemplo 700", status = "CLOSED"),
        )
    }
}
