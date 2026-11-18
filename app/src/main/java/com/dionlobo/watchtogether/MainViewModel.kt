package com.dionlobo.watchtogether

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository()

    var uid by mutableStateOf<String?>(null)
        private set
    var myName by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var pendingJoinCode by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                val id = repo.ensureSignedIn()
                uid = id
                myName = repo.profile(id)?.name ?: ""
                startActivityWatcher(id)
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Could not sign in"
            }
        }
    }

    private fun startActivityWatcher(uid: String) {
        viewModelScope.launch {
            repo.activityEventsFlow(uid).collect { (groupId, event) ->
                if (event.uid != uid && event.body.isNotBlank()) {
                    showActivityNotification(getApplication(), event.body, groupId)
                }
            }
        }
    }

    fun submitJoinCode(code: String?) {
        pendingJoinCode = code
    }

    fun saveName(name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val id = uid ?: repo.ensureSignedIn()
                uid = id
                repo.saveProfile(id, name.trim())
                myName = name.trim()
                onDone()
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Could not save name"
            } finally {
                busy = false
            }
        }
    }

    fun createGroup(name: String, onDone: (Group) -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val id = uid ?: repo.ensureSignedIn()
                val group = repo.createGroup(name, id, myName)
                onDone(group)
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Could not create group"
            } finally {
                busy = false
            }
        }
    }

    fun joinGroup(code: String, onDone: (Group?) -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val id = uid ?: repo.ensureSignedIn()
                val group = repo.joinGroup(code, id, myName)
                if (group != null) pendingJoinCode = null
                onDone(group)
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Could not join group"
            } finally {
                busy = false
            }
        }
    }

    fun clearMessage() {
        message = null
    }
}
