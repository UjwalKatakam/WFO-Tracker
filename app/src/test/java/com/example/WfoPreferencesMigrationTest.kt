package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.WfoPreferences
import com.example.data.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WfoPreferencesMigrationTest {

    @Test
    fun testLegacyGitTokenMigration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyKey = stringPreferencesKey("git_token")
        val encKey = stringPreferencesKey("enc_git_token")

        // Seed legacy plaintext token directly
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
    }
}
