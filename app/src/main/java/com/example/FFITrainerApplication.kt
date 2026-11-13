package com.example

import android.app.Application
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.repository.TrainingRepository
import com.example.util.FeedbackEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FFITrainerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val trainingRepository: TrainingRepository by lazy {
        TrainingRepository(
            sessionDao = database.trainingSessionDao(),
            profileDao = database.userProfileDao(),
            sensitivityDao = database.sensitivitySettingsDao()
        )
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val feedbackEngine: FeedbackEngine by lazy {
        FeedbackEngine()
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            trainingRepository.ensureSeedData()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        feedbackEngine.release()
    }
}
