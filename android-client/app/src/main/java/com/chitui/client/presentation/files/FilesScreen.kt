package com.chitui.client.presentation.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chitui.client.data.model.FileListData
import com.chitui.client.data.model.PrintFile
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilesScreen(
    files: List<PrintFile>,
    storageInfo: FileListData?,
    onDeleteFile: (String) -> Unit,
    onPrintFile: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<PrintFile?>(null) }
    var sortBy by remember { mutableStateOf(SortBy.NAME) }
    var sortAscending by remember { mutableStateOf(true) }

    val sortedFiles = remember(files, sortBy, sortAscending) {
        val sorted = when (sortBy) {
            SortBy.NAME -> files.sortedBy { it.name.lowercase() }
            SortBy.SIZE -> files.sortedBy { it.size }
            SortBy.DATE -> files.sortedBy { it.createTime }
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Storage Info Header
        if (storageInfo != null) {
            StorageInfoCard(storageInfo)
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${files.size} files",
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sort button
                var showSortMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, "Sort")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by Name") },
                        onClick = {
                            sortBy = SortBy.NAME
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortBy == SortBy.NAME) Icon(Icons.Default.Check, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Size") },
                        onClick = {
                            sortBy = SortBy.SIZE
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortBy == SortBy.SIZE) Icon(Icons.Default.Check, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Date") },
                        onClick = {
                            sortBy = SortBy.DATE
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortBy == SortBy.DATE) Icon(Icons.Default.Check, null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text(if (sortAscending) "Descending" else "Ascending") },
                        onClick = {
                            sortAscending = !sortAscending
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (sortAscending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                null
                            )
                        }
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
            }
        }

        Divider()

        // File List
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No files found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedFiles, key = { it.fileUrl }) { file ->
                    FileListItem(
                        file = file,
                        onPrintClick = { onPrintFile(file.fileUrl) },
                        onDeleteClick = {
                            fileToDelete = file
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete \"${fileToDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(fileToDelete!!.fileUrl)
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    fileToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StorageInfoCard(storageInfo: FileListData) {
    val usedPercent = if (storageInfo.totalCapacity > 0) {
        (storageInfo.totalCapacity - storageInfo.freeCapacity).toFloat() / storageInfo.totalCapacity
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${(usedPercent * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { usedPercent },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatBytes(storageInfo.totalCapacity - storageInfo.freeCapacity) + " used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = formatBytes(storageInfo.freeCapacity) + " free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FileListItem(
    file: PrintFile,
    onPrintClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon and info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatBytes(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatDate(file.createTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (file.layer != null) {
                        Text(
                            text = "${file.layer} layers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPrintClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Print",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Print") },
                        onClick = {
                            onPrintClick()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

private enum class SortBy {
    NAME, SIZE, DATE
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}
