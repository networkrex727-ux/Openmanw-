package com.example.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.TextRange
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DBConnection
import com.example.data.SavedApiRequest
import com.example.data.VcsCommit
import java.io.File

// Theme Color definitions
object DevHiveColors {
    val Background = Color(0xFFFAFAFA)      // Warm pure white canvas
    val Surface = Color(0xFFFFFFFF)         // Crisp card white
    val EditorBackground = Color(0xFFF1F5F9) // Warm high-contrast coding grey
    val SurfaceVariant = Color(0xFFE2E8F0)  // Inputs and subtle containers
    val Border = Color(0xFFCBD5E1)          // Thin modern border line
    val Accent = Color(0xFF2563EB)          // Royal Blue brand color (Primary)
    val Green = Color(0xFF16A34A)           // Warm forest success green
    val Red = Color(0xFFDC2626)             // Alert bright crimson
    val Yellow = Color(0xFFD97706)          // Amber/warning focus gold
    val Purple = Color(0xFF7C3AED)          // Modern royal violet
    val TextPrimary = Color(0xFF0F172A)     // Ink black primary copy
    val TextSecondary = Color(0xFF475569)   // Graphite secondary subtext
}

enum class WorkstationMode {
    FILES, DATABASE, API_TESTER, SERVER_ROOM, VCS
}

@Composable
fun DevHiveUI(viewModel: DevHiveViewModel) {
    // FilesWorkspace is now the main layout, maximizing space and avoiding cluttered buttons.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DevHiveColors.Background)
    ) {
        FilesWorkspaceScreen(viewModel)
    }
}

