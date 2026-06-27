package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PhpMyAdminWorkspace(viewModel: DevHiveViewModel) {
    val connections by viewModel.savedConnections.collectAsStateWithLifecycle()
    var selectedTabItem by remember { mutableStateOf(0) } // 0 = Browse, 1 = Structure, 2 = SQL, 3 = Insert Row
    val phpMyAdminPort = 8080
    var sqlInputText by remember { mutableStateOf(viewModel.dbConsoleQuery) }
    
    // Insert row fields
    var insertNameText by remember { mutableStateOf("") }
    var insertValueText by remember { mutableStateOf("") }

    // Auto-connect to database workspace at first startup
    LaunchedEffect(Unit) {
        if (viewModel.activeDbConnection == null && connections.isNotEmpty()) {
            viewModel.connectToSQLite(connections.first())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)) // phpMyAdmin warm-light grey
    ) {
        // phpMyAdmin Web Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C3E50)) // Slate gray
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "php",
                    color = Color(0xFFF39C12), // Brand orange
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "MyAdmin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "v5.2.1",
                    color = Color(0xFFBDC3C7),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Server Address bar indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF34495E))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "localhost:$phpMyAdminPort/phpmyadmin",
                    color = Color(0xFF2ECC71), // Minty green
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sub-Header info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAEDED))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, Color(0xFFD5D8DC)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Database server: 127.0.0.1 via TCP/IP | Connection: root@localhost",
                fontSize = 10.sp,
                color = Color(0xFF2C3E50),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "● Server Sandbox Online",
                fontSize = 10.sp,
                color = Color(0xFF27AE60),
                fontWeight = FontWeight.Bold
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // SIDEBAR: phpMyAdmin Left Navigation Tree
            Column(
                modifier = Modifier
                    .width(155.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFEAECEE))
                    .border(1.dp, Color(0xFFD5D8DC))
                    .padding(8.dp)
            ) {
                Text(
                    text = "DATABASES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F8C8D),
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Storage, contentDescription = null, tint = Color(0xFFE67E22), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "devhive_database",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TABLES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F8C8D),
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (viewModel.dbTables.isEmpty()) {
                    Text("No tables loaded.", fontSize = 10.sp, color = Color.Gray)
                } else {
                    LazyColumn {
                        items(viewModel.dbTables) { table ->
                            val isSelected = viewModel.selectedTable == table
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadTableRows(table)
                                        selectedTabItem = 0 // Auto go to Browse
                                    }
                                    .background(if (isSelected) Color(0xFFD4E6F1) else Color.Transparent)
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = null,
                                    tint = Color(0xFF3498DB),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = table,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color(0xFF1B4F72) else Color(0xFF2C3E50),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // VIEWPORT: phpMyAdmin Main Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                val activeTable = viewModel.selectedTable
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.TableChart, contentDescription = null, tint = Color(0xFF1ABC9C), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeTable != null) "Table: $activeTable" else "Select a Table",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Action Navigation Tabs (Browse, Structure, SQL, Insert Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tabLabels = listOf("Browse", "Structure", "SQL", "Insert")
                    tabLabels.forEachIndexed { index, label ->
                        val active = selectedTabItem == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (active) Color(0xFFD5F5E3) else Color(0xFFEAEDED))
                                .border(1.dp, if (active) Color(0xFF2ECC71) else Color(0xFFBDC3C7), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .clickable { selectedTabItem = index }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color(0xFF196F3D) else Color(0xFF5D6D7E)
                            )
                        }
                    }
                }
                Divider(color = Color(0xFFBDC3C7))

                Spacer(modifier = Modifier.height(10.dp))

                // Content render switches
                when (selectedTabItem) {
                    0 -> { // Browse
                        if (activeTable == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Click a table name in the sidebar database map to inspect records.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F8F5))
                                        .padding(6.dp)
                                        .border(1.dp, Color(0xFFA3E4D7)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Showing entries 0 - ${viewModel.tableRows.size - 1} (${viewModel.tableRows.size} total row records, Query completed in 0.0001s)",
                                        fontSize = 10.sp,
                                        color = Color(0xFF117864),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                                    Column {
                                        // Headers
                                        Row(modifier = Modifier.background(Color(0xFFEAEDED))) {
                                            viewModel.tableColumns.forEach { col ->
                                                Box(
                                                    modifier = Modifier
                                                        .border(1.dp, Color(0xFFBDC3C7))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                        .widthIn(min = 100.dp)
                                                ) {
                                                    Text(
                                                        text = col,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2C3E50),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        // Rows list
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            itemsIndexed(viewModel.tableRows) { index, rowList ->
                                                val backgroundCol = if (index % 2 == 0) Color.White else Color(0xFFF9EBEA).copy(alpha = 0.5f)
                                                Row(modifier = Modifier.background(backgroundCol)) {
                                                    rowList.forEach { valStr ->
                                                        Box(
                                                            modifier = Modifier
                                                                .border(1.dp, Color(0xFFEAEDED))
                                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                                                .widthIn(min = 100.dp)
                                                        ) {
                                                            Text(
                                                                text = valStr,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF2C3E50),
                                                                fontFamily = FontFamily.Monospace,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Structure schema
                        if (activeTable == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a table to view schemas and field layouts.", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text("Schema Structure Matrix", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    // Header structure
                                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFEAECEE))) {
                                        listOf("Field", "Type", "Null", "Key", "Default", "Extra").forEach { item ->
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFBDC3C7)).padding(4.dp)) {
                                                Text(item, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Fields list mapping
                                    viewModel.tableColumns.forEach { col ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text(col, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2C3E50))
                                            }
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text(if (col == "id" || col == "port") "INT" else "VARCHAR(512)", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text(if (col == "id") "NO" else "YES", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text(if (col == "id") "PRI" else "", fontSize = 9.sp, color = Color(0xFFE67E22), fontWeight = FontWeight.Bold)
                                            }
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text("NULL", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Box(modifier = Modifier.weight(1f).border(1.dp, Color(0xFFEAEDED)).padding(4.dp)) {
                                                Text(if (col == "id") "auto_increment" else "", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // SQL Query Sandbox
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Run SQL query against schema devhive_database:", fontSize = 11.sp, color = Color(0xFF2C3E50))
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .border(1.dp, Color(0xFFBDC3C7))
                                    .background(Color(0xFFFBFCFC))
                                    .padding(6.dp)
                            ) {
                                BasicTextField(
                                    value = sqlInputText,
                                    onValueChange = { sqlInputText = it },
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF2C3E50)),
                                    cursorBrush = SolidColor(Color(0xFF2ECC71)),
                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row {
                                Button(
                                    onClick = {
                                        viewModel.dbConsoleQuery = sqlInputText
                                        viewModel.executeSqlStatement()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Go (Execute)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        sqlInputText = "SELECT * FROM db_connections;"
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Load connections", fontSize = 11.sp, color = Color(0xFF7F8C8D))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            if (viewModel.dbConsoleMessage.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8F5)),
                                    border = BorderStroke(1.dp, Color(0xFF2ECC71)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = viewModel.dbConsoleMessage,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF196F3D),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            if (viewModel.dbConsoleError.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFADBD8)),
                                    border = BorderStroke(1.dp, Color(0xFFE74C3C)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = viewModel.dbConsoleError,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF78281F),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    3 -> { // Insert record Row
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Insert dynamic record into: ${activeTable ?: "None"}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (activeTable == null) {
                                Text("No active table highlighted. Select one from the sidebar.", fontSize = 10.sp, color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    OutlinedTextField(
                                        value = insertNameText,
                                        onValueChange = { insertNameText = it },
                                        label = { Text("Value / Name") },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                    OutlinedTextField(
                                        value = insertValueText,
                                        onValueChange = { insertValueText = it },
                                        label = { Text("Details / Host / Type") },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            if (insertNameText.isNotBlank() && insertValueText.isNotBlank()) {
                                                val c1 = viewModel.tableColumns.getOrNull(1) ?: "name"
                                                val c2 = viewModel.tableColumns.getOrNull(2) ?: "type"
                                                val sql = "INSERT INTO $activeTable ($c1, $c2) VALUES ('$insertNameText', '$insertValueText');"
                                                viewModel.dbConsoleQuery = sql
                                                viewModel.executeSqlStatement()
                                                insertNameText = ""
                                                insertValueText = ""
                                                selectedTabItem = 0 // Browse
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("INSERT GO (phpMyAdmin)", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
