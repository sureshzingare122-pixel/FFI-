package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.TrainingMode
import com.example.data.model.TrainingSession
import com.example.data.repository.TrainingRepository
import com.example.ui.theme.FFITrainerProTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: TrainingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TrainingRepository(
            sessionDao = db.trainingSessionDao(),
            profileDao = db.userProfileDao(),
            sensitivityDao = db.sensitivitySettingsDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify app name string matches FFI Trainer Pro`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("FFI Trainer Pro", context.getString(R.string.app_name))
    }

    @Test
    fun `room seeds profiles and prevents duplicate training session records`() = runBlocking {
        repository.ensureSeedData()
        val profiles = repository.profilesFlow.first()
        assertEquals(listOf("RUSH", "DEFENSIVE", "SNIPER"), profiles.map { it.profileId })

        val session = TrainingSession(
            sessionKey = "unique-session-001",
            mode = TrainingMode.PRACTICE_ARENA.name,
            subMode = "FREE_RANGE",
            profileName = "RUSH",
            timestampMs = 1700000000000L,
            durationMs = 45000L,
            totalAttempts = 20,
            successfulHits = 15,
            missedTargets = 5,
            accuracyPercent = 75f,
            centerZoneAccuracyPercent = 40f,
            avgReactionTimeMs = 310f,
            flickAccuracyPercent = 75f,
            score = 1850,
            performanceRatio = 1.35f
        )

        val firstInsert = repository.recordSession(session)
        val duplicateInsert = repository.recordSession(session)

        assertTrue(firstInsert)
        assertFalse(duplicateInsert)

        val savedSessions = repository.allSessionsDesc.first()
        assertEquals(1, savedSessions.size)
        assertEquals(1850, savedSessions.first().score)
    }

    @Test
    fun `navigation tabs open all four functional screens`() {
        val app = ApplicationProvider.getApplicationContext<FFITrainerApplication>()
        composeTestRule.setContent {
            FFITrainerProTheme {
                FFITrainerRoot(app = app, showSplashInitially = false)
            }
        }
        composeTestRule.waitForIdle()

        // 1. PRACTICE tab active initially
        composeTestRule.onNodeWithTag("practice_arena_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("practice_fire_button").assertIsDisplayed()

        // 2. TRAINER tab & sub-tabs
        composeTestRule.onNodeWithTag("nav_tab_trainer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trainer_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trainer_subtab_aim_reaction").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("aim_trainer_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trainer_subtab_calibrate").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_calibration_button").performScrollTo().assertIsDisplayed()

        // 3. STATISTICS tab (empty state initially)
        composeTestRule.onNodeWithTag("nav_tab_statistics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("statistics_screen").assertIsDisplayed()

        // 4. SETTINGS tab & privacy declaration
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRIVACY & SECURITY DECLARATION").performScrollTo().assertIsDisplayed()
    }
}
