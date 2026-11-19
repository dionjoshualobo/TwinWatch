package com.dionlobo.watchtogether

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

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
    var circleGroups by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var pendingJoinCode by mutableStateOf<String?>(null)
        private set
    var darkTheme by mutableStateOf(prefs.getBoolean("dark_theme", false))
        private set
    val appVersion: String = AppUpdater.currentVersion(app)
    var updateChecking by mutableStateOf(false)
        private set
    var updateAvailable by mutableStateOf<AppUpdate?>(null)
        private set
    var updateDownloading by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0)
        private set
    var updateReady by mutableStateOf<File?>(null)
        private set
    var updateNeedsPermission by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                val id = repo.ensureSignedIn()
                val profile = repo.profile(id)
                uid = id
                myName = profile?.name ?: ""
                circleGroups = profile?.circleGroups ?: emptyMap()
                ready = true
                startActivityWatcher(id)
                ReminderScheduler.schedule(getApplication())
                checkForUpdate(silent = true)
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

    fun saveCircleGroups(groups: Map<String, String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            busy = true
            try {
                val id = uid ?: repo.ensureSignedIn()
                repo.setCircleGroups(id, groups)
                circleGroups = groups
                onDone()
            } catch (e: Exception) {
                if (!e.isCancellation()) message = e.localizedMessage ?: "Could not save circle groups"
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

    fun checkForUpdate(force: Boolean = false, silent: Boolean = false) {
        viewModelScope.launch {
            if (updateDownloading) return@launch
            if (!force && !AppUpdater.shouldCheck(getApplication())) return@launch
            updateChecking = true
            AppUpdater.markChecked(getApplication())
            try {
                val next = AppUpdater.latestUpdate(getApplication())
                if (next != null) {
                    if (next.versionName != updateAvailable?.versionName) updateReady = null
                    updateAvailable = next
                } else {
                    updateAvailable = null
                    updateReady = null
                    if (!silent) message = "You're up to date"
                }
            } catch (e: Exception) {
                if (!e.isCancellation() && !silent) {
                    message = "Could not check for updates: ${e.localizedMessage}"
                }
            } finally {
                updateChecking = false
            }
        }
    }

    fun startUpdate() {
        val update = updateAvailable ?: return
        if (updateReady != null) {
            installUpdate()
            return
        }
        if (!AppUpdater.canInstall(getApplication())) {
            updateNeedsPermission = true
            openInstallSettings()
            return
        }
        viewModelScope.launch {
            updateDownloading = true
            updateProgress = 0
            try {
                val file = AppUpdater.download(getApplication(), update) { updateProgress = it }
                if (updateAvailable?.versionName == update.versionName) {
                    updateReady = file
                    installUpdate()
                }
            } catch (e: Exception) {
                if (!e.isCancellation()) message = "Could not download update: ${e.localizedMessage}"
            } finally {
                updateDownloading = false
            }
        }
    }

    fun installUpdate() {
        val file = updateReady ?: return
        if (!AppUpdater.canInstall(getApplication())) {
            updateNeedsPermission = true
            openInstallSettings()
            return
        }
        try {
            getApplication<Application>().startActivity(AppUpdater.installIntent(getApplication(), file))
            updateNeedsPermission = false
        } catch (e: Exception) {
            message = "Could not open installer: ${e.localizedMessage}"
        }
    }

    fun openInstallSettings() {
        try {
            getApplication<Application>().startActivity(
                AppUpdater.unknownSourcesIntent(getApplication())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            message = "Could not open install settings: ${e.localizedMessage}"
        }
    }

    fun retryUpdateAfterPermission() {
        if (!updateNeedsPermission || updateAvailable == null) return
        if (AppUpdater.canInstall(getApplication())) {
            updateNeedsPermission = false
            startUpdate()
        }
    }

    fun dismissUpdate() {
        updateAvailable = null
        updateNeedsPermission = false
    }

    fun clearMessage() {
        message = null
    }
}
