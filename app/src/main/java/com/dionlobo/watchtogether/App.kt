package com.dionlobo.watchtogether

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object Groups : Screen()
    data object Join : Screen()
    data class GroupDetail(val group: Group) : Screen()
    data class ShowDetail(val group: Group, val show: ShowEntry) : Screen()
    data class AddShow(val group: Group) : Screen()
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        setContent {
            AppTheme {
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
        val uri = intent?.data
        if (uri?.scheme == "watchtogether" && uri.host == "join") {
            uri.lastPathSegment?.let { vm.submitJoinCode(it) }
        }
    }
}

@Composable
fun AppRoot(vm: MainViewModel, startGroupId: String?) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val current = stack.last()

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.size - 1) }

    val context = LocalContext.current
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
        is Screen.GroupDetail -> GroupDetailScreen(vm, current.group, ::go)
        is Screen.ShowDetail -> ShowDetailScreen(vm, current.group, current.show)
        is Screen.AddShow -> AddShowScreen(vm, current.group) { stack.removeAt(stack.size - 1) }
    }

    vm.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearMessage() },
            title = { Text("Something went wrong") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.clearMessage() }) { Text("OK") } }
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
    LaunchedEffect(uid, vm.myName, vm.pendingJoinCode, startGroupId) {
        if (uid == null) return@LaunchedEffect
        when {
            vm.myName.isBlank() -> replace(Screen.Onboarding)
            vm.pendingJoinCode != null -> replace(Screen.Join)
            startGroupId != null -> {
                val group = vm.repo.group(startGroupId)
                if (group != null) replace(Screen.GroupDetail(group)) else replace(Screen.Groups)
            }
            else -> replace(Screen.Groups)
        }
    }
    DecorBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("WATCH", style = MaterialTheme.typography.displayLarge, color = Brand.Paper)
            Text("TOGETHER", style = MaterialTheme.typography.displayLarge, color = Brand.Amber)
            AccentDivider()
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Brand.Mint)
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
            modifier = Modifier.padding(28.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedEnter {
                Column {
                    Text("HELLO.", style = MaterialTheme.typography.displayLarge, color = Brand.Amber)
                    AccentDivider()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "What should your group call you?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Brand.Paper
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No account, no email. Just a name your people will see when you mark an episode.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(34.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    vm.saveName(name, onDone)
                },
                enabled = name.isNotBlank() && !vm.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Let's go", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: MainViewModel, go: (Screen) -> Unit) {
    val uid = vm.uid!!
    val groups by remember(uid) { vm.repo.groupsFlow(uid) }.collectAsState(emptyList())
    var createDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your groups") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { go(Screen.Join) }) {
                androidx.compose.material3.Icon(Icons.Default.GroupAdd, contentDescription = "Join group")
            }
        }
    ) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
                Text("YOUR CIRCLES", style = MaterialTheme.typography.displayLarge, color = Brand.Paper)
                AccentDivider()
                Spacer(Modifier.height(20.dp))
                if (groups.isEmpty()) {
                    AnimatedEnter {
                        Column {
                            Text("Nothing here yet.", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Start a group, then share the code below with your people.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(groups) { index, group ->
                        AnimatedEnter(index * 70) {
                            Card(onClick = { go(Screen.GroupDetail(group)) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(group.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                        Row {
                                            group.memberUids.take(3).forEachIndexed { i, member ->
                                                val colors = listOf(Brand.Amber, Brand.Mint, Brand.Coral)
                                                Avatar(member.take(1).uppercase(), colors[i % colors.size])
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "CODE  ${group.inviteCode}",
                                        color = Brand.Amber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { createDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create a new group")
                }
            }
        }
    }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text("New group") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (groupName.isNotBlank()) vm.createGroup(groupName) { group ->
                        createDialog = false
                        go(Screen.GroupDetail(group))
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } }
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

    Scaffold(topBar = { TopAppBar(title = { Text("Join a group") }) }) { padding ->
        DecorBackground {
            Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
                Text("GOT A CODE?", style = MaterialTheme.typography.headlineLarge, color = Brand.Amber)
                AccentDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Six characters is all you need. The link your friend sent works too.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Group code") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row {
                    Button(
                        onClick = {
                            vm.joinGroup(code) { group ->
                                if (group != null) replace(Screen.GroupDetail(group))
                            }
                        },
                        enabled = code.length >= 4 && !vm.busy,
                        modifier = Modifier.height(50.dp)
                    ) { Text("Join the group") }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = back, modifier = Modifier.height(50.dp)) { Text("Back") }
                }
            }
        }
    }
}
