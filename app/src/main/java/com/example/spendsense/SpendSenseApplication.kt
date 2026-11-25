package com.example.spendsense

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpendSenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        checkNotNull(FirebaseApp.initializeApp(this)) {
            "Firebase is not configured. Add the project's google-services.json file."
        }
        AppCheckProviderSetup.install()
    }
}
