package com.example.laburin.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session (token included) encrypted with AES-GCM; the key lives in the Android Keystore and never leaves it (RNF-09).
 * Anything that can't be decrypted is treated as logged out rather than crashing.
 */
@Singleton
class KeystoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionStore {

    @Serializable
    private data class Payload(
        val token: String,
        val userId: String,
        val role: String,
        val displayName: String,
        val email: String,
    )

    override val stored: Flow<StoredSession?> = dataStore.data
        .map { prefs -> prefs[SESSION]?.let(::decrypt) }
        .distinctUntilChanged()

    override suspend fun save(session: StoredSession) {
        val payload = with(session) {
            Payload(token, this.session.userId, this.session.role.name, this.session.displayName, this.session.email)
        }
        val encrypted = encrypt(Json.encodeToString(Payload.serializer(), payload))
        dataStore.edit { it[SESSION] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(SESSION) }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): StoredSession? = try {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        val plain = cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).toString(Charsets.UTF_8)
        val p = Json.decodeFromString(Payload.serializer(), plain)
        val role = UserRole.valueOf(p.role)
        StoredSession(p.token, Session(p.userId, role, p.displayName, p.email))
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        // Bad Base64, too short for an IV, malformed JSON or unknown role.
        null
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        val SESSION = stringPreferencesKey("session")
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "laburin_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
