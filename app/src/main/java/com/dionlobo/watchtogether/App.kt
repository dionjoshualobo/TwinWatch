package com.dionlobo.watchtogether

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object Groups : Screen()
    data object Join : Screen()
    data object Settings : Screen()
    data class GroupDetail(val group: Group) : Screen()
    data class MemberDetail(val group: Group, val member: Member) : Screen()
    data class ShowDetail(val group: Group, val show: ShowEntry) : Screen()
    data class GroupProgress(val group: Group, val show: ShowEntry) : Screen()
    data class AddShow(val group: Group) : Screen()
}

internal fun extractInviteCode(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
    val raw = when {
        uri?.scheme == "watchtogether" && uri.host == "join" ->
            uri.getQueryParameter("code") ?: uri.lastPathSegment
        else -> trimmed
    } ?: trimmed
    return raw.filter { it.isLetterOrDigit() }.uppercase().take(6)
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        setContent {
            AppTheme(darkTheme = vm.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(vm, intent.getStringExtra("groupId"))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "watchtogether" && uri.host == "join") {
            val code = extractInviteCode(uri.toString())
            if (code.length >= 4) vm.submitJoinCode(code)
        }
    }
}

@Composable
fun AppRoot(vm: MainViewModel, startGroupId: String?) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val current = stack.last()

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.size - 1) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(vm.uid) {
        if (vm.uid != null && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun go(screen: Screen) {
        stack.add(screen)
    }

    fun replace(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    when (current) {
        Screen.Splash -> SplashScreen(vm, startGroupId, ::replace, ::go)
        Screen.Onboarding -> OnboardingScreen(vm) { replace(Screen.Groups) }
        Screen.Groups -> GroupsScreen(vm, ::go)
        Screen.Join -> JoinScreen(vm, ::replace, { stack.removeAt(stack.size - 1) })
        Screen.Settings -> SettingsScreen(vm) { stack.removeAt(stack.size - 1) }
        is Screen.GroupDetail -> GroupDetailScreen(vm, current.group, ::go)
        is Screen.MemberDetail -> MemberDetailScreen(vm, current.group, current.member, ::go)
        is Screen.ShowDetail -> ShowDetailScreen(vm, current.group, current.show, ::go)
        is Screen.GroupProgress -> GroupProgressScreen(vm, current.group, current.show, ::go)
        is Screen.AddShow -> AddShowScreen(vm, current.group) { stack.removeAt(stack.size - 1) }
    }

    vm.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearMessage() },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Something went wrong", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Brand.Muted) },
            confirmButton = {
                PunchButton("OK", { vm.clearMessage() }, modifier = Modifier.width(110.dp), color = Brand.Coral)
            }
        )
    }
}

