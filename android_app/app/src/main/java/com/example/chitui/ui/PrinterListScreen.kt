/**
 * Printer List Screen (Jetpack Compose)
 *
 * This composable shows a list of all printers with their status.
 * Features:
 * - Pull-to-refresh
 * - Real-time status updates
 * - Click to view printer details
 * - Print control buttons
 *
 * Usage in MainActivity:
 *   setContent {
 *       ChitUITheme {
 *           PrinterListScreen(viewModel)
 *       }
 *   }
 */

package com.example.chitui.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chitui.api.Printer
import com.example.chitui.viewmodel.PrinterViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterListScreen(
    viewModel: PrinterViewModel,
    onPrinterClick: (Printer) -> Unit = {}
) {
    // Observe ViewModel data
    val printers by viewModel.printers.observeAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val error by viewModel.error.observeAsState()
    val isConnected by viewModel.isConnected.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChitUI Printers") },
                actions = {
                    // Connection status indicator
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = if (isConnected) "Connected" else "Disconnected",
                        tint = if (isConnected) Color.Green else Color.Red,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isLoading),
            onRefresh = { viewModel.loadPrinters() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Error message
                error?.let { errorMessage ->
                    ErrorBanner(
                        message = errorMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                }

                // Printer list
                if (printers.isEmpty() && !isLoading) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(printers) { printer ->
                            PrinterCard(
                                printer = printer,
                                onClick = { onPrinterClick(printer) },
                                onPauseClick = { viewModel.pausePrint(printer.id) },
                                onResumeClick = { viewModel.resumePrint(printer.id) },
                                onStopClick = { viewModel.stopPrint(printer.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterCard(
    printer: Printer,
    onClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Printer name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = printer.name ?: "Unnamed Printer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = printer.ip ?: "Unknown IP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Status badge
                StatusBadge(status = printer.status ?: "unknown")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current file and progress
            printer.current_file?.let { fileName ->
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )

                printer.progress?.let { progress ->
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Print control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onPauseClick) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause"
                        )
                    }
                    IconButton(onClick = onResumeClick) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume"
                        )
                    }
                    IconButton(onClick = onStopClick) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, text) = when (status.lowercase()) {
        "connected" -> Color.Green to "Connected"
        "printing" -> Color.Blue to "Printing"
        "paused" -> Color.Yellow to "Paused"
        "error" -> Color.Red to "Error"
        else -> Color.Gray to "Unknown"
    }

    Surface(
        color = backgroundColor.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = backgroundColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Print,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                text = "No printers found",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text(
                text = "Pull down to refresh",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
