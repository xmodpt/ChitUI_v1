package com.chitui.remote.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chitui.remote.data.api.ApiClient
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
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedPrinter == null) {
                // No printer selected
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
                // Show selected printer detail view
                PrinterDetailScreen(printer = selectedPrinter!!, viewModel = viewModel, allPrinters = printers)
            }
        }
    }
}

@Composable
fun PrinterDetailScreen(
    printer: com.chitui.remote.data.models.Printer,
    viewModel: MainViewModel,
    allPrinters: Map<String, com.chitui.remote.data.models.Printer>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Printer Header - similar to ChitUI's printer preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Printer image
                if (printer.image != null) {
                    AsyncImage(
                        model = "${ApiClient.getBaseUrl()}/${printer.image}",
                        contentDescription = "Printer image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Printer",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Printer info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${printer.brand ?: "Unknown"} ${printer.model ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = printer.ip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Print Card - ChitUI style
        printer.status?.let { status ->
            if (status.printing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Print",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = status.status.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        Text(
                            text = "${status.printPercent ?: 0}%",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = ((status.printPercent ?: 0) / 100f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Layer and time info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Layer: ${status.currentLayer ?: 0} / ${status.totalLayer ?: 0}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            status.printTimeRemaining?.let { timeRemaining ->
                                Text(
                                    text = formatTime(timeRemaining),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Print controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pausePrint(printer.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Pause")
                            }
                            Button(
                                onClick = { viewModel.stopPrint(printer.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Stop")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Camera Card - ChitUI style
        var cameraRefreshKey by remember { mutableStateOf(0) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { cameraRefreshKey++ }) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Refresh"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Camera stream
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "${ApiClient.getBaseUrl()}/camera/${printer.id}/snapshot?t=$cameraRefreshKey",
                            contentDescription = "Camera feed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            error = painterResource(android.R.drawable.ic_menu_camera),
                            placeholder = painterResource(android.R.drawable.ic_menu_camera)
                        )
                    }
                }
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Printer image if available
                printer.image?.let { imagePath ->
                    AsyncImage(
                        model = "${ApiClient.getBaseUrl()}/$imagePath",
                        contentDescription = "Printer image",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
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

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Settings,
                        contentDescription = if (expanded) "Close" else "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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

                    status.printTimeRemaining?.let { timeRemaining ->
                        Text(
                            text = "Time remaining: ${formatTime(timeRemaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded controls
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Printer Controls",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPrinting = printer.status?.printing == true

                    if (isPrinting) {
                        Button(
                            onClick = { viewModel.pausePrint(printer.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause")
                        }
                        Button(
                            onClick = { viewModel.stopPrint(printer.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop")
                        }
                    } else {
                        Button(
                            onClick = { /* TODO: Show file picker */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Print")
                        }
                    }
                }
            }
        }
    }
}

// Helper function to format time in seconds to HH:MM:SS
fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
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
fun CameraScreen(printers: Map<String, com.chitui.remote.data.models.Printer>) {
    if (printers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "No cameras",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No printers with cameras",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            printers.forEach { (id, printer) ->
                CameraCard(printer = printer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCard(printer: com.chitui.remote.data.models.Printer) {
    var refreshKey by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${printer.name} Camera",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = { refreshKey++ }) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Refresh camera",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera snapshot
            AsyncImage(
                model = "${ApiClient.getBaseUrl()}/camera/${printer.id}/snapshot?t=$refreshKey",
                contentDescription = "Camera feed for ${printer.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                error = painterResource(android.R.drawable.ic_menu_camera),
                placeholder = painterResource(android.R.drawable.ic_menu_camera)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap refresh icon to update camera view",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
