package com.chitui.client.presentation.printer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitui.client.data.model.Printer
import com.chitui.client.data.model.PrinterAttributes
import com.chitui.client.data.model.PrinterStatus
import com.chitui.client.presentation.theme.OnlineGreen
import com.chitui.client.presentation.theme.PrintingOrange

@Composable
fun PrinterInfoScreen(
    printer: Printer,
    status: PrinterStatus?,
    attributes: PrinterAttributes?,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    var showStopDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Printer Header Card
        PrinterHeaderCard(printer)

        // Print Status Card (if printing)
        if (status != null && status.printStatus != 0) {
            PrintStatusCard(
                status = status,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStopClick = { showStopDialog = true }
            )
        }

        // Machine Info Card
        MachineInfoCard(printer, attributes)

        // Current Status Card
        CurrentStatusCard(status)
    }

    // Stop Print Confirmation Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Print") },
            text = { Text("Are you sure you want to stop the current print?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStopClick()
                        showStopDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrinterHeaderCard(printer: Printer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Print,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = printer.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${printer.brandName} ${printer.machineName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = printer.ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AssistChip(
                onClick = {},
                label = {
                    Text(if (printer.online) "Online" else "Offline")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(8.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (printer.online) OnlineGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                    labelColor = if (printer.online) OnlineGreen else MaterialTheme.colorScheme.error,
                    leadingIconContentColor = if (printer.online) OnlineGreen else MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
fun PrintStatusCard(
    status: PrinterStatus,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrintingOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Printing",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrintingOrange
                )
                AssistChip(
                    onClick = {},
                    label = { Text(getStatusText(status.printStatus)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = PrintingOrange.copy(alpha = 0.2f),
                        labelColor = PrintingOrange
                    )
                )
            }

            if (status.currentFileName != null) {
                Text(
                    text = status.currentFileName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${(status.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Layers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Layer ${status.currentLayer} / ${status.totalLayer}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status.printStatus == 5) { // Paused
                    Button(
                        onClick = onResumeClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Resume")
                    }
                } else {
                    Button(
                        onClick = onPauseClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Pause")
                    }
                }

                OutlinedButton(
                    onClick = onStopClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
fun MachineInfoCard(printer: Printer, attributes: PrinterAttributes?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Machine Information",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            InfoRow("Model", printer.machineName)
            InfoRow("Brand", printer.brandName)
            InfoRow("Mainboard ID", printer.mainboardId)
            InfoRow("Protocol Version", printer.protocolVersion)
            InfoRow("Firmware Version", printer.firmwareVersion)

            if (attributes?.size != null) {
                InfoRow(
                    "Build Volume",
                    "${attributes.size.x} × ${attributes.size.y} × ${attributes.size.z} mm"
                )
            }
        }
    }
}

@Composable
fun CurrentStatusCard(status: PrinterStatus?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleMedium
            )

            Divider()

            if (status != null) {
                InfoRow("Machine Status", getMachineStatusText(status.machineStatus))
                InfoRow("Print Status", getStatusText(status.printStatus))
                if (status.errorCode != null && status.errorCode != 0) {
                    InfoRow("Error Code", status.errorCode.toString())
                }
            } else {
                Text(
                    text = "No status information available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getMachineStatusText(status: Int): String {
    return when (status) {
        0 -> "Idle"
        1 -> "Printing"
        2 -> "File Transferring"
        3 -> "Testing"
        else -> "Unknown ($status)"
    }
}

private fun getStatusText(status: Int): String {
    return when (status) {
        0 -> "Idle"
        1 -> "Homing"
        2 -> "Dropping"
        3 -> "Exposing"
        4 -> "Lifting"
        5 -> "Paused"
        6 -> "Stopped"
        7 -> "Complete"
        else -> "Unknown ($status)"
    }
}