// --- Responsive Side Navigation Rail (Tablets) ---
@Composable
fun DevHiveNavigationRail(
    activeMode: WorkstationMode,
    onModeChange: (WorkstationMode) -> Unit
) {
    NavigationRail(
        containerColor = DevHiveColors.Surface,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Logo",
                    tint = DevHiveColors.Accent,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DEVHIVE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DevHiveColors.TextPrimary
                )
            }
        },
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            DevHiveRailItem(
                mode = WorkstationMode.FILES,
                icon = Icons.Filled.Folder,
                label = "Explorer",
                activeMode = activeMode,
                onModeChange = onModeChange
            )
            DevHiveRailItem(
                mode = WorkstationMode.DATABASE,
                icon = Icons.Filled.Storage,
                label = "Database",
                activeMode = activeMode,
                onModeChange = onModeChange
            )
            DevHiveRailItem(
                mode = WorkstationMode.API_TESTER,
                icon = Icons.Filled.Terminal,
                label = "API",
                activeMode = activeMode,
                onModeChange = onModeChange
            )
            DevHiveRailItem(
                mode = WorkstationMode.SERVER_ROOM,
                icon = Icons.Filled.Dns,
                label = "Servers",
                activeMode = activeMode,
                onModeChange = onModeChange
            )
            DevHiveRailItem(
                mode = WorkstationMode.VCS,
                icon = Icons.Filled.History,
                label = "VCS",
                activeMode = activeMode,
                onModeChange = onModeChange
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DevHiveRailItem(
    mode: WorkstationMode,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeMode: WorkstationMode,
    onModeChange: (WorkstationMode) -> Unit
) {
    val selected = activeMode == mode
    val tint = if (selected) DevHiveColors.Accent else DevHiveColors.TextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("rail_item_${mode.name.lowercase()}")
            .clip(RoundedCornerShape(8.dp))
            .clickable { onModeChange(mode) }
            .padding(8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = tint,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

// --- Responsive Bottom Navigation Bar (Phones) ---
@Composable
fun DevHiveBottomBar(
    activeMode: WorkstationMode,
    onModeChange: (WorkstationMode) -> Unit
) {
    NavigationBar(
        containerColor = DevHiveColors.Surface,
        modifier = Modifier.height(65.dp)
    ) {
        DevHiveBottomBarItem(
            mode = WorkstationMode.FILES,
            icon = Icons.Filled.Folder,
            label = "Explorer",
            activeMode = activeMode,
            onModeChange = onModeChange
        )
        DevHiveBottomBarItem(
            mode = WorkstationMode.DATABASE,
            icon = Icons.Filled.Storage,
            label = "Database",
            activeMode = activeMode,
            onModeChange = onModeChange
        )
        DevHiveBottomBarItem(
            mode = WorkstationMode.API_TESTER,
            icon = Icons.Filled.Terminal,
            label = "API",
            activeMode = activeMode,
            onModeChange = onModeChange
        )
        DevHiveBottomBarItem(
            mode = WorkstationMode.SERVER_ROOM,
            icon = Icons.Filled.Dns,
            label = "Servers",
            activeMode = activeMode,
            onModeChange = onModeChange
        )
        DevHiveBottomBarItem(
            mode = WorkstationMode.VCS,
            icon = Icons.Filled.History,
            label = "VCS",
            activeMode = activeMode,
            onModeChange = onModeChange
        )
    }
}

@Composable
fun RowScope.DevHiveBottomBarItem(
    mode: WorkstationMode,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeMode: WorkstationMode,
    onModeChange: (WorkstationMode) -> Unit
) {
    val selected = activeMode == mode
    NavigationBarItem(
        selected = selected,
        onClick = { onModeChange(mode) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) DevHiveColors.Accent else DevHiveColors.TextSecondary
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) DevHiveColors.TextPrimary else DevHiveColors.TextSecondary
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = DevHiveColors.SurfaceVariant
        ),
        modifier = Modifier.testTag("bottom_item_${mode.name.lowercase()}")
    )
}

// ==========================================
// SCREEN 1: FILE EXPLORER & SYNTAX CODE EDITOR
// ==========================================
@Composable
fun FullscreenOverlay(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DevHiveColors.Background),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DevHiveColors.Border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header row of modal popup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DevHiveColors.Surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DevHiveColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = DevHiveColors.Red)
                    }
                }
                Divider(color = DevHiveColors.Border)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FilesWorkspaceScreen(viewModel: DevHiveViewModel) {
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var createFileType by remember { mutableStateOf("html") }
    var createFileName by remember { mutableStateOf("") }
    
    // Toggle between Code Editor and Live preview in-app browser!
    var editorModeTab by remember { mutableStateOf(0) } // 0 = Editor, 1 = Live Preview

    // Overlay manage states
    var activeOverlayPanel by remember { mutableStateOf<String?>(null) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var isGlobalSearching by remember { mutableStateOf(false) }
    var searchInputText by remember { mutableStateOf("") }

    // Storage Permission launcher & status check
    val context = LocalContext.current
    var storagePermissionGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        storagePermissionGranted = results.values.all { it }
    }

    // Dynamic Notifications Permission Request
    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.POST_NOTIFICATIONS"
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Dashboard: Workspace directory, add action triggers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = "Workspace",
                tint = DevHiveColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "workspace/devhive/",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DevHiveColors.TextPrimary
                )
                Text(
                    text = if (storagePermissionGranted) "✅ Real Storage Access Granted" else "⚠️ Local Sandbox Mode",
                    fontSize = 10.sp,
                    color = if (storagePermissionGranted) DevHiveColors.Green else Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
            }

            // Global recursive search toggle button
            IconButton(
                onClick = {
                    isGlobalSearching = !isGlobalSearching
                    if (!isGlobalSearching) {
                        searchInputText = ""
                        viewModel.performGlobalSearch("")
                    }
                },
                modifier = Modifier.size(32.dp).testTag("global_search_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Global Search",
                    tint = if (isGlobalSearching) DevHiveColors.Accent else DevHiveColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Add File Button
            IconButton(
                onClick = {
                    createFileType = "html"
                    createFileName = ""
                    showCreateFileDialog = true
                },
                modifier = Modifier.size(32.dp).testTag("create_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New File",
                    tint = DevHiveColors.Green
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Three-dot master options dropdown trigger
            Box {
                IconButton(
                    onClick = { showDropdownMenu = true },
                    modifier = Modifier.size(32.dp).testTag("three_dots_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options Menu",
                        tint = DevHiveColors.TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false },
                    modifier = Modifier.background(DevHiveColors.Surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("🔌 Servers & Ports Control", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            activeOverlayPanel = "SERVERS"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("🗄️ phpMyAdmin Panel", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            activeOverlayPanel = "DATABASE"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Storage, contentDescription = null, tint = DevHiveColors.Green, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("💻 Developer Terminal Shell", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            activeOverlayPanel = "TERMINAL"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Code, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("⚡ REST API Request Tester", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            activeOverlayPanel = "API_TESTER"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("🧬 Git VCS Version Control", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            activeOverlayPanel = "VCS"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.History, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("📁 File Manager & ZIP Share", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            viewModel.setFileManagerRoot(false) // default workspace
                            activeOverlayPanel = "FILE_MANAGER"
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("🔔 Request Push Notifications", fontSize = 12.sp, color = DevHiveColors.TextPrimary) },
                        onClick = {
                            showDropdownMenu = false
                            notificationLauncher.launch("android.permission.POST_NOTIFICATIONS")
                        },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = DevHiveColors.Yellow, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Quick Toggle Panel (Code vs WebView Preview!)
            if (viewModel.isServerRunning) {
                Spacer(modifier = Modifier.width(8.dp))
                TabRow(
                    selectedTabIndex = editorModeTab,
                    containerColor = DevHiveColors.SurfaceVariant,
                    indicator = {},
                    divider = {},
                    modifier = Modifier
                        .width(180.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = editorModeTab == 0,
                        onClick = { editorModeTab = 0 },
                        text = { Text("Code", fontSize = 11.sp, color = DevHiveColors.TextPrimary) }
                    )
                    Tab(
                        selected = editorModeTab == 1,
                        onClick = { editorModeTab = 1 },
                        text = { Text("Live Preview", fontSize = 11.sp, color = DevHiveColors.Green) }
                    )
                }
            } else {
                editorModeTab = 0 // Enforce code
            }
        }

        // Global Search bar and drop down items (VS Code style!)
        if (isGlobalSearching) {
            var replaceInputText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DevHiveColors.SurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Find Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchInputText,
                        onValueChange = {
                            searchInputText = it
                            viewModel.performGlobalSearch(it)
                        },
                        textStyle = TextStyle(color = DevHiveColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(DevHiveColors.Accent),
                        modifier = Modifier.weight(1f).testTag("global_search_input"),
                        decorationBox = { innerTextField ->
                            if (searchInputText.isEmpty()) {
                                Text("Search content in all workspace files...", color = DevHiveColors.TextSecondary, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchInputText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInputText = ""
                            viewModel.performGlobalSearch("")
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear", tint = DevHiveColors.TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Replace Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, tint = DevHiveColors.Yellow, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = replaceInputText,
                        onValueChange = { replaceInputText = it },
                        textStyle = TextStyle(color = DevHiveColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(DevHiveColors.Accent),
                        modifier = Modifier.weight(1f).testTag("global_replace_input"),
                        decorationBox = { innerTextField ->
                            if (replaceInputText.isEmpty()) {
                                Text("Replace with...", color = DevHiveColors.TextSecondary, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )

                    Button(
                        onClick = {
                            if (searchInputText.isNotEmpty()) {
                                viewModel.performGlobalReplace(searchInputText, replaceInputText)
                                replaceInputText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Green),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Replace All", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            if (searchInputText.isNotEmpty() && viewModel.globalSearchResults.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, DevHiveColors.Border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .heightIn(max = 240.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(viewModel.globalSearchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Open file
                                        viewModel.openFile(result.file)
                                        // Deactivate search
                                        isGlobalSearching = false
                                        searchInputText = ""
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.file.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DevHiveColors.Accent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Line ${result.lineNumber}: ${result.lineContent}",
                                        fontSize = 11.sp,
                                        color = DevHiveColors.TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = DevHiveColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                            Divider(color = DevHiveColors.Border.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Beautiful storage permission banner if not authorized
        if (!storagePermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)), // Dark Amber
                border = BorderStroke(1.dp, Color(0xFFD97706)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    } else {
                        launcher.launch(
                            arrayOf(
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Permission Protected",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "File Sandbox Mode (Tap to authorize actual device file storage directory accesses)",
                        fontSize = 11.sp,
                        color = Color(0xFFFEF3C7),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD97706))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("AUTHORIZE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDEBAR: File list (Vertical Tree)
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(DevHiveColors.Surface)
                    .drawBehind {
                        drawLine(
                            color = DevHiveColors.Border,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "EXPLORER",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DevHiveColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(viewModel.currentFiles) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openFile(item.file) }
                                .background(if (viewModel.activeTab == item.file) DevHiveColors.SurfaceVariant else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.FilePresent,
                                contentDescription = item.name,
                                tint = if (item.file.name == "welcome_notes.txt") DevHiveColors.Yellow else DevHiveColors.Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DevHiveColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteWorkspaceFile(item.file) },
                                modifier = Modifier.size(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete File",
                                    tint = DevHiveColors.Red,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // RIGHT CONTENT: Monaco-level editor with real-time Syntax Highlighting Transformation!
            Column(modifier = Modifier.fillMaxSize()) {
                if (editorModeTab == 1 && viewModel.isServerRunning) {
                    // LIVE PREVIEW: Render in-app browser
                    LiveBrowserPreview(port = viewModel.serverPort)
                } else {
                    // TAB HEADER ROW
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(35.dp)
                            .background(DevHiveColors.SurfaceVariant)
                    ) {
                        items(viewModel.openTabs) { tabFile ->
                            val isActive = viewModel.activeTab == tabFile
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(if (isActive) DevHiveColors.EditorBackground else Color.Transparent)
                                    .clickable { viewModel.openFile(tabFile) }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tabFile.name,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isActive) DevHiveColors.TextPrimary else DevHiveColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.closeTab(tabFile) },
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = DevHiveColors.TextSecondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ACTIVE TEXT EDITOR
                    viewModel.activeTab?.let { activeFile ->
                        var showFindReplace by remember(activeFile) { mutableStateOf(false) }
                        var findText by remember(activeFile) { mutableStateOf("") }
                        var replaceText by remember(activeFile) { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DevHiveColors.EditorBackground)
                        ) {
                            // Save indicator bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DevHiveColors.SurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Editing",
                                    tint = DevHiveColors.Accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Editing: ${activeFile.name}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DevHiveColors.TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                // Find & Replace Toggle Button
                                IconButton(
                                    onClick = { showFindReplace = !showFindReplace },
                                    modifier = Modifier.size(18.dp).testTag("find_replace_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Find & Replace",
                                        tint = if (showFindReplace) DevHiveColors.Accent else DevHiveColors.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.saveActiveFile(viewModel.activeFileContent) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "Save file",
                                        tint = DevHiveColors.Green,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Find & Replace Panel
                            if (showFindReplace) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DevHiveColors.Surface)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .drawBehind {
                                            drawLine(
                                                color = DevHiveColors.Border,
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1f
                                            )
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = findText,
                                        onValueChange = { findText = it },
                                        placeholder = { Text("Find...", color = DevHiveColors.TextSecondary, fontSize = 11.sp) },
                                        textStyle = TextStyle(color = DevHiveColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DevHiveColors.Accent,
                                            unfocusedBorderColor = DevHiveColors.Border
                                        ),
                                        modifier = Modifier.weight(1.3f).height(38.dp).testTag("editor_find_input"),
                                        shape = RoundedCornerShape(4.dp),
                                        maxLines = 1
                                    )
                                    OutlinedTextField(
                                        value = replaceText,
                                        onValueChange = { replaceText = it },
                                        placeholder = { Text("Replace...", color = DevHiveColors.TextSecondary, fontSize = 11.sp) },
                                        textStyle = TextStyle(color = DevHiveColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DevHiveColors.Accent,
                                            unfocusedBorderColor = DevHiveColors.Border
                                        ),
                                        modifier = Modifier.weight(1.3f).height(38.dp).testTag("editor_replace_input"),
                                        shape = RoundedCornerShape(4.dp),
                                        maxLines = 1
                                    )
                                    Button(
                                        onClick = {
                                            if (findText.isNotEmpty()) {
                                                viewModel.activeFileContent = viewModel.activeFileContent.replaceFirst(findText, replaceText)
                                                viewModel.saveActiveFile(viewModel.activeFileContent)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(30.dp).testTag("editor_replace_next_btn"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Replace", fontSize = 10.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            if (findText.isNotEmpty()) {
                                                viewModel.activeFileContent = viewModel.activeFileContent.replace(findText, replaceText)
                                                viewModel.saveActiveFile(viewModel.activeFileContent)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Green),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(30.dp).testTag("editor_replace_all_btn"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("All", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            // Interactive scrollable text field with real-time Syntax Highlighting and line numbers!
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Draw Line Numbers
                                val lines = viewModel.activeFileContent.lines()
                                val lineCount = maxOf(lines.size, 1)
                                Column(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .fillMaxHeight()
                                        .background(DevHiveColors.Surface)
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    for (i in 1..lineCount) {
                                        Text(
                                            text = "$i",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = DevHiveColors.TextSecondary,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                }

                                // Interactive Text editor
                                var textSelection by remember(activeFile) { mutableStateOf<TextRange>(TextRange.Zero) }
                                val textFieldValue = TextFieldValue(
                                    text = viewModel.activeFileContent,
                                    selection = textSelection
                                )

                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = {
                                        viewModel.activeFileContent = it.text
                                        textSelection = it.selection
                                        // Auto-save behavior simulated
                                        viewModel.saveActiveFile(it.text)
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = DevHiveColors.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(DevHiveColors.Accent),
                                    visualTransformation = SyntaxHighlightTransformation(activeFile.extension),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                        .testTag("code_editor_field")
                                )
                            }
                        }
                    } ?: run {
                        // EMPTY WORKSPACE PRESENTATION
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DevHiveColors.EditorBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Terminal,
                                    contentDescription = "No file open",
                                    tint = DevHiveColors.TextSecondary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "DevHive Workspace Editor",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DevHiveColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select a workspace file to start editing.",
                                    fontSize = 11.sp,
                                    color = DevHiveColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue: Create File
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = {
                Text(
                    "Create Workspace Asset",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select asset extension types:", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("html", "css", "js", "sql", "py", "txt").forEach { ext ->
                            val active = createFileType == ext
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) DevHiveColors.Accent else DevHiveColors.Surface)
                                    .clickable { createFileType = ext }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ext.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) DevHiveColors.Background else DevHiveColors.TextPrimary
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = createFileName,
                        onValueChange = { createFileName = it },
                        label = { Text("Asset Name", fontSize = 12.sp) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().testTag("create_file_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createFileName.isNotBlank()) {
                            viewModel.createWorkspaceFile(createFileName, createFileType)
                        }
                        showCreateFileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel", color = DevHiveColors.TextSecondary)
                }
            }
        )
    }

    // Modal Fullscreen Overlays manager for all tools
    if (activeOverlayPanel != null) {
        when (activeOverlayPanel) {
            "SERVERS" -> {
                FullscreenOverlay(
                    title = "🔌 Servers & Ports Control",
                    icon = Icons.Filled.Settings,
                    onClose = { activeOverlayPanel = null }
                ) {
                    ServerRoomScreen(viewModel)
                }
            }
            "DATABASE" -> {
                FullscreenOverlay(
                    title = "🗄️ phpMyAdmin Panel",
                    icon = Icons.Filled.Storage,
                    onClose = { activeOverlayPanel = null }
                ) {
                    DatabaseBrowserScreen(viewModel)
                }
            }
            "TERMINAL" -> {
                FullscreenOverlay(
                    title = "💻 Developer Terminal Shell",
                    icon = Icons.Filled.Code,
                    onClose = { activeOverlayPanel = null }
                ) {
                    var terminalInput by remember { mutableStateOf("") }
                    val focusManager = LocalFocusManager.current
                    val scrollState = rememberScrollState()

                    LaunchedEffect(viewModel.terminalHistory.size) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, DevHiveColors.Border)
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = "=== DEVHIVE LINUX SANDBOX TERMINAL ENVIRONMENT ===",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Cyan
                            )
                            Text(
                                text = "Type 'help' to see local system shell command offerings.\n",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )

                            viewModel.terminalHistory.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (log.startsWith("devhive@android")) Color(0xFF38BDF8) else if (log.contains("  Error")) Color(0xFFF87171) else Color(0xFF34D399)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "devhive@android:~/workspace$ ",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8)
                                )
                                BasicTextField(
                                    value = terminalInput,
                                    onValueChange = { terminalInput = it },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (terminalInput.isNotBlank()) {
                                            viewModel.executeTerminalCommand(terminalInput)
                                            terminalInput = ""
                                        }
                                    }),
                                    cursorBrush = SolidColor(Color(0xFF34D399)),
                                    modifier = Modifier.weight(1f).testTag("popup_terminal_input")
                                )
                            }
                        }
                    }
                }
            }
            "API_TESTER" -> {
                FullscreenOverlay(
                    title = "⚡ REST API Request Tester",
                    icon = Icons.Filled.Send,
                    onClose = { activeOverlayPanel = null }
                ) {
                    ApiTesterScreen(viewModel)
                }
            }
            "VCS" -> {
                FullscreenOverlay(
                    title = "🧬 Git VCS Version Control",
                    icon = Icons.Filled.History,
                    onClose = { activeOverlayPanel = null }
                ) {
                    VersionControlScreen(viewModel)
                }
            }
            "FILE_MANAGER" -> {
                FullscreenOverlay(
                    title = "📁 Advanced Storage & File Manager",
                    icon = Icons.Filled.Folder,
                    onClose = { activeOverlayPanel = null }
                ) {
                    FileManagerScreen(viewModel)
                }
            }
        }
    }
}

// In-app WebView client for live server previews!
@Composable
fun LiveBrowserPreview(port: Int) {
    val context = LocalContext.current
    val url = "http://localhost:$port/index.html"
    
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Webview",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = url,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f)
                )
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    webView.loadUrl(url)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Visual Transformation implementing our neon syntax highlighter on typing!
class SyntaxHighlightTransformation(private val extension: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightSyntax(text.text, extension)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

fun highlightSyntax(text: String, extension: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    
    // Theme palette
    val keywordColor = Color(0xFFF472B6)  // Neon Pink
    val stringColor = Color(0xFF34D399)   // Emerald Green
    val commentColor = Color(0xFF64748B)  // Muted Slate Gray
    val numberColor = Color(0xFFFBBF24)   // Amber Yellow
    val tagColor = Color(0xFF60A5FA)      // Electric Blue

    val ext = extension.lowercase()

    try {
        if (ext == "html" || ext == "xml") {
            // Highlight tags
            val tagRegex = Regex("<[^>]+>")
            tagRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = tagColor), match.range.first, match.range.last + 1)
            }
            // Highlight strings
            val strRegex = Regex("\"[^\"]*\"|'[^']*'")
            strRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }
            // HTML Comments
            val commentRegex = Regex("<!--[\\s\\S]*?-->")
            commentRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
        } else if (ext == "sql") {
            val keywords = listOf(
                "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
                "CREATE", "TABLE", "DATABASE", "DROP", "ALTER", "JOIN", "LEFT", "RIGHT", "ON",
                "PRAGMA", "EXPLAIN", "ANALYZE", "PRIMARY", "KEY", "AUTOINCREMENT", "NOT", "NULL", "INTEGER", "TEXT"
            )
            // String constants
            val strRegex = Regex("'[^']*'")
            strRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }
            // Case-insensitive keywords
            keywords.forEach { kw ->
                val kwRegex = Regex("\\b$kw\\b", RegexOption.IGNORE_CASE)
                kwRegex.findAll(text).forEach { match ->
                    builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
            }
            // Comments
            val commentRegex = Regex("--.*")
            commentRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
        } else {
            // General Languages (JS, PY, KT, TXT)
            val keywords = when (ext) {
                "py" -> listOf("def", "class", "import", "from", "return", "if", "elif", "else", "for", "while", "in", "None", "True", "False")
                "js" -> listOf("let", "const", "var", "function", "class", "import", "from", "return", "if", "else", "for", "while", "true", "false", "null")
                "kt" -> listOf("fun", "class", "val", "var", "import", "package", "return", "if", "else", "for", "while", "true", "false", "null")
                else -> listOf("class", "return", "if", "else", "for", "while")
            }

            // Apply Keywords styles
            keywords.forEach { kw ->
                val kwRegex = Regex("\\b$kw\\b")
                kwRegex.findAll(text).forEach { match ->
                    builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
            }

            // String Constants
            val strRegex = Regex("\"[^\"]*\"|'[^']*'")
            strRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            // Constant Numbers
            val numRegex = Regex("\\b\\d+\\b")
            numRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            // Comments
            val commentRegex = Regex("//.*|#.*")
            commentRegex.findAll(text).forEach { match ->
                builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
        }
    } catch (e: Exception) {}

    return builder.toAnnotatedString()
}


// ==========================================
// SCREEN 2: SQLITE DATABASE EXPLORER & RUNNER
// ==========================================
@Composable
fun DatabaseBrowserScreen(viewModel: DevHiveViewModel) {
    PhpMyAdminWorkspace(viewModel)
}

@Composable
fun DatabaseBrowserScreenOld(viewModel: DevHiveViewModel) {
    val connections by viewModel.savedConnections.collectAsStateWithLifecycle()
    var showCreateConnection by remember { mutableStateOf(false) }
    var newConnName by remember { mutableStateOf("") }
    var newConnDbFile by remember { mutableStateOf("devhive_database") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Storage, contentDescription = "DB Browser", tint = DevHiveColors.Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SQL DATABASE MANAGER",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = DevHiveColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showCreateConnection = true },
                colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent),
                modifier = Modifier.height(32.dp).testTag("add_connection_btn")
            ) {
                Text("New Link", fontSize = 11.sp, color = DevHiveColors.Background)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Connections & tables Left Panel
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .background(DevHiveColors.Surface)
                    .drawBehind {
                        drawLine(
                            color = DevHiveColors.Border,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(8.dp)
            ) {
                // List active connections
                Text("CONNECTIONS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.height(130.dp)) {
                    items(connections) { conn ->
                        val isActive = viewModel.activeDbConnection?.id == conn.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.connectToSQLite(conn) }
                                .background(if (isActive) DevHiveColors.SurfaceVariant else Color.Transparent)
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = if (isActive) DevHiveColors.Green else DevHiveColors.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = conn.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DevHiveColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (conn.name != "DevHive Internals (SQLite)") {
                                IconButton(
                                    onClick = { viewModel.deleteConnectionFromDb(conn.id) },
                                    modifier = Modifier.size(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete Link",
                                        tint = DevHiveColors.Red,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = DevHiveColors.Border)
                Spacer(modifier = Modifier.height(10.dp))

                // List of active Tables
                Text("ACTIVE TABLES", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                if (viewModel.dbTables.isEmpty()) {
                    Text("No Tables Loaded", fontSize = 10.sp, color = DevHiveColors.TextSecondary, modifier = Modifier.padding(6.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(viewModel.dbTables) { table ->
                            val isSelected = viewModel.selectedTable == table
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadTableRows(table) }
                                    .background(if (isSelected) DevHiveColors.SurfaceVariant else Color.Transparent)
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilePresent,
                                    contentDescription = null,
                                    tint = DevHiveColors.Accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = table,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DevHiveColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Right SQL console and rows visualization
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // phpMyAdmin-themed dashboard card representing MySQL server status
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, DevHiveColors.Border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22C55E)), // phpMyAdmin Theme Green
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PMA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "phpMyAdmin v5.2 (Database Control)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DevHiveColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DevHiveColors.Green.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("MySQL Active", fontSize = 8.sp, color = DevHiveColors.Green, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Database Host: 127.0.0.1:3306 | User: root@localhost",
                                fontSize = 10.sp,
                                color = DevHiveColors.TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.dbConsoleQuery = "SELECT name, tbl_name FROM sqlite_master WHERE type='table';"
                                viewModel.executeSqlStatement()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Tables", fontSize = 9.sp, color = DevHiveColors.Background)
                        }
                    }
                }

                // Interactive SQL Terminal editor
                Card(
                    colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    border = BorderStroke(1.dp, DevHiveColors.Border)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DevHiveColors.SurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SQL QUERY WORKBENCH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextPrimary, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { viewModel.executeSqlStatement() },
                                modifier = Modifier.size(20.dp).testTag("run_query_btn")
                            ) {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Run Query", tint = DevHiveColors.Green, modifier = Modifier.size(16.dp))
                            }
                        }
                        BasicTextField(
                            value = viewModel.dbConsoleQuery,
                            onValueChange = { viewModel.dbConsoleQuery = it },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DevHiveColors.TextPrimary),
                            cursorBrush = SolidColor(DevHiveColors.Accent),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("query_input_field")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Error console
                if (viewModel.dbConsoleError.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = viewModel.dbConsoleError,
                            color = Color(0xFFFECACA),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Success Message
                if (viewModel.dbConsoleMessage.isNotBlank()) {
                    Text(
                        text = viewModel.dbConsoleMessage,
                        color = DevHiveColors.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Interactive Query result grid
                Text("QUERY OUTPUT GRID", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                val cols = if (viewModel.selectedTable != null && viewModel.tableColumns.isNotEmpty()) {
                    viewModel.tableColumns
                } else {
                    viewModel.dbConsoleColumns
                }

                val rows = if (viewModel.selectedTable != null && viewModel.tableRows.isNotEmpty()) {
                    viewModel.tableRows
                } else {
                    viewModel.dbConsoleRows
                }

                if (cols.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DevHiveColors.EditorBackground)
                            .border(1.dp, DevHiveColors.Border),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Query Results Displayed", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
                        modifier = Modifier.fillMaxSize(),
                        border = BorderStroke(1.dp, DevHiveColors.Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Headers list
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DevHiveColors.SurfaceVariant)
                                    .padding(vertical = 6.dp)
                            ) {
                                cols.forEach { header ->
                                    Text(
                                        text = header.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = DevHiveColors.TextPrimary,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                            // Rows data
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(rows) { rowData ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        rowData.forEach { cell ->
                                            Text(
                                                text = cell,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (cell == "NULL") DevHiveColors.TextSecondary else DevHiveColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                    Divider(color = DevHiveColors.SurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateConnection) {
        var newConnType by remember { mutableStateOf("MySQL") }
        var newConnHost by remember { mutableStateOf("127.0.0.1") }
        var newConnPort by remember { mutableStateOf("3306") }
        var newConnUser by remember { mutableStateOf("root") }
        var newConnPass by remember { mutableStateOf("devhive_secure_root") }
        var newConnDbName by remember { mutableStateOf("devhive_database") }

        AlertDialog(
            onDismissRequest = { showCreateConnection = false },
            title = { Text("Add Database Connection", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DevHiveColors.TextPrimary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newConnName,
                        onValueChange = { newConnName = it },
                        label = { Text("Connection Profile Name") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().testTag("conn_name_input")
                    )

                    // Type Selector
                    Column {
                        Text("Database Engine Type", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("MySQL", "SQLite", "PostgreSQL", "MongoDB").forEach { type ->
                                val selected = newConnType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) DevHiveColors.Accent else DevHiveColors.SurfaceVariant)
                                        .clickable {
                                            newConnType = type
                                            if (type == "SQLite") {
                                                newConnHost = "Local File"
                                                newConnPort = "0"
                                                newConnUser = "local"
                                            } else {
                                                newConnHost = "127.0.0.1"
                                                newConnPort = if (type == "MySQL") "3306" else if (type == "PostgreSQL") "5432" else "27017"
                                                newConnUser = "root"
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) DevHiveColors.Background else DevHiveColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newConnHost,
                        onValueChange = { newConnHost = it },
                        label = { Text("Server Host IP / Path") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().testTag("conn_host_input"),
                        enabled = newConnType != "SQLite"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newConnPort,
                            onValueChange = { newConnPort = it },
                            label = { Text("Port") },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            enabled = newConnType != "SQLite"
                        )
                        OutlinedTextField(
                            value = newConnUser,
                            onValueChange = { newConnUser = it },
                            label = { Text("Username") },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1.5f),
                            enabled = newConnType != "SQLite"
                        )
                    }

                    OutlinedTextField(
                        value = newConnPass,
                        onValueChange = { newConnPass = it },
                        label = { Text("Password (Secret)") },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newConnType != "SQLite"
                    )

                    OutlinedTextField(
                        value = newConnDbName,
                        onValueChange = { newConnDbName = it },
                        label = { Text("Database / Schema Name") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().testTag("conn_file_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newConnName.isNotBlank() && newConnDbName.isNotBlank()) {
                            val portInt = newConnPort.toIntOrNull() ?: 0
                            viewModel.saveConnectionToDb(
                                name = newConnName,
                                type = newConnType,
                                host = newConnHost,
                                port = portInt,
                                user = newConnUser,
                                databaseName = newConnDbName
                            )
                        }
                        showCreateConnection = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent)
                ) {
                    Text("Add Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateConnection = false }) {
                    Text("Cancel", color = DevHiveColors.TextSecondary)
                }
            }
        )
    }
}


// ==========================================
// SCREEN 3: POSTMAN-STYLE REST API CLIENT
// ==========================================
@Composable
fun ApiTesterScreen(viewModel: DevHiveViewModel) {
    val savedRequests by viewModel.savedApiRequests.collectAsStateWithLifecycle()
    var showSaveRequestDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var saveCollection by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Control panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Terminal, contentDescription = "API Client", tint = DevHiveColors.Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REST API CLIENT",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = DevHiveColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showSaveRequestDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.AccentVariant()),
                modifier = Modifier.height(32.dp).testTag("save_req_btn")
            ) {
                Text("Save Request", fontSize = 11.sp, color = DevHiveColors.TextPrimary)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar of Saved Requests
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(DevHiveColors.Surface)
                    .drawBehind {
                        drawLine(
                            color = DevHiveColors.Border,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(8.dp)
            ) {
                Text("SAVED API", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                if (savedRequests.isEmpty()) {
                    Text("No saved calls", fontSize = 10.sp, color = DevHiveColors.TextSecondary, modifier = Modifier.padding(4.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(savedRequests) { req ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadSavedApiRequest(req) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(getMethodColor(req.method))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(text = req.method, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = req.name,
                                    fontSize = 11.sp,
                                    color = DevHiveColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteApiRequestFromDb(req.id) },
                                    modifier = Modifier.size(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = DevHiveColors.Red, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Right API Console view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Address Bar & execution action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expandedMethod by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandedMethod = true },
                            colors = ButtonDefaults.buttonColors(containerColor = getMethodColor(viewModel.apiMethod)),
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                            modifier = Modifier.height(48.dp).testTag("api_method_selector")
                        ) {
                            Text(viewModel.apiMethod, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedMethod,
                            onDismissRequest = { expandedMethod = false }
                        ) {
                            listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, fontWeight = FontWeight.Bold, color = getMethodColor(m)) },
                                    onClick = {
                                        viewModel.apiMethod = m
                                        expandedMethod = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.apiUrl,
                        onValueChange = { viewModel.apiUrl = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DevHiveColors.Accent,
                            unfocusedBorderColor = DevHiveColors.Border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("api_url_input"),
                        shape = RoundedCornerShape(0.dp)
                    )

                    Button(
                        onClick = { viewModel.sendApiRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Green),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 0.dp, bottomStart = 0.dp),
                        modifier = Modifier.height(48.dp).testTag("api_send_btn")
                    ) {
                        if (viewModel.isApiSending) {
                            CircularProgressIndicator(color = DevHiveColors.Background, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Filled.Send, contentDescription = "Send", tint = DevHiveColors.Background)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Headers parameters configuration list
                Text("HTTP HEADERS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                viewModel.apiHeaders.forEachIndexed { idx, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { viewModel.updateHeaderField(idx, it, pair.second) },
                            placeholder = { Text("Header Name", fontSize = 11.sp) },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { viewModel.updateHeaderField(idx, pair.first, it) },
                            placeholder = { Text("Value", fontSize = 11.sp) },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.removeHeaderField(idx) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = DevHiveColors.Red)
                        }
                    }
                }
                TextButton(
                    onClick = { viewModel.addHeaderField() },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = DevHiveColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Header", fontSize = 11.sp, color = DevHiveColors.Accent)
                }

                // Request body block
                if (viewModel.apiMethod != "GET" && viewModel.apiMethod != "DELETE") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("REQUEST BODY (JSON)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = viewModel.apiBody,
                        onValueChange = { viewModel.apiBody = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("api_body_input"),
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = DevHiveColors.Border)
                Spacer(modifier = Modifier.height(12.dp))

                // Response visualization
                Text("RESPONSE WORKBENCH", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.apiResponseStatus.isNotBlank()) {
                    // Response Metadata Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        ResponseBadge(label = "STATUS", value = viewModel.apiResponseStatus, color = if (viewModel.apiResponseStatus.contains("200")) DevHiveColors.Green else DevHiveColors.Red)
                        ResponseBadge(label = "TIME", value = viewModel.apiResponseTime, color = DevHiveColors.Yellow)
                        ResponseBadge(label = "SIZE", value = viewModel.apiResponseSize, color = DevHiveColors.Accent)
                    }

                    // Response payload viewer
                    Text("RESPONSE BODY", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DevHiveColors.Border)
                    ) {
                        Text(
                            text = viewModel.apiResponseBody,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = DevHiveColors.Green,
                            modifier = Modifier
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(DevHiveColors.EditorBackground)
                            .border(1.dp, DevHiveColors.Border),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Send a request to fetch API body payloads", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                    }
                }
            }
        }
    }

    if (showSaveRequestDialog) {
        AlertDialog(
            onDismissRequest = { showSaveRequestDialog = false },
            title = { Text("Save Request Details", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("Request Name") },
                        modifier = Modifier.fillMaxWidth().testTag("req_name_input")
                    )
                    OutlinedTextField(
                        value = saveCollection,
                        onValueChange = { saveCollection = it },
                        label = { Text("Collection Category") },
                        modifier = Modifier.fillMaxWidth().testTag("req_coll_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiRequestToDb(saveName, saveCollection)
                        showSaveRequestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF10B981)
        "POST" -> Color(0xFFFBBF24)
        "PUT" -> Color(0xFF3B82F6)
        "DELETE" -> Color(0xFFEF4444)
        else -> Color(0xFF8B5CF6)
    }
}

@Composable
fun ResponseBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DevHiveColors.Surface)
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $value",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


// ==========================================
// SCREEN 4: SERVER ROOM & SERVICES FLEET
// ==========================================
@Composable
fun ServerRoomScreen(viewModel: DevHiveViewModel) {
    var serverTabSelected by remember { mutableStateOf(0) } // 0 = Web Server, 1 = Docker Simulator, 2 = Port Scanner

    Column(modifier = Modifier.fillMaxSize()) {
        // Core Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Dns, contentDescription = "Servers", tint = DevHiveColors.Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DEVELOPER SERVICES CENTER",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = DevHiveColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Horizontal navigation tabs for Services
        TabRow(
            selectedTabIndex = serverTabSelected,
            containerColor = DevHiveColors.Surface,
            contentColor = DevHiveColors.Accent
        ) {
            Tab(selected = serverTabSelected == 0, onClick = { serverTabSelected = 0 }) {
                Text("Web Server", fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(10.dp), color = if (serverTabSelected == 0) DevHiveColors.TextPrimary else DevHiveColors.TextSecondary)
            }
            Tab(selected = serverTabSelected == 1, onClick = { serverTabSelected = 1 }) {
                Text("Mock Docker", fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(10.dp), color = if (serverTabSelected == 1) DevHiveColors.TextPrimary else DevHiveColors.TextSecondary)
            }
            Tab(selected = serverTabSelected == 2, onClick = { serverTabSelected = 2 }) {
                Text("Port Scanner", fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(10.dp), color = if (serverTabSelected == 2) DevHiveColors.TextPrimary else DevHiveColors.TextSecondary)
            }
        }

        // Active panel layout
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when (serverTabSelected) {
                0 -> WebServerConsoleView(viewModel)
                1 -> DockerSimulatorView(viewModel)
                2 -> LocalPortScannerView(viewModel)
            }
        }
    }
}

@Composable
fun WebServerConsoleView(viewModel: DevHiveViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Quick control toggler
        Card(
            colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, DevHiveColors.Border)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Built-in Socket Web Server",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DevHiveColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Serves workspace files on: http://localhost:${viewModel.serverPort}",
                        fontSize = 11.sp,
                        color = DevHiveColors.TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { viewModel.toggleWebServer() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isServerRunning) DevHiveColors.Red else DevHiveColors.Green),
                    modifier = Modifier.testTag("toggle_server_btn")
                ) {
                    Icon(imageVector = if (viewModel.isServerRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = DevHiveColors.Background)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (viewModel.isServerRunning) "STOP" else "START", color = DevHiveColors.Background, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Web server logs screen
        Text("LIVE WEBSERVER ACCESS LOGS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
            modifier = Modifier.fillMaxSize(),
            border = BorderStroke(1.dp, DevHiveColors.Border)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(viewModel.serverLogs) { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (log.contains("200 OK")) DevHiveColors.Green else if (log.contains("404")) DevHiveColors.Red else DevHiveColors.Accent
                    )
                }
            }
        }
    }
}

@Composable
fun DockerSimulatorView(viewModel: DevHiveViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mock Docker Container Dashboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DevHiveColors.TextPrimary)
                Text("Local sandboxed simulation of container stacks", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(DevHiveColors.Green.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Engine: Online", fontSize = 10.sp, color = DevHiveColors.Green, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Fleet container card list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.dockerContainers) { container ->
                    val isRunning = container.status == "RUNNING"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
                        border = BorderStroke(1.dp, DevHiveColors.Border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isRunning) DevHiveColors.Green else DevHiveColors.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = container.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DevHiveColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.toggleContainer(container.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isRunning) DevHiveColors.Red else DevHiveColors.Green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(text = "Image: ${container.image} | Port: ${container.port}", fontSize = 10.sp, color = DevHiveColors.TextSecondary)
                            
                            if (isRunning) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "CPU: ${container.cpu}%", fontSize = 9.sp, color = DevHiveColors.Accent, fontFamily = FontFamily.Monospace)
                                    Text(text = "RAM: ${container.ram}", fontSize = 9.sp, color = DevHiveColors.Yellow, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Container standard logs stream
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("DOCKER ENGINE CONSOLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
                    modifier = Modifier.fillMaxSize(),
                    border = BorderStroke(1.dp, DevHiveColors.Border)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(viewModel.dockerLogs) { log ->
                            Text(text = log, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = DevHiveColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalPortScannerView(viewModel: DevHiveViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, DevHiveColors.Border)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Developer Socket Port Scanner", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DevHiveColors.TextPrimary)
                    Text("Scans localhost for open standard services", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                }
                Button(
                    onClick = { viewModel.runLocalPortScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent),
                    modifier = Modifier.testTag("scan_ports_btn")
                ) {
                    if (viewModel.isPortScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DevHiveColors.Background)
                    } else {
                        Text("SCAN", color = DevHiveColors.Background, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scanning reports display
        Text("PORT SCANNER DIAGNOSTIC LOG", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
            modifier = Modifier.fillMaxSize(),
            border = BorderStroke(1.dp, DevHiveColors.Border)
        ) {
            if (viewModel.scannedPorts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap scan to diagnose open local ports", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    items(viewModel.scannedPorts) { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (res.isOpen) DevHiveColors.Green.copy(alpha = 0.2f) else DevHiveColors.Red.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (res.isOpen) "OPEN" else "CLOSED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.isOpen) DevHiveColors.Green else DevHiveColors.Red
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Port ${res.port}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DevHiveColors.TextPrimary,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                text = res.service,
                                fontSize = 11.sp,
                                color = DevHiveColors.TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SCREEN 5: VERSION CONTROL (GIT SIMULATOR) & TERMINAL
// ==========================================
@Composable
fun VersionControlScreen(viewModel: DevHiveViewModel) {
    val commits by viewModel.gitCommits.collectAsStateWithLifecycle()
    var terminalInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.History, contentDescription = "Git Panel", tint = DevHiveColors.Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VERSION CONTROL ROOM",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = DevHiveColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.resetVcsHistory() }) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset VCS", tint = DevHiveColors.Red)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar: Staging files and Commit input
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .background(DevHiveColors.Surface)
                    .drawBehind {
                        drawLine(
                            color = DevHiveColors.Border,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(8.dp)
            ) {
                Text("CHANGED FILES", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                
                if (viewModel.unstagedFiles.isEmpty() && viewModel.stagedFiles.isEmpty()) {
                    Text("No file changes", fontSize = 10.sp, color = DevHiveColors.TextSecondary, modifier = Modifier.padding(6.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        item {
                            if (viewModel.unstagedFiles.isNotEmpty()) {
                                Text("Unstaged:", fontSize = 9.sp, color = DevHiveColors.Yellow, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(viewModel.unstagedFiles) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.name, fontSize = 10.sp, color = DevHiveColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.stageFile(file) }, modifier = Modifier.size(16.dp)) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Stage", tint = DevHiveColors.Green, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        item {
                            if (viewModel.stagedFiles.isNotEmpty()) {
                                Text("Staged:", fontSize = 9.sp, color = DevHiveColors.Green, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(viewModel.stagedFiles) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(file.name, fontSize = 10.sp, color = DevHiveColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.unstageFile(file) }, modifier = Modifier.size(16.dp)) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Unstage", tint = DevHiveColors.Red, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = DevHiveColors.Border)
                Spacer(modifier = Modifier.height(10.dp))

                // Commit action form
                Text("COMMIT ASSETS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = viewModel.commitMessage,
                    onValueChange = { viewModel.commitMessage = it },
                    placeholder = { Text("Commit Message", fontSize = 10.sp) },
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("commit_msg_input"),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { viewModel.commitChanges(viewModel.commitMessage) },
                    colors = ButtonDefaults.buttonColors(containerColor = DevHiveColors.Accent),
                    modifier = Modifier.fillMaxWidth().height(32.dp).testTag("commit_btn"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("COMMIT", fontSize = 10.sp, color = DevHiveColors.Background, fontWeight = FontWeight.Bold)
                }
            }

            // Right view: Split Terminal Console and Commits graph history
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // LEFT COLUMN: Commits timelines node graph list!
                    Column(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                        Text("VCS TIMELINE COMMIT GRAPH", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        if (commits.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().border(1.dp, DevHiveColors.Border).background(DevHiveColors.EditorBackground), contentAlignment = Alignment.Center) {
                                Text("No Commits Timeline History", fontSize = 11.sp, color = DevHiveColors.TextSecondary)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(commits) { commit ->
                                    val isSelected = viewModel.selectedCommitHash == commit.commitHash
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.loadCommitDiffs(commit) }
                                            .background(if (isSelected) DevHiveColors.SurfaceVariant else Color.Transparent)
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Draw Git Node Graph
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .drawBehind {
                                                    // Node line
                                                    drawLine(
                                                        color = DevHiveColors.Purple,
                                                        start = Offset(size.width / 2, 0f),
                                                        end = Offset(size.width / 2, size.height),
                                                        strokeWidth = 3f
                                                    )
                                                    // Central dot node representing commit
                                                    drawCircle(
                                                        color = if (isSelected) DevHiveColors.Accent else DevHiveColors.Purple,
                                                        radius = 6f
                                                    )
                                                }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = commit.message,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DevHiveColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Hash: [${commit.commitHash}]",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = DevHiveColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // RIGHT COLUMN: File Diffs visualizer
                    Column(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                        Text("VCS FILE DIFF PREVIEW", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DevHiveColors.EditorBackground),
                            modifier = Modifier.fillMaxSize(),
                            border = BorderStroke(1.dp, DevHiveColors.Border)
                        ) {
                            if (viewModel.selectedCommitDiffs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select commit node to preview", fontSize = 10.sp, color = DevHiveColors.TextSecondary)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    items(viewModel.selectedCommitDiffs) { diff ->
                                        Text(text = "File: ${diff.filePath}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DevHiveColors.Accent)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        diff.removedLines.forEach { line ->
                                            Text(text = line, fontSize = 10.sp, color = DevHiveColors.Red, fontFamily = FontFamily.Monospace)
                                        }
                                        diff.addedLines.forEach { line ->
                                            Text(text = line, fontSize = 10.sp, color = DevHiveColors.Green, fontFamily = FontFamily.Monospace)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = DevHiveColors.SurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = DevHiveColors.Border)
                Spacer(modifier = Modifier.height(10.dp))

                // BOTTOM PANEL: Active Sandboxed Shell Console
                Text("DEVHIVE ACTIVE SHELL TERMINAL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = DevHiveColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    border = BorderStroke(1.dp, DevHiveColors.Border)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        // Scrolling history console
                        val scrollState = rememberLazyListState()
                        LaunchedEffect(viewModel.terminalHistory.size) {
                            if (viewModel.terminalHistory.isNotEmpty()) {
                                scrollState.animateScrollToItem(viewModel.terminalHistory.size - 1)
                            }
                        }
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(viewModel.terminalHistory) { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (log.startsWith("devhive@android")) DevHiveColors.Accent else if (log.startsWith("  Error")) DevHiveColors.Red else DevHiveColors.Green
                                )
                            }
                        }
                        
                        // Input line
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "devhive@android:~$ ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = DevHiveColors.Accent
                            )
                            BasicTextField(
                                value = terminalInput,
                                onValueChange = { terminalInput = it },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (terminalInput.isNotBlank()) {
                                        viewModel.executeTerminalCommand(terminalInput)
                                        terminalInput = ""
                                    }
                                    focusManager.clearFocus()
                                }),
                                cursorBrush = SolidColor(DevHiveColors.Green),
                                modifier = Modifier.weight(1f).testTag("terminal_input_field")
                            )
                        }
                    }
                }
            }
        }
    }
}

// Extra color extensions
@Composable
fun DevHiveColors.AccentVariant(): Color {
    return Color(0xFF0284C7)
}

@Composable
fun FileManagerScreen(viewModel: DevHiveViewModel) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var renameName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshFmFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DevHiveColors.Background)
            .padding(16.dp)
    ) {
        // Mode Selector: Workspace vs Device Storage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.setFileManagerRoot(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!viewModel.isBrowsingDeviceStorage) DevHiveColors.Accent else DevHiveColors.SurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Workspace Project", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = { viewModel.setFileManagerRoot(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isBrowsingDeviceStorage) DevHiveColors.Accent else DevHiveColors.SurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Device Storage", fontSize = 11.sp, color = Color.White)
            }
        }

        // Navigation header: Up Arrow + Current Path
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DevHiveColors.Surface)
                .border(1.dp, DevHiveColors.Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateFmUp() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Up",
                    tint = DevHiveColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = viewModel.fmCurrentDirectory.absolutePath,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = DevHiveColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New item",
                    tint = DevHiveColors.Green
                )
            }

            IconButton(
                onClick = { viewModel.refreshFmFiles() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = DevHiveColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Files List
        if (viewModel.fmCurrentFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This directory is empty.",
                    color = DevHiveColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(viewModel.fmCurrentFiles) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DevHiveColors.Surface),
                        border = BorderStroke(1.dp, DevHiveColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    viewModel.navigateFmTo(file)
                                } else {
                                    viewModel.openFile(file)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                contentDescription = null,
                                tint = if (file.isDirectory) DevHiveColors.Accent else DevHiveColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DevHiveColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (file.isDirectory) "Folder" else "${file.length() / 1024} KB | ${file.extension.uppercase()}",
                                    fontSize = 10.sp,
                                    color = DevHiveColors.TextSecondary
                                )
                            }

                            // Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // ZIP Button
                                IconButton(
                                    onClick = { viewModel.fmZipItem(file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Archive,
                                        contentDescription = "Zip",
                                        tint = DevHiveColors.Green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = { viewModel.fmShareItem(context, file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = DevHiveColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Rename Button
                                IconButton(
                                    onClick = {
                                        showRenameDialog = file
                                        renameName = file.name
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Rename",
                                        tint = DevHiveColors.Yellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { viewModel.fmDeleteItem(file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = DevHiveColors.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New File/Folder Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Item", color = DevHiveColors.TextPrimary) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isCreatingFolder,
                            onClick = { isCreatingFolder = false }
                        )
                        Text("File", color = DevHiveColors.TextPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = isCreatingFolder,
                            onClick = { isCreatingFolder = true }
                        )
                        Text("Folder", color = DevHiveColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createName.isNotBlank()) {
                            viewModel.fmCreateItem(createName, isCreatingFolder)
                            createName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DevHiveColors.Surface
        )
    }

    // Rename Dialog
    showRenameDialog?.let { targetFile ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Item", color = DevHiveColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameName.isNotBlank() && renameName != targetFile.name) {
                            viewModel.fmRenameItem(targetFile, renameName)
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DevHiveColors.Surface
        )
    }
}

