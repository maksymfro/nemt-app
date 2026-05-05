package com.mobapps.nemt.push

/**
 * Contract for **server-sent** FCM data messages when trip [com.mobapps.nemt.data.TripLifecycleStatus]
 * changes (e.g. after Firestore `trips/{id}` update).
 *
 * Implement in Cloud Functions / backend:
 * - Listen to `trips` writes, read `riderUid`, load `users/{uid}.fcmToken`, send message with these keys.
 *
 * [NemtFirebaseMessagingService] displays `title` and `body` in the trip notification channel.
 */
object TripFcmContract {
    const val DATA_TITLE = "title"
    const val DATA_BODY = "body"
    const val DATA_TRIP_ID = "tripId"
    const val DATA_NEW_STATUS = "lifecycleStatus"
}
