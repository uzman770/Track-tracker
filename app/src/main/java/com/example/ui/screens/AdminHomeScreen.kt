package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StatusComing
import com.example.ui.theme.StatusDelayed
import com.example.ui.theme.StatusNotComing
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    viewModel: AppViewModel
) {
    val adminName by viewModel.currentAdminName.collectAsState()
    val adminId by viewModel.currentAdminId.collectAsState()
    val areasWithStatus by viewModel.areasWithStatusList.collectAsState()

    // Filter areas managed by this specific admin
    val managedAreas = areasWithStatus.filter { it.area.adminId == adminId }

    // Dropdown Selection State for area
    var expandedAreaDropdown by remember { mutableStateOf(false) }
    var selectedAreaIndex by remember { mutableStateOf(0) }
    val currentSelectedArea = if (managedAreas.isNotEmpty() && selectedAreaIndex < managedAreas.size) managedAreas[selectedAreaIndex] else null

    // Daily update state variables
    var activeStatus by remember { mutableStateOf("coming") } // "coming" | "not_coming" | "delayed"
    var noteInput by remember { mutableStateOf("") }

    // New Custom Area creation variables
    var showCreateAreaSection by remember { mutableStateOf(false) }
    var newAreaName by remember { mutableStateOf("") }
    var newAreaSchedule by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Managing: $adminName", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.RoleSelection) },
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.RoleSelection) },
                        modifier = Modifier.testTag("admin_switch_role_button")
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Switch Persona")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Info Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = "Admin icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = adminName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Assigned Areas: ${managedAreas.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (managedAreas.isEmpty()) {
                // Empty state if no areas managed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Areas Managed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You are not assigned to manage any neighborhoods yet. Create an area below.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Section 1: Today's Status Manager Form
                Text(
                    text = "Broadcast Today's Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Area Selector Dropdown
                        Text(
                            text = "Select Neighborhood:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedAreaDropdown,
                            onExpandedChange = { expandedAreaDropdown = !expandedAreaDropdown }
                        ) {
                            OutlinedTextField(
                                value = currentSelectedArea?.area?.name ?: "Select Area",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAreaDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("admin_area_dropdown"),
                                shape = MaterialTheme.shapes.medium
                            )

                            ExposedDropdownMenu(
                                expanded = expandedAreaDropdown,
                                onDismissRequest = { expandedAreaDropdown = false }
                            ) {
                                managedAreas.forEachIndexed { index, areaWithStatus ->
                                    DropdownMenuItem(
                                        text = { Text(areaWithStatus.area.name) },
                                        onClick = {
                                            selectedAreaIndex = index
                                            expandedAreaDropdown = false
                                        },
                                        modifier = Modifier.testTag("admin_dropdown_item_${areaWithStatus.area.id}")
                                    )
                                }
                            }
                        }

                        // Display Selected Area's Schedule and Active Status
                        currentSelectedArea?.let { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Schedule: ${item.area.schedule}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Current Status: ${item.todayStatus?.status?.uppercase() ?: "NONE"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.todayStatus?.status) {
                                        "coming" -> StatusComing
                                        "not_coming" -> StatusNotComing
                                        "delayed" -> StatusDelayed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // Status Selector Buttons (Coming, Not Coming, Delayed)
                        Text(
                            text = "Set Pickup Status:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Coming Today Button
                            StatusSegmentButton(
                                label = "Coming",
                                icon = "🚛",
                                isSelected = activeStatus == "coming",
                                activeColor = StatusComing,
                                onClick = { activeStatus = "coming" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_status_coming")
                            )

                            // Not Coming Button
                            StatusSegmentButton(
                                label = "No Pickup",
                                icon = "❌",
                                isSelected = activeStatus == "not_coming",
                                activeColor = StatusNotComing,
                                onClick = { activeStatus = "not_coming" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_status_not_coming")
                            )

                            // Delayed Button
                            StatusSegmentButton(
                                label = "Delayed",
                                icon = "⏰",
                                isSelected = activeStatus == "delayed",
                                activeColor = StatusDelayed,
                                onClick = { activeStatus = "delayed" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_status_delayed")
                            )
                        }

                        // Note text input (optional)
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("Custom Note (optional)") },
                            placeholder = { Text("e.g., Arriving after 2 PM due to service repair") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_note_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = "Note Icon") },
                            shape = MaterialTheme.shapes.medium
                        )

                        // Submit Button
                        Button(
                            onClick = {
                                currentSelectedArea?.let { item ->
                                    viewModel.setDailyStatus(
                                        areaId = item.area.id,
                                        status = activeStatus,
                                        note = if (noteInput.isNotBlank()) noteInput else null
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Broadcast sent successfully to ${item.area.name}!"
                                        )
                                    }
                                    noteInput = "" // Clear after update
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_broadcast_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "Upload Broadcast")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Broadcast Status Update", fontWeight = FontWeight.Bold)
                        }

                        // Quick view history button
                        currentSelectedArea?.let { item ->
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(AppScreen.AdminHistory(item.area.id)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_view_history_jump"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = "History")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View ${item.area.name} Status History")
                            }
                        }
                    }
                }
            }

            // Section 2: Create Custom Residential Area Expandable Form
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateAreaSection = !showCreateAreaSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddHomeWork, contentDescription = "Add Area", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Create New Residential Area",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showCreateAreaSection = !showCreateAreaSection }) {
                            Icon(
                                imageVector = if (showCreateAreaSection) Icons.Default.Check else Icons.Default.PendingActions,
                                contentDescription = "Expand/Collapse"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCreateAreaSection) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newAreaName,
                                onValueChange = { newAreaName = it },
                                label = { Text("Neighborhood Name") },
                                placeholder = { Text("e.g., Shady Pines") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_create_area_name"),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )

                            OutlinedTextField(
                                value = newAreaSchedule,
                                onValueChange = { newAreaSchedule = it },
                                label = { Text("Weekly Pickup Schedule") },
                                placeholder = { Text("e.g., Monday, Wednesday") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_create_area_schedule"),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )

                            Button(
                                onClick = {
                                    if (newAreaName.isNotBlank() && newAreaSchedule.isNotBlank()) {
                                        viewModel.createCustomArea(newAreaName, newAreaSchedule)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Neighborhood area created successfully!")
                                        }
                                        newAreaName = ""
                                        newAreaSchedule = ""
                                        showCreateAreaSection = false
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Please fill out both name and schedule.")
                                        }
                                    }
                                },
                                enabled = newAreaName.isNotBlank() && newAreaSchedule.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_submit_create_area_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Create Area", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatusSegmentButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected) 0.dp else 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
