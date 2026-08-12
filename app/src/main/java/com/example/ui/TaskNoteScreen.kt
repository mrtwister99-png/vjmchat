package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskNoteCommentEntity
import com.example.data.TaskNoteEntity
import com.example.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskNoteScreen(
    viewModel: MainViewModel,
    tasksAndNotes: List<TaskNoteEntity>,
    activeUser: UserEntity?,
    onBack: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onTasksOpened()
    }
    var filterType by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<TaskNoteEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<TaskNoteEntity?>(null) }
    var showCompleted by remember { mutableStateOf(false) }

    val pendingWidgetTask by viewModel.pendingWidgetTask.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pendingWidgetTask) {
        if (pendingWidgetTask != null) { showAddDialog = true }
    }

    val isTaskType: (TaskNoteEntity) -> Boolean = { item ->
        item.type.equals("TASK", true) || item.type.equals("ÚKOLY", true) || item.type.uppercase().contains("TASK")
    }
    val isNoteType: (TaskNoteEntity) -> Boolean = { item ->
        item.type.equals("NOTE", true) || item.type.equals("POZNÁMKY", true)
    }

    val filteredList = remember(tasksAndNotes, filterType) {
        when (filterType) {
            "TASK" -> tasksAndNotes.filter { isTaskType(it) && !it.isCompleted }
            "NOTE" -> tasksAndNotes.filter { isNoteType(it) }
            else -> tasksAndNotes.filter { !it.isCompleted || isNoteType(it) }
        }
    }
    val completedList = remember(tasksAndNotes, filterType) {
        val comp = tasksAndNotes.filter { it.isCompleted }
        when(filterType) {
            "TASK" -> comp.filter { isTaskType(it) }
            "NOTE" -> emptyList()
            else -> comp
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Úkoly & Poznámky", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showCompleted = true },
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White
                ) {
                    Image(painter = painterResource(id = R.drawable.ukol_done), contentDescription = "Splněné", modifier = Modifier.size(26.dp))
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White
                ) {
                    Image(painter = painterResource(id = R.drawable.ukol_plus), contentDescription = "Přidat", modifier = Modifier.size(26.dp))
                }
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                SegmentedButton(selected = filterType == "ALL", onClick = { filterType = "ALL" }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)) { Text("Vše (${tasksAndNotes.count { !it.isCompleted || isNoteType(it) }})") }
                SegmentedButton(selected = filterType == "TASK", onClick = { filterType = "TASK" }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)) { Text("Úkoly (${tasksAndNotes.count { isTaskType(it) && !it.isCompleted }})") }
                SegmentedButton(selected = filterType == "NOTE", onClick = { filterType = "NOTE" }, shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)) { Text("Poznámky (${tasksAndNotes.count { isNoteType(it) }})") }
            }
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if(completedList.isNotEmpty()) "Vše hotovo! Koukni do splněných." else "Žádné položky", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredList) { item ->
                        TaskNoteItemRow(
                            item = item,
                            activeUserId = activeUser?.id ?: "",
                            onToggle = { viewModel.toggleTaskCompletion(item) },
                            onRequestDelete = { viewModel.deleteTaskNoteDirect(item.id) },
                            onOpenDetail = { selectedItemForDetail = item },
                            onEdit = { if (activeUser?.id == item.authorId) { itemToEdit = item } }
                        )
                    }
                }
            }
        }
    }

    if (showCompleted) {
        AlertDialog(
            onDismissRequest = { showCompleted = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Splněné úkoly (${completedList.size})", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (completedList.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Zatím žádný splněný úkol",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        items(completedList) { item ->
                            TaskNoteItemRow(
                                item = item,
                                activeUserId = activeUser?.id ?: "",
                                onToggle = { viewModel.toggleTaskCompletion(item) },
                                onRequestDelete = { viewModel.deleteTaskNoteDirect(item.id) },
                                onOpenDetail = { selectedItemForDetail = item },
                                onEdit = {},
                                isReadOnly = true
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCompleted = false }) { Text("Zavřít", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) } }
        )
    }

    // Dialog for adding task or note
    val widgetTitle = pendingWidgetTask?.first ?: ""
    val widgetType = pendingWidgetTask?.second ?: "TASK"
    if (showAddDialog) {
        TaskNoteEditorDialog(
            existingItem = null,
            initialTitle = widgetTitle,
            initialType = widgetType,
            onDismiss = { showAddDialog = false; viewModel.clearPendingWidgetTask() },
            onSave = { type, title, content, categoryColorHex, reminderTs, priority ->
                viewModel.addTaskOrNote(
                    type = type,
                    title = title,
                    content = content,
                    colorHex = categoryColorHex,
                    isBold = false,
                    isUnderline = false,
                    textColorHex = "#FFFFFF",
                    fontSize = 16,
                    reminderTimestamp = reminderTs,
                    priority = priority
                )
                showAddDialog = false
            }
        )
    }

    // Dialog for editing existing task or note (ONLY author)
    if (itemToEdit != null) {
        TaskNoteEditorDialog(
            existingItem = itemToEdit,
            onDismiss = { itemToEdit = null },
            onSave = { type, title, content, categoryColorHex, reminderTs, priority ->
                itemToEdit?.let { orig ->
                    viewModel.updateTaskOrNote(
                        orig.copy(
                            type = type,
                            title = title,
                            content = content,
                            colorHex = categoryColorHex,
                            isBold = false,
                            isUnderline = false,
                            textColorHex = "#FFFFFF",
                            fontSizeSp = 16,
                            reminderTimestamp = reminderTs,
                            priority = priority
                        )
                    )
                }
                itemToEdit = null
            }
        )
    }

    // Detail & Comments Dialog for all members
    if (selectedItemForDetail != null) {
        TaskNoteDetailDialog(
            item = selectedItemForDetail!!,
            activeUser = activeUser,
            viewModel = viewModel,
            onDismiss = { selectedItemForDetail = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskNoteItemRow(
    item: TaskNoteEntity,
    activeUserId: String,
    onToggle: () -> Unit,
    onRequestDelete: () -> Unit,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    isReadOnly: Boolean = false
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isTaskType = item.type.equals("TASK", true) || item.type.equals("ÚKOLY", true) || item.type.uppercase().contains("TASK")
    val categoryColor = try { Color(android.graphics.Color.parseColor(item.colorHex ?: "#3B82F6")) } catch(e:Exception){ Color(0xFF3B82F6) }
    val baseCardColor = Color(0xFF0F172A)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isTaskType) Arrangement.End else Arrangement.Start
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).combinedClickable(onClick = onOpenDetail, onLongClick = {}),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = baseCardColor),
        border = when(item.priority) {
            "HIGH" -> androidx.compose.foundation.BorderStroke(3.dp, priorityColor(item.priority))
            "MEDIUM" -> androidx.compose.foundation.BorderStroke(2.dp, priorityColor(item.priority))
            "LOW" -> androidx.compose.foundation.BorderStroke(1.dp, priorityColor(item.priority))
            else -> null
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().background(categoryColor.copy(alpha = 0.20f)))
            Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (isTaskType) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E), uncheckedColor = Color.White),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = if (item.isCompleted) Color.Gray else Color.White,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "(Autor: ${item.authorName})", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    if (item.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.content, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                    if (item.reminderTimestamp != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF334155)).padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(item.reminderTimestamp!!)),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (!isReadOnly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                   if (activeUserId == item.authorId) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF64748B)).border(1.5.dp, Color(0xFF94A3B8), CircleShape).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.pen), contentDescription = "Upravit", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF64748B)).border(1.5.dp, Color(0xFF94A3B8), CircleShape).clickable { showDeleteConfirm = true }, contentAlignment = Alignment.Center) {
                            Image(painter = painterResource(id = R.drawable.kos), contentDescription = "Smazat", modifier = Modifier.size(18.dp))
                        }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF334155)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if(isTaskType) "ÚKOL" else "POZNÁMKA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(prioritySymbol(item.priority), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
    }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(16.dp)),
            containerColor = Color(0xFF1E293B),
            title = { Text("Smazat?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Opravdu smazat '${item.title}'?", color = Color(0xFFE2E8F0)) },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onRequestDelete() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("ANO", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("NE", color = Color.Gray) } }
        )
    }
}

