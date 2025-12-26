package com.chitui.client.presentation.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitui.client.data.model.Plugin
import com.chitui.client.data.model.RelayConfig
import com.chitui.client.data.model.RelayState
import com.chitui.client.data.model.SystemInfo
import com.chitui.client.data.model.SystemStats
import com.chitui.client.data.repository.ChitUIRepository
import com.chitui.client.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PluginsScreen(
    plugins: List<Plugin>,
    repository: ChitUIRepository
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Plugins",
            style = MaterialTheme.typography.titleLarge
        )

        if (plugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No plugins available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plugins) { plugin ->
                    when (plugin.id) {
                        "gpio_relay_control" -> GPIORelayPlugin(repository)
                        "rpi_stats" -> RPiStatsPlugin(repository)
                        else -> GenericPluginCard(plugin)
                    }
                }
            }
        }
    }
}

@Composable
fun GenericPluginCard(plugin: Plugin) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = plugin.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Version ${plugin.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AssistChip(
                onClick = {},
                label = { Text(if (plugin.enabled) "Enabled" else "Disabled") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (plugin.enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            )
        }
    }
}

@Composable
fun GPIORelayPlugin(repository: ChitUIRepository) {
    var relayState by remember { mutableStateOf<RelayState?>(null) }
    var relayConfig by remember { mutableStateOf<RelayConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (val result = repository.getRelayStatus()) {
            is Resource.Success -> relayState = result.data
            else -> {}
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "GPIO Relay Control",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(Icons.Default.Power, "Relays")
            }

            Divider()

            if (relayState != null) {
                RelaySwitch(
                    label = relayConfig?.relay1Name ?: "Relay 1",
                    checked = relayState!!.relay1,
                    onToggle = {
                        scope.launch {
                            repository.toggleRelay(1)
                            delay(300)
                            when (val result = repository.getRelayStatus()) {
                                is Resource.Success -> relayState = result.data
                                else -> {}
                            }
                        }
                    }
                )

                RelaySwitch(
                    label = relayConfig?.relay2Name ?: "Relay 2",
                    checked = relayState!!.relay2,
                    onToggle = {
                        scope.launch {
                            repository.toggleRelay(2)
                            delay(300)
                            when (val result = repository.getRelayStatus()) {
                                is Resource.Success -> relayState = result.data
                                else -> {}
                            }
                        }
                    }
                )

                RelaySwitch(
                    label = relayConfig?.relay3Name ?: "Relay 3",
                    checked = relayState!!.relay3,
                    onToggle = {
                        scope.launch {
                            repository.toggleRelay(3)
                            delay(300)
                            when (val result = repository.getRelayStatus()) {
                                is Resource.Success -> relayState = result.data
                                else -> {}
                            }
                        }
                    }
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun RelaySwitch(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
fun RPiStatsPlugin(repository: ChitUIRepository) {
    var systemInfo by remember { mutableStateOf<SystemInfo?>(null) }
    var systemStats by remember { mutableStateOf<SystemStats?>(null) }

    LaunchedEffect(Unit) {
        // Load system info once
        when (val result = repository.getSystemInfo()) {
            is Resource.Success -> systemInfo = result.data
            else -> {}
        }

        // Refresh stats every 5 seconds
        while (true) {
            when (val result = repository.getSystemStats()) {
                is Resource.Success -> systemStats = result.data
                else -> {}
            }
            delay(5000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "System Statistics",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(Icons.Default.Computer, "System")
            }

            Divider()

            if (systemInfo != null) {
                Text(
                    text = "${systemInfo!!.model} (${systemInfo!!.hostname})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = systemInfo!!.os,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (systemStats != null) {
                Divider()

                StatRow(
                    label = "CPU Usage",
                    value = "${systemStats!!.cpuPercent.toInt()}%",
                    progress = systemStats!!.cpuPercent / 100f
                )

                systemStats!!.cpuTemp?.let { temp ->
                    StatRow(
                        label = "CPU Temperature",
                        value = "${temp.toInt()}°C",
                        progress = temp / 100f
                    )
                }

                StatRow(
                    label = "Memory",
                    value = "${systemStats!!.memoryPercent.toInt()}%",
                    progress = systemStats!!.memoryPercent / 100f
                )

                StatRow(
                    label = "Disk",
                    value = "${systemStats!!.diskPercent.toInt()}%",
                    progress = systemStats!!.diskPercent / 100f
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
