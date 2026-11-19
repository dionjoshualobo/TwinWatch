package com.dionlobo.watchtogether

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun epLabel(season: Int, number: Int) =
    "S${season.toString().padStart(2, '0')}E${number.toString().padStart(2, '0')}"

private data class MarkTarget(val episode: TvEpisode, val earlierUnwatched: List<TvEpisode>)

private data class DetailsTarget(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val summary: String,
    val meta: String = "",
    val showId: Int? = null
)

private fun plainText(html: String): String =
    HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(vm: MainViewModel, group: Group, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val members by remember(group.id) { vm.repo.membersFlow(group.id) }.collectAsState(emptyList())
    val shows by remember(group.id) { vm.repo.showsFlow(group.id) }.collectAsState(emptyList())
    val notes by remember(group.id, uid) { vm.repo.notesFlow(group.id, uid) }.collectAsState(emptyList())
    val events by remember(group.id) { vm.repo.eventsFlow(group.id) }.collectAsState(emptyList())
    val memberIds = members.map { it.uid }
    val watchedByMember by remember(group.id, memberIds) {
        vm.repo.memberWatchedFlow(group.id, memberIds)
    }.collectAsState(emptyMap())
    var tab by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var detailsTarget by remember { mutableStateOf<DetailsTarget?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val accents = listOf(Brand.Blue, Brand.Mint, Brand.Coral, Brand.Amber)

    fun copy(text: String, label: String) {
        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text))) }
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text(group.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                actions = {
                    PunchButton(
                        text = group.inviteCode,
                        onClick = { copy(group.inviteCode, "Code copied") },
                        modifier = Modifier.width(132.dp).height(48.dp),
                        color = Brand.Amber,
                        contentColor = Brand.OnAccent,
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Brand.OnAccent) }
                    )
                    if (group.createdBy == uid) {
                        PunchIconButton(onClick = { confirmDelete = true }, color = Brand.Coral, contentColor = Brand.OnAccent) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete circle", tint = Brand.OnAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    PunchTabs(listOf("Shows", "Activity", "Notes", "Members"), tab) { tab = it }
                }
                Spacer(Modifier.height(14.dp))
                when (tab) {
                    0 -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                    ) {
                        if (shows.isEmpty()) {
                            item {
                                SoftCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(20.dp)) {
                                        StatusPill("NO SHOWS", Brand.Amber)
                                        Spacer(Modifier.height(12.dp))
                                        Text("The shelf is empty.", style = MaterialTheme.typography.headlineMedium, color = Brand.Ink)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Add a show to start tracking episodes.", color = Brand.Muted)
                                    }
                                }
                            }
                        }
                        itemsIndexed(shows) { index, show ->
                            AnimatedEnter(index * 50) {
                                PunchCard(
                                    onClick = { go(Screen.ShowDetail(group, show)) },
                                    accent = accents[index % accents.size]
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ShowThumb(show.imageUrl, Brand.Blue)
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(show.name, style = MaterialTheme.typography.titleLarge, color = Brand.Ink)
                                            Spacer(Modifier.height(5.dp))
                                            addedByText(show, members)
                                        }
                                        PunchIconButton(
                                            onClick = {
                                                detailsTarget = DetailsTarget(
                                                    title = show.name,
                                                    subtitle = "SHOW DETAILS",
                                                    imageUrl = show.imageUrl,
                                                    summary = show.summary,
                                                    meta = "TVMaze",
                                                    showId = show.id.toIntOrNull()
                                                )
                                            },
                                            color = Brand.Surface,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "Show details", tint = Brand.Ink, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                    ) {
                        if (events.isEmpty()) {
                            item { Text("No activity yet. Mark an episode to get things moving.", color = Brand.Muted) }
                        }
                        itemsIndexed(events) { index, event ->
                            Row(
                                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    Modifier.width(14.dp).fillMaxHeight(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (index < events.size - 1) {
                                        Box(Modifier.width(2.dp).fillMaxHeight().background(Brand.Border))
                                    }
                                    Box(
                                        Modifier.size(12.dp).clip(CircleShape)
                                            .background(if (index == 0) Brand.Blue else Brand.Mint)
                                            .border(2.dp, Brand.Stroke, CircleShape)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(event.body.ifBlank { event.authorName }, color = Brand.Ink, fontSize = 15.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${event.authorName} · ${timeText(event.createdAt)}",
                                        fontSize = 11.sp,
                                        color = Brand.Muted
                                    )
                                }
                            }
                        }
                    }
                     2 -> LazyColumn(
                         modifier = Modifier.weight(1f),
                         verticalArrangement = Arrangement.spacedBy(10.dp),
                         contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                     ) {
                         if (notes.isEmpty()) {
                             item { Text("No notes yet. Add them from a show or episode.", color = Brand.Muted) }
                         }
                         itemsIndexed(notes) { index, note ->
                             AnimatedEnter(index * 50) {
                                 PunchCard(
                                     onClick = {
                                         shows.firstOrNull { it.id == note.showId }?.let { go(Screen.ShowDetail(group, it)) }
                                     },
                                     accent = if (note.isPublic) Brand.Mint else Brand.Coral
                                 ) {
                                     Column(Modifier.padding(16.dp)) {
                                         Text(note.text, style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
                                         Spacer(Modifier.height(12.dp))
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             Avatar(
                                                 note.authorName.take(1).uppercase(),
                                                 accents[index % accents.size],
                                                 size = 30.dp
                                             )
                                             Spacer(Modifier.width(10.dp))
                                             Column(Modifier.weight(1f)) {
                                                 Text(
                                                     if (note.uid == uid) "${note.authorName} (you)" else note.authorName,
                                                     fontSize = 12.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = Brand.Ink
                                                 )
                                                 Text(
                                                     "${noteContext(note, shows)} · ${timeText(note.createdAt)}",
                                                     fontSize = 11.sp,
                                                     color = Brand.Muted
                                                 )
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                     }
                     3 -> LazyColumn(
                         modifier = Modifier.weight(1f),
                         verticalArrangement = Arrangement.spacedBy(10.dp),
                         contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                     ) {
                         itemsIndexed(members) { index, member ->
                             val memberWatched = watchedByMember[member.uid].orEmpty()
                             val showCount = memberWatched.map { it.showId }.distinct().size
                             AnimatedEnter(index * 50) {
                                 PunchCard(
                                     onClick = { go(Screen.MemberDetail(group, member)) },
                                     accent = accents[index % accents.size]
                                 ) {
                                     Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                         Avatar(member.name.take(1).uppercase(), accents[index % accents.size])
                                         Spacer(Modifier.width(12.dp))
                                         Column(Modifier.weight(1f)) {
                                             Text(
                                                 if (member.uid == uid) "${member.name} (you)" else member.name,
                                                 style = MaterialTheme.typography.titleMedium,
                                                 color = Brand.Ink
                                             )
                                             Spacer(Modifier.height(4.dp))
                                             Text(
                                                 "${memberWatched.size} ${if (memberWatched.size == 1) "episode" else "episodes"} · $showCount ${if (showCount == 1) "show" else "shows"}",
                                                 fontSize = 12.sp,
                                                 color = Brand.Muted
                                             )
                                             Spacer(Modifier.height(2.dp))
                                             Text("Tap for progress and notes", fontSize = 11.sp, color = Brand.Blue)
                                         }
                                         StatusPill("${memberWatched.size}", accents[index % accents.size])
                                     }
                                 }
                             }
                         }
                     }
                }
                if (tab == 0) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        PunchButton(
                            text = "Add a show",
                            onClick = { go(Screen.AddShow(group)) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Brand.Blue,
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Brand.OnPrimary) }
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete circle?", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently removes \"${group.name}\" and all of its shows, notes, and progress for everyone.",
                    color = Brand.Muted
                )
            },
            confirmButton = {
                PunchButton(
                    text = "Delete",
                    modifier = Modifier.width(120.dp),
                    color = Brand.Coral,
                    contentColor = Brand.OnAccent,
                    onClick = {
                        confirmDelete = false
                        vm.launchApp {
                            runCatching { vm.repo.deleteGroup(group.id) }
                                .onSuccess { go(Screen.Groups) }
                                .onFailure { if (!it.isCancellation()) vm.message = "Could not delete circle: ${it.localizedMessage}" }
                        }
                    }
                )
            },
            dismissButton = {
                PunchButton(
                    text = "Cancel",
                    modifier = Modifier.width(120.dp),
                    color = Brand.InkSoft,
                    contentColor = Brand.Ink,
                    borderColor = Brand.Border,
                    onClick = { confirmDelete = false }
                )
            }
        )
    }

    detailsTarget?.let { DetailsDialog(it) { detailsTarget = null } }
}

@Composable
private fun GroupProgressList(
    members: List<Member>,
    watched: Map<String, List<WatchedEpisode>>,
    showId: String,
    total: Int,
    currentUid: String,
    onMember: (Member) -> Unit
) {
    val accents = listOf(Brand.Blue, Brand.Mint, Brand.Coral, Brand.Amber)
    val rows = members
        .map { member -> member to watched[member.uid].orEmpty().count { it.showId == showId } }
        .sortedWith(compareByDescending<Pair<Member, Int>> { it.second }.thenBy { it.first.name })

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill("PROGRESS", Brand.Blue)
                Spacer(Modifier.weight(1f))
                Text("$total EPISODES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Brand.Muted)
            }
            Spacer(Modifier.height(14.dp))
            if (rows.isEmpty()) {
                Text("No one here yet.", color = Brand.Muted)
            }
            rows.forEachIndexed { index, row ->
                val member = row.first
                val count = row.second
                val fraction = if (total == 0) 0f else count.toFloat() / total
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onMember(member) }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${index + 1}",
                            modifier = Modifier.width(18.dp),
                            color = Brand.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Avatar(member.name.take(1).uppercase(), accents[index % accents.size], size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (member.uid == currentUid) "${member.name} (you)" else member.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = Brand.Ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                Text("$count/$total", color = Brand.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(6.dp)).background(Brand.PaperDim)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                                        .background(Brush.horizontalGradient(listOf(accents[index % accents.size], Brand.Mint)))
                                )
                            }
                        }
                    }
                }
                if (index < rows.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupProgressScreen(vm: MainViewModel, group: Group, show: ShowEntry, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val members by remember(group.id) { vm.repo.membersFlow(group.id) }.collectAsState(emptyList())
    val memberIds = members.map { it.uid }
    val memberWatched by remember(group.id, memberIds) {
        vm.repo.memberWatchedFlow(group.id, memberIds)
    }.collectAsState(emptyMap())
    val counts = members.map { memberWatched[it.uid].orEmpty().count { episode -> episode.showId == show.id } }
    val total = TvmazeApi.cachedEpisodes(show.id.toInt())?.size
        ?: memberWatched.values.flatten().filter { it.showId == show.id }.distinctBy { "${it.season}-${it.number}" }.size
    val average = if (counts.isEmpty() || total == 0) 0f else counts.average().toFloat() / total

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text("PROGRESS", style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShowThumb(show.imageUrl, Brand.Blue)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(show.name.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Brand.Ink)
                                Spacer(Modifier.height(6.dp))
                                Text("${members.size} ${if (members.size == 1) "MEMBER" else "MEMBERS"} · ${(average * 100).toInt()}% AVERAGE", color = Brand.Muted)
                            }
                        }
                    }
                }
                item {
                    GroupProgressList(
                        members = members,
                        watched = memberWatched,
                        showId = show.id,
                        total = total,
                        currentUid = uid,
                        onMember = { go(Screen.MemberDetail(group, it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(vm: MainViewModel, group: Group, member: Member, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val watched by remember(group.id, member.uid) { vm.repo.watchedFlow(group.id, member.uid) }.collectAsState(emptyList())
    val shows by remember(group.id) { vm.repo.showsFlow(group.id) }.collectAsState(emptyList())
    val notes by remember(group.id, member.uid) { vm.repo.memberPublicNotesFlow(group.id, member.uid) }.collectAsState(emptyList())
    val accents = listOf(Brand.Blue, Brand.Mint, Brand.Coral, Brand.Amber)
    val watchedShows = watched.groupBy { it.showId }.toSortedMap(
        compareBy { showId -> shows.firstOrNull { it.id == showId }?.name ?: showId }
    )

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text(member.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(member.name.take(1).uppercase(), accents[0], size = 46.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (member.uid == uid) "${member.name} (you)" else member.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Brand.Ink
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${watched.size} ${if (watched.size == 1) "episode" else "episodes"} across ${watchedShows.size} ${if (watchedShows.size == 1) "show" else "shows"}",
                                    color = Brand.Muted
                                )
                            }
                        }
                    }
                }
                item { Eyebrow("PROGRESS", Brand.Blue) }
                if (watchedShows.isEmpty()) {
                    item { Text("Nothing watched yet.", color = Brand.Muted) }
                }
                watchedShows.forEach { (showId, episodesWatched) ->
                    item {
                        val show = shows.firstOrNull { it.id == showId }
                        val total = show?.let { TvmazeApi.cachedEpisodes(it.id.toInt())?.size } ?: episodesWatched.size
                        PunchCard(
                            onClick = { show?.let { go(Screen.ShowDetail(group, it)) } },
                            accent = accents[shows.indexOfFirst { it.id == showId }.coerceAtLeast(0) % accents.size]
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(showName(shows, showId), color = Brand.Ink, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("${episodesWatched.size} of $total watched", color = Brand.Muted, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                episodesWatched.groupBy { it.season }.toSortedMap().forEach { (season, seasonEpisodes) ->
                                    Spacer(Modifier.height(6.dp))
                                    Text("SEASON $season", color = Brand.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        seasonEpisodes.joinToString("  ·  ") { epLabel(it.season, it.number) },
                                        color = Brand.Ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                item { Eyebrow("PUBLIC NOTES", Brand.Amber) }
                if (notes.isEmpty()) {
                    item { Text("No public notes from ${member.name} yet.", color = Brand.Muted) }
                }
                itemsIndexed(notes) { index, note ->
                    AnimatedEnter(index * 50) {
                        PunchCard(
                            onClick = {
                                shows.firstOrNull { it.id == note.showId }?.let { go(Screen.ShowDetail(group, it)) }
                            },
                            accent = Brand.Mint
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(note.text, style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "${noteContext(note, shows)} · ${timeText(note.createdAt)}",
                                    fontSize = 11.sp,
                                    color = Brand.Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowThumb(url: String, accent: Color) {
    Box(
        Modifier
            .size(width = 52.dp, height = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.25f), Brand.Amber.copy(alpha = 0.2f))))
            .border(2.dp, Brand.Stroke, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brand.ImageScrim))
        } else {
            Text("WT", color = Brand.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun addedByText(show: ShowEntry, members: List<Member>) {
    Text(
        "Added by ${members.firstOrNull { it.uid == show.addedBy }?.name ?: "someone"}",
        fontSize = 12.sp,
        color = Brand.Muted
    )
}

private fun noteScopeLabel(note: Note) = when (note.scope) {
    "season" -> "Season ${note.season}"
    "episode" -> epLabel(note.season, note.episode)
    else -> "Show note"
}

private fun showName(shows: List<ShowEntry>, showId: String) =
    shows.firstOrNull { it.id == showId }?.name ?: if (showId.isBlank()) "Circle note" else "Removed show"

private fun noteContext(note: Note, shows: List<ShowEntry>) =
    "${showName(shows, note.showId)} · ${noteScopeLabel(note)}"

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
fun ShowDetailScreen(vm: MainViewModel, group: Group, show: ShowEntry, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val watched by remember(group.id, uid) { vm.repo.watchedFlow(group.id, uid) }.collectAsState(emptyList())
    val cachedEpisodes = remember(show.id) { TvmazeApi.cachedEpisodes(show.id.toInt()) }
    var episodes by remember(show.id) { mutableStateOf(cachedEpisodes ?: emptyList()) }
    var loading by remember(show.id) { mutableStateOf(cachedEpisodes == null) }
    var rangeTarget by remember { mutableStateOf<MarkTarget?>(null) }
    var noteTarget by remember { mutableStateOf<Pair<String, Pair<Int, Int>>?>(null) }
    var detailsTarget by remember { mutableStateOf<DetailsTarget?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmRewatch by remember { mutableStateOf(false) }
    var selectedSeason by remember(show.id) { mutableStateOf<Int?>(null) }
    var seasonMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(show.id) {
        try {
            episodes = TvmazeApi.episodes(show.id.toInt()).sortedWith(compareBy({ it.season }, { it.number }))
        } catch (e: Exception) {
            if (!e.isCancellation()) vm.message = "Could not load episodes: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    val watchedIds = watched.map { it.id }.toSet()
    val seasons = episodes.groupBy { it.season }.toSortedMap()
    val seasonOptions = seasons.keys.toList()
    val selectedEpisodes = selectedSeason?.let { seasons[it].orEmpty() }.orEmpty()

    LaunchedEffect(seasonOptions) {
        if (seasonOptions.isNotEmpty() && selectedSeason !in seasonOptions) selectedSeason = seasonOptions.first()
    }

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text(show.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                actions = {
                    PunchIconButton(
                        onClick = {
                            detailsTarget = DetailsTarget(
                                title = show.name,
                                subtitle = "SHOW DETAILS",
                                imageUrl = show.imageUrl,
                                summary = show.summary,
                                meta = "TVMaze",
                                showId = show.id.toIntOrNull()
                            )
                        },
                        color = Brand.Surface
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Show details", tint = Brand.Ink)
                    }
                    if (show.addedBy == uid || group.createdBy == uid) {
                        PunchIconButton(onClick = { confirmRemove = true }, color = Brand.Coral, contentColor = Brand.OnAccent) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove show", tint = Brand.OnAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            if (loading) {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShimmerBar(76.dp, 108.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                ShimmerBar(180.dp, 22.dp)
                                Spacer(Modifier.height(10.dp))
                                ShimmerBar(120.dp, 12.dp)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    items(6) { ShowSkeletonRow() }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val done = episodes.count { watchedIds.contains(it.id.toString()) }
                        val progress = if (episodes.isEmpty()) 0f else done.toFloat() / episodes.size
                        val complete = episodes.isNotEmpty() && done == episodes.size
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ShowThumb(show.imageUrl, Brand.Blue)
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            show.name.uppercase(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Brand.Ink
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text("$done of ${episodes.size} watched", color = Brand.Muted)
                                        if (complete) {
                                            Spacer(Modifier.height(8.dp))
                                            StatusPill("COMPLETE", Brand.Mint)
                                        }
                                    }
                                    ProgressRing(progress)
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PunchButton(
                                        text = "Add a note",
                                        onClick = { noteTarget = "show" to (0 to 0) },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        color = Brand.InkSoft,
                                        contentColor = Brand.Ink,
                                        borderColor = Brand.Border,
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = Brand.Ink) }
                                    )
                                    PunchButton(
                                        text = "Progress",
                                        onClick = { go(Screen.GroupProgress(group, show)) },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        color = Brand.Blue,
                                        contentColor = Brand.OnPrimary,
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.OnPrimary) }
                                    )
                                }
                                if (complete) {
                                    Spacer(Modifier.height(10.dp))
                                    PunchButton(
                                        text = "Watch again",
                                        onClick = { confirmRewatch = true },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        color = Brand.Amber,
                                        contentColor = Brand.OnAccent
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.weight(1f)) {
                                    PunchButton(
                                        text = "Choose season",
                                        onClick = { seasonMenuOpen = true },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        color = Brand.InkSoft,
                                        contentColor = Brand.Ink,
                                        borderColor = Brand.Border,
                                        leadingIcon = {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Brand.Ink)
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = seasonMenuOpen,
                                        onDismissRequest = { seasonMenuOpen = false },
                                        modifier = Modifier.background(Brand.Surface)
                                    ) {
                                        seasonOptions.forEach { season ->
                                            DropdownMenuItem(
                                                text = { Text("Season $season", color = Brand.Ink) },
                                                trailingIcon = {
                                                    if (season == selectedSeason) Text("✓", color = Brand.Mint, fontWeight = FontWeight.Black)
                                                },
                                                onClick = {
                                                    selectedSeason = season
                                                    seasonMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                                selectedSeason?.let { season ->
                                    Spacer(Modifier.width(10.dp))
                                    PunchIconButton(
                                        onClick = { noteTarget = "season" to (season to 0) },
                                        color = Brand.Amber
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Season note", tint = Brand.OnAccent)
                                    }
                                }
                            }
                        }
                    }
                    items(selectedEpisodes) { ep ->
                        val isWatched = watchedIds.contains(ep.id.toString())
                        PunchCard(
                            onClick = {
                                if (isWatched) {
                                    vm.launchApp { vm.repo.unmarkEpisode(group.id, uid, ep.id) }
                                } else {
                                    val index = episodes.indexOfFirst { it.id == ep.id }
                                    val earlierUnwatched = if (index > 0) {
                                        episodes.take(index).filterNot { watchedIds.contains(it.id.toString()) }
                                    } else {
                                        emptyList()
                                    }
                                    if (earlierUnwatched.isNotEmpty()) {
                                        rangeTarget = MarkTarget(ep, earlierUnwatched)
                                    } else {
                                        vm.launchApp {
                                            vm.repo.markEpisodes(group.id, uid, vm.myName, show, listOf(ep), "marked ${epLabel(ep.season, ep.number)}")
                                        }
                                    }
                                }
                            },
                            accent = if (isWatched) Brand.Mint else Brand.Border
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape)
                                        .background(if (isWatched) Brand.Mint else Brand.Surface)
                                        .border(2.dp, if (isWatched) Brand.Stroke else Brand.Border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isWatched) Icon(Icons.Default.Check, contentDescription = null, tint = Brand.OnAccent, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${epLabel(ep.season, ep.number)}  ${ep.name}",
                                        fontSize = 14.sp,
                                        color = Brand.Ink,
                                        fontWeight = if (isWatched) FontWeight.Normal else FontWeight.Bold
                                    )
                                    if (isWatched) Text("WATCHED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Brand.Mint)
                                }
                                 PunchIconButton(
                                     onClick = {
                                         detailsTarget = DetailsTarget(
                                             title = "${epLabel(ep.season, ep.number)}  ${ep.name}",
                                             subtitle = "EPISODE DETAILS",
                                             imageUrl = ep.imageOriginalUrl.ifBlank { ep.imageUrl },
                                             summary = ep.summary,
                                             meta = if (ep.airdate.isNotBlank()) "Aired ${ep.airdate}" else "TVMaze"
                                         )
                                     },
                                     color = Brand.Surface,
                                     modifier = Modifier.size(42.dp)
                                 ) {
                                     Icon(Icons.Default.Visibility, contentDescription = "Episode details", tint = Brand.Ink, modifier = Modifier.size(18.dp))
                                 }
                                 Spacer(Modifier.width(6.dp))
                                 PunchIconButton(
                                     onClick = { noteTarget = "episode" to (ep.season to ep.number) },
                                     color = Brand.Surface,
                                     modifier = Modifier.size(42.dp)
                                 ) {
                                     Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Episode note", tint = Brand.Ink, modifier = Modifier.size(18.dp))
                                 }
                            }
                        }
                    }
                }
            }
        }
    }

    rangeTarget?.let { target ->
        val range = target.earlierUnwatched + target.episode
        val labelStart = epLabel(target.earlierUnwatched.first().season, target.earlierUnwatched.first().number)
        val labelEnd = epLabel(target.episode.season, target.episode.number)
        val summary = "marked $labelStart–$labelEnd"

        AlertDialog(
            onDismissRequest = { rangeTarget = null },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Mark earlier episodes?", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "There ${if (target.earlierUnwatched.size == 1) "is 1 earlier episode" else "are ${target.earlierUnwatched.size} earlier episodes"} " +
                        "not watched yet. Mark all ${range.size} up to $labelEnd, or only this one?",
                    color = Brand.Muted
                )
            },
            confirmButton = {
                PunchButton(
                    text = "Mark all",
                    modifier = Modifier.width(104.dp),
                    color = Brand.Mint,
                    contentColor = Brand.OnAccent,
                    onClick = {
                        vm.launchApp { vm.repo.markEpisodes(group.id, uid, vm.myName, show, range, summary) }
                        rangeTarget = null
                    }
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PunchButton(
                        text = "Only this",
                        modifier = Modifier.width(96.dp),
                        color = Brand.Blue,
                        contentColor = Brand.OnPrimary,
                        onClick = {
                            vm.launchApp { vm.repo.markEpisodes(group.id, uid, vm.myName, show, listOf(target.episode), "marked $labelEnd") }
                            rangeTarget = null
                        }
                    )
                    PunchButton(
                        text = "Cancel",
                        modifier = Modifier.width(96.dp),
                        color = Brand.InkSoft,
                        contentColor = Brand.Ink,
                        borderColor = Brand.Border,
                        onClick = { rangeTarget = null }
                    )
                }
            }
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
            vm.launchApp {
                vm.repo.addNote(
                    group.id, uid, vm.myName, show, scopeName,
                    numbers.first, numbers.second, text, isPublic
                )
                noteTarget = null
            }
        }
    }

    if (confirmRewatch) {
        AlertDialog(
            onDismissRequest = { confirmRewatch = false },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Watch again?", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = { Text("This clears your watched markers for this show so you can track a rewatch. Group activity and notes stay.", color = Brand.Muted) },
            confirmButton = {
                PunchButton(
                    text = "Reset",
                    modifier = Modifier.width(120.dp),
                    color = Brand.Amber,
                    contentColor = Brand.OnAccent,
                    onClick = {
                        confirmRewatch = false
                        vm.launchApp {
                            runCatching {
                                vm.repo.resetShowProgress(group.id, uid, episodes.map { it.id.toString() })
                            }.onFailure { if (!it.isCancellation()) vm.message = "Could not reset progress: ${it.localizedMessage}" }
                        }
                    }
                )
            },
            dismissButton = {
                PunchButton(
                    text = "Cancel",
                    modifier = Modifier.width(120.dp),
                    color = Brand.InkSoft,
                    contentColor = Brand.Ink,
                    borderColor = Brand.Border,
                    onClick = { confirmRewatch = false }
                )
            }
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Remove show?", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${show.name}\" from this circle? Its notes and activity stay in the history.", color = Brand.Muted) },
            confirmButton = {
                PunchButton(
                    text = "Remove",
                    modifier = Modifier.width(120.dp),
                    color = Brand.Coral,
                    contentColor = Brand.OnAccent,
                    onClick = {
                        confirmRemove = false
                        vm.launchApp {
                            runCatching { vm.repo.removeShow(group.id, show.id) }
                                .onSuccess { go(Screen.GroupDetail(group)) }
                                .onFailure { if (!it.isCancellation()) vm.message = "Could not remove show: ${it.localizedMessage}" }
                        }
                    }
                )
            },
            dismissButton = {
                PunchButton(
                    text = "Cancel",
                    modifier = Modifier.width(120.dp),
                    color = Brand.InkSoft,
                    contentColor = Brand.Ink,
                    borderColor = Brand.Border,
                    onClick = { confirmRemove = false }
                )
            }
        )
    }

    detailsTarget?.let { DetailsDialog(it) { detailsTarget = null } }
}

@Composable
fun NoteDialog(label: String, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Surface,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Note about $label", color = Brand.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = wtFieldColors()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPublic) "Visible to the group" else "Only visible to me", color = Brand.Ink)
                }
            }
        },
        confirmButton = {
            PunchButton(
                text = "Save",
                enabled = text.isNotBlank(),
                modifier = Modifier.width(110.dp),
                color = Brand.Blue,
                onClick = { if (text.isNotBlank()) onSave(text.trim(), isPublic) }
            )
        },
        dismissButton = {
            PunchButton("Cancel", onDismiss, Modifier.width(110.dp), color = Brand.InkSoft, contentColor = Brand.Ink, borderColor = Brand.Border)
        }
    )
}

@Composable
private fun DetailsDialog(target: DetailsTarget, onDismiss: () -> Unit) {
    var summary by remember(target) { mutableStateOf(target.summary) }
    var imageUrl by remember(target) { mutableStateOf(target.imageUrl) }

    LaunchedEffect(target) {
        val showId = target.showId ?: return@LaunchedEffect
        if (summary.isNotBlank() && imageUrl.isNotBlank()) return@LaunchedEffect
        runCatching { TvmazeApi.cachedShow(showId) ?: TvmazeApi.show(showId) }
            .onSuccess { fetched ->
                if (summary.isBlank()) summary = fetched.summary.orEmpty()
                if (imageUrl.isBlank()) imageUrl = fetched.image?.medium ?: fetched.image?.original.orEmpty()
            }
    }

    val body = plainText(summary)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Surface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Column {
                Text(target.subtitle, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Brand.Blue, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(target.title, color = Brand.Ink, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = target.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(Brand.PaperDim)
                    )
                    Spacer(Modifier.height(14.dp))
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(18.dp)).background(Brand.PaperDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NO IMAGE FROM TVMAZE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Brand.Muted, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                }
                if (target.meta.isNotBlank()) {
                    Text(target.meta, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand.Muted)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    if (body.isNotBlank()) body else "TVMaze does not provide a description for this.",
                    color = Brand.Ink,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            PunchButton("Close", onDismiss, Modifier.width(110.dp), color = Brand.Blue, contentColor = Brand.OnPrimary)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowScreen(vm: MainViewModel, group: Group, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TvShow>>(TvmazeApi.cachedSearch("") ?: emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            searching = false
            hasSearched = false
            return@LaunchedEffect
        }
        TvmazeApi.cachedSearch(q)?.let {
            results = it
            searching = false
            hasSearched = true
            return@LaunchedEffect
        }
        results = emptyList()
        searching = true
        delay(220)
        try {
            results = TvmazeApi.search(q)
            hasSearched = true
        } catch (e: Exception) {
            if (!e.isCancellation()) vm.message = "Search failed: ${e.localizedMessage}"
        } finally {
            searching = false
        }
    }

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text("ADD A SHOW", style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
                ScreenHeader(
                    step = "03 / ADD TO CIRCLE",
                    title = "Find a show.",
                    subtitle = "Search once, add it, and the whole group sees it.",
                    accent = Brand.Mint
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search TVMaze") },
                    placeholder = { Text("Try a show title") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Brand.Blue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = wtFieldColors()
                )
                Spacer(Modifier.height(14.dp))
                when {
                    searching -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(5) { ShowSkeletonRow() }
                    }
                    results.isEmpty() -> {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) {
                                StatusPill(if (!hasSearched) "READY WHEN YOU ARE" else "NO MATCHES", Brand.Amber)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (!hasSearched) "Type at least two letters." else "Try a shorter or more exact title.",
                                    color = Brand.Ink
                                )
                            }
                        }
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(results) { index, show ->
                            AnimatedEnter(index * 40) {
                                PunchCard(
                                    onClick = {
                                        vm.launchApp {
                                            vm.repo.addShow(group.id, show, vm.uid!!)
                                            onBack()
                                        }
                                    },
                                    accent = listOf(Brand.Blue, Brand.Mint, Brand.Coral, Brand.Amber)[index % 4]
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ShowThumb(show.image?.medium.orEmpty(), Brand.Mint)
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(show.name, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                            Spacer(Modifier.height(3.dp))
                                            Text("Tap to add to ${group.name}", fontSize = 11.sp, color = Brand.Muted)
                                        }
                                        PunchIconButton(
                                            onClick = {
                                                vm.launchApp {
                                                    vm.repo.addShow(group.id, show, vm.uid!!)
                                                    onBack()
                                                }
                                            },
                                            color = Brand.Mint
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Brand.OnAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PunchButton(
                    text = "Back to circle",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    color = Brand.Surface,
                    contentColor = Brand.Ink
                )
            }
        }
    }
}
