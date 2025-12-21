package com.chitui.remote.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.chitui.remote.ui.theme.StatusConnected
import com.chitui.remote.ui.theme.StatusDisconnected
import com.chitui.remote.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    if (!isAuthenticated) {
        ConnectionScreen(viewModel = viewModel)
    } else {
        MainAppScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(viewModel: MainViewModel) {
    var serverUrl by remember { mutableStateOf("http://") }
    var password by remember { mutableStateOf("") }

    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val savedServerUrl by viewModel.serverUrl.collectAsState()

    LaunchedEffect(savedServerUrl) {
        if (savedServerUrl.isNotEmpty()) {
            serverUrl = savedServerUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChitUI Remote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Printer",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Connect to ChitUI Server",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.x:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = connectionState !is MainViewModel.ConnectionState.Connecting
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = connectionState !is MainViewModel.ConnectionState.Connecting
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val (statusColor, statusText) = when (connectionState) {
                    is MainViewModel.ConnectionState.Connected -> StatusConnected to "Connected"
                    is MainViewModel.ConnectionState.Connecting -> StatusDisconnected to "Connecting..."
                    is MainViewModel.ConnectionState.Disconnected -> StatusDisconnected to "Disconnected"
                    is MainViewModel.ConnectionState.Error -> StatusDisconnected to "Error"
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = statusColor)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (serverUrl.isNotBlank() && password.isNotBlank()) {
                        viewModel.connect(serverUrl.trim(), password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState !is MainViewModel.ConnectionState.Connecting &&
                        serverUrl.isNotBlank() && password.isNotBlank()
            ) {
                if (connectionState is MainViewModel.ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Connect")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val printers by viewModel.printers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChitUI Remote") },
                actions = {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Disconnect")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Printers") },
                    label = { Text("Printers") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Folder, contentDescription = "Files") },
                    label = { Text("Files") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Videocam, contentDescription = "Camera") },
                    label = { Text("Camera") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = "System") },
                    label = { Text("System") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> PrintersScreen(viewModel = viewModel, printers = printers)
                1 -> FilesScreen()
                2 -> CameraScreen()
                3 -> SystemScreen()
            }
        }
    }
}

@Composable
fun PrintersScreen(viewModel: MainViewModel, printers: Map<String, com.chitui.remote.data.models.Printer>) {
    if (printers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "No printers",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No printers found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            printers.forEach { (id, printer) ->
                PrinterCard(printer = printer, viewModel = viewModel)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterCard(printer: com.chitui.remote.data.models.Printer, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${printer.brand} ${printer.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = printer.ip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Printer",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            printer.status?.let { status ->
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Status: ${status.status}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (status.printing && status.printPercent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (status.printPercent / 100f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${status.printPercent}% - ${status.currentLayer}/${status.totalLayer}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Files - Coming Soon")
    }
}

@Composable
fun CameraScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Camera - Coming Soon")
    }
}

@Composable
fun SystemScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("System Info - Coming Soon")
    }
}
