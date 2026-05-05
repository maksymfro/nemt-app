package com.mobapps.nemt.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Rider-scoped trips in Firestore collection `trips`.
 *
 * Expected Firestore rules (example): only authenticated users can read/write documents
 * where `riderUid == request.auth.uid`.
 */
object TripsFirestoreRepository {

    private const val COLLECTION = "trips"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val tripsCollection by lazy { firestore.collection(COLLECTION) }

    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    private var listener: ListenerRegistration? = null

    private val demoSeedRequestedForUid: MutableSet<String> =
        ConcurrentHashMap.newKeySet()

    fun startListening(riderUid: String) {
        listener?.remove()
        listener = tripsCollection
            .whereEqualTo("riderUid", riderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _trips.value = emptyList()
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty() && demoSeedRequestedForUid.add(riderUid)) {
                    seedDemoTripsIfEmpty(riderUid)
                }
                val list = docs.mapNotNull { it.toTripRecord() }
                _trips.value = list.sortedWith(
                    compareByDescending<TripRecord> { it.scheduledAtMillis }
                        .thenByDescending { it.createdAtMillis }
                )
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    fun createTrip(
        riderUid: String,
        from: String,
        to: String,
        scheduledAtMillis: Long,
        dateTimeDisplay: String,
        vehicle: String,
        patientName: String,
        mobilitySupportSnapshot: String?,
        accessibilityNeedsSnapshot: String?,
        onComplete: (Result<String>) -> Unit
    ) {
        val docRef = tripsCollection.document()
        val id = docRef.id
        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "id" to id,
            "riderUid" to riderUid,
            "lifecycleStatus" to TripLifecycleStatus.ACCEPTED.name,
            "from" to from,
            "to" to to,
            "scheduledAtMillis" to scheduledAtMillis,
            "dateTimeDisplay" to dateTimeDisplay,
            "vehicle" to vehicle,
            "patientName" to patientName,
            "mobilitySupportSnapshot" to mobilitySupportSnapshot,
            "accessibilityNeedsSnapshot" to accessibilityNeedsSnapshot,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdAtMillis" to now,
            "updatedAtMillis" to now
        )
        docRef.set(data)
            .addOnSuccessListener { onComplete(Result.success(id)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun updateTripFields(
        tripId: String,
        newDateTimeDisplay: String,
        newFrom: String,
        newTo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        tripsCollection.document(tripId)
            .update(
                mapOf(
                    "dateTimeDisplay" to newDateTimeDisplay,
                    "from" to newFrom,
                    "to" to newTo,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedAtMillis" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun cancelTrip(tripId: String, onComplete: (Result<Unit>) -> Unit) {
        tripsCollection.document(tripId)
            .update(
                mapOf(
                    "lifecycleStatus" to TripLifecycleStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedAtMillis" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    /**
     * Optional demo seed when a new rider has no trips (Spain sample data).
     * Writes a batch; listener will refresh.
     */
    private fun seedDemoTripsIfEmpty(riderUid: String) {
        val batch = firestore.batch()
        val templates = demoTripTemplates()
        templates.forEachIndexed { index, template ->
            val ref = tripsCollection.document()
            val now = System.currentTimeMillis()
            batch.set(
                ref,
                hashMapOf(
                    "id" to ref.id,
                    "riderUid" to riderUid,
                    "lifecycleStatus" to template.status.name,
                    "from" to template.from,
                    "to" to template.to,
                    "scheduledAtMillis" to template.scheduledAtMillis,
                    "dateTimeDisplay" to template.dateTimeDisplay,
                    "vehicle" to template.vehicle,
                    "patientName" to template.patientName,
                    "mobilitySupportSnapshot" to template.mobility,
                    "accessibilityNeedsSnapshot" to template.accessibility,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAtMillis" to now + index,
                    "updatedAtMillis" to now + index
                )
            )
        }
        batch.commit()
    }

    private data class DemoTemplate(
        val status: TripLifecycleStatus,
        val from: String,
        val to: String,
        val scheduledAtMillis: Long,
        val dateTimeDisplay: String,
        val vehicle: String,
        val patientName: String,
        val mobility: String?,
        val accessibility: String?
    )

    private fun demoTripTemplates(): List<DemoTemplate> {
        val random = Random(91)
        val base = System.currentTimeMillis()
        val pickups = listOf(
            "Calle de Atocha 27, Madrid",
            "Carrer de Mallorca 201, Barcelona",
            "Avenida de la Constitución 10, Sevilla",
            "Paseo de la Alameda 14, Valencia",
            "Calle Larios 6, Málaga",
            "Gran Vía de Colón 22, Granada",
            "Plaza de España 3, Zaragoza",
            "Calle Uría 8, Oviedo",
            "Calle Triana 45, Las Palmas",
            "Rúa do Vilar 19, Santiago de Compostela"
        )
        val destinations = listOf(
            "Hospital Universitario La Paz, Madrid",
            "Hospital Clínic de Barcelona, Barcelona",
            "Hospital Virgen del Rocío, Sevilla",
            "Hospital La Fe, Valencia",
            "Hospital Regional de Málaga, Málaga",
            "Hospital San Cecilio, Granada",
            "Hospital Miguel Servet, Zaragoza",
            "Hospital de Cruces, Bilbao",
            "Hospital Son Espases, Palma",
            "Hospital Universitario de Navarra, Pamplona"
        )
        val vehicles = listOf(
            "Unit 12 · Wheelchair Van",
            "Unit 7 · Accessible Sedan",
            "Unit 3 · Lift-equipped Van",
            "Unit 19 · Mobility Assist"
        )
        val names = listOf(
            "For: Sofia R.",
            "For: Daniel M.",
            "For: Elena C.",
            "For: Pablo G."
        )
        fun route(i: Int): Pair<String, String> {
            val from = pickups[i % pickups.size]
            val to = destinations[(i + random.nextInt(1, 4)) % destinations.size]
            return from to to
        }
        val out = mutableListOf<DemoTemplate>()
        repeat(4) { i ->
            val (from, to) = route(i)
            out += DemoTemplate(
                status = TripLifecycleStatus.REQUESTED,
                from = from,
                to = to,
                scheduledAtMillis = base + (i + 1) * 86_400_000L,
                dateTimeDisplay = "Upcoming · Day ${i + 1}",
                vehicle = vehicles[i % vehicles.size],
                patientName = names[i % names.size],
                mobility = "Walker support",
                accessibility = "Large text preferred"
            )
        }
        repeat(3) { i ->
            val (from, to) = route(i + 4)
            out += DemoTemplate(
                status = TripLifecycleStatus.COMPLETED,
                from = from,
                to = to,
                scheduledAtMillis = base - (i + 2) * 86_400_000L,
                dateTimeDisplay = "Completed · ${i + 1}d ago",
                vehicle = vehicles[(i + 1) % vehicles.size],
                patientName = names[(i + 1) % names.size],
                mobility = null,
                accessibility = null
            )
        }
        repeat(3) { i ->
            val (from, to) = route(i + 7)
            out += DemoTemplate(
                status = TripLifecycleStatus.CANCELLED,
                from = from,
                to = to,
                scheduledAtMillis = base - (i + 1) * 43_200_000L,
                dateTimeDisplay = "Cancelled · ${i + 1}d ago",
                vehicle = vehicles[(i + 2) % vehicles.size],
                patientName = names[(i + 2) % names.size],
                mobility = null,
                accessibility = null
            )
        }
        return out
    }

    private fun DocumentSnapshot.toTripRecord(): TripRecord? {
        val tripId = id
        val rider = getString("riderUid") ?: return null
        val status = TripLifecycleStatus.fromString(getString("lifecycleStatus"))
        val from = getString("from").orEmpty()
        val to = getString("to").orEmpty()
        val scheduled = getLong("scheduledAtMillis") ?: 0L
        val display = getString("dateTimeDisplay").orEmpty()
        val vehicle = getString("vehicle").orEmpty()
        val patient = getString("patientName").orEmpty()
        val mobility = getString("mobilitySupportSnapshot")
        val accessibility = getString("accessibilityNeedsSnapshot")
        val createdMs = (get("createdAt") as? Timestamp)?.toDate()?.time
            ?: getLong("createdAtMillis") ?: 0L
        val updatedMs = (get("updatedAt") as? Timestamp)?.toDate()?.time
            ?: getLong("updatedAtMillis") ?: 0L
        return TripRecord(
            id = getString("id") ?: tripId,
            riderUid = rider,
            lifecycleStatus = status,
            from = from,
            to = to,
            scheduledAtMillis = scheduled,
            dateTimeDisplay = display,
            vehicle = vehicle,
            patientName = patient,
            mobilitySupportSnapshot = mobility,
            accessibilityNeedsSnapshot = accessibility,
            createdAtMillis = createdMs,
            updatedAtMillis = updatedMs
        )
    }

    /** True if pickup and destination are the same place (by title or very close coordinates). */
    fun areStopsEffectivelyDuplicate(
        pickupTitle: String,
        pickupSubtitle: String,
        pickupLatLng: LatLng?,
        destTitle: String,
        destSubtitle: String,
        destLatLng: LatLng?
    ): Boolean {
        val a = "${pickupTitle.trim().lowercase()}|${pickupSubtitle.trim().lowercase()}"
        val b = "${destTitle.trim().lowercase()}|${destSubtitle.trim().lowercase()}"
        if (a.isNotBlank() && a == b) return true
        if (pickupLatLng != null && destLatLng != null) {
            val meters = SphericalUtil.computeDistanceBetween(pickupLatLng, destLatLng)
            if (meters < 80.0) return true
        }
        return false
    }
}
