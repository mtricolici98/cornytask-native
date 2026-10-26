package com.nobadhabbits.cornytask.features.medat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.MedatDayType
import com.nobadhabbits.cornytask.data.MedatItemProgress
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.util.Date

data class MedatData(
    // Keyed by plan item id.
    val progress: Map<String, MedatItemProgress> = emptyMap(),
    // Only dates that aren't plain study days.
    val dayTypes: Map<LocalDate, MedatDayType> = emptyMap()
)

/**
 * Progress through the MedAT plan, stored in users/{uid}/medat: one doc per touched item,
 * plus one [CALENDAR_DOC] holding the break/simulation days.
 */
class MedatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private fun userDoc() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    private fun progressCollection(): CollectionReference? = userDoc()?.collection("medat")

    fun getDataFlow(): Flow<MedatData> = callbackFlow {
        var subscription: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            subscription?.remove()
            subscription = null

            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(MedatData())
            } else {
                subscription = firestore.collection("users").document(user.uid).collection("medat")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(MedatData())
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val (calendarDocs, itemDocs) = snapshot.documents.partition { it.id == CALENDAR_DOC }
                            val days = calendarDocs.firstOrNull()?.get("days") as? Map<*, *>
                            trySend(
                                MedatData(
                                    progress = itemDocs
                                        .mapNotNull { it.toObject(MedatItemProgress::class.java)?.copy(id = it.id) }
                                        .associateBy { it.id },
                                    dayTypes = days.orEmpty().mapNotNull { (date, type) ->
                                        val parsedType = MedatDayType.entries.firstOrNull { it.name == type }
                                        val parsedDate = runCatching { LocalDate.parse(date.toString()) }.getOrNull()
                                        if (parsedType == null || parsedDate == null) null else parsedDate to parsedType
                                    }.toMap()
                                )
                            )
                        }
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            subscription?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    /** Ticked off by hand: pays [coins] unicorns and logs it in History. */
    fun tick(itemId: String, title: String, coins: Int) {
        val userDoc = userDoc() ?: return
        val historyRef = userDoc.collection("history").document()
        firestore.runBatch { batch ->
            batch.set(historyRef, History(title = "MedAT: $title", rewardCoins = coins))
            batch.set(
                userDoc.collection("medat").document(itemId),
                mapOf(
                    "id" to itemId,
                    "isDone" to true,
                    "completedAt" to Date(),
                    "rewardCoins" to coins,
                    "historyId" to historyRef.id
                ),
                SetOptions.merge()
            )
        }
        userRepository.addCoins(coins)
    }

    /** Unticked by hand: takes back whatever ticking it paid. */
    fun untick(progress: MedatItemProgress) {
        val userDoc = userDoc() ?: return
        firestore.runBatch { batch ->
            progress.historyId?.let { batch.delete(userDoc.collection("history").document(it)) }
            batch.set(
                userDoc.collection("medat").document(progress.id),
                mapOf(
                    "id" to progress.id,
                    "isDone" to false,
                    "completedAt" to null,
                    "rewardCoins" to 0,
                    "historyId" to null
                ),
                SetOptions.merge()
            )
        }
        if (progress.rewardCoins > 0) userRepository.spendCoins(progress.rewardCoins)
    }

    /** Done via a linked time goal or TODO, which pays its own reward. */
    fun setDone(itemIds: Collection<String>, isDone: Boolean) {
        if (itemIds.isEmpty()) return
        mergeAll(itemIds, mapOf("isDone" to isDone, "completedAt" to if (isDone) Date() else null))
    }

    /** "Didn't do it": the item moves to the next plan day, so everything after it shifts back. */
    fun postpone(itemId: String, currentDay: Int) {
        mergeAll(
            listOf(itemId),
            mapOf("day" to currentDay + 1, "postponedCount" to FieldValue.increment(1))
        )
    }

    fun linkTimeGoal(itemIds: Collection<String>, timeGoalId: String) {
        mergeAll(itemIds, mapOf("timeGoalId" to timeGoalId))
    }

    fun linkTodo(itemIds: Collection<String>, todoId: String) {
        mergeAll(itemIds, mapOf("todoId" to todoId))
    }

    fun setDayType(date: LocalDate, type: MedatDayType) {
        val doc = progressCollection()?.document(CALENDAR_DOC) ?: return
        // Study days are the default, so they're stored as "no mark".
        val value: Any = if (type == MedatDayType.STUDY) FieldValue.delete() else type.name
        doc.set(mapOf("days" to mapOf(date.toString() to value)), SetOptions.merge())
    }

    private fun mergeAll(itemIds: Collection<String>, fields: Map<String, Any?>) {
        val collection = progressCollection() ?: return
        firestore.runBatch { batch ->
            itemIds.forEach { id ->
                batch.set(collection.document(id), fields + ("id" to id), SetOptions.merge())
            }
        }
    }

    companion object {
        // Lives in the same collection so the existing security rule covers it.
        const val CALENDAR_DOC = "_calendar"
    }
}