@Composable
fun SplashScreen(
    vm: MainViewModel,
    startGroupId: String?,
    replace: (Screen) -> Unit,
    go: (Screen) -> Unit
) {
    val uid = vm.uid
    LaunchedEffect(vm.ready, uid, vm.myName, vm.pendingJoinCode, startGroupId) {
        if (!vm.ready || uid == null) return@LaunchedEffect
        when {
            vm.myName.isBlank() -> replace(Screen.Onboarding)
            vm.pendingJoinCode != null -> replace(Screen.Join)
            startGroupId != null -> {
                try {
                    val group = vm.repo.group(startGroupId)
                    if (group != null) replace(Screen.GroupDetail(group)) else replace(Screen.Groups)
                } catch (e: Exception) {
                    if (!e.isCancellation()) vm.message = e.localizedMessage ?: "Could not open circle"
                }
            }
            else -> replace(Screen.Groups)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.splash_waiting_room),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color(0x990A0508)))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedEnter {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(74.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF5A1019))
                            .border(2.dp, Color(0xFFD9B46E), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("TWIN", style = MaterialTheme.typography.displayLarge, color = Color(0xFFF4E2C5))
                    Text("WATCH", style = MaterialTheme.typography.displayLarge, color = Color(0xFFFFB454))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Your group's episode ledger.",
                        color = Color(0xFFE8DFCD),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = Color(0xFFFFB454), strokeWidth = 4.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(vm: MainViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DecorBackground {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedEnter {
                ScreenHeader(
                    step = "00 / TAKE YOUR SEAT",
                    title = "Let's watch together.",
                    subtitle = "No account. No email. Just a name your people will recognize.",
                    accent = Brand.Coral
                )
            }
            Spacer(Modifier.height(22.dp))
            AnimatedEnter(80) {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("01  YOUR NAME", color = Brand.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("What should we call you?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = wtFieldColors()
                        )
                        Spacer(Modifier.height(18.dp))
                        Text("02  WHAT HAPPENS NEXT", color = Brand.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepDot("1", Brand.Blue)
                            Spacer(Modifier.width(10.dp))
                            Text("Start a circle or paste a friend's code.", color = Brand.Ink)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepDot("2", Brand.Mint)
                            Spacer(Modifier.width(10.dp))
                            Text("Add shows and mark episodes as you watch.", color = Brand.Ink)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PunchButton(
                text = "Enter the room",
                enabled = name.isNotBlank() && !vm.busy,
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.saveName(name, onDone)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepDot(number: String, color: Color) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(color).border(2.dp, Brand.Stroke, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = Brand.OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun wtFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Brand.Surface,
    unfocusedContainerColor = Brand.Surface,
    disabledContainerColor = Brand.PaperDim,
    focusedTextColor = Brand.Ink,
    unfocusedTextColor = Brand.Ink,
    disabledTextColor = Brand.Muted,
    focusedIndicatorColor = Brand.Blue,
    unfocusedIndicatorColor = Brand.Border,
    cursorColor = Brand.Blue,
    focusedLabelColor = Brand.Blue,
    unfocusedLabelColor = Brand.Muted,
    disabledLabelColor = Brand.Muted
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: MainViewModel, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val groups by remember(uid) { vm.repo.groupsFlow(uid) }.collectAsState(emptyList())
    var createDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    val accents = listOf(Brand.Blue, Brand.Mint, Brand.Coral, Brand.Amber)

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Brand.Blue).border(1.dp, Brand.Stroke, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text("TWINWATCH", style = MaterialTheme.typography.labelLarge, color = Brand.Ink)
                    }
                },
                actions = {
                    IconButton(onClick = { go(Screen.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Brand.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.Paper,
                    titleContentColor = Brand.Ink
                )
            )
        }
    ) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
                ScreenHeader(
                    step = "01 / YOUR CIRCLES",
                    title = "Your circles.",
                    subtitle = "Pick up where the group left off."
                )
                Spacer(Modifier.height(18.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groups.isEmpty()) {
                        item {
                            AnimatedEnter {
                                SoftCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(22.dp)) {
                                        StatusPill("EMPTY ROOM", Brand.Amber)
                                        Spacer(Modifier.height(14.dp))
                                        Text("Nothing on the board yet.", style = MaterialTheme.typography.headlineMedium, color = Brand.Ink)
                                        Spacer(Modifier.height(6.dp))
                                        Text("Start a circle, then share the code with your people.", color = Brand.Muted)
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed(groups) { index, group ->
                        AnimatedEnter(index * 60) {
                            PunchCard(
                                onClick = { go(Screen.GroupDetail(group)) },
                                accent = accents[index % accents.size]
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                                            .background(accents[index % accents.size])
                                            .border(2.dp, Brand.Stroke, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            (index + 1).toString().padStart(2, '0'),
                                            color = Brand.OnAccent,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(group.name, style = MaterialTheme.typography.titleLarge, color = Brand.Ink)
                                        Spacer(Modifier.height(7.dp))
                                        Text(
                                            "CODE ${group.inviteCode}  ·  ${group.memberUids.size} MEMBERS",
                                            color = Brand.Blue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PunchButton(
                    text = "Create a new circle",
                    onClick = { createDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))
                PunchButton(
                    text = "Join with a code",
                    onClick = { go(Screen.Join) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Brand.Surface,
                    contentColor = Brand.Ink,
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
                )
            }
        }
    }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("New circle", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Circle name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = wtFieldColors()
                )
            },
            confirmButton = {
                PunchButton(
                    text = "Create",
                    enabled = groupName.isNotBlank() && !vm.busy,
                    modifier = Modifier.width(116.dp),
                    onClick = {
                        vm.createGroup(groupName.trim()) { group ->
                            go(Screen.GroupDetail(group))
                        }
                        groupName = ""
                        createDialog = false
                    }
                )
            },
            dismissButton = {
                PunchButton("Cancel", { createDialog = false }, Modifier.width(116.dp), color = Brand.InkSoft, contentColor = Brand.Ink, borderColor = Brand.Border)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    var name by remember(vm.myName) { mutableStateOf(vm.myName) }
    val circles by remember(vm.uid) { vm.repo.groupsFlow(vm.uid!!) }.collectAsState(emptyList())
    var editingCircle by remember { mutableStateOf<Group?>(null) }

    fun nextGroupLabel(): String {
        val used = vm.circleGroups.values.mapNotNull { it.removePrefix("Group ").toIntOrNull() }
        return "Group ${(used.maxOrNull() ?: 0) + 1}"
    }

    fun saveCircleLabel(circle: Group, label: String?) {
        val next = vm.circleGroups.toMutableMap()
        if (label == null) next.remove(circle.id) else next[circle.id] = label
        vm.saveCircleGroups(next.toMap()) { editingCircle = null }
    }

    fun pairCircles(circle: Group, other: Group) {
        val label = vm.circleGroups[other.id] ?: vm.circleGroups[circle.id] ?: nextGroupLabel()
        val next = vm.circleGroups.toMutableMap()
        next[circle.id] = label
        next[other.id] = label
        vm.saveCircleGroups(next.toMap()) { editingCircle = null }
    }

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Brand.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            Column(
                modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                ScreenHeader(
                    step = "04 / PREFERENCES",
                    title = "Make it yours.",
                    subtitle = "Change your name, group circles, or dim the room.",
                    accent = Brand.Mint
                )
                Spacer(Modifier.height(18.dp))
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("YOUR NAME", color = Brand.Mint, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("What should we call you?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = wtFieldColors()
                        )
                        Spacer(Modifier.height(14.dp))
                        PunchButton(
                            text = "Save name",
                            onClick = { vm.saveName(name.trim()) { } },
                            enabled = name.isNotBlank() && name.trim() != vm.myName && !vm.busy,
                            modifier = Modifier.fillMaxWidth(),
                            color = Brand.Mint,
                            contentColor = Brand.OnAccent
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("CIRCLE SYNC GROUPS", color = Brand.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Circles in the same group sync watched episodes straight away. Pick another circle, or leave this one ungrouped to be asked each time.",
                            color = Brand.Muted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        if (circles.isEmpty()) {
                            Text("Join a circle to set up syncing.", color = Brand.Muted)
                        } else {
                            circles.forEach { circle ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(circle.name, color = Brand.Ink, fontWeight = FontWeight.Bold)
                                        Text(vm.circleGroups[circle.id] ?: "Ungrouped", color = Brand.Muted, fontSize = 11.sp)
                                    }
                                    PunchButton(
                                        text = "Choose",
                                        onClick = { editingCircle = circle },
                                        modifier = Modifier.width(104.dp),
                                        color = Brand.Surface,
                                        contentColor = Brand.Ink,
                                        borderColor = Brand.Border
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("DARK MODE", color = Brand.Blue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Easier on the eyes when the lights are low.", color = Brand.Muted, fontSize = 13.sp)
                        }
                        Switch(
                            checked = vm.darkTheme,
                            onCheckedChange = { vm.setDarkMode(it) }
                        )
                    }
                }
            }
        }
    }

    editingCircle?.let { circle ->
        AlertDialog(
            onDismissRequest = { editingCircle = null },
            containerColor = Brand.Surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Sync ${circle.name}", color = Brand.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        "Choose a circle to sync with. If it is already grouped, this circle joins that whole group.",
                        color = Brand.Muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    PunchButton(
                        text = "Ungrouped",
                        onClick = { saveCircleLabel(circle, null) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Brand.InkSoft,
                        contentColor = Brand.Ink,
                        borderColor = Brand.Border
                    )
                    Spacer(Modifier.height(8.dp))
                    circles.filter { it.id != circle.id }.forEach { other ->
                        Row(
                            Modifier.fillMaxWidth().clickable { pairCircles(circle, other) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(other.name, color = Brand.Ink, fontWeight = FontWeight.Bold)
                                Text(vm.circleGroups[other.id] ?: "Ungrouped", color = Brand.Muted, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.Ink)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PunchButton(
                        text = "New group",
                        onClick = { saveCircleLabel(circle, nextGroupLabel()) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Brand.Blue,
                        contentColor = Brand.OnPrimary
                    )
                }
            },
            confirmButton = {
                PunchButton("Close", { editingCircle = null }, Modifier.width(110.dp), color = Brand.InkSoft, contentColor = Brand.Ink, borderColor = Brand.Border)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScreen(vm: MainViewModel, replace: (Screen) -> Unit, back: () -> Unit) {
    var code by remember { mutableStateOf(vm.pendingJoinCode ?: "") }

    LaunchedEffect(vm.pendingJoinCode) {
        vm.pendingJoinCode?.let { pending ->
            code = pending
            vm.joinGroup(pending) { group ->
                if (group != null) replace(Screen.GroupDetail(group))
            }
        }
    }

    Scaffold(
        containerColor = Brand.Paper,
        topBar = {
            TopAppBar(
                title = { Text("JOIN A CIRCLE", style = MaterialTheme.typography.labelLarge, color = Brand.Ink) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Paper)
            )
        }
    ) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
                ScreenHeader(
                    step = "02 / JOIN CIRCLE",
                    title = "Got a code?",
                    subtitle = "Six characters is all you need. Paste the full invite link and we'll read the code for you.",
                    accent = Brand.Mint
                )
                Spacer(Modifier.height(28.dp))
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("GROUP CODE", color = Brand.Mint, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = extractInviteCode(it) },
                            label = { Text("Code or invite link") },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                color = Brand.Ink
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = wtFieldColors()
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                PunchButton(
                    text = "Join the circle",
                    enabled = code.length >= 4 && !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.joinGroup(code) { group ->
                            if (group != null) replace(Screen.GroupDetail(group))
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                PunchButton(
                    text = "Back",
                    onClick = back,
                    modifier = Modifier.fillMaxWidth(),
                    color = Brand.Surface,
                    contentColor = Brand.Ink
                )
            }
        }
    }
}
