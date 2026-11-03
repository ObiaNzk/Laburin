package com.example.laburin.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class KeystoreSessionStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var file: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    private val session = StoredSession(
        token = "secret-token-123",
        session = Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "persona@demo.laburin"),
    )

    @Before
    fun setUp() {
        file = File(context.cacheDir, "session-test-${UUID.randomUUID()}.preferences_pb")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun savedSessionIsReadBack() = runTest {
        val store = KeystoreSessionStore(dataStore)

        store.save(session)

        assertEquals(session, store.stored.first())
    }

    @Test
    fun tokenAndEmailAreNotStoredInPlainText() = runTest {
        KeystoreSessionStore(dataStore).save(session)

        val raw = dataStore.data.first().asMap().values.joinToString()
        assertFalse(raw.contains("secret-token-123"))
        assertFalse(raw.contains("persona@demo.laburin"))
    }

    @Test
    fun clearRemovesTheSession() = runTest {
        val store = KeystoreSessionStore(dataStore)
        store.save(session)

        store.clear()

        assertNull(store.stored.first())
    }

    @Test
    fun aNewInstanceStillReadsTheSession() = runTest {
        KeystoreSessionStore(dataStore).save(session)

        assertEquals(session, KeystoreSessionStore(dataStore).stored.first())
    }

    @Test
    fun corruptedDataIsTreatedAsLoggedOut() = runTest {
        dataStore.edit { it[stringPreferencesKey("session")] = "not-valid-ciphertext" }

        assertNull(KeystoreSessionStore(dataStore).stored.first())
    }
}
