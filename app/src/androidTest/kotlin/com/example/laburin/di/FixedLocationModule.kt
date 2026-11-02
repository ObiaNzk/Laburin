package com.example.laburin.di

import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.repository.LocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/** Tests must not depend on where the device is: pin it to the Obelisco, where the fake seed data lives. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocationModule::class])
object FixedLocationModule {

    @Provides
    fun provideLocationProvider(): LocationProvider = object : LocationProvider {
        override suspend fun currentLocation() = LocationResult.Located(GeoLocation(-34.6037, -58.3816))
    }
}
