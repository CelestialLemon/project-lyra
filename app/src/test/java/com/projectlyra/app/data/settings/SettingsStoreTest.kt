package com.projectlyra.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreTest {
    private lateinit var context: Context
    private lateinit var crypto: ApiKeyCrypto
    private lateinit var store: SettingsStore

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        crypto = mockk()
        store = SettingsStore(context, apiKeyCrypto = crypto)
        store.updateApiKey("")
        store.updateReminder(enabled = true, hour = 20, minute = 0)
        store.updateIncludeApiKeyInBackup(false)
    }

    @Test
    fun updateApiKey_encryptsAndPersistsValue() = runBlocking {
        every { crypto.encrypt("abc123") } returns EncryptedApiKeyPayload(ciphertext = "cipher", iv = "iv")
        every { crypto.decrypt("cipher", "iv") } returns "abc123"

        val saved = store.updateApiKey("  abc123  ")
        val settings = store.settings.first()

        assertTrue(saved)
        assertEquals("abc123", settings.apiKey)
        verify(exactly = 1) { crypto.encrypt("abc123") }
        verify(exactly = 1) { crypto.decrypt("cipher", "iv") }
    }

    @Test
    fun updateApiKey_returnsFalseWhenEncryptionFails() = runBlocking {
        every { crypto.encrypt("bad-key") } returns null

        val saved = store.updateApiKey("bad-key")

        assertFalse(saved)
    }

    @Test
    fun updateApiKey_blankClearsStoredValue() = runBlocking {
        every { crypto.encrypt("token") } returns EncryptedApiKeyPayload(ciphertext = "cipher2", iv = "iv2")
        every { crypto.decrypt("cipher2", "iv2") } returns "token"
        store.updateApiKey("token")

        val cleared = store.updateApiKey("   ")
        val settings = store.settings.first()

        assertTrue(cleared)
        assertEquals("", settings.apiKey)
    }

    @Test
    fun updateReminder_persistsSchedule() = runBlocking {
        store.updateReminder(enabled = false, hour = 8, minute = 30)

        val settings = store.settings.first()

        assertFalse(settings.reminderEnabled)
        assertEquals(8, settings.reminderHour)
        assertEquals(30, settings.reminderMinute)
    }

    @Test
    fun updateIncludeApiKeyInBackup_persistsFlag() = runBlocking {
        store.updateIncludeApiKeyInBackup(true)

        val settings = store.settings.first()

        assertTrue(settings.includeApiKeyInBackup)
    }
}
