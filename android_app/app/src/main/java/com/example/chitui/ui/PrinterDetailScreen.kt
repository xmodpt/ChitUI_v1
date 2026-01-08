/**
 * PrinterDetailScreen - Detailed view of a single printer
 *
 * Features:
 * - Printer image (if configured)
 * - Current status and progress
 * - Print control buttons (pause, resume, stop)
 * - File list from printer storage and USB
 * - Start print from file
 */

package com.example.chitui.ui

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chitui.api.Printer
import com.example.chitui.api.PrintFile
import com.example.chitui.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterDetailScreen(
    printerId: String,
    viewModel: PrinterViewModel,
    onBackClick: () -> Unit
) {
    val selectedPrinter by viewModel.selectedPrinter.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)

    // Load printer details when screen opens
    LaunchedEffect(printerId) {
        viewModel.loadPrinterInfo(printerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedPrinter?.name ?: "Printer Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPrinterInfo(printerId) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && selectedPrinter == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            selectedPrinter?.let { printer ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Printer Image
                    item {
                        PrinterImageCard(printer)
                    }

                    // Status Card
                    item {
                        PrinterStatusCard(printer)
                    }

                    // Control Buttons
                    item {
                        PrintControlCard(printer, viewModel)
                    }

                    // Files Header
                    item {
                        Text(
                            text = "Files on Printer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // File List
                    val files = printer.Attributes?.FileList
                    if (files.isNullOrEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No files found",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(files) { file ->
                            FileCard(
                                file = file,
                                onPrintClick = {
                                    file.FileName?.let {
                                        viewModel.startPrint(printerId, it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterImageCard(printer: Printer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Load printer image from server
            // Format: http://server/printer/images?id=printer_id
            val imageUrl = printer.image_url ?: "placeholder"

            if (imageUrl != "placeholder") {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Printer Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Default Printer",
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PrinterStatusCard(printer: Printer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status:", fontWeight = FontWeight.Bold)
                StatusBadge(printer.Attributes?.CurrentStatus ?: "Unknown")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("IP Address:", fontWeight = FontWeight.Bold)
                Text(printer.ip ?: "Unknown")
            }

            printer.Attributes?.CurrentFile?.let { currentFile ->
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Current File:", fontWeight = FontWeight.Bold)
                Text(currentFile)

                printer.Attributes.PrintingLayer?.let { current ->
                    printer.Attributes.TotalLayer?.let { total ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Layer: $current / $total")

                        val progress = if (total > 0) (current.toFloat() / total.toFloat()) else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                printer.Attributes.RemainTime?.let { remainTime ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Time Remaining: ${formatTime(remainTime)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PrintControlCard(printer: Printer, viewModel: PrinterViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Print Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pause Button
                FilledTonalButton(
                    onClick = { viewModel.pausePrint(printer.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, "Pause", Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pause")
                }

                Spacer(Modifier.width(8.dp))

                // Resume Button
                FilledTonalButton(
                    onClick = { viewModel.resumePrint(printer.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, "Resume", Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resume")
                }

                Spacer(Modifier.width(8.dp))

                // Stop Button
                FilledTonalButton(
                    onClick = { viewModel.stopPrint(printer.id) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, "Stop", Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
fun FileCard(file: PrintFile, onPrintClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.FileName ?: "Unknown File",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatFileSize(file.FileSize ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                file.CreatTime?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            FilledTonalButton(onClick = onPrintClick) {
                Icon(Icons.Default.PlayArrow, "Print")
                Spacer(Modifier.width(4.dp))
                Text("Print")
            }
        }
    }
}

// Helper Functions
fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        minutes > 0 -> String.format("%dm %ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
