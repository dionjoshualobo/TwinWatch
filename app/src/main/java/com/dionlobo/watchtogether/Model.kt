package com.dionlobo.watchtogether

import com.google.firebase.firestore.DocumentSnapshot

data class UserProfile(
    val uid: String = "",
    val name: String = ""
)

data class Group(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val memberUids: List<String> = emptyList()
)

data class Member(
    val uid: String = "",
    val name: String = "",
    val joinedAt: Long = 0L
)

data class ShowEntry(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val addedBy: String = "",
    val addedAt: Long = 0L
)

data class TvEpisode(
    val id: Int = 0,
    val name: String = "",
    val season: Int = 0,
    val number: Int = 0,
    val airdate: String = "",
    val imageUrl: String = ""
)

data class WatchedEpisode(
    val id: String = "",
    val season: Int = 0,
    val number: Int = 0,
    val name: String = "",
    val markedAt: Long = 0L
)

data class Note(
    val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val scope: String = "show",
    val showId: String = "",
    val season: Int = 0,
    val episode: Int = 0,
    val text: String = "",
    val isPublic: Boolean = true,
    val createdAt: Long = 0L
)

data class GroupEvent(
    val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val body: String = "",
    val createdAt: Long = 0L,
    val showId: String = ""
)

private fun DocumentSnapshot.str(key: String): String = (get(key) as? String) ?: ""
private fun DocumentSnapshot.long(key: String): Long = (get(key) as? Number)?.toLong() ?: 0L
private fun DocumentSnapshot.int(key: String): Int = (get(key) as? Number)?.toInt() ?: 0
private fun DocumentSnapshot.bool(key: String): Boolean = (get(key) as? Boolean) ?: false

fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(id, str("name"))

fun DocumentSnapshot.toGroup(): Group = Group(
    id = id,
    name = str("name"),
    inviteCode = str("inviteCode"),
    createdBy = str("createdBy"),
    createdAt = long("createdAt"),
    memberUids = (get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
)

fun DocumentSnapshot.toMember(): Member = Member(
    uid = id,
    name = str("name"),
    joinedAt = long("joinedAt")
)

fun DocumentSnapshot.toShowEntry(): ShowEntry = ShowEntry(
    id = id,
    name = str("name"),
    imageUrl = str("imageUrl"),
    addedBy = str("addedBy"),
    addedAt = long("addedAt")
)

fun DocumentSnapshot.toWatchedEpisode(): WatchedEpisode = WatchedEpisode(
    id = id,
    season = int("season"),
    number = int("number"),
    name = str("name"),
    markedAt = long("markedAt")
)

fun DocumentSnapshot.toNote(): Note = Note(
    id = id,
    uid = str("uid"),
    authorName = str("authorName"),
    scope = str("scope"),
    showId = str("showId"),
    season = int("season"),
    episode = int("episode"),
    text = str("text"),
    isPublic = bool("isPublic"),
    createdAt = long("createdAt")
)

fun DocumentSnapshot.toGroupEvent(): GroupEvent = GroupEvent(
    id = id,
    uid = str("uid"),
    authorName = str("authorName"),
    body = str("body"),
    createdAt = long("createdAt"),
    showId = str("showId")
)
