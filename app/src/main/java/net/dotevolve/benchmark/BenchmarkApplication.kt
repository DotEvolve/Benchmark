package net.dotevolve.benchmark

import android.app.Application
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

class BenchmarkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        val currentProcessName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            packageName
        }

        val cacheSettings = if (currentProcessName == packageName) {
            PersistentCacheSettings.newBuilder().build()
        } else {
            MemoryCacheSettings.newBuilder().build()
        }

        val settings = firestoreSettings {
            setLocalCacheSettings(cacheSettings)
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
