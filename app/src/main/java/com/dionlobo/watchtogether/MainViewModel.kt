package com.dionlobo.watchtogether

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun Throwable.isCancellation(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is CancellationException) return true
        cause = cause.cause
    }
    return false
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository()
    private val prefs = app.getSharedPreferences("watch_together_prefs", Context.MODE_PRIVATE)

    var ready by mutableStateOf(false)
        private set
    var uid by mutableStateOf<String?>(null)
        private set
    var myName by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var pendingJoinCode by mutableStateOf<String?>(null)
        private set
    var darkTheme by mutableStateOf(prefs.getBoolean("dark_theme", false))
        private set

    init {
        viewModelScope.launch {
            try {
                val id = repo.ensureSignedIn()
                val name = repo.profile(id)?.name ?: ""
                uid = id
                myName = name
                ready = true
                startActivityWatcher(id)
                ReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                if (!e.isCancellation()) message = e.localizedMessage ?: "Could not sign in"
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
                repo.updateName(id, name.trim())
                myName = name.trim()
                onDone()
            } catch (e: Exception) {
                if (!e.isCancellation()) message = e.localizedMessage ?: "Could not save name"
            } finally {
                busy = false
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        darkTheme = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun createGroup(name: String, onDone: (Group) -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val id = uid ?: repo.ensureSignedIn()
                val group = repo.createGroup(name, id, myName)
                onDone(group)
            } catch (e: Exception) {
                if (!e.isCancellation()) message = e.localizedMessage ?: "Could not create group"
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
                if (!e.isCancellation()) message = e.localizedMessage ?: "Could not join group"
            } finally {
                busy = false
            }
        }
    }

    fun launchApp(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            if (!e.isCancellation()) message = e.localizedMessage ?: "Something went wrong"
        }
    }

    fun clearMessage() {
        message = null
    }
}
