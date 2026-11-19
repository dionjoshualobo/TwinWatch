package com.dionlobo.watchtogether

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class Repository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user!!.uid
    }

    suspend fun saveProfile(uid: String, name: String) {
        db.collection("users").document(uid)
            .set(mapOf("name" to name, "createdAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun profile(uid: String): UserProfile? {
        val snap = db.collection("users").document(uid).get().await()
        return if (snap.exists()) snap.toUserProfile() else null
    }

    suspend fun updateName(uid: String, name: String) {
        val clean = name.trim()
        val groups = db.collection("groups").whereArrayContains("memberUids", uid).get().await()
        db.runBatch { batch ->
            batch.set(
                db.collection("users").document(uid),
                mapOf("name" to clean, "createdAt" to FieldValue.serverTimestamp())
            )
            groups.documents.forEach { group ->
                batch.update(
                    group.reference.collection("members").document(uid),
                    mapOf("name" to clean)
                )
            }
        }.await()
    }

    fun groupsFlow(uid: String): Flow<List<Group>> = callbackFlow {
        val listener = db.collection("groups").whereArrayContains("memberUids", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { it.toGroup() }.sortedBy { it.name })
            }
        awaitClose { listener.remove() }
    }

    fun membersFlow(groupId: String): Flow<List<Member>> = callbackFlow {
        val listener = db.collection("groups").document(groupId).collection("members")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { it.toMember() }.sortedBy { it.name })
            }
        awaitClose { listener.remove() }
    }

    fun showsFlow(groupId: String): Flow<List<ShowEntry>> = callbackFlow {
        val listener = db.collection("groups").document(groupId).collection("shows")
            .orderBy("addedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { it.toShowEntry() })
            }
        awaitClose { listener.remove() }
    }

    fun watchedFlow(groupId: String, uid: String): Flow<List<WatchedEpisode>> = callbackFlow {
        val listener = db.collection("groups").document(groupId)
            .collection("watched").document(uid).collection("episodes")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { it.toWatchedEpisode() })
            }
        awaitClose { listener.remove() }
    }

    fun memberWatchedFlow(groupId: String, memberUids: List<String>): Flow<Map<String, List<WatchedEpisode>>> = callbackFlow {
        val watched = memberUids.associateWith { emptyList<WatchedEpisode>() }.toMutableMap()
        val listeners = memberUids.map { uid ->
            db.collection("groups").document(groupId).collection("watched")
                .document(uid).collection("episodes")
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    watched[uid] = snap.documents.map { it.toWatchedEpisode() }
                        .sortedWith(compareBy({ it.season }, { it.number }))
                    trySend(watched.toMap())
                }
        }
        trySend(watched.toMap())
        awaitClose { listeners.forEach { it.remove() } }
    }

    fun notesFlow(groupId: String, uid: String): Flow<List<Note>> = callbackFlow {
        val base = db.collection("groups").document(groupId).collection("notes")
        val publicNotes = mutableMapOf<String, Note>()
        val myNotes = mutableMapOf<String, Note>()

        fun publish() {
            val all = (publicNotes.values + myNotes.values)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            trySend(all)
        }

        fun read(snap: com.google.firebase.firestore.QuerySnapshot?, map: MutableMap<String, Note>) {
            if (snap == null) return
            map.clear()
            snap.documents.forEach { map[it.id] = it.toNote() }
            publish()
        }

        val l1 = base.whereEqualTo("isPublic", true)
            .addSnapshotListener { snap, _ -> read(snap, publicNotes) }
        val l2 = base.whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ -> read(snap, myNotes) }
        awaitClose { l1.remove(); l2.remove() }
    }

    fun memberPublicNotesFlow(groupId: String, memberUid: String): Flow<List<Note>> = callbackFlow {
        val listener = db.collection("groups").document(groupId).collection("notes")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snap, _ ->
                val notes = snap?.documents.orEmpty().map { it.toNote() }.filter { it.uid == memberUid }
                trySend(notes.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun eventsFlow(groupId: String): Flow<List<GroupEvent>> = callbackFlow {
        val listener = db.collection("groups").document(groupId).collection("events")
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(60)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { it.toGroupEvent() })
            }
        awaitClose { listener.remove() }
    }

    fun activityEventsFlow(uid: String): Flow<Pair<String, GroupEvent>> = callbackFlow {
        val eventListeners = mutableListOf<ListenerRegistration>()
        val seen = mutableMapOf<String, MutableSet<String>>()
        val initialized = mutableSetOf<String>()

        val groupsListener = db.collection("groups").whereArrayContains("memberUids", uid)
            .addSnapshotListener { groupSnap, _ ->
                eventListeners.forEach { it.remove() }
                eventListeners.clear()

                groupSnap?.documents?.forEach { groupDoc ->
                    val groupId = groupDoc.id
                    val eventIds = seen.getOrPut(groupId) { mutableSetOf() }
                    val firstSnapshot = initialized.add(groupId)

                    val listener = db.collection("groups").document(groupId).collection("events")
                        .addSnapshotListener { snap, error ->
                            if (error != null || snap == null) return@addSnapshotListener
                            if (snap.metadata.hasPendingWrites()) return@addSnapshotListener

                            val fresh = if (firstSnapshot) {
                                snap.documents.forEach { eventIds.add(it.id) }
                                emptyList()
                            } else {
                                snap.documents.filter { !eventIds.contains(it.id) }
                            }
                            fresh.forEach { eventIds.add(it.id) }
                            fresh.forEach { trySend(groupId to it.toGroupEvent()) }
                        }
                    eventListeners.add(listener)
                }
            }

        awaitClose {
            groupsListener.remove()
            eventListeners.forEach { it.remove() }
        }
    }

    suspend fun group(groupId: String): Group? {
        val snap = db.collection("groups").document(groupId).get().await()
        return if (snap.exists()) snap.toGroup() else null
    }

    suspend fun groupByCode(code: String): Group? {
        val snap = db.collection("groups").whereEqualTo("inviteCode", code.trim().uppercase()).limit(1).get().await()
        return snap.documents.firstOrNull()?.toGroup()
    }

    private suspend fun memberIds(groupId: String): List<String> =
        group(groupId)?.memberUids ?: emptyList()

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    suspend fun createGroup(name: String, uid: String, myName: String): Group {
        val ref = db.collection("groups").document()
        var code = newCode()
        while (groupByCode(code) != null) code = newCode()
        val group = Group(ref.id, name.trim(), code, uid, System.currentTimeMillis(), listOf(uid))
        ref.set(
            mapOf(
                "name" to group.name,
                "inviteCode" to group.inviteCode,
                "createdBy" to uid,
                "createdAt" to group.createdAt,
                "memberUids" to group.memberUids
            )
        ).await()
        addMember(ref.id, uid, myName, group.memberUids)
        subscribe(ref.id)
        return group
    }

    suspend fun joinGroup(code: String, uid: String, myName: String): Group? {
        val group = groupByCode(code) ?: return null
        if (!group.memberUids.contains(uid)) {
            db.collection("groups").document(group.id)
                .update("memberUids", FieldValue.arrayUnion(uid)).await()
            addMember(group.id, uid, myName, group.memberUids + uid)
        }
        subscribe(group.id)
        return group
    }

    private suspend fun addMember(groupId: String, uid: String, name: String, memberUids: List<String>) {
        db.collection("groups").document(groupId).collection("members").document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "name" to name,
                    "memberUids" to memberUids,
                    "joinedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun subscribe(groupId: String) {
        runCatching { messaging.subscribeToTopic("g_$groupId").await() }
    }

    suspend fun addShow(groupId: String, show: TvShow, uid: String) {
        val ref = db.collection("groups").document(groupId).collection("shows").document(show.id.toString())
        ref.set(
            mapOf(
                "name" to show.name,
                "imageUrl" to (show.image?.medium ?: show.image?.original ?: ""),
                "addedBy" to uid,
                "memberUids" to memberIds(groupId),
                "addedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun markEpisodes(
        groupId: String,
        uid: String,
        authorName: String,
        show: ShowEntry,
        episodes: List<TvEpisode>,
        summary: String
    ) {
        val uniqueEpisodes = episodes.distinctBy { it.id }
        if (uniqueEpisodes.isEmpty()) return
        val watched = db.collection("groups").document(groupId)
            .collection("watched").document(uid).collection("episodes")
        val groupMemberUids = memberIds(groupId)
        val chunks = uniqueEpisodes.chunked(300)
        val events = db.collection("groups").document(groupId).collection("events")
        chunks.forEachIndexed { index, chunk ->
            db.runBatch { batch ->
                chunk.forEach { ep ->
                    batch.set(
                        watched.document(ep.id.toString()),
                        mapOf(
                            "season" to ep.season,
                            "number" to ep.number,
                            "name" to ep.name,
                            "showId" to show.id,
                            "memberUids" to groupMemberUids,
                            "markedAt" to System.currentTimeMillis()
                        )
                    )
                }
                if (index == chunks.lastIndex) {
                    batch.set(
                        events.document(),
                        mapOf(
                            "uid" to uid,
                            "authorName" to authorName,
                            "showId" to show.id,
                            "memberUids" to groupMemberUids,
                            "body" to "$authorName $summary of ${show.name}",
                            "createdAt" to System.currentTimeMillis()
                        )
                    )
                }
            }.await()
        }
    }

    suspend fun unmarkEpisode(groupId: String, uid: String, episodeId: Int) {
        db.collection("groups").document(groupId).collection("watched")
            .document(uid).collection("episodes").document(episodeId.toString()).delete().await()
    }

    suspend fun resetShowProgress(groupId: String, uid: String, episodeIds: List<String>) {
        val episodes = db.collection("groups").document(groupId).collection("watched")
            .document(uid).collection("episodes")
        episodeIds.chunked(300).forEach { chunk ->
            db.runBatch { batch ->
                chunk.forEach { id -> batch.delete(episodes.document(id)) }
            }.await()
        }
    }

    suspend fun removeShow(groupId: String, showId: String) {
        db.collection("groups").document(groupId).collection("shows").document(showId).delete().await()
    }

    suspend fun deleteGroup(groupId: String) {
        val root = db.collection("groups").document(groupId)
        val watched = root.collection("watched")
        watched.get().await().documents.forEach { member ->
            deleteCollection(member.reference.collection("episodes"))
        }
        deleteCollection(watched)
        deleteCollection(root.collection("shows"))
        deleteCollection(root.collection("members"))
        deleteCollection(root.collection("notes"))
        deleteCollection(root.collection("events"))
        root.delete().await()
        runCatching { messaging.unsubscribeFromTopic("g_$groupId").await() }
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        while (true) {
            val docs = collection.limit(300).get().await().documents
            if (docs.isEmpty()) return
            db.runBatch { batch ->
                docs.forEach { batch.delete(it.reference) }
            }.await()
        }
    }

    suspend fun addNote(
        groupId: String,
        uid: String,
        authorName: String,
        show: ShowEntry,
        scope: String,
        season: Int,
        episode: Int,
        text: String,
        isPublic: Boolean
    ) {
        val where = when (scope) {
            "season" -> "S${season.toString().padStart(2, '0')} of ${show.name}"
            "episode" -> "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')} of ${show.name}"
            else -> show.name
        }
        val groupMemberUids = memberIds(groupId)
        db.runBatch { batch ->
            batch.set(
                db.collection("groups").document(groupId).collection("notes").document(),
                mapOf(
                    "uid" to uid,
                    "authorName" to authorName,
                    "scope" to scope,
                    "showId" to show.id,
                    "season" to season,
                    "episode" to episode,
                    "text" to text,
                    "isPublic" to isPublic,
                    "memberUids" to groupMemberUids,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            if (isPublic) {
                batch.set(
                    db.collection("groups").document(groupId).collection("events").document(),
                    mapOf(
                        "uid" to uid,
                        "authorName" to authorName,
                        "showId" to show.id,
                        "memberUids" to groupMemberUids,
                        "body" to "$authorName wrote a note about $where",
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }
        }.await()
    }
}
