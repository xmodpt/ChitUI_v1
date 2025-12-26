package com.chitui.client.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chitui.client.data.model.Printer
import com.chitui.client.data.repository.ChitUIRepository
import com.chitui.client.presentation.files.FilesScreen
import com.chitui.client.presentation.plugins.PluginsScreen
import com.chitui.client.presentation.printer.PrinterInfoScreen
import com.chitui.client.presentation.theme.OnlineGreen
import com.chitui.client.presentation.theme.OfflineRed
import com.chitui.client.util.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val repository = remember { ChitUIRepository(preferencesManager) }
    val viewModel = remember { MainViewModel(repository, preferencesManager) }
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val tabs = listOf("Printer", "Files", "Plugins")

    // Show toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            // In a real app, use SnackbarHost
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChitUI Client")
                        if (uiState.isConnected) {
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnlineGreen
                            )
                        } else {
                            Text(
                                text = "Disconnected",
                                style = MaterialTheme.typography.bodySmall,
                                color = OfflineRed
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.requestPrinters() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Discover Printers") },
                            onClick = {
                                viewModel.discoverPrinters()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Search, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                viewModel.logout(onLogout)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Logout, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Printer List Sidebar
            PrinterListSidebar(
                printers = uiState.printers,
                selectedPrinter = uiState.selectedPrinter,
                onPrinterSelected = { viewModel.selectPrinter(it) },
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
            )

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.selectedPrinter != null) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    // Tab Content
                    when (selectedTab) {
                        0 -> PrinterInfoScreen(
                            printer = uiState.selectedPrinter!!,
                            status = uiState.printerStatus,
                            attributes = uiState.printerAttributes,
                            onPauseClick = { viewModel.pausePrint() },
                            onResumeClick = { viewModel.resumePrint() },
                            onStopClick = { viewModel.stopPrint() }
                        )
                        1 -> FilesScreen(
                            files = uiState.files,
                            storageInfo = uiState.storageInfo,
                            onDeleteFile = { viewModel.deleteFile(it) },
                            onPrintFile = { viewModel.startPrint(it) },
                            onRefresh = { viewModel.refreshFiles() }
                        )
                        2 -> PluginsScreen(
                            plugins = uiState.plugins,
                            repository = repository
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No printer selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (uiState.printers.isEmpty()) {
                                Button(onClick = { viewModel.discoverPrinters() }) {
                                    Icon(Icons.Default.Search, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Discover Printers")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PrinterListSidebar(
    printers: List<Printer>,
    selectedPrinter: Printer?,
    onPrinterSelected: (Printer) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Printers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Divider()

            if (printers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No printers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn {
                    items(printers) { printer ->
                        PrinterListItem(
                            printer = printer,
                            isSelected = printer.id == selectedPrinter?.id,
                            onClick = { onPrinterSelected(printer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterListItem(
    printer: Printer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
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
                    text = printer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = printer.ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }

            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = "Status",
                modifier = Modifier.size(12.dp),
                tint = if (printer.online) OnlineGreen else OfflineRed
            )
        }
    }
}