@Composable
fun TaskNoteEditorDialog(
    existingItem: TaskNoteEntity?,
    initialTitle: String = "",
    initialType: String = "TASK",
    onDismiss: () -> Unit,
    onSave: (type: String, title: String, content: String, categoryColorHex: String, reminderTs: Long?, priority: String) -> Unit
) {
    var selectedType by remember(existingItem, initialType) { mutableStateOf(existingItem?.type ?: initialType) }
    var title by remember(existingItem, initialTitle) { mutableStateOf(existingItem?.title ?: initialTitle.ifBlank { "" }) }
    var content by remember { mutableStateOf(existingItem?.content ?: "") }
    var selectedCategoryColor by remember { mutableStateOf(existingItem?.colorHex ?: "#FF9500") }
    var reminderTimestamp by remember { mutableStateOf<Long?>(existingItem?.reminderTimestamp) }
    var selectedPriority by remember { mutableStateOf(existingItem?.priority ?: "NONE") }

    val context = LocalContext.current

    val categoryOptions = listOf(
        "#FF9500" to "1",
        "#FF0000" to "2",
        "#FF5E00" to "3",
        "#FA915C" to "4",
        "#FFC800" to "5"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = {
            Text(
                text = if (existingItem == null) "Přidat $selectedType" else "Upravit $selectedType",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == "TASK",
                        onClick = { selectedType = "TASK" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Úkol")
                    }
                    SegmentedButton(
                        selected = selectedType == "NOTE",
                        onClick = { selectedType = "NOTE" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Poznámka")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název", color = Color.White) },
                    placeholder = { Text("Název", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Obsah...", color = Color.White) },
                    placeholder = { Text("Obsah...", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Kategorie barvy - nad prioritu
                Text("Kategorie:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    categoryOptions.forEach { (hex, name) ->
                        val isSel = selectedCategoryColor == hex
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch(e:Exception){ Color.Blue }
                        Box(
                            modifier = Modifier
                               .size(28.dp)
                               .clip(CircleShape)
                               .background(c)
                               .border(if (isSel) 3.dp else 1.dp, if (isSel) Color.White else Color.Gray, CircleShape)
                               .clickable { selectedCategoryColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Priority selector - 4 stavy jako v chatu
                Text("Priorita:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf("NONE", "LOW", "MEDIUM", "HIGH").forEach { pKey ->
                        PriorityBadge(
                            priority = pKey,
                            selected = selectedPriority == pKey,
                            onClick = { selectedPriority = pKey },
                            size = 38.dp
                        )
                    }
                }

                if (selectedType == "TASK") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val setCal = Calendar.getInstance()
                                            setCal.set(year, month, day, hour, minute)
                                            reminderTimestamp = setCal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (reminderTimestamp != null) {
                                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    "Připomenutí: ${sdf.format(Date(reminderTimestamp!!))}"
                                } else "Nastavit datum & čas připomenutí",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canSave = title.isNotBlank()
            Button(
                onClick = { if (canSave) onSave(selectedType, title, content, selectedCategoryColor, reminderTimestamp, selectedPriority) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), disabledContainerColor = Color(0xFF334155))
            ) {
                Text("Uložit", color = if (canSave) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun TaskNoteDetailDialog(
    item: TaskNoteEntity,
    activeUser: UserEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.getCommentsForTaskNote(item.id).collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (item.content.isNotBlank()) {
                    Text(
                        text = item.content,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (item.reminderTimestamp != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.reminderTimestamp!!)),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Vytvořil: ${item.authorName} • Editací: ${item.editCount}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Komentáře člena týmu (${comments.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.height(4.dp))

                              LazyColumn(
                    state = listState,
                    modifier = Modifier
                       .fillMaxWidth()
                       .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(comments) { c ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(c.authorName, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), fontSize = 11.sp)
                                Text(c.text, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Přidat komentář...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                viewModel.addTaskNoteComment(item.id, newCommentText)
                                newCommentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = Color(0xFF3B82F6))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) {
                Text("Zavřít")
            }
        }
    )
}
