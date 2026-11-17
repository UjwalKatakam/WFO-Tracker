package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.WfoPreferences
import com.example.data.preferences.dataStore
import com.example.util.SecurePreferencesHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WfoPreferencesMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }
        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val inMemoryKey = keyGen.generateKey()
        SecurePreferencesHelper.secretKeyProvider = { inMemoryKey }
    }

    @After
    fun tearDown() {
        SecurePreferencesHelper.secretKeyProvider = null
    }

    @Test
    fun secureHelperReturnsNullOnFailureAndNeverReturnsPlaintextOrCiphertext() {
        val secret = "ghp_secretTokenValue999"
        val encrypted = SecurePreferencesHelper.encrypt(secret)
        assertTrue(!encrypted.isNullOrEmpty())
        assertNotEquals(secret, encrypted)
        assertEquals(secret, SecurePreferencesHelper.decrypt(encrypted!!))

        // Simulate KeyStore / cipher key failure
        SecurePreferencesHelper.secretKeyProvider = { throw IllegalStateException("KeyStore unavailable") }

        val failedEncrypt = SecurePreferencesHelper.encrypt(secret)
        assertNull("Encryption failure must return null, never plaintext", failedEncrypt)

        val failedDecrypt = SecurePreferencesHelper.decrypt(encrypted)
        assertNull("Decryption failure must return null, never raw ciphertext", failedDecrypt)
    }

    @Test
    fun testLegacyGitTokenMigrationSucceeds() = runBlocking {
        val legacyKey = stringPreferencesKey("git_token")
        val encKey = stringPreferencesKey("enc_git_token")

        context.dataStore.edit { prefs ->
            prefs[legacyKey] = "ghp_testLegacySecretToken123"
            prefs.remove(encKey)
        }

        val preferences = WfoPreferences(context)
        preferences.migrateLegacyTokenIfNeeded()

        val finalPrefs = context.dataStore.data.first()
        assertFalse("Legacy plaintext token must be removed from disk", finalPrefs.contains(legacyKey))
        assertTrue("Encrypted token must exist on disk", finalPrefs.contains(encKey))

        val settings = preferences.settingsFlow.first()
        assertEquals("ghp_testLegacySecretToken123", settings.gitToken)
        assertFalse(settings.tokenUnreadable)
    }

    @Test
    fun migrationRetainsLegacyKeyWhenEncryptionFails() = runBlocking {
        val legacyKey = stringPreferencesKey("git_token")
        val encKey = stringPreferencesKey("enc_git_token")

        context.dataStore.edit { prefs ->
            prefs[legacyKey] = "ghp_legacyShouldRemain"
            prefs.remove(encKey)
        }

        val preferences = WfoPreferences(
            context = context,
            encryptFn = { null },
            decryptFn = { it }
        )
        preferences.migrateLegacyTokenIfNeeded()

        val finalPrefs = context.dataStore.data.first()
        assertEquals("ghp_legacyShouldRemain", finalPrefs[legacyKey])
        assertFalse("enc_git_token must not be written when encryption fails", finalPrefs.contains(encKey))
    }

    @Test
    fun updateSettingsReturnsFalseAndPreservesPreviousEncryptedTokenOnFailure() = runBlocking {
        val legacyKey = stringPreferencesKey("git_token")
        val encKey = stringPreferencesKey("enc_git_token")

        context.dataStore.edit { prefs ->
            prefs[encKey] = "ENC_PREVIOUS_TOKEN"
        }

        val preferences = WfoPreferences(
            context = context,
            encryptFn = { null },
            decryptFn = { "previous_decrypted" }
        )

        val savedSecurely = preferences.updateSettings(
            minHours = 8.0,
            targetDaysPerWeek = 3,
            shiftStart = "12:00",
            lat = 17.4,
            lon = 78.3,
            radiusKm = 0.5,
            employeeName = "Test User",
            gitToken = "ghp_newTokenShouldNotBePlaintext",
            gistId = "gist123",
            notifyOnTarget = true,
            gpsVerificationEnabled = false
        )

        assertFalse("updateSettings must return false when encryption fails", savedSecurely)
        val finalPrefs = context.dataStore.data.first()
        assertEquals("ENC_PREVIOUS_TOKEN", finalPrefs[encKey])
        assertFalse("Plaintext legacy key must never be written on encryption failure", finalPrefs.contains(legacyKey))
    }

    @Test
    fun settingsFlowExposesEmptyTokenAndTokenUnreadableWhenDecryptFails() = runBlocking {
        val encKey = stringPreferencesKey("enc_git_token")
        context.dataStore.edit { prefs ->
            prefs[encKey] = "CORRUPTED_CIPHERTEXT"
        }

        val preferences = WfoPreferences(
            context = context,
            encryptFn = { "enc:$it" },
            decryptFn = { null }
        )

        val settings = preferences.settingsFlow.first()
        assertEquals("", settings.gitToken)
        assertTrue("tokenUnreadable must be true when decryption returns null", settings.tokenUnreadable)
    }
}
