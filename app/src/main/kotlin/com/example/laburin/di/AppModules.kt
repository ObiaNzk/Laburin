package com.example.laburin.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.laburin.BuildConfig
import com.example.laburin.data.connectivity.AndroidConnectivityObserver
import com.example.laburin.data.local.LaburinDatabase
import com.example.laburin.data.local.OfferDao
import com.example.laburin.data.local.OfferLocalDataSource
import com.example.laburin.data.local.RoomOfferLocalDataSource
import com.example.laburin.data.local.SyncMetadataDao
import com.example.laburin.data.location.AndroidGeocoder
import com.example.laburin.data.location.DeviceLocationSource
import com.example.laburin.data.location.FallbackLocationProvider
import com.example.laburin.data.location.FusedDeviceLocationSource
import com.example.laburin.data.preferences.DataStoreFiltersRepository
import com.example.laburin.data.remote.FakeOfferRemoteDataSource
import com.example.laburin.data.remote.OfferApi
import com.example.laburin.data.remote.OfferRemoteDataSource
import com.example.laburin.data.repository.OfflineFirstOfferRepository
import com.example.laburin.data.applications.ApplicationsRemoteDataSource
import com.example.laburin.data.applications.DefaultApplicationRepository
import com.example.laburin.data.cv.CvCache
import com.example.laburin.data.publish.BusinessRemoteDataSource
import com.example.laburin.data.publish.DefaultApplicantsRepository
import com.example.laburin.data.publish.DefaultMyOffersRepository
import com.example.laburin.domain.repository.ApplicantsRepository
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.data.cv.DataStoreCvCache
import com.example.laburin.data.cv.DefaultCvRepository
import com.example.laburin.data.local.ApplicantDao
import com.example.laburin.data.notifications.AndroidNotifier
import com.example.laburin.data.notifications.DataStoreKnownApplicants
import com.example.laburin.data.notifications.KnownApplicantsStore
import com.example.laburin.domain.repository.Notifier
import com.example.laburin.data.local.ApplicationDao
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PublishedOfferDao
import com.example.laburin.data.local.RoomTransactionRunner
import com.example.laburin.data.local.TransactionRunner
import com.example.laburin.data.local.ViewedOfferDao
import com.example.laburin.data.sync.WorkManagerSyncScheduler
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.domain.repository.SyncScheduler
import com.example.laburin.data.auth.AuthRemoteDataSource
import com.example.laburin.data.auth.DefaultAuthRepository
import com.example.laburin.data.auth.KeystoreSessionStore
import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.domain.repository.ConnectivityObserver
import com.example.laburin.domain.repository.FiltersRepository
import com.example.laburin.domain.repository.Geocoder
import com.example.laburin.domain.repository.LocationProvider
import com.example.laburin.domain.repository.OfferRepository
import com.example.laburin.domain.repository.AuthRepository
import com.example.laburin.domain.repository.SessionExpiryNotifier
import com.example.laburin.domain.repository.SessionRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.laburin.data.auth.AuthApi
import com.example.laburin.data.auth.RetrofitAuthRemoteDataSource
import com.example.laburin.data.applications.ApplicationsApi
import com.example.laburin.data.applications.RetrofitApplicationsRemoteDataSource
import com.example.laburin.data.publish.BusinessApi
import com.example.laburin.data.publish.RetrofitBusinessRemoteDataSource
import com.example.laburin.data.remote.ApiConfig
import com.example.laburin.data.remote.RetrofitOfferRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import javax.inject.Provider
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LaburinDatabase =
        Room.databaseBuilder(context, LaburinDatabase::class.java, LaburinDatabase.NAME)
            .addMigrations(
                LaburinDatabase.MIGRATION_1_2, LaburinDatabase.MIGRATION_2_3, LaburinDatabase.MIGRATION_3_4,
                LaburinDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides
    fun provideOfferDao(db: LaburinDatabase): OfferDao = db.offerDao()

    @Provides
    fun provideSyncMetadataDao(db: LaburinDatabase): SyncMetadataDao = db.syncMetadataDao()

    @Provides
    fun provideApplicationDao(db: LaburinDatabase): ApplicationDao = db.applicationDao()

    @Provides
    fun providePendingActionDao(db: LaburinDatabase): PendingActionDao = db.pendingActionDao()

    @Provides
    fun providePublishedOfferDao(db: LaburinDatabase): PublishedOfferDao = db.publishedOfferDao()

    @Provides
    fun provideApplicantDao(db: LaburinDatabase): ApplicantDao = db.applicantDao()

    @Provides
    fun provideViewedOfferDao(db: LaburinDatabase): ViewedOfferDao = db.viewedOfferDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    @Provides
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .apply {
            // BASIC logs method/URL/status only: never bodies, which may carry personal data.
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = buildRetrofit(client, json, BuildConfig.API_BASE_URL)

    fun buildRetrofit(client: OkHttpClient, json: Json, baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideOfferApi(retrofit: Retrofit): OfferApi = retrofit.create(OfferApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideApplicationsApi(retrofit: Retrofit): ApplicationsApi = retrofit.create(ApplicationsApi::class.java)

    @Provides
    @Singleton
    fun provideBusinessApi(retrofit: Retrofit): BusinessApi = retrofit.create(BusinessApi::class.java)
}

/**
 * Real Go API when `laburin.apiBaseUrl` is set in local.properties, in-memory FakeBackend otherwise.
 * Providers keep the unused side from being built (Retrofit rejects an empty base URL).
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    private val fake = ApiConfig.usesFakeBackend(BuildConfig.API_BASE_URL)

    @Provides
    fun provideOfferRemote(
        fakeRemote: Provider<FakeOfferRemoteDataSource>,
        real: Provider<RetrofitOfferRemoteDataSource>,
    ): OfferRemoteDataSource = if (fake) fakeRemote.get() else real.get()

    @Provides
    fun provideAuthRemote(backend: Provider<FakeBackend>, real: Provider<RetrofitAuthRemoteDataSource>): AuthRemoteDataSource =
        if (fake) backend.get() else real.get()

    @Provides
    fun provideApplicationsRemote(
        backend: Provider<FakeBackend>,
        real: Provider<RetrofitApplicationsRemoteDataSource>,
    ): ApplicationsRemoteDataSource = if (fake) backend.get() else real.get()

    @Provides
    fun provideBusinessRemote(
        backend: Provider<FakeBackend>,
        real: Provider<RetrofitBusinessRemoteDataSource>,
    ): BusinessRemoteDataSource = if (fake) backend.get() else real.get()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindOfferRepository(impl: OfflineFirstOfferRepository): OfferRepository

    @Binds
    abstract fun bindOfferLocalDataSource(impl: RoomOfferLocalDataSource): OfferLocalDataSource

    @Binds
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver

    @Binds
    abstract fun bindFiltersRepository(impl: DataStoreFiltersRepository): FiltersRepository

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindSessionRepository(impl: DefaultAuthRepository): SessionRepository

    @Binds
    abstract fun bindNotifier(impl: AndroidNotifier): Notifier

    @Binds
    abstract fun bindKnownApplicantsStore(impl: DataStoreKnownApplicants): KnownApplicantsStore

    @Binds
    abstract fun bindSessionExpiryNotifier(impl: DefaultAuthRepository): SessionExpiryNotifier

    @Binds
    abstract fun bindSessionStore(impl: KeystoreSessionStore): SessionStore

    @Binds
    abstract fun bindMyOffersRepository(impl: DefaultMyOffersRepository): MyOffersRepository

    @Binds
    abstract fun bindApplicantsRepository(impl: DefaultApplicantsRepository): ApplicantsRepository

    @Binds
    @Singleton
    abstract fun bindApplicationRepository(impl: DefaultApplicationRepository): ApplicationRepository

    @Binds
    abstract fun bindCvRepository(impl: DefaultCvRepository): CvRepository

    @Binds
    abstract fun bindCvCache(impl: DataStoreCvCache): CvCache

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}

/** Separate so instrumented tests can replace the device location with @TestInstallIn. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindLocationProvider(impl: FallbackLocationProvider): LocationProvider

    @Binds
    abstract fun bindDeviceLocationSource(impl: FusedDeviceLocationSource): DeviceLocationSource
}

/** Separate so instrumented tests can replace geocoding with @TestInstallIn. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GeocodingModule {

    @Binds
    abstract fun bindGeocoder(impl: AndroidGeocoder): Geocoder
}
