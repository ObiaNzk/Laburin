package com.example.laburin.fakes

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.SearchFilters
import com.example.laburin.domain.repository.ConnectivityObserver
import com.example.laburin.domain.repository.FiltersRepository
import com.example.laburin.domain.repository.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class FakeLocationProvider(var result: LocationResult = LocationResult.Located(OBELISCO)) : LocationProvider {
    override suspend fun currentLocation(): LocationResult = result
}

class FakeConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
    override val isOnline = MutableStateFlow(online)
}

class FakeFiltersRepository : FiltersRepository {
    override val filters = MutableStateFlow(SearchFilters())
    override suspend fun setRadiusKm(radiusKm: Int) = filters.update { it.copy(radiusKm = radiusKm) }
    override suspend fun setCategory(category: Category?) = filters.update { it.copy(category = category) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
