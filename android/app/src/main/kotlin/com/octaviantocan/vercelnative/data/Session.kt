package com.octaviantocan.vercelnative.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Persistent storage for the bearer token. */
interface TokenStore {
    /** @return the stored token, or null when none is stored or it cannot be decrypted. */
    suspend fun read(): String?

    suspend fun write(token: String)

    suspend fun clear()
}

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** [TokenStore] that keeps the token encrypted with an Android Keystore AES-GCM key inside DataStore. */
class KeystoreTokenStore(context: Context) : TokenStore {
    private val dataStore = context.applicationContext.tokenDataStore

    override suspend fun read(): String? {
        val stored = dataStore.data.map { it[KEY] }.first() ?: return null
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }.getOrNull()
    }

    override suspend fun write(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val sealed = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        dataStore.edit { it[KEY] = Base64.encodeToString(sealed, Base64.NO_WRAP) }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "vercel-native.session.token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        val KEY = stringPreferencesKey("token")
    }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object SignedIn : SessionState
}

/** Holds the current bearer token and whether the user is signed in. */
class Session(private val store: TokenStore) {
    private val transition = Mutex()
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Loading)
    private val mutableGeneration = MutableStateFlow(0L)
    /** Changes even when one signed-in token directly replaces another. */
    val generation: StateFlow<Long> = mutableGeneration
    /** Clears per-session caches synchronously before observers see a replacement token. */
    var onTokenChanged: ((departed: String?, current: String?) -> Unit)? = null

    @Volatile
    private var current: String? = null

    val state: StateFlow<SessionState> = mutableState

    /** The bearer token to attach to requests, or null when signed out. */
    val token: String? get() = current

    /** Loads the stored token; call once at startup. */
    suspend fun restore() = transition.withLock {
        changeToken(store.read())
        mutableState.value = if (current == null) SessionState.SignedOut else SessionState.SignedIn
    }

    suspend fun signIn(token: String) = transition.withLock {
        store.write(token)
        changeToken(token)
        mutableState.value = SessionState.SignedIn
    }

    /** Forgets the token and moves to signed-out. */
    suspend fun clear() = transition.withLock {
        changeToken(null)
        mutableState.value = SessionState.SignedOut
        store.clear()
    }

    /**
     * Signs out because the server rejected [rejected]; call from a network thread, never the main thread.
     * @param rejected the token the rejected request carried; a token from a newer sign-in is kept.
     */
    fun invalidate(rejected: String) = runBlocking {
        transition.withLock {
            if (current != rejected) return@withLock
            changeToken(null)
            mutableState.value = SessionState.SignedOut
            store.clear()
        }
    }

    private fun changeToken(next: String?) {
        val departed = current
        current = next
        if (departed != next) {
            runCatching { onTokenChanged?.invoke(departed, next) }
            mutableGeneration.value += 1
        }
    }
}
