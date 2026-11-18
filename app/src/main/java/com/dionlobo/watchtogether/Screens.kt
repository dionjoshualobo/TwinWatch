package com.dionlobo.watchtogether

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun epLabel(season: Int, number: Int) =
    "S${season.toString().padStart(2, '0')}E${number.toString().padStart(2, '0')}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(vm: MainViewModel, group: Group, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val members by remember(group.id) { vm.repo.membersFlow(group.id) }.collectAsState(emptyList())
    val shows by remember(group.id) { vm.repo.showsFlow(group.id) }.collectAsState(emptyList())
    val notes by remember(group.id, uid) { vm.repo.notesFlow(group.id, uid) }.collectAsState(emptyList())
    val events by remember(group.id) { vm.repo.eventsFlow(group.id) }.collectAsState(emptyList())
    var tab by remember { mutableStateOf(0) }
    var invite by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                actions = {
                    IconButton(onClick = { invite = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Invite")
                    }
                }
            )
        },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { go(Screen.AddShow(group)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add show")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Brand.Ink)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f)) {
                    members.take(4).forEachIndexed { index, member ->
                        val colors = listOf(Brand.Amber, Brand.Mint, Brand.Coral, Brand.Paper)
                        Avatar(member.name.take(1).uppercase(), colors[index % colors.size])
                    }
                }
                Text(
                    "CODE ${group.inviteCode}",
                    color = Brand.Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brand.InkSoft)
                        .border(1.dp, Brand.Amber.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            TabRow(selectedTabIndex = tab, containerColor = Brand.InkSoft, contentColor = Brand.Paper) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Shows") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Activity") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Notes") })
            }
            when (tab) {
                0 -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    if (shows.isEmpty()) {
                        item { Text("No shows yet. Add one with the + button.") }
                    }
                    items(shows) { show ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                go(Screen.ShowDetail(group, show))
                            }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (show.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = show.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(width = 48.dp, height = 68.dp)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(show.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    addedByText(show, members)
                                }
                            }
                        }
                    }
                }
                1 -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    if (events.isEmpty()) item { Text("No activity yet. Mark an episode to get things moving.") }
                    itemsIndexed(events) { index, event ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(10.dp).clip(CircleShape).background(
                                        if (index == 0) Brand.Amber else Brand.Mint.copy(alpha = 0.6f)
                                    )
                                )
                                if (index < events.size - 1) {
                                    Spacer(Modifier.height(6.dp).width(2.dp).background(Brand.InkSoft))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    event.body.ifBlank { event.authorName },
                                    fontSize = 14.sp,
                                    color = Brand.Paper
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${event.authorName} · ${timeText(event.createdAt)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                2 -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                     if (notes.isEmpty()) item { Text("No notes yet. Add them from a show or episode.") }
                     itemsIndexed(notes) { index, note ->
                         AnimatedEnter(index * 60) {
                             Card(
                                 Modifier.fillMaxWidth().border(
                                     2.dp,
                                     if (note.isPublic) Brand.Mint.copy(alpha = 0.7f) else Brand.Coral.copy(alpha = 0.7f),
                                     RoundedCornerShape(14.dp)
                                 )
                             ) {
                                 Column(Modifier.padding(14.dp)) {
                                     Text(
                                         "${note.authorName} · ${noteScopeLabel(note)}",
                                         fontSize = 12.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = if (note.isPublic) Brand.Mint else Brand.Coral
                                     )
                                     Spacer(Modifier.height(6.dp))
                                     Text(note.text, color = Brand.Paper)
                                     Spacer(Modifier.height(6.dp))
                                     Text(timeText(note.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                 }
                             }
                         }
                     }
                }
            }
        }
    }

    if (invite) {
        AlertDialog(
            onDismissRequest = { invite = false },
            title = { Text("Invite people") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        group.inviteCode,
                        style = MaterialTheme.typography.displayLarge,
                        color = Brand.Amber,
                        letterSpacing = 8.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Share this code, or send the link below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Join my Watch Together group (${group.name}) with code ${group.inviteCode}\nwatchtogether://join/${group.inviteCode}"
                        )
                    }
                    context.startActivity(Intent.createChooser(share, "Share invite"))
                }) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { invite = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun addedByText(show: ShowEntry, members: List<Member>) {
    Text("Added by ${members.firstOrNull { it.uid == show.addedBy }?.name ?: "someone"}", fontSize = 12.sp)
}

private fun noteScopeLabel(note: Note) = when (note.scope) {
    "season" -> "Season ${note.season}"
    "episode" -> epLabel(note.season, note.episode)
    else -> "Show"
}

private fun timeText(ts: Long): String {
    if (ts <= 0L) return ""
    val minutes = (System.currentTimeMillis() - ts) / 60000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} h ago"
        else -> "${minutes / 1440} d ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(vm: MainViewModel, group: Group, show: ShowEntry) {
    val uid = vm.uid!!
    val watched by remember(group.id, uid) { vm.repo.watchedFlow(group.id, uid) }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var episodes by remember { mutableStateOf<List<TvEpisode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var rangeTarget by remember { mutableStateOf<TvEpisode?>(null) }
    var noteTarget by remember { mutableStateOf<Pair<String, Pair<Int, Int>>?>(null) }

    LaunchedEffect(show.id) {
        try {
            episodes = TvmazeApi.episodes(show.id.toInt()).sortedWith(compareBy({ it.season }, { it.number }))
        } catch (e: Exception) {
            vm.message = "Could not load episodes: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    val watchedIds = watched.map { it.id }.toSet()
    val seasons = episodes.groupBy { it.season }.toSortedMap()

    Scaffold(topBar = { TopAppBar(title = { Text(show.name) }) }) { padding ->
        if (loading) {
            Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    val done = episodes.count { watchedIds.contains(it.id.toString()) }
                    val progress = if (episodes.isEmpty()) 0f else done.toFloat() / episodes.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (show.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = show.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 76.dp, height = 108.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Brand.Amber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                show.name.uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Brand.Paper
                            )
                            AccentDivider()
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "$done of ${episodes.size} watched",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { noteTarget = "show" to (0 to 0) }) {
                                Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add a note")
                            }
                        }
                        ProgressRing(progress)
                    }
                    Spacer(Modifier.height(18.dp))
                }

                seasons.forEach { (season, seasonEpisodes) ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Season $season", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { noteTarget = "season" to (season to 0) }) {
                                Icon(Icons.Default.Comment, contentDescription = "Season note")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(seasonEpisodes) { ep ->
                        val isWatched = watchedIds.contains(ep.id.toString())
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isWatched) Brand.InkSoft else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    if (isWatched) {
                                        scope.launch { vm.repo.unmarkEpisode(group.id, uid, ep.id) }
                                    } else {
                                        val previous = watched
                                            .filter { it.season < ep.season || (it.season == ep.season && it.number < ep.number) }
                                            .maxByOrNull { it.season * 10000 + it.number }
                                        if (previous != null) {
                                            rangeTarget = ep
                                        } else {
                                            scope.launch {
                                                vm.repo.markEpisodes(group.id, uid, vm.myName, show, listOf(ep), "marked ${epLabel(ep.season, ep.number)}")
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isWatched) Brand.Mint else androidx.compose.ui.graphics.Color.Transparent)
                                    .border(2.dp, if (isWatched) Brand.Mint else Brand.Muted, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWatched) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Watched",
                                        tint = Brand.Ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${epLabel(ep.season, ep.number)}  ${ep.name}",
                                    fontSize = 14.sp,
                                    color = if (isWatched) Brand.Muted else Brand.Paper
                                )
                                if (isWatched) Text("Watched", fontSize = 11.sp, color = Brand.Mint)
                            }
                            IconButton(onClick = { noteTarget = "episode" to (ep.season to ep.number) }) {
                                Icon(Icons.Default.Comment, contentDescription = "Episode note", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    rangeTarget?.let { target ->
        val previous = watched
            .filter { it.season < target.season || (it.season == target.season && it.number < target.number) }
            .maxByOrNull { it.season * 10000 + it.number }
        val fromIndex = previous?.let { p -> episodes.indexOfFirst { it.id.toString() == p.id } }
        val toIndex = episodes.indexOfFirst { it.id == target.id }
        val range = if (fromIndex != null && toIndex > fromIndex) episodes.subList(fromIndex + 1, toIndex + 1) else listOf(target)
        val labelStart = range.firstOrNull()?.let { epLabel(it.season, it.number) } ?: ""
        val labelEnd = range.lastOrNull()?.let { epLabel(it.season, it.number) } ?: ""
        val summary = if (range.size == 1) "marked $labelEnd" else "marked $labelStart–$labelEnd"

        AlertDialog(
            onDismissRequest = { rangeTarget = null },
            title = { Text("Mark episodes?") },
            text = { Text("Mark $summary (${range.size} episode${if (range.size == 1) "" else "s"})? Everyone in the group will be notified.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { vm.repo.markEpisodes(group.id, uid, vm.myName, show, range, summary) }
                    rangeTarget = null
                }) { Text("Mark all") }
            },
            dismissButton = { TextButton(onClick = { rangeTarget = null }) { Text("Cancel") } }
        )
    }

    noteTarget?.let { target ->
        val (scopeName, numbers) = target
        val label = when (scopeName) {
            "season" -> "Season ${numbers.first}"
            "episode" -> epLabel(numbers.first, numbers.second)
            else -> show.name
        }
        NoteDialog(
            label = label,
            onDismiss = { noteTarget = null }
        ) { text, isPublic ->
            scope.launch {
                vm.repo.addNote(
                    group.id, uid, vm.myName, show, scopeName,
                    numbers.first, numbers.second, text, isPublic
                )
                noteTarget = null
            }
        }
    }
}

@Composable
fun NoteDialog(label: String, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note about $label") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPublic) "Visible to the group" else "Only visible to me")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim(), isPublic) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowScreen(vm: MainViewModel, group: Group, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TvShow>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        try {
            results = TvmazeApi.search(query.trim())
        } catch (e: Exception) {
            vm.message = "Search failed: ${e.localizedMessage}"
        } finally {
            searching = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Add a show") }) }) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
                Text("FIND A SHOW", style = MaterialTheme.typography.headlineLarge, color = Brand.Amber)
                AccentDivider()
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search TVMaze") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                if (searching) CircularProgressIndicator(color = Brand.Mint)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(results) { index, show ->
                        AnimatedEnter(index * 50) {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    scope.launch {
                                        vm.repo.addShow(group.id, show, vm.uid!!)
                                        onBack()
                                    }
                                }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (!show.image?.medium.isNullOrBlank()) {
                                        AsyncImage(
                                            model = show.image!!.medium,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 52.dp, height = 74.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(show.name, fontWeight = FontWeight.SemiBold, color = Brand.Paper)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
