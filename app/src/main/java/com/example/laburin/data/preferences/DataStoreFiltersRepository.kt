package com.example.laburin.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.SearchFilters
import com.example.laburin.domain.repository.FiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreFiltersRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FiltersRepository {

    override val filters: Flow<SearchFilters> = dataStore.data
        .map { prefs ->
            SearchFilters(
                radiusKm = prefs[RADIUS_KM]?.takeIf { it in SearchFilters.RADIUS_OPTIONS_KM }
                    ?: SearchFilters.DEFAULT_RADIUS_KM,
                category = prefs[CATEGORY]?.let { name -> Category.entries.firstOrNull { it.name == name } },
            )
        }
        .distinctUntilChanged()

    override suspend fun setRadiusKm(radiusKm: Int) {
        dataStore.edit { it[RADIUS_KM] = radiusKm }
    }

    override suspend fun setCategory(category: Category?) {
        dataStore.edit { prefs ->
            if (category == null) prefs.remove(CATEGORY) else prefs[CATEGORY] = category.name
        }
    }

    private companion object {
        val RADIUS_KM = intPreferencesKey("search_radius_km")
        val CATEGORY = stringPreferencesKey("search_category")
    }
}
