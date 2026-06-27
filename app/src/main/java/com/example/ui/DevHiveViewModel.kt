package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DBConnection
import com.example.data.DevHiveDatabase
import com.example.data.DevHiveRepository
import com.example.data.SavedApiRequest
import com.example.data.VcsCommit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// Supporting classes
data class FileItem(
    val file: File,
    val isDirectory: Boolean,
    val name: String,
    val level: Int,
    var isExpanded: Boolean = false
)

data class FileDiff(
    val filePath: String,
    val status: String, // ADDED, MODIFIED, DELETED
    val addedLines: List<String> = emptyList(),
    val removedLines: List<String> = emptyList()
)

data class FileSearchResult(
    val file: File,
    val lineNumber: Int,
    val lineContent: String
)

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val port: String,
    var status: String, // RUNNING, STOPPED
    var cpu: Float = 0f,
    var ram: String = "0 MB"
)

data class PortScanResult(
    val port: Int,
    val service: String,
    val isOpen: Boolean
)

class DevHiveViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DevHiveDatabase.getDatabase(application)
    private val repository = DevHiveRepository(db.devHiveDao())

    // Workspace Folder
    val workspaceDir = File(application.filesDir, "DevHiveWorkspace")

    // --- State: Workspace Files ---
    var currentFiles = mutableStateListOf<FileItem>()
        private set

    // --- State: Global Search ---
    var globalSearchQuery by mutableStateOf("")
    val globalSearchResults = mutableStateListOf<FileSearchResult>()

    var openTabs = mutableStateListOf<File>()
        private set

    var activeTab by mutableStateOf<File?>(null)

    var activeFileContent by mutableStateOf("")

    // --- State: Room flows ---
    val savedConnections: StateFlow<List<DBConnection>> = repository.connections
        .asStateFlowState(emptyList())

    val savedApiRequests: StateFlow<List<SavedApiRequest>> = repository.apiRequests
        .asStateFlowState(emptyList())

    val gitCommits: StateFlow<List<VcsCommit>> = repository.commits
        .asStateFlowState(emptyList())

    // --- State: API Tester ---
    var apiMethod by mutableStateOf("GET")
    var apiUrl by mutableStateOf("https://httpbin.org/get")
    var apiHeaders = mutableStateListOf<Pair<String, String>>()
    var apiBody by mutableStateOf("{\n  \"name\": \"DevHive Client\"\n}")
    var isApiSending by mutableStateOf(false)

    var apiResponseStatus by mutableStateOf("")
    var apiResponseTime by mutableStateOf("")
    var apiResponseSize by mutableStateOf("")
    var apiResponseBody by mutableStateOf("")
    val apiResponseHeaders = mutableStateListOf<Pair<String, String>>()

    // --- State: Local Server ---
    var isServerRunning by mutableStateOf(false)
    var serverPort by mutableStateOf(8080)
    val serverLogs = mutableStateListOf<String>()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // --- State: MySQL Server Stack ---
    var isMySqlRunning by mutableStateOf(false)
    private var mysqlSocket: ServerSocket? = null
    private var mysqlJob: Job? = null

    // --- State: Database Manager ---
    var activeDbConnection by mutableStateOf<DBConnection?>(null)
    val dbTables = mutableStateListOf<String>()
    var selectedTable by mutableStateOf<String?>(null)
    val tableColumns = mutableStateListOf<String>()
    val tableRows = mutableStateListOf<List<String>>()

    var dbConsoleQuery by mutableStateOf("SELECT name, tbl_name FROM sqlite_master WHERE type='table';")
    val dbConsoleColumns = mutableStateListOf<String>()
    val dbConsoleRows = mutableStateListOf<List<String>>()
    var dbConsoleMessage by mutableStateOf("")
    var dbConsoleError by mutableStateOf("")

    private var activeSQLiteDb: SQLiteDatabase? = null

    // --- State: Local VCS (Git Simulator) ---
    val stagedFiles = mutableStateListOf<File>()
    val unstagedFiles = mutableStateListOf<File>()
    var commitMessage by mutableStateOf("")
    var selectedCommitDiffs = mutableStateListOf<FileDiff>()
    var selectedCommitHash by mutableStateOf("")

    // --- State: Docker Simulator ---
    var dockerConnected by mutableStateOf(true)
    val dockerContainers = mutableStateListOf<DockerContainer>()
    val dockerLogs = mutableStateListOf<String>()
    private var dockerJob: Job? = null

    // --- State: Terminal Console ---
    val terminalHistory = mutableStateListOf<String>()
    var terminalDirectory by mutableStateOf<File>(workspaceDir)

    // --- State: Port Scanner ---
    val scannedPorts = mutableStateListOf<PortScanResult>()
    var isPortScanning by mutableStateOf(false)

    init {
        // Prepare local directory
        initWorkspace()
        refreshFiles()
        initSavedState()
        initDockerMock()
        
        // Add greeting to Terminal
        terminalHistory.add("DevHive OS Terminal v1.0 [Ready]")
        terminalHistory.add("Type 'help' for available commands. Current path: ~/workspace")
    }

    // Pre-populate flows to StateFlow helper
    private fun <T> Flow<T>.asStateFlowState(initial: T): StateFlow<T> {
        val flow = MutableStateFlow(initial)
        viewModelScope.launch {
            collect { flow.value = it }
        }
        return flow.asStateFlow()
    }

    private fun initSavedState() {
        viewModelScope.launch {
            // Seed a default SQLite connection pointing to DevHive's database!
            repository.connections.collect { list ->
                if (list.isEmpty()) {
                    repository.saveConnection(
                        DBConnection(
                            name = "DevHive Internals (SQLite)",
                            type = "SQLite",
                            host = "Local",
                            port = 0,
                            user = "dev",
                            databaseName = "devhive_database"
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            // Seed default API requests
            repository.apiRequests.collect { list ->
                if (list.isEmpty()) {
                    repository.saveApiRequest(
                        SavedApiRequest(
                            collectionName = "Default Examples",
                            name = "Get HTTP Details",
                            method = "GET",
                            url = "https://httpbin.org/get",
                            headersJson = "[]",
                            body = ""
                        )
                    )
                    repository.saveApiRequest(
                        SavedApiRequest(
                            collectionName = "Default Examples",
                            name = "Post JSON Payload",
                            method = "POST",
                            url = "https://httpbin.org/post",
                            headersJson = "[\"Content-Type:application/json\"]",
                            body = "{\n  \"title\": \"DevHive Post\",\n  \"platform\": \"Android\"\n}"
                        )
                    )
                }
            }
        }
    }

    // --- 1. Workspace Logic ---
    private fun initWorkspace() {
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }

        // Add some premium sample files if the workspace is empty
        val welcomeFile = File(workspaceDir, "welcome_notes.txt")
        if (!welcomeFile.exists()) {
            welcomeFile.writeText(
                """==================================================
  🐝 DEVHIVE: ALL-IN-ONE MOBILE WORKSTATION 🐝
==================================================

Welcome to your offline developer workspace on Android! 

This app simulates a full desktop developer IDE in the palm of your hand. 
Here are some interactive things you can do right now:

1. HTML/JS LIVE PREVIEW:
   - Edit the 'index.html', 'style.css', or 'script.js' files in this folder.
   - Go to the 'SERVER' panel and click 'Start Server'.
   - Turn on 'Live Preview' to render your files in a live, in-app browser!
   - This starts a real background Socket HTTP Web Server serving port 8080!

2. SQL CONSOLE & DB BROWSER:
   - Connect to 'DevHive Internals' in the 'DATABASE' panel.
   - Run raw SQL queries against this app's own SQLite Room database!
   - Run: SELECT * FROM db_connections;
   - You can edit connections, logs, and more dynamically!

3. REAL REST API TESTER:
   - Go to 'API' tab, select POST or GET, and tap 'Send'.
   - Enter headers, request payloads, and view formatted, colored JSON!

4. LOCAL VCS (GIT SIMULATOR):
   - Modify a file (e.g. style.css) and go to the 'VCS' panel.
   - Stage changes, write a commit message, and commit!
   - View your timeline commitments in a gorgeous node graph tree!

5. PORT SCANNER:
   - Scan standard development ports on localhost or any IP!

6. ACTIVE SHELL TERMINAL:
   - Execute real local terminal commands! Type 'ls', 'pwd', or 'date'.

Crafted with premium Jetpack Compose design styling. Enjoy!
"""
            )
            
            val htmlFile = File(workspaceDir, "index.html")
            htmlFile.writeText(
                """<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DevHive Live Preview</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="card">
        <h1>🐝 DevHive Live Server</h1>
        <p class="subtitle">Served directly from your Android phone's local storage!</p>
        <div class="status-badge">
            <span class="dot pulse"></span>
            <span class="status-text">Server Status: Online</span>
        </div>
        <p class="description">Edit index.html or style.css in the DevHive Editor, save, and refresh this preview to see live styling updates.</p>
        <button id="actionBtn" class="btn">Tap Me</button>
        <p id="msg" class="hidden-msg">Awesome! Live JavaScript is fully active!</p>
    </div>
    <script src="script.js"></script>
</body>
</html>
"""
            )

            val cssFile = File(workspaceDir, "style.css")
            cssFile.writeText(
                """body {
    background-color: #0F172A;
    color: #F8FAFC;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 90vh;
    margin: 0;
    padding: 20px;
}

.card {
    background-color: #1E293B;
    border: 1px solid #334155;
    border-radius: 16px;
    padding: 30px;
    max-width: 400px;
    text-align: center;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
}

h1 {
    color: #38BDF8;
    margin-top: 0;
    font-size: 24px;
}

.subtitle {
    color: #94A3B8;
    font-size: 14px;
    margin-bottom: 20px;
}

.status-badge {
    display: inline-flex;
    align-items: center;
    background-color: #065F46;
    border: 1px solid #059669;
    border-radius: 9999px;
    padding: 6px 12px;
    margin-bottom: 20px;
}

.dot {
    width: 8px;
    height: 8px;
    background-color: #34D399;
    border-radius: 50%;
    margin-right: 8px;
}

.pulse {
    animation: pulse-animation 1.5s infinite;
}

@keyframes pulse-animation {
    0% { transform: scale(0.9); opacity: 0.8; }
    50% { transform: scale(1.2); opacity: 1; }
    100% { transform: scale(0.9); opacity: 0.8; }
}

.status-text {
    color: #A7F3D0;
    font-size: 12px;
    font-weight: 600;
}

.description {
    color: #CBD5E1;
    font-size: 14px;
    line-height: 1.6;
}

.btn {
    background-color: #38BDF8;
    color: #0F172A;
    border: none;
    border-radius: 8px;
    padding: 10px 20px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s;
    margin-top: 15px;
}

.btn:active {
    background-color: #0EA5E9;
}

.hidden-msg {
    display: none;
    color: #34D399;
    font-size: 13px;
    margin-top: 15px;
    font-style: italic;
}
"""
            )

            val jsFile = File(workspaceDir, "script.js")
            jsFile.writeText(
                """document.getElementById('actionBtn').addEventListener('click', function() {
    var msg = document.getElementById('msg');
    if (msg.style.display === 'block') {
        msg.style.display = 'none';
    } else {
        msg.style.display = 'block';
    }
});
"""
            )

            val sqlFile = File(workspaceDir, "analytics.sql")
            sqlFile.writeText(
                """-- Analytical Workspace Queries
-- Inspect the tables and check connections metadata
SELECT * FROM sqlite_master WHERE type = 'table';

-- Query internal database logs
SELECT * FROM db_connections ORDER BY name ASC;
"""
            )

            val pyFile = File(workspaceDir, "app.py")
            pyFile.writeText(
                """# DevHive Sample Python Backend Controller
def calculate_network_latency(ping_ms, hops):
    print("Analyzing DevHive server hops...")
    base_overhead = 1.2
    return (ping_ms * base_overhead) + (hops * 0.5)

# Simulate call
latency = calculate_network_latency(45.2, 4)
print(f"Calculated Latency: {latency} ms")
"""
            )

            val indexPhpFile = File(workspaceDir, "index.php")
            indexPhpFile.writeText(
                """<?php
echo "<h1>🐝 Welcome to DevHive PHP & MySQL Server Stack!</h1>";
echo "<p>Running live on Android port 8080!</p>";
${'$'}host = "127.0.0.1";
${'$'}user = "root";
${'$'}password = "devhive123";
echo "<ul>";
echo "<li><strong>Database Host:</strong> " . ${'$'}host . ":3306</li>";
echo "<li><strong>User:</strong> " . ${'$'}user . "</li>";
echo "<li><strong>Server Type:</strong> Integrated MariaDB/MySQL Sandbox Engine</li>";
echo "</ul>";

echo "<h3>Executing real SQL query via PHP:</h3>";
// Query our real database connections!
${'$'}db = new PDO("sqlite:devhive_database");
${'$'}results = ${'$'}db->query("SELECT name, host, user, databaseName FROM db_connections");
echo "<table border='1' cellpadding='8' style='border-collapse: collapse; background-color: #1e293b; color: white;'>";
echo "<tr><th>Connection Name</th><th>Host</th><th>User</th><th>Database Name</th></tr>";
foreach (${'$'}results as ${'$'}row) {
    echo "<tr>";
    echo "<td>" . ${'$'}row['name'] . "</td>";
    echo "<td>" . ${'$'}row['host'] . "</td>";
    echo "<td>" . ${'$'}row['user'] . "</td>";
    echo "<td>" . ${'$'}row['databaseName'] . "</td>";
    echo "</tr>";
}
echo "</table>";
?>
"""
            )

            val dbDemoPhpFile = File(workspaceDir, "database_demo.php")
            dbDemoPhpFile.writeText(
                """<?php
echo "<h2>MySQL & SQLite Dynamic Demo</h2>";
${'$'}db = new PDO("sqlite:devhive_database");

echo "<h4>Inserting a new server connection log entry...</h4>";
${'$'}db->query("INSERT INTO db_connections (name, type, host, port, user, databaseName) VALUES ('PHP Dynamic Conn', 'MySQL', '192.168.1.100', 3306, 'php_user', 'php_schema')");

echo "<p style='color: #10b981;'>Row inserted successfully! Reading all rows:</p>";
${'$'}list = ${'$'}db->query("SELECT name, type, host, user FROM db_connections");
foreach (${'$'}list as ${'$'}row) {
    echo "<div>🔌 <b>" . ${'$'}row['name'] . "</b> (" . ${'$'}row['type'] . ") at " . ${'$'}row['user'] . "@" . ${'$'}row['host'] . "</div>";
}
?>
"""
            )
        }
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val rootList = workspaceDir.listFiles() ?: emptyArray()
            // Sort: directories first, then alphabetical
            val sorted = rootList.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            
            val list = sorted.map {
                FileItem(
                    file = it,
                    isDirectory = it.isDirectory,
                    name = it.name,
                    level = 0,
                    isExpanded = false
                )
            }
            withContext(Dispatchers.Main) {
                currentFiles.clear()
                currentFiles.addAll(list)
                checkVcsChanges() // Refresh Git changes as files are modified!
            }
        }
    }

    fun openFile(file: File) {
        if (file.isDirectory) return
        viewModelScope.launch(Dispatchers.IO) {
            val content = try {
                file.readText()
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
            withContext(Dispatchers.Main) {
                if (!openTabs.contains(file)) {
                    openTabs.add(file)
                }
                activeTab = file
                activeFileContent = content
            }
        }
    }

    fun closeTab(file: File) {
        val index = openTabs.indexOf(file)
        if (index != -1) {
            openTabs.removeAt(index)
            if (activeTab == file) {
                activeTab = if (openTabs.isNotEmpty()) {
                    openTabs[maxOf(0, index - 1)]
                } else {
                    null
                }
                activeTab?.let { openFile(it) } ?: run { activeFileContent = "" }
            }
        }
    }

    fun saveActiveFile(content: String) {
        val file = activeTab ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(content)
                withContext(Dispatchers.Main) {
                    activeFileContent = content
                    checkVcsChanges()
                }
            } catch (e: Exception) {
                Log.e("DevHive", "Failed to save file", e)
            }
        }
    }

    fun createWorkspaceFile(name: String, extension: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = if (name.contains(".")) name else "$name.$extension"
            val newFile = File(workspaceDir, fileName)
            try {
                if (!newFile.exists()) {
                    newFile.createNewFile()
                    newFile.writeText("// New $extension Workspace File")
                }
                refreshFiles()
            } catch (e: Exception) {
                Log.e("DevHive", "File creation failed", e)
            }
        }
    }

    fun deleteWorkspaceFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.deleteRecursively()
                withContext(Dispatchers.Main) {
                    closeTab(file)
                    refreshFiles()
                }
            } catch (e: Exception) {
                Log.e("DevHive", "File delete failed", e)
            }
        }
    }

    // --- State: Dynamic File Manager (For Workspace and External Device Storage) ---
    var isBrowsingDeviceStorage by mutableStateOf(false)
    var fmCurrentDirectory by mutableStateOf<File>(workspaceDir)
    val fmCurrentFiles = mutableStateListOf<File>()

    fun setFileManagerRoot(deviceStorage: Boolean) {
        isBrowsingDeviceStorage = deviceStorage
        val root = if (deviceStorage) {
            android.os.Environment.getExternalStorageDirectory()
        } else {
            workspaceDir
        }
        fmCurrentDirectory = root
        refreshFmFiles()
    }

    fun navigateFmTo(dir: File) {
        if (dir.isDirectory) {
            fmCurrentDirectory = dir
            refreshFmFiles()
        }
    }

    fun navigateFmUp() {
        val parent = fmCurrentDirectory.parentFile
        if (parent != null) {
            val limitRoot = if (isBrowsingDeviceStorage) {
                android.os.Environment.getExternalStorageDirectory().parentFile ?: android.os.Environment.getExternalStorageDirectory()
            } else {
                workspaceDir
            }
            if (fmCurrentDirectory.absolutePath != limitRoot.absolutePath) {
                fmCurrentDirectory = parent
                refreshFmFiles()
            }
        }
    }

    fun refreshFmFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = fmCurrentDirectory.listFiles() ?: emptyArray()
            val sorted = list.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            withContext(Dispatchers.Main) {
                fmCurrentFiles.clear()
                fmCurrentFiles.addAll(sorted)
            }
        }
    }

    fun fmCreateItem(name: String, isFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = File(fmCurrentDirectory, name)
            try {
                if (isFolder) {
                    target.mkdirs()
                } else {
                    target.createNewFile()
                    target.writeText("// New File created in File Manager\n")
                }
                refreshFmFiles()
                refreshFiles() // sync workspace if inside workspace
            } catch (e: Exception) {
                Log.e("DevHive", "FM Item Creation failed", e)
            }
        }
    }

    fun fmDeleteItem(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.deleteRecursively()
                withContext(Dispatchers.Main) {
                    closeTab(file)
                    refreshFmFiles()
                    refreshFiles() // sync workspace
                }
            } catch (e: Exception) {
                Log.e("DevHive", "FM Item Delete failed", e)
            }
        }
    }

    fun fmRenameItem(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dest = File(file.parentFile, newName)
                if (file.renameTo(dest)) {
                    withContext(Dispatchers.Main) {
                        refreshFmFiles()
                        refreshFiles() // sync workspace
                    }
                }
            } catch (e: Exception) {
                Log.e("DevHive", "FM Item Rename failed", e)
            }
        }
    }

    fun fmZipItem(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipFile = File(file.parentFile, "${file.nameWithoutExtension}.zip")
                java.io.FileOutputStream(zipFile).use { fos ->
                    java.util.zip.ZipOutputStream(fos).use { zos ->
                        if (file.isDirectory) {
                            addFolderToZip("", file, zos)
                        } else {
                            val buf = ByteArray(1024)
                            java.io.FileInputStream(file).use { fis ->
                                zos.putNextEntry(java.util.zip.ZipEntry(file.name))
                                var len: Int
                                while (fis.read(buf).also { len = it } > 0) {
                                    zos.write(buf, 0, len)
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    refreshFmFiles()
                    refreshFiles()
                }
            } catch (e: Exception) {
                Log.e("DevHive", "Zipping item failed", e)
            }
        }
    }

    private fun addFolderToZip(path: String, srcFolder: File, zip: java.util.zip.ZipOutputStream) {
        val list = srcFolder.listFiles() ?: return
        for (file in list) {
            val entryPath = if (path.isEmpty()) file.name else "$path/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(entryPath, file, zip)
            } else {
                val buf = ByteArray(1024)
                try {
                    java.io.FileInputStream(file).use { input ->
                        zip.putNextEntry(java.util.zip.ZipEntry(entryPath))
                        var len: Int
                        while (input.read(buf).also { len = it } > 0) {
                            zip.write(buf, 0, len)
                        }
                    }
                } catch (e: Exception) {
                    // Skip unreadable files gracefully
                }
            }
        }
    }

    fun fmShareItem(context: android.content.Context, file: File) {
        try {
            val authority = "${context.packageName}.provider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (file.extension == "zip") "application/zip" else "*/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share via DevHive"))
        } catch (e: Exception) {
            Log.e("DevHive", "FM Share Item failed", e)
        }
    }

    // --- 2. REST API Client Engine ---
    fun addHeaderField() {
        apiHeaders.add(Pair("", ""))
    }

    fun removeHeaderField(index: Int) {
        if (index in apiHeaders.indices) {
            apiHeaders.removeAt(index)
        }
    }

    fun updateHeaderField(index: Int, key: String, value: String) {
        if (index in apiHeaders.indices) {
            apiHeaders[index] = Pair(key, value)
        }
    }

    fun sendApiRequest() {
        if (isApiSending) return
        isApiSending = true
        apiResponseStatus = "Sending..."
        apiResponseBody = ""
        apiResponseTime = ""
        apiResponseSize = ""
        apiResponseHeaders.clear()

        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder().url(apiUrl)

            // Add Headers
            apiHeaders.forEach { (key, value) ->
                if (key.isNotBlank()) {
                    requestBuilder.addHeader(key.trim(), value.trim())
                }
            }

            // Body
            if (apiMethod != "GET" && apiMethod != "DELETE" && apiMethod != "HEAD") {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                requestBuilder.method(apiMethod, apiBody.toRequestBody(mediaType))
            } else {
                requestBuilder.method(apiMethod, null)
            }

            val startTime = System.currentTimeMillis()
            try {
                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    val responseBytes = response.body?.bytes() ?: byteArrayOf()
                    val responseString = String(responseBytes)

                    withContext(Dispatchers.Main) {
                        apiResponseStatus = "${response.code} ${response.message}"
                        apiResponseTime = "$duration ms"
                        apiResponseSize = "${responseBytes.size} B"
                        
                        // Parse Headers
                        response.headers.forEach { pair ->
                            apiResponseHeaders.add(Pair(pair.first, pair.second))
                        }

                        // Format Response Body (JSON or plain text)
                        apiResponseBody = try {
                            prettyPrintJson(responseString)
                        } catch (e: Exception) {
                            responseString
                        }
                        isApiSending = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    apiResponseStatus = "Connection Failure"
                    apiResponseBody = "Error: ${e.message ?: "Unknown Connection Host Failure"}"
                    isApiSending = false
                }
            }
        }
    }

    fun saveApiRequestToDb(name: String, collection: String) {
        viewModelScope.launch {
            // Serialize headers list to basic string (e.g. Header1:Value1,Header2:Value2)
            val list = apiHeaders.filter { it.first.isNotBlank() }.map { "${it.first}:${it.second}" }
            val headersSerialized = list.joinToString(";")
            
            repository.saveApiRequest(
                SavedApiRequest(
                    collectionName = collection.ifBlank { "My Collection" },
                    name = name.ifBlank { "API Request" },
                    method = apiMethod,
                    url = apiUrl,
                    headersJson = headersSerialized,
                    body = apiBody
                )
            )
        }
    }

    fun loadSavedApiRequest(request: SavedApiRequest) {
        apiMethod = request.method
        apiUrl = request.url
        apiBody = request.body
        apiHeaders.clear()
        
        if (request.headersJson.isNotBlank()) {
            val split = request.headersJson.split(";")
            split.forEach { h ->
                val hSplit = h.split(":", limit = 2)
                if (hSplit.size == 2) {
                    apiHeaders.add(Pair(hSplit[0], hSplit[1]))
                }
            }
        }
    }

    fun deleteApiRequestFromDb(id: Int) {
        viewModelScope.launch {
            repository.deleteApiRequest(id)
        }
    }

    private fun prettyPrintJson(rawJson: String): String {
        var indentLevel = 0
        val indent = "  "
        val result = StringBuilder()
        var inQuotes = false
        
        var i = 0
        while (i < rawJson.length) {
            val char = rawJson[i]
            when (char) {
                '"' -> {
                    inQuotes = !inQuotes
                    result.append(char)
                }
                '{', '[' -> {
                    result.append(char)
                    if (!inQuotes) {
                        indentLevel++
                        result.append("\n")
                        result.append(indent.repeat(indentLevel))
                    }
                }
                '}', ']' -> {
                    if (!inQuotes) {
                        indentLevel--
                        result.append("\n")
                        result.append(indent.repeat(indentLevel))
                    }
                    result.append(char)
                }
                ',' -> {
                    result.append(char)
                    if (!inQuotes) {
                        result.append("\n")
                        result.append(indent.repeat(indentLevel))
                    }
                }
                ':' -> {
                    result.append(char)
                    if (!inQuotes) {
                        result.append(" ")
                    }
                }
                else -> {
                    if (char != ' ' || inQuotes) {
                        result.append(char)
                    }
                }
            }
            i++
        }
        return result.toString().trim()
    }

    // --- 3. SQLite Database Browser Engine ---
    fun connectToSQLite(connection: DBConnection) {
        try {
            activeSQLiteDb?.close()
        } catch (e: Exception) {}

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If it's the internal db, load actual application room db!
                val dbFile = if (connection.databaseName == "devhive_database") {
                    getApplication<Application>().getDatabasePath("devhive_database")
                } else {
                    File(workspaceDir, connection.databaseName)
                }

                val sqlDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                withContext(Dispatchers.Main) {
                    activeSQLiteDb = sqlDb
                    activeDbConnection = connection
                    refreshDbTables()
                    dbConsoleError = ""
                    dbConsoleMessage = "Connected to database: ${connection.name}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dbConsoleError = "DB Connect Error: ${e.message}"
                }
            }
        }
    }

    fun disconnectDb() {
        activeSQLiteDb?.close()
        activeSQLiteDb = null
        activeDbConnection = null
        dbTables.clear()
        selectedTable = null
        tableColumns.clear()
        tableRows.clear()
    }

    private fun refreshDbTables() {
        val db = activeSQLiteDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tablesList = mutableListOf<String>()
            try {
                val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence';", null)
                if (cursor.moveToFirst()) {
                    do {
                        tablesList.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
                cursor.close()
                withContext(Dispatchers.Main) {
                    dbTables.clear()
                    dbTables.addAll(tablesList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dbConsoleError = "Failed to list tables: ${e.message}"
                }
            }
        }
    }

    fun loadTableRows(tableName: String) {
        val db = activeSQLiteDb ?: return
        selectedTable = tableName
        tableColumns.clear()
        tableRows.clear()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cursor = db.rawQuery("SELECT * FROM `$tableName` LIMIT 100;", null)
                val columns = cursor.columnNames.toList()
                val rows = mutableListOf<List<String>>()
                
                if (cursor.moveToFirst()) {
                    do {
                        val row = mutableListOf<String>()
                        for (i in 0 until cursor.columnCount) {
                            row.add(cursor.getString(i) ?: "NULL")
                        }
                        rows.add(row)
                    } while (cursor.moveToNext())
                }
                cursor.close()

                withContext(Dispatchers.Main) {
                    tableColumns.addAll(columns)
                    tableRows.addAll(rows)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dbConsoleError = "Error loading rows: ${e.message}"
                }
            }
        }
    }

    fun executeSqlStatement() {
        val db = activeSQLiteDb ?: run {
            dbConsoleError = "No active SQLite connection."
            return
        }
        val sql = dbConsoleQuery.trim()
        if (sql.isBlank()) return

        dbConsoleColumns.clear()
        dbConsoleRows.clear()
        dbConsoleError = ""
        dbConsoleMessage = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (sql.uppercase().startsWith("SELECT") || sql.uppercase().startsWith("PRAGMA") || sql.uppercase().startsWith("EXPLAIN")) {
                    val cursor = db.rawQuery(sql, null)
                    val cols = cursor.columnNames.toList()
                    val rows = mutableListOf<List<String>>()
                    
                    if (cursor.moveToFirst()) {
                        do {
                            val r = mutableListOf<String>()
                            for (i in 0 until cursor.columnCount) {
                                r.add(cursor.getString(i) ?: "NULL")
                            }
                            rows.add(r)
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                    withContext(Dispatchers.Main) {
                        dbConsoleColumns.addAll(cols)
                        dbConsoleRows.addAll(rows)
                        dbConsoleMessage = "Query executed successfully: ${rows.size} rows found."
                    }
                } else {
                    db.execSQL(sql)
                    withContext(Dispatchers.Main) {
                        dbConsoleMessage = "DDL/DML command completed successfully."
                        refreshDbTables()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dbConsoleError = e.message ?: "SQL Execution Error"
                }
            }
        }
    }

    fun saveConnectionToDb(name: String, type: String, host: String, port: Int, user: String, databaseName: String) {
        viewModelScope.launch {
            repository.saveConnection(
                DBConnection(
                    name = name,
                    type = type,
                    host = host,
                    port = port,
                    user = user,
                    databaseName = databaseName
                )
            )
        }
    }

    fun deleteConnectionFromDb(id: Int) {
        viewModelScope.launch {
            repository.deleteConnection(id)
        }
    }

    // --- 4. Web Server & Browser Simulator ---
    fun toggleWebServer() {
        if (isServerRunning) {
            stopWebServer()
        } else {
            startWebServer()
        }
    }

    private fun startWebServer() {
        if (isServerRunning) return
        isServerRunning = true
        serverLogs.add("[System] Initializing DevHive Local Web Server...")
        sendSystemNotification(
            "DevHive HTTP Web Server Started",
            "PHP-MySQL Server online on: http://localhost:$serverPort [PHP 8.2 Engine Active]"
        )
        
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = ServerSocket(serverPort)
                serverSocket = socket
                withContext(Dispatchers.Main) {
                    serverLogs.add("[System] Web Server active on: http://localhost:$serverPort")
                    serverLogs.add("[System] Serving root directory: DevHiveWorkspace/")
                }

                while (isServerRunning) {
                    val clientSocket = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }
                    handleServerClient(clientSocket)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    serverLogs.add("[Error] Server socket crashed: ${e.localizedMessage}")
                    isServerRunning = false
                }
            }
        }
    }

    private fun handleServerClient(clientSocket: Socket) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val out: OutputStream = clientSocket.getOutputStream()
                val writer = PrintWriter(out)

                val requestLine = reader.readLine() ?: ""
                if (requestLine.isNotBlank()) {
                    val split = requestLine.split(" ")
                    if (split.size >= 2) {
                        val method = split[0]
                        var path = split[1]

                        // Strip queries
                        if (path.contains("?")) {
                            path = path.substring(0, path.indexOf("?"))
                        }

                        if (path == "/") path = "/index.html"

                        val targetFile = File(workspaceDir, path.removePrefix("/"))
                        if (targetFile.exists() && !targetFile.isDirectory) {
                            val isPhp = targetFile.extension.lowercase() == "php"
                            val contentType = if (isPhp) "text/html" else getMimeType(targetFile.extension)
                            val contentBytes = if (isPhp) {
                                val phpContent = targetFile.readText()
                                executePhpScript(phpContent).toByteArray(kotlin.text.Charsets.UTF_8)
                            } else {
                                targetFile.readBytes()
                            }

                            writer.println("HTTP/1.1 200 OK")
                            writer.println("Server: DevHive Android PHP-MySQL Server Stack")
                            writer.println("Content-Type: $contentType")
                            writer.println("Content-Length: ${contentBytes.size}")
                            writer.println("Connection: close")
                            writer.println()
                            writer.flush()
                            out.write(contentBytes)
                            out.flush()

                            withContext(Dispatchers.Main) {
                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                serverLogs.add("[$timestamp] $method $path -> 200 OK (${contentBytes.size} Bytes) [PHP Engine Active]")
                            }
                        } else {
                            val errorMsg = "<h1>404 File Not Found</h1><p>The developer file '$path' does not exist in workspace.</p>"
                            writer.println("HTTP/1.1 404 Not Found")
                            writer.println("Content-Type: text/html")
                            writer.println("Content-Length: ${errorMsg.length}")
                            writer.println("Connection: close")
                            writer.println()
                            writer.println(errorMsg)
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                serverLogs.add("[$timestamp] $method $path -> 404 NOT FOUND")
                            }
                        }
                    }
                }
                clientSocket.close()
            } catch (e: Exception) {
                Log.e("DevHiveServer", "Error serving client", e)
            }
        }
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "php" -> "text/html"
            else -> "text/plain"
        }
    }

    // --- Integrated PHP Interpreter & SQLite/MySQL Bridge for fully functional local files ---
    fun executePhpScript(scriptContent: String): String {
        val output = java.lang.StringBuilder()
        var index = 0
        val len = scriptContent.length

        // Variable store
        val variables = mutableMapOf<String, Any>()
        variables["_SERVER"] = mapOf("SERVER_SOFTWARE" to "DevHive PHP/8.2.12 Engine (Android)", "REQUEST_METHOD" to "GET")
        variables["db_host"] = "127.0.0.1"
        variables["db_port"] = "3306"
        variables["db_user"] = "root"
        variables["db_pass"] = "devhive_secure_root"
        variables["db_name"] = "devhive_database"

        while (index < len) {
            val phpStart = scriptContent.indexOf("<?php", index)
            if (phpStart == -1) {
                output.append(scriptContent.substring(index))
                break
            }

            output.append(scriptContent.substring(index, phpStart))

            var phpEnd = scriptContent.indexOf("?>", phpStart)
            if (phpEnd == -1) {
                phpEnd = len
            }

            val codeBlock = scriptContent.substring(phpStart + 5, phpEnd).trim()
            index = if (phpEnd == len) len else phpEnd + 2

            // Run code block
            output.append(executePhpScriptBlock(codeBlock, variables))
        }
        return output.toString()
    }

    private fun executePhpScriptBlock(code: String, variables: MutableMap<String, Any>): String {
        var processedCode = code
        val foreachRegex = """foreach\s*\(\s*(\$\w+)\s+as\s+(\$\w+)\s*\)\s*\{([^}]+)\}""".toRegex()
        
        // Loop execution handler
        var match = foreachRegex.find(processedCode)
        while (match != null) {
            val fullMatch = match.value
            val collVarName = match.groupValues[1].removePrefix("$")
            val itemVarName = match.groupValues[2].removePrefix("$")
            val loopBody = match.groupValues[3]
            
            val collection = variables[collVarName]
            val loopOutput = java.lang.StringBuilder()
            
            if (collection is List<*>) {
                for (item in collection) {
                    if (item != null) {
                        val scopedVars = variables.toMutableMap()
                        scopedVars[itemVarName] = item
                        
                        val bodyStmts = parsePhpStatements(loopBody)
                        for (stmt in bodyStmts) {
                            val trimmedStmt = stmt.trim()
                            if (trimmedStmt.isEmpty()) continue
                            
                            if (trimmedStmt.startsWith("echo")) {
                                val expr = trimmedStmt.removePrefix("echo").trim()
                                val valStr = evaluatePhpExpression(expr, scopedVars)
                                loopOutput.append(valStr)
                            } else if (trimmedStmt.startsWith("$")) {
                                val eqIdx = trimmedStmt.indexOf("=")
                                if (eqIdx != -1) {
                                    val vName = trimmedStmt.substring(0, eqIdx).trim().removePrefix("$")
                                    val expr = trimmedStmt.substring(eqIdx + 1).trim()
                                    scopedVars[vName] = evaluatePhpExpression(expr, scopedVars)
                                }
                            }
                        }
                    }
                }
            }
            
            // Replace foreach chunk with an echo of the pre-processed output
            val safeOutput = loopOutput.toString().replace("\"", "\\\"").replace("\n", "\\n")
            processedCode = processedCode.replace(fullMatch, "echo \"" + safeOutput + "\";")
            match = foreachRegex.find(processedCode)
        }

        // Run sequential statements
        val statements = parsePhpStatements(processedCode)
        val finalOutput = java.lang.StringBuilder()
        for (stmt in statements) {
            val trimmed = stmt.trim()
            if (trimmed.isEmpty()) continue
            try {
                if (trimmed.startsWith("echo")) {
                    val expr = trimmed.removePrefix("echo").trim()
                    val valStr = evaluatePhpExpression(expr, variables)
                    finalOutput.append(valStr)
                } else if (trimmed.startsWith("$")) {
                    val eqIdx = trimmed.indexOf("=")
                    if (eqIdx != -1) {
                        val varName = trimmed.substring(0, eqIdx).trim().removePrefix("$")
                        val expr = trimmed.substring(eqIdx + 1).trim()
                        
                        if (expr.contains("new PDO") || expr.contains("sqlite_open") || expr.contains("mysqli_connect")) {
                            variables[varName] = "DB_CONN"
                        } else if (expr.contains("->query(") || expr.contains("mysqli_query(")) {
                            // Extract sql query
                            val queryStart = expr.indexOf("query(") + 6
                            var queryEnd = expr.lastIndexOf(")")
                            if (queryEnd == -1) queryEnd = expr.length
                            var rawQuery = expr.substring(queryStart, queryEnd).trim()
                            if (rawQuery.startsWith("\"") || rawQuery.startsWith("'")) {
                                rawQuery = rawQuery.substring(1, rawQuery.length - 1)
                            }
                            // Replace variables
                            for ((k, v) in variables) {
                                if (v is String || v is Number) {
                                    rawQuery = rawQuery.replace("$$k", v.toString())
                                }
                            }
                            val rows = runSqlForPhp(rawQuery)
                            variables[varName] = rows
                        } else {
                            variables[varName] = evaluatePhpExpression(expr, variables)
                        }
                    }
                }
            } catch (e: Exception) {
                finalOutput.append("<div style='color: #ef4444; background: #2a1111; padding: 12px; border-radius: 6px;'>")
                finalOutput.append("<b>PHP Execution Error:</b> " + e.localizedMessage)
                finalOutput.append("</div>")
            }
        }
        return finalOutput.toString()
    }

    private fun evaluatePhpExpression(expr: String, variables: Map<String, Any>): String {
        val parts = splitPhpConcat(expr)
        val sb = java.lang.StringBuilder()
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                sb.append(trimmed.substring(1, trimmed.length - 1))
            } else if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
                sb.append(trimmed.substring(1, trimmed.length - 1))
            } else if (trimmed.startsWith("$")) {
                if (trimmed.contains("[")) {
                    val varName = trimmed.substring(0, trimmed.indexOf("[")).removePrefix("$").trim()
                    val keyPart = trimmed.substring(trimmed.indexOf("[") + 1, trimmed.lastIndexOf("]")).trim()
                    val key = if (keyPart.startsWith("'") || keyPart.startsWith("\"")) {
                        keyPart.substring(1, keyPart.length - 1)
                    } else {
                        keyPart
                    }
                    val map = variables[varName]
                    if (map is Map<*, *>) {
                        sb.append(map[key]?.toString() ?: "")
                    } else if (map is List<*>) {
                        val idx = key.toIntOrNull()
                        if (idx != null && idx in map.indices) {
                            val row = map[idx]
                            if (row is Map<*, *>) {
                                sb.append(row["name"] ?: row.toString())
                            } else {
                                sb.append(row?.toString() ?: "")
                            }
                        }
                    }
                } else {
                    val varName = trimmed.removePrefix("$")
                    sb.append(variables[varName]?.toString() ?: "")
                }
            } else {
                sb.append(trimmed)
            }
        }
        return sb.toString()
    }

    private fun splitPhpConcat(expr: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inDoubleQuotes = false
        var inSingleQuotes = false
        var i = 0
        while (i < expr.length) {
            val char = expr[i]
            if (char == '"' && i > 0 && expr[i - 1] != '\\') {
                inDoubleQuotes = !inDoubleQuotes
                current.append(char)
            } else if (char == '\'' && i > 0 && expr[i - 1] != '\\') {
                inSingleQuotes = !inSingleQuotes
                current.append(char)
            } else if (char == '.' && !inDoubleQuotes && !inSingleQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else {
                current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun parsePhpStatements(code: String): List<String> {
        val list = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inDoubleQuotes = false
        var inSingleQuotes = false
        var i = 0
        while (i < code.length) {
            val char = code[i]
            if (char == '"' && i > 0 && code[i - 1] != '\\') {
                inDoubleQuotes = !inDoubleQuotes
                current.append(char)
            } else if (char == '\'' && i > 0 && code[i - 1] != '\\') {
                inSingleQuotes = !inSingleQuotes
                current.append(char)
            } else if (char == ';' && !inDoubleQuotes && !inSingleQuotes) {
                list.add(current.toString())
                current.setLength(0)
            } else {
                current.append(char)
            }
            i++
        }
        if (current.isNotEmpty()) {
            list.add(current.toString())
        }
        return list
    }

    private fun runSqlForPhp(sql: String): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        var db = activeSQLiteDb
        var closedDbAfterQuery = false
        if (db == null) {
            try {
                val dbFile = getApplication<Application>().getDatabasePath("devhive_database")
                db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                closedDbAfterQuery = true
            } catch (e: Exception) {
                Log.e("PHP_SQL", "Failed to auto-open devhive_database", e)
            }
        }

        if (db != null) {
            try {
                val cursor = db.rawQuery(sql, null)
                val cols = cursor.columnNames
                if (cursor.moveToFirst()) {
                    do {
                        val rowMap = mutableMapOf<String, Any>()
                        for (i in cols.indices) {
                            val colName = cols[i]
                            val valStr = cursor.getString(i) ?: ""
                            rowMap[colName] = valStr
                            rowMap[i.toString()] = valStr
                        }
                        results.add(rowMap)
                    } while (cursor.moveToNext())
                }
                cursor.close()
            } catch (e: Exception) {
                Log.e("PHP_SQL", "SQL error in PHP query", e)
                val errMap = mapOf("error" to (e.localizedMessage ?: "SQL error"))
                results.add(errMap)
            } finally {
                if (closedDbAfterQuery) {
                    try { db?.close() } catch (ex: Exception) {}
                }
            }
        }
        return results
    }

    private fun stopWebServer() {
        isServerRunning = false
        sendSystemNotification(
            "DevHive HTTP Web Server Stopped",
            "Local PHP-HTTP server is now offline."
        )
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverJob?.cancel()
        serverJob = null
        serverLogs.add("[System] Web Server stopped.")
    }

    // --- MySQL Server Stack Controls ---
    fun toggleMySql() {
        if (isMySqlRunning) {
            stopMySql()
        } else {
            startMySql()
        }
    }

    private fun startMySql() {
        if (isMySqlRunning) return
        isMySqlRunning = true
        sendSystemNotification(
            "DevHive MySQL Server Started",
            "MySQL database engine is live on port 3306! host: 127.0.0.1, user: root"
        )
        serverLogs.add("[MySQL] Initializing MariaDB/MySQL engine sandbox...")
        serverLogs.add("[MySQL] Server bound to address: 127.0.0.1:3306")
        
        mysqlJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = ServerSocket(3306)
                mysqlSocket = socket
                while (isMySqlRunning) {
                    val client = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val out = client.getOutputStream()
                            // Write standard MariaDB / MySQL handshaking greeting
                            out.write("J\u0000\u0000\u0000\n8.0.35-0ubuntu0.22.04.1\u0000\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000".toByteArray())
                            out.flush()
                            client.close()
                        } catch (ex: Exception) {}
                    }
                }
            } catch (e: Exception) {
                isMySqlRunning = false
                withContext(Dispatchers.Main) {
                    serverLogs.add("[MySQL Error] Failed starting server socket: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun stopMySql() {
        isMySqlRunning = false
        sendSystemNotification(
            "DevHive MySQL Server Stopped",
            "MySQL database server is now offline."
        )
        serverLogs.add("[MySQL] Server stack shutdown initiated.")
        try {
            mysqlSocket?.close()
        } catch (e: Exception) {}
        mysqlJob?.cancel()
        mysqlJob = null
        serverLogs.add("[MySQL] Engine offline.")
    }

    // --- System Push Notification System Utility ---
    fun sendSystemNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "devhive_server_channel"
        val notificationManager = context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DevHive Server Stack Status",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System notifications for DevHive server statuses"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        try {
            notificationManager.notify(Random.nextInt(1000, 9999), notification)
        } catch (e: Exception) {
            Log.e("DevHiveNotification", "Permission or security exception posting notification", e)
        }
    }

    // --- Global Fast File Contents Search (VS Code style) ---
    fun performGlobalSearch(query: String) {
        globalSearchQuery = query
        globalSearchResults.clear()
        if (query.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<FileSearchResult>()
            fun searchDir(dir: File) {
                val list = dir.listFiles() ?: return
                for (file in list) {
                    if (file.isDirectory) {
                        searchDir(file)
                    } else {
                        val ext = file.extension.lowercase()
                        // Skip binary or heavy directories
                        if (file.name.startsWith(".")) continue
                        if (ext in listOf("html", "css", "js", "php", "sql", "txt", "json", "py", "sh", "md", "xml", "kt", "java")) {
                            try {
                                var lineNum = 1
                                file.forEachLine { line ->
                                    if (line.contains(query, ignoreCase = true)) {
                                        results.add(FileSearchResult(file, lineNum, line.trim()))
                                    }
                                    lineNum++
                                }
                            } catch(e: Exception) {
                                // Ignore unreadable or binary files gracefully
                            }
                        }
                    }
                }
            }
            searchDir(workspaceDir)
            withContext(Dispatchers.Main) {
                globalSearchResults.clear()
                globalSearchResults.addAll(results)
            }
        }
    }

    fun performGlobalReplace(findQuery: String, replaceQuery: String) {
        if (findQuery.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            fun replaceInDir(dir: File) {
                val list = dir.listFiles() ?: return
                for (file in list) {
                    if (file.isDirectory) {
                        replaceInDir(file)
                    } else {
                        val ext = file.extension.lowercase()
                        if (file.name.startsWith(".")) continue
                        if (ext in listOf("html", "css", "js", "php", "sql", "txt", "json", "py", "sh", "md", "xml", "kt", "java")) {
                            try {
                                val originalContent = file.readText()
                                if (originalContent.contains(findQuery)) {
                                    val newContent = originalContent.replace(findQuery, replaceQuery)
                                    file.writeText(newContent)
                                }
                            } catch(e: Exception) {
                                // Ignore gracefully
                            }
                        }
                    }
                }
            }
            replaceInDir(workspaceDir)
            withContext(Dispatchers.Main) {
                // Refresh open active file content if it was replaced
                activeTab?.let { activeFile ->
                    try {
                        activeFileContent = activeFile.readText()
                    } catch(e: Exception) {}
                }
                // Refresh file search results
                if (globalSearchQuery.isNotEmpty()) {
                    performGlobalSearch(globalSearchQuery)
                }
                refreshFiles()
            }
        }
    }

    // --- 5. VCS (Git Simulator) Logic ---
    private fun checkVcsChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            val rootList = workspaceDir.listFiles() ?: emptyArray()
            val uFiles = mutableListOf<File>()
            
            rootList.forEach { file ->
                if (file.isFile) {
                    // Check if file is modified relative to a mock Git state, or just put all files in unstaged if modified!
                    // To keep simulator functional: let's track the modified timestamp.
                    // If files have unstaged status, keep track in list.
                    if (!stagedFiles.contains(file)) {
                        uFiles.add(file)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                unstagedFiles.clear()
                unstagedFiles.addAll(uFiles)
            }
        }
    }

    fun stageFile(file: File) {
        if (!stagedFiles.contains(file)) {
            stagedFiles.add(file)
            unstagedFiles.remove(file)
        }
    }

    fun unstageFile(file: File) {
        if (stagedFiles.contains(file)) {
            stagedFiles.remove(file)
            if (!unstagedFiles.contains(file)) {
                unstagedFiles.add(file)
            }
        }
    }

    fun stageAllFiles() {
        stagedFiles.addAll(unstagedFiles)
        unstagedFiles.clear()
    }

    fun unstageAllFiles() {
        unstagedFiles.addAll(stagedFiles)
        stagedFiles.clear()
    }

    fun commitChanges(message: String) {
        if (stagedFiles.isEmpty()) return
        val msg = message.ifBlank { "Update files" }
        viewModelScope.launch(Dispatchers.IO) {
            val author = "mrkhatab112@gmail.com"
            val hash = Random.nextInt(1000000, 9999999).toString(16).uppercase()
            
            val filesMap = stagedFiles.associate { it.name to "MODIFIED" }
            val mapSerialized = filesMap.map { "${it.key}:${it.value}" }.joinToString(";")

            val commitObj = VcsCommit(
                commitHash = hash,
                message = msg,
                author = author,
                timestamp = System.currentTimeMillis(),
                changedFilesJson = mapSerialized
            )

            repository.saveCommit(commitObj)

            withContext(Dispatchers.Main) {
                stagedFiles.clear()
                commitMessage = ""
                checkVcsChanges()
                refreshFiles()
            }
        }
    }

    fun loadCommitDiffs(commit: VcsCommit) {
        selectedCommitHash = commit.commitHash
        selectedCommitDiffs.clear()

        val list = mutableListOf<FileDiff>()
        if (commit.changedFilesJson.isNotBlank()) {
            val split = commit.changedFilesJson.split(";")
            split.forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val name = parts[0]
                    val status = parts[1]
                    list.add(
                        FileDiff(
                            filePath = name,
                            status = status,
                            addedLines = listOf("+ class DevHiveWorkstation {", "+     val platform = \"Android\"", "+ }"),
                            removedLines = listOf("- class OlderIde {", "-     val laggy = true", "- }")
                        )
                    )
                }
            }
        }
        selectedCommitDiffs.addAll(list)
    }

    fun resetVcsHistory() {
        viewModelScope.launch {
            repository.clearAllCommits()
            selectedCommitHash = ""
            selectedCommitDiffs.clear()
            stagedFiles.clear()
            checkVcsChanges()
        }
    }

    // --- 6. Terminal Console Engine ---
    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        terminalHistory.add("devhive@android:~/workspace$ $trimmed")
        val args = trimmed.split(" ")
        val command = args[0].lowercase()

        when (command) {
            "help" -> {
                terminalHistory.add("Available commands inside DevHive Sandbox Shell:")
                terminalHistory.add("  help               Show this panel")
                terminalHistory.add("  ls                 List workspace files")
                terminalHistory.add("  pwd                Print current directory")
                terminalHistory.add("  cat <file>         Show contents of a file")
                terminalHistory.add("  echo <text>        Print text payload")
                terminalHistory.add("  server             Show local web server state")
                terminalHistory.add("  mysql              Enter sandboxed MySQL command line console")
                terminalHistory.add("  php <file.php>     Execute workspace PHP server script")
                terminalHistory.add("  ports              Scan standard developer socket ports")
                terminalHistory.add("  git log            Show simulated local VCS commits")
                terminalHistory.add("  ping -c <n> <host> Execute network system ping (Real Host ping)")
                terminalHistory.add("  clear              Clear terminal history")
            }
            "clear" -> {
                terminalHistory.clear()
            }
            "ls" -> {
                val list = workspaceDir.listFiles() ?: emptyArray()
                if (list.isEmpty()) {
                    terminalHistory.add("Workspace is empty.")
                } else {
                    list.forEach { file ->
                        val prefix = if (file.isDirectory) "📁 [DIR] " else "📄 [FILE]"
                        val size = if (file.isFile) " (${file.length()} B)" else ""
                        terminalHistory.add("  $prefix ${file.name}$size")
                    }
                }
            }
            "pwd" -> {
                terminalHistory.add("  ~/workspace -> " + workspaceDir.absolutePath)
            }
            "server" -> {
                terminalHistory.add("  Web Server Running: $isServerRunning")
                terminalHistory.add("  Web Server Port: $serverPort")
                terminalHistory.add("  Active Logs Stream Size: ${serverLogs.size}")
            }
            "ports" -> {
                terminalHistory.add("  Triggering background Port Scan checker...")
                runLocalPortScan()
            }
            "mysql" -> {
                terminalHistory.add("Welcome to the MySQL/MariaDB Monitor. Commands end with ;")
                terminalHistory.add("Your MySQL connection ID is 108")
                terminalHistory.add("Server version: 8.0.35-0ubuntu0.22.04.1 (Ubuntu)")
                terminalHistory.add("Connection: localhost via UNIX socket")
                terminalHistory.add("")
                terminalHistory.add("mysql> Connected to local sandbox schema. Use the 'DATABASE' tab for visual phpMyAdmin & query execution!")
            }
            "php" -> {
                if (args.size > 1) {
                    val scriptName = args[1]
                    val targetFile = File(workspaceDir, scriptName)
                    if (targetFile.exists() && targetFile.isFile) {
                        terminalHistory.add("  [PHP 8.2.12 Engine] Executing $scriptName...")
                        try {
                            val rawContent = targetFile.readText()
                            val parsedOutput = executePhpScript(rawContent)
                            // Clean HTML tags for terminal rendering
                            val cleanOutput = parsedOutput
                                .replace(Regex("<[^>]*>"), "")
                                .replace("&nbsp;", " ")
                                .trim()
                            
                            terminalHistory.add("  --------------------------------------")
                            cleanOutput.split("\n").forEach { line ->
                                if (line.isNotBlank()) {
                                    terminalHistory.add("  $line")
                                }
                            }
                            terminalHistory.add("  --------------------------------------")
                            terminalHistory.add("  Exit Code: 0 (Success)")
                        } catch (e: Exception) {
                            terminalHistory.add("  PHP Fatal Error: ${e.localizedMessage}")
                        }
                    } else {
                        terminalHistory.add("  PHP Fatal Error: Failed opening required '$scriptName' (include_path='.:/usr/share/php')")
                    }
                } else {
                    terminalHistory.add("  PHP 8.2.12 (cli) (built: Oct 24 2026 18:22:45) (NTS)")
                    terminalHistory.add("  Copyright (c) The PHP Group")
                    terminalHistory.add("  Usage: php <script.php>")
                }
            }
            "git" -> {
                if (args.size > 1 && args[1] == "log") {
                    val commits = gitCommits.value
                    if (commits.isEmpty()) {
                        terminalHistory.add("  No commits found. Go to VCS tab to make a commit.")
                    } else {
                        commits.forEach { c ->
                            terminalHistory.add("  * Commit [${c.commitHash}] - ${c.message} (by ${c.author})")
                        }
                    }
                } else {
                    terminalHistory.add("  Usage: git log")
                }
            }
            "cat" -> {
                if (args.size > 1) {
                    val targetFile = File(workspaceDir, args[1])
                    if (targetFile.exists() && targetFile.isFile) {
                        try {
                            val lines = targetFile.readLines()
                            lines.forEach { terminalHistory.add("  $it") }
                        } catch (e: Exception) {
                            terminalHistory.add("  Error: Could not read file.")
                        }
                    } else {
                        terminalHistory.add("  Error: File not found '${args[1]}'")
                    }
                } else {
                    terminalHistory.add("  Usage: cat <filename>")
                }
            }
            "echo" -> {
                val text = args.drop(1).joinToString(" ")
                terminalHistory.add("  $text")
            }
            "ping" -> {
                terminalHistory.add("  Pinging system loopback...")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val host = if (args.size > 2) args.last() else "8.8.8.8"
                        val count = if (args.size > 2 && args[1] == "-c") args[2].toIntOrNull() ?: 3 else 3
                        
                        // Run real system process ping!
                        val process = Runtime.getRuntime().exec("ping -c $count $host")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val finalLine = line ?: ""
                            withContext(Dispatchers.Main) {
                                terminalHistory.add("  $finalLine")
                            }
                        }
                        process.waitFor()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            terminalHistory.add("  Ping error: ${e.message}")
                        }
                    }
                }
            }
            else -> {
                // Try executing as a real low-level Android shell command!
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val process = Runtime.getRuntime().exec(trimmed)
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val errReader = BufferedReader(InputStreamReader(process.errorStream))
                        var line: String?
                        var outputAdded = false
                        
                        while (reader.readLine().also { line = it } != null) {
                            val finalLine = line ?: ""
                            withContext(Dispatchers.Main) {
                                terminalHistory.add("  $finalLine")
                            }
                            outputAdded = true
                        }
                        
                        while (errReader.readLine().also { line = it } != null) {
                            val finalLine = line ?: ""
                            withContext(Dispatchers.Main) {
                                terminalHistory.add("  $finalLine")
                            }
                            outputAdded = true
                        }
                        
                        process.waitFor()
                        if (!outputAdded) {
                            withContext(Dispatchers.Main) {
                                terminalHistory.add("  Command returned exit code: ${process.exitValue()}")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            terminalHistory.add("  bash: command not found: $command")
                        }
                    }
                }
            }
        }
    }

    // --- 7. Docker Manager Simulator ---
    private fun initDockerMock() {
        dockerContainers.add(DockerContainer("1", "apache-php-server", "httpd:alpine-php", "80:80", "RUNNING", 1.5f, "18 MB"))
        dockerContainers.add(DockerContainer("2", "mysql-database", "mysql:8.0", "3306:3306", "RUNNING", 3.2f, "72 MB"))
        dockerContainers.add(DockerContainer("3", "phpmyadmin-web", "phpmyadmin:latest", "8081:80", "RUNNING", 0.7f, "14 MB"))
        dockerContainers.add(DockerContainer("4", "redis-cache", "redis:6-alpine", "6379:6379", "RUNNING", 0.4f, "6 MB"))

        dockerLogs.add("[Docker] Connection established successfully.")
        dockerLogs.add("[Docker] Monitoring container resources...")

        // Spin background fluctuations for stats
        dockerJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                if (dockerConnected) {
                    dockerContainers.forEachIndexed { index, container ->
                        if (container.status == "RUNNING") {
                            val nextCpu = Random.nextFloat() * 12.0f
                            val nextRam = Random.nextInt(10, 150)
                            dockerContainers[index] = container.copy(
                                cpu = Math.round(nextCpu * 10f) / 10f,
                                ram = "$nextRam MB"
                            )
                        }
                    }
                    if (Random.nextInt(10) > 7) {
                        val active = dockerContainers.filter { it.status == "RUNNING" }
                        if (active.isNotEmpty()) {
                            val sample = active.random()
                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            dockerLogs.add("[$timestamp] [${sample.name}] Health Check Passed. CPU at ${sample.cpu}%.")
                        }
                    }
                }
            }
        }
    }

    fun toggleContainer(id: String) {
        val index = dockerContainers.indexOfFirst { it.id == id }
        if (index != -1) {
            val container = dockerContainers[index]
            val newStatus = if (container.status == "RUNNING") "STOPPED" else "RUNNING"
            dockerContainers[index] = container.copy(
                status = newStatus,
                cpu = 0f,
                ram = if (newStatus == "RUNNING") "24 MB" else "0 MB"
            )
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            dockerLogs.add("[$timestamp] Container '${container.name}' status updated to $newStatus")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWebServer()
        activeSQLiteDb?.close()
        dockerJob?.cancel()
    }

    // --- 8. Port Scanner Logic ---
    fun runLocalPortScan() {
        if (isPortScanning) return
        isPortScanning = true
        scannedPorts.clear()
        
        val portsToScan = listOf(21, 22, 80, 443, 1433, 3306, 5432, 6379, 8080, 27017)
        val services = mapOf(
            21 to "FTP",
            22 to "SSH",
            80 to "HTTP Web Server",
            443 to "HTTPS SSL Server",
            1433 to "MS-SQL Database",
            3306 to "MySQL Database Server",
            5432 to "PostgreSQL Server",
            6379 to "Redis Cache Service",
            8080 to "HTTP Alt Server",
            27017 to "MongoDB Database"
        )

        viewModelScope.launch(Dispatchers.IO) {
            portsToScan.forEach { port ->
                var isOpen = false
                try {
                    // Try to scan socket locally
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress("127.0.0.1", port), 200)
                    socket.close()
                    isOpen = true
                } catch (e: Exception) {
                    // Port is closed
                }

                // Or, if our web server is running, mark 8080 as real online!
                if (port == serverPort && isServerRunning) {
                    isOpen = true
                }

                val scanResult = PortScanResult(
                    port = port,
                    service = services[port] ?: "Custom Service",
                    isOpen = isOpen
                )

                withContext(Dispatchers.Main) {
                    scannedPorts.add(scanResult)
                }
            }
            withContext(Dispatchers.Main) {
                isPortScanning = false
                terminalHistory.add("  Port Scanner report compiled successfully. Go to Settings/Ports to view.")
            }
        }
    }
}
