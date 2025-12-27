package com.chitui.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chitui.android.data.Printer
import com.chitui.android.data.PrinterState
import com.chitui.android.data.PrinterStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterDetailScreen(
    viewModel: PrinterDetailViewModel,
    onBack: () -> Unit
) {
    val printer by viewModel.printer.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(printer?.name ?: "Printer Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (printer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Printer Info Card
                PrinterInfoCard(printer!!)

                // Status Card
                if (printer!!.status != null) {
                    PrinterStatusCard(printer!!.status!!)
                }

                // Controls Card
                PrintControlsCard(
                    status = printer!!.status,
                    canStart = viewModel.canStartPrint(),
                    canPause = viewModel.canPausePrint(),
                    canResume = viewModel.canResumePrint(),
                    canStop = viewModel.canStopPrint(),
                    onStart = { /* File selection would go here */ },
                    onPause = { viewModel.pausePrint() },
                    onResume = { viewModel.resumePrint() },
                    onStop = { viewModel.stopPrint() }
                )

                // Upload Progress
                if (uiState.isUploading) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Uploading...",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.uploadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${uiState.uploadProgress}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterInfoCard(printer: Printer) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Printer Information",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            InfoRow("IP Address", "${printer.ip}:${printer.port}")
            if (printer.machineName != null) {
                InfoRow("Machine Name", printer.machineName)
            }
            if (printer.model != null) {
                InfoRow("Model", printer.model)
            }
            if (printer.firmwareVersion != null) {
                InfoRow("Firmware", printer.firmwareVersion)
            }
            InfoRow("Status", if (printer.isConnected) "Connected" else "Disconnected")
        }
    }
}

@Composable
fun PrinterStatusCard(status: PrinterStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status.state) {
                PrinterState.PRINTING -> MaterialTheme.colorScheme.primaryContainer
                PrinterState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Badge(
                    containerColor = when (status.state) {
                        PrinterState.PRINTING -> MaterialTheme.colorScheme.primary
                        PrinterState.PAUSED -> MaterialTheme.colorScheme.tertiary
                        PrinterState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Text(status.state.displayName)
                }
            }

            if (status.currentFile != null) {
                Divider()
                InfoRow("Current File", status.currentFile)
            }

            if (status.state == PrinterState.PRINTING || status.state == PrinterState.PAUSED) {
                Divider()

                // Progress
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${status.progress}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { status.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Layer info
                if (status.totalLayers > 0) {
                    InfoRow("Layer", "${status.currentLayer} / ${status.totalLayers}")
                }

                // Time remaining
                if (status.printTimeRemaining > 0) {
                    val hours = status.printTimeRemaining / 3600
                    val minutes = (status.printTimeRemaining % 3600) / 60
                    InfoRow("Time Remaining", String.format("%dh %dm", hours, minutes))
                }
            }

            if (status.errorMessage != null) {
                Divider()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = status.errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PrintControlsCard(
    status: PrinterStatus?,
    canStart: Boolean,
    canPause: Boolean,
    canResume: Boolean,
    canStop: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Print Controls",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pause/Resume button
                if (canPause) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                } else if (canResume) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }
                }

                // Stop button
                if (canStop) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            if (canStart) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Print")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
