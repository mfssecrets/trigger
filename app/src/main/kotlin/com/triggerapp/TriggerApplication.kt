package com.triggerapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.data.di.dataModule
import com.triggerapp.di.appModule
import com.triggerapp.feature.auth.di.authFeatureModule
import com.triggerapp.feature.chat.di.chatFeatureModule
import com.triggerapp.feature.home.di.homeFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application entry: registers Koin modules and the FCM notification channel.
 *
 * App Check is installed in [AppCheckInitProvider].
 * @author udit
 */
class TriggerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDummyKeyForPreviewTestingOnly12345")
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setProjectId("trigger-chat-preview")
                    .setDatabaseUrl("https://trigger-chat-preview-default-rtdb.firebaseio.com")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            } catch (e: Exception) {
                android.util.Log.w("TriggerApplication", "Fallback Firebase initialization warning", e)
            }
        }
        startKoin {
            androidContext(this@TriggerApplication)
            modules(
                dataModule,
                authFeatureModule,
                homeFeatureModule,
                chatFeatureModule,
                appModule,
            )
        }
        val channel = NotificationChannel(
            TriggerStrings.Notification.CHANNEL_ID_MESSAGES,
            TriggerStrings.Notification.CHANNEL_NAME_MESSAGES,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
