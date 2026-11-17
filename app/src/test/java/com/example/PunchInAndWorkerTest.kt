package com.example

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WfoDatabase
import com.example.data.network.GistResponse
import com.example.data.network.GistUpdateRequest
import com.example.data.network.GitHubGistApi
import com.example.data.preferences.WfoPreferences
import com.example.data.preferences.WfoSettings
import com.example.data.preferences.dataStore
import com.example.data.repository.WfoRepository
import com.example.ui.viewmodel.WfoViewModel
import com.example.work.GistSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PunchInAndWorkerTest {

    private lateinit var application: Application
    private lateinit var database: WfoDatabase
    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
        application.dataStore.edit { it.clear() }
        database = Room.inMemoryDatabaseBuilder(application, WfoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() = runBlocking {
        viewModelStore.clear()
        application.dataStore.edit { it.clear() }
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun requestPunchInUsesCurrentTimeByDefaultAndRespectsExplicitStartMillis() = runBlocking {
        val prefs = WfoPreferences(
            context = application,
            encryptFn = { it },
            decryptFn = { it }
        )
        val repo = WfoRepository(
            context = application,
            database = database,
            preferences = prefs
        )

        fun newVm(key: String, r: WfoRepository): WfoViewModel {
            return ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return WfoViewModel(application, r) as T
                    }
                }
            )[key, WfoViewModel::class.java]
        }

        val viewModel = newVm("vm1", repo)

        // 1. Punch in with explicit past startMillis (simulating "Arrived earlier" sheet)
        val explicitEarlierMillis = System.currentTimeMillis() - (30 * 60 * 1000L)
        viewModel.requestPunchIn(application, startMillis = explicitEarlierMillis)
        withTimeout(3000) {
            viewModel.uiState.first { it.isPunchedIn && it.activeStartMillis == explicitEarlierMillis }
        }

        // 2. End session & recreate ViewModel/Preferences to verify no offset leaks or persists
        repo.clearActiveSessionOnly()
        withTimeout(3000) {
            viewModel.uiState.first { !it.isPunchedIn }
        }

        val prefs2 = WfoPreferences(
            context = application,
            encryptFn = { it },
            decryptFn = { it }
        )
        val repo2 = WfoRepository(
            context = application,
            database = database,
            preferences = prefs2
        )
        val viewModel2 = newVm("vm2", repo2)

        val beforeNow = System.currentTimeMillis()
        viewModel2.requestPunchIn(application)
        val stateAfterDefaultPunch = withTimeout(3000) {
            viewModel2.uiState.first { it.isPunchedIn && it.activeStartMillis != null }
        }
        val afterNow = System.currentTimeMillis()

        val recordedStart = stateAfterDefaultPunch.activeStartMillis!!
        assertTrue(
            "Default requestPunchIn must use System.currentTimeMillis(), got $recordedStart not in [$beforeNow, $afterNow]",
            recordedStart in beforeNow..afterNow
        )

        // Verify WfoSettings contains no persisted transit offset field
        val fieldNames = WfoSettings::class.java.declaredFields.map { it.name }
        assertFalse(
            "WfoSettings should not persist lastUsedTransitOffsetMinutes",
            fieldNames.contains("lastUsedTransitOffsetMinutes")
        )

        repo2.clearActiveSessionOnly()
    }

    @Test
    fun gistSyncWorkerSkipsSyncWhenTokenIsEmptyOrUnreadable() = runBlocking {
        val encKey = stringPreferencesKey("enc_git_token")
        val gistKey = stringPreferencesKey("gist_id")
        application.dataStore.edit { prefs ->
            prefs[encKey] = "UNREADABLE_CIPHERTEXT"
            prefs[gistKey] = "gist12345"
        }

        var apiCalled = false
        val failingApi = object : GitHubGistApi {
            override suspend fun getGist(
                gistId: String,
                authHeader: String,
                accept: String
            ): Response<GistResponse> {
                apiCalled = true
                throw AssertionError("Network API must not be called when token is unreadable")
            }

            override suspend fun updateGist(
                gistId: String,
                authHeader: String,
                request: GistUpdateRequest,
                accept: String
            ): Response<GistResponse> {
                apiCalled = true
                throw AssertionError("Network API must not be called when token is unreadable")
            }
        }

        val prefs = WfoPreferences(
            context = application,
            encryptFn = { it },
            decryptFn = { null } // simulates unreadable KeyStore token
        )
        val repo = WfoRepository(
            context = application,
            database = database,
            preferences = prefs,
            gistApi = failingApi
        )

        val workerResult = GistSyncWorker.runSync(repo)
        assertEquals(androidx.work.ListenableWorker.Result.success(), workerResult)
        assertFalse("GistSyncWorker must skip network call when token is empty/unreadable", apiCalled)
        assertTrue(prefs.settingsFlow.first().tokenUnreadable)
    }
}
