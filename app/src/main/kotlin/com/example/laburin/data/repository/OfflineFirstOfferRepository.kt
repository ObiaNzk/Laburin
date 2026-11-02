package com.example.laburin.data.repository

import com.example.laburin.data.local.OfferLocalDataSource
import com.example.laburin.data.mapper.toDomain
import com.example.laburin.data.mapper.toEntityOrNull
import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.OfferRemoteDataSource
import com.example.laburin.domain.geo.Haversine
import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** The UI only reads from Room; the network only writes into Room. */
class OfflineFirstOfferRepository @Inject constructor(
    private val local: OfferLocalDataSource,
    private val remote: OfferRemoteDataSource,
    private val clock: Clock,
) : OfferRepository {

    override fun observeInArea(area: BoundingBox, category: Category?, query: String): Flow<List<Offer>> =
        local.observeInArea(area, category, query).map { entities -> entities.map { it.toDomain() } }

    override fun observeLastSyncedAt(): Flow<Instant?> = local.observeLastSyncedAt()

    override suspend fun refresh(center: GeoLocation, radiusKm: Int, category: Category?): RefreshResult {
        val dtos = try {
            remote.getOffers(center.latitude, center.longitude, radiusKm, category?.name)
        } catch (e: IOException) {
            return RefreshResult.NetworkError
        } catch (e: ApiException) {
            return RefreshResult.ServerError(e.code)
        }
        val area = Haversine.boundingBox(center, radiusKm * 1_000.0)
        local.replaceInArea(area, category, dtos.mapNotNull { it.toEntityOrNull() })
        local.setLastSyncedAt(clock.instant())
        return RefreshResult.Success
    }

    override fun observeOffer(id: String): Flow<Offer?> = local.observeById(id).map { it?.toDomain() }

    override suspend fun refreshOffer(id: String): RefreshResult {
        val dto = try {
            remote.getOffer(id)
        } catch (e: IOException) {
            return RefreshResult.NetworkError
        } catch (e: ApiException) {
            return RefreshResult.ServerError(e.code)
        }
        val entity = dto?.takeIf { it.status == ACTIVE_STATUS }?.toEntityOrNull()
        if (entity == null) local.delete(id) else local.upsert(entity)
        return RefreshResult.Success
    }

    private companion object {
        const val ACTIVE_STATUS = "ACTIVE"
    }
}
