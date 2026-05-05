package com.mobapps.nemt

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.mobapps.nemt.notifications.NemtNotifications

class NemtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NemtNotifications.ensureChannels(this)
        val key = BuildConfig.MAPS_API_KEY
        if (key.isNotBlank()) {
            MapsInitializer.initialize(applicationContext)
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, key)
            }
        }
        registerFcmTokenIfSignedIn()
    }

    private fun registerFcmTokenIfSignedIn() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }
}
