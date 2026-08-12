package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarCommentEntity
import com.example.data.CalendarEventEntity
import com.example.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    calendarEvents: List<CalendarEventEntity>,
    activeUser: UserEntity?,
    onBack: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onCalendarOpened()
    }
    val context = LocalContext.current

    val currentCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH)) } // 0-indexed
    var selectedDayTimestamp by remember { mutableStateOf(currentCal.timeInMillis) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMonthYearSelector by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    val pendingWidgetCalendar by viewModel.pendingWidgetCalendar.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pendingWidgetCalendar) {
        if (pendingWidgetCalendar != null) showAddEventDialog = true
    }

    var selectedEventForDetail by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var eventToEdit by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val monthNames = listOf(
        "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
        "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
    )

    // Events filter
    val displayedMonthEvents = remember(calendarEvents, displayedYear, displayedMonth, searchQuery, isSearchActive) {
        if (isSearchActive && searchQuery.isNotBlank()) {
            calendarEvents.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        } else {
            calendarEvents.filter { evt ->
                val cal = Calendar.getInstance().apply { timeInMillis = evt.startDateTimestamp }
                cal.get(Calendar.YEAR) == displayedYear && cal.get(Calendar.MONTH) == displayedMonth
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showMonthYearSelector = true }
                    ) {
                        Text(
                            text = "${monthNames[displayedMonth]} $displayedYear ▼",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Image(painter = painterResource(id = R.drawable.lupa), contentDescription = "Hledat", modifier = Modifier.size(20.dp))
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            val now = Calendar.getInstance()
                            displayedYear = now.get(Calendar.YEAR)
                            displayedMonth = now.get(Calendar.MONTH)
                            selectedDayTimestamp = now.timeInMillis
                            isSearchActive = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00c73c)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("DNES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Hledat událost, poznámku", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color.White.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }

            // Monthly Day Grid - zelený styl + SWIPE doleva/doprava pro další/předchozí měsíc - šipky smazány
            var swipeOffset by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset < -120) {
                            if (displayedMonth == 11) { displayedMonth = 0; displayedYear++ } else displayedMonth++
                        } else if (swipeOffset > 120) {
                            if (displayedMonth == 0) { displayedMonth = 11; displayedYear-- } else displayedMonth--
                        }
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            }) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF047000))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne").forEach { d -> Text(text = d, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val cal = Calendar.getInstance().apply { set(Calendar.YEAR, displayedYear); set(Calendar.MONTH, displayedMonth); set(Calendar.DAY_OF_MONTH, 1) }
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                        val todayCal = Calendar.getInstance()
                        val isCurrentMonthToday = todayCal.get(Calendar.YEAR) == displayedYear && todayCal.get(Calendar.MONTH) == displayedMonth
                        val todayDayNumber = todayCal.get(Calendar.DAY_OF_MONTH)
                        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            items(firstDayOfWeek) { Box(modifier = Modifier.size(36.dp)) }
                            items(daysInMonth) { dayIdx ->
                                val dayNum = dayIdx + 1
                                val isToday = isCurrentMonthToday && dayNum == todayDayNumber
                                val isSelectedDay = remember(selectedDayTimestamp) {
                                    val sCal = Calendar.getInstance().apply { timeInMillis = selectedDayTimestamp }
                                    sCal.get(Calendar.YEAR) == displayedYear && sCal.get(Calendar.MONTH) == displayedMonth && sCal.get(Calendar.DAY_OF_MONTH) == dayNum
                                }
                                val dayEvents = displayedMonthEvents.filter { evt -> val eCal = java.util.Calendar.getInstance().apply { timeInMillis = evt.startDateTimestamp }; eCal.get(java.util.Calendar.DAY_OF_MONTH) == dayNum }
                                val hasHigh = dayEvents.any { it.priority == "HIGH" }
                                val hasMedium = dayEvents.any { it.priority == "MEDIUM" }
                                val hasLow = dayEvents.any { it.priority == "LOW" }
                                val maxPriority = when { hasHigh -> "HIGH"; hasMedium -> "MEDIUM"; hasLow -> "LOW"; else -> "NONE" }
                                val priorityBorder = when(maxPriority){
                                    "HIGH" -> androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
                                    "MEDIUM" -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF97316).copy(alpha = 0.5f))
                                    "LOW" -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEAB308).copy(alpha = 0.5f))
                                    else -> null
                                }
                                val dayFillAlpha by animateFloatAsState(
                                    targetValue = when {
                                        isToday -> 1f
                                        isSelectedDay -> 0.32f
                                        else -> 0f
                                    },
                                    label = "dayFillAlpha"
                                )
                                val dayScale by animateFloatAsState(
                                    targetValue = if (isSelectedDay) 1.08f else 1f,
                                    label = "dayScale"
                                )
                                var dayModifier = Modifier
                                    .padding(2.dp)
                                    .size(36.dp)
                                    .graphicsLayer(scaleX = dayScale, scaleY = dayScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00c73c).copy(alpha = dayFillAlpha))
                                if (priorityBorder != null) {
                                    dayModifier = dayModifier.border(priorityBorder, CircleShape)
                                }
                                dayModifier = dayModifier.border(if (isSelectedDay) 1.5.dp else 0.dp, Color(0xFF12db70), CircleShape)
                                Box(modifier = dayModifier.clickable { val selCal = Calendar.getInstance().apply { set(displayedYear, displayedMonth, dayNum) }; selectedDayTimestamp = selCal.timeInMillis }, contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "$dayNum", color = if (isToday) Color.White else Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = if (isToday || isSelectedDay) FontWeight.Bold else FontWeight.Normal)
                                        if (dayEvents.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                dayEvents.take(3).forEach { evt -> val dotColor = try { Color(android.graphics.Color.parseColor(evt.colorCategoryHex)) } catch (e: Exception) { Color(0xFF00c73c) }; Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Events List Header & Add Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Události v měsíci (${displayedMonthEvents.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { showAddEventDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00c73c))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Přidat událost", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Events List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (displayedMonthEvents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Žádné události v tomto období", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedMonthEvents) { evt ->
                            CalendarEventItemRow(
                                event = evt,
                                activeUserId = activeUser?.id ?: "",
                                onClickItem = { selectedEventForDetail = evt },
                                onEdit = {
                                    if (activeUser?.id == evt.authorId) {
                                        eventToEdit = evt
                                    }
                                },
                                onDelete = { viewModel.deleteCalendarEvent(evt.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Month & Year Selector Dialog
    if (showMonthYearSelector) {
        AlertDialog(
            onDismissRequest = { showMonthYearSelector = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { displayedYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = null, tint = Color.White)
                    }
                    Text("$displayedYear", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { displayedYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White)
                    }
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(12) { mIdx ->
                        val isSelected = mIdx == displayedMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF00c73c) else Color(0xFF0F172A))
                                .clickable {
                                    displayedMonth = mIdx
                                    showMonthYearSelector = false
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthNames[mIdx],
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthYearSelector = false }) {
                    Text("Zavřít", color = Color(0xFF60A5FA))
                }
            }
        )
    }

    // Add Event Dialog

    if (showAddEventDialog) {
        val calendarContext = LocalContext.current
        val todayStart = remember {
            java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 9); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }.timeInMillis
        }
        val widgetCalTitle = pendingWidgetCalendar
        val effectiveTimestamp = if (widgetCalTitle != null) todayStart else selectedDayTimestamp
        CalendarEventEditorDialog(
            existingEvent = null,
            initialTitle = widgetCalTitle ?: "",
            defaultTimestamp = effectiveTimestamp,
            onDismiss = { showAddEventDialog = false; viewModel.clearPendingWidgetCalendarEvent() },
            onSave = { title, desc, colorHex, startTs, endTs, isAllDay, loc, isRec, recRule, remMins, priority ->
                viewModel.addCalendarEvent(
                    title = title,
                    description = desc,
                    colorCategoryHex = colorHex,
                    startTs = startTs,
                    endTs = endTs,
                    isAllDay = isAllDay,
                    location = loc,
                    isRecurring = isRec,
                    recurrenceRule = recRule,
                    reminderMins = remMins,
                    priority = priority
                )
                showAddEventDialog = false
                viewModel.clearPendingWidgetCalendarEvent()
            }
        )
    }

    // Edit Event Dialog (Author only)
    if (eventToEdit != null) {
        CalendarEventEditorDialog(
            existingEvent = eventToEdit,
            defaultTimestamp = eventToEdit!!.startDateTimestamp,
            onDismiss = { eventToEdit = null },
            onSave = { title, desc, colorHex, startTs, endTs, isAllDay, loc, isRec, recRule, remMins, priority ->
                eventToEdit?.let { orig ->
                    viewModel.updateCalendarEvent(
                        orig.copy(
                            title = title,
                            description = desc,
                            colorCategoryHex = colorHex,
                            startDateTimestamp = startTs,
                            endDateTimestamp = endTs,
                            isAllDay = isAllDay,
                            location = loc,
                            isRecurring = isRec,
                            recurrenceRule = recRule,
                            reminderMinutes = remMins,
                            priority = priority
                        )
                    )
                }
                eventToEdit = null
            }
        )
    }

    // Detail & Comments Dialog for Event
    if (selectedEventForDetail != null) {
        CalendarEventDetailDialog(
            event = selectedEventForDetail!!,
            activeUser = activeUser,
            viewModel = viewModel,
            onDismiss = { selectedEventForDetail = null }
        )
    }
}

@Composable
fun CalendarEventItemRow(
    event: CalendarEventEntity,
    activeUserId: String,
    onClickItem: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(event.colorCategoryHex))
    } catch (e: Exception) {
        Color(0xFF06B6D4)
    }

    val formattedTime = remember(event.startDateTimestamp, event.endDateTimestamp, event.isAllDay) {
        if (event.isAllDay) "Celý den"
        else {
            val sdf = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
            "${sdf.format(Date(event.startDateTimestamp))} - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.endDateTimestamp))}"
        }
    }

    val priorityBorder = when (event.priority) {
        "HIGH" -> androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFEF4444))
        "MEDIUM" -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF97316))
        "LOW" -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickItem),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = priorityBorder ?: androidx.compose.foundation.BorderStroke(1.5.dp, categoryColor.copy(alpha = 0.9f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().background(categoryColor.copy(alpha = 0.20f)))
            Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(8.dp).background(categoryColor))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp).padding(start = 12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor).border(1.dp, Color.White.copy(alpha=0.6f), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = event.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formattedTime, color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                       
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeUserId == event.authorId) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF64748B)).border(1.5.dp, Color(0xFF94A3B8), CircleShape).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.pen), contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF64748B)).border(1.5.dp, Color(0xFF94A3B8), CircleShape).clickable { showDeleteConfirm = true }, contentAlignment = Alignment.Center) {
                                                           Image(painter = painterResource(id = R.drawable.kos), contentDescription = "Smazat", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Autor: ${event.authorName} ${prioritySymbol(event.priority)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E293B),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.85f),
            title = { Text("Smazat událost?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Opravdu chceš smazat '${event.title}'?", color = Color(0xFFE2E8F0), fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                    Text("ANO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("NE", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun CalendarEventEditorDialog(
    existingEvent: CalendarEventEntity?,
    defaultTimestamp: Long,
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        colorHex: String,
        startTs: Long,
        endTs: Long,
        isAllDay: Boolean,
        loc: String,
        isRec: Boolean,
        recRule: String,
        remMins: Int,
        priority: String
    ) -> Unit
) {
    var title by remember(existingEvent, initialTitle) { mutableStateOf(existingEvent?.title ?: initialTitle) }
    var desc by remember { mutableStateOf(existingEvent?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(existingEvent?.colorCategoryHex ?: "#FF9500") }
    var startTs by remember { mutableStateOf(existingEvent?.startDateTimestamp ?: defaultTimestamp) }
    var endTs by remember { mutableStateOf(existingEvent?.endDateTimestamp ?: (defaultTimestamp + 3600000)) }
    var isAllDay by remember { mutableStateOf(existingEvent?.isAllDay ?: false) }
    var location by remember { mutableStateOf(existingEvent?.location ?: "") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceRule by remember { mutableStateOf("Žádné") }
    var reminderMins by remember { mutableStateOf(existingEvent?.reminderMinutes ?: 15) }
    var priority by remember { mutableStateOf(existingEvent?.priority ?: "NONE") }

    val context = LocalContext.current

    val categoryColors = listOf(
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
        title = { Text(if (existingEvent == null) "Nová událost v kalendáři" else "Upravit událost", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Title - výraznější bílá
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název události", color = Color.White, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Název události", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color.White.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Category Color circle selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kategorie barva:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(8.dp))
                    categoryColors.forEach { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Cyan })
                                .border(if (selectedColorHex == hex) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColorHex = hex }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. All Day switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Celodenní událost", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00c73c),
                            uncheckedThumbColor = Color(0xFF12db70),
                            checkedTrackColor = Color(0xFF047000),
                            uncheckedTrackColor = Color(0xFF334155),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }

                // 4. Start & End Pickers
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = startTs }
                                DatePickerDialog(context, { _, y, m, d ->
                                    TimePickerDialog(context, { _, h, min ->
                                        val sCal = Calendar.getInstance().apply { set(y, m, d, h, min) }
                                        startTs = sCal.timeInMillis
                                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Začátek:\n${SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(startTs))}", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                        }

                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = endTs }
                                DatePickerDialog(context, { _, y, m, d ->
                                    TimePickerDialog(context, { _, h, min ->
                                        val eCal = Calendar.getInstance().apply { set(y, m, d, h, min) }
                                        endTs = eCal.timeInMillis
                                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Konec:\n${SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(endTs))}", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Poznámka k události", color = Color.White, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Poznámka k události", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().height(65.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color.White.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    )
                )

                               Spacer(modifier = Modifier.height(8.dp))

                Text("Priorita:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf("NONE", "LOW", "MEDIUM", "HIGH").forEach { pKey ->
                        PriorityBadge(
                            priority = pKey,
                            selected = priority == pKey,
                            onClick = { priority = pKey },
                            size = 38.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canSave = title.isNotBlank()
            Button(
                onClick = { if (canSave) onSave(title, desc, selectedColorHex, startTs, endTs, isAllDay, location, isRecurring, recurrenceRule, reminderMins, priority) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00c73c), disabledContainerColor = Color(0xFF334155))
            ) {
                Text("Uložit událost", fontWeight = FontWeight.Bold, color = if (canSave) Color.White else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Storno", color = Color.Gray) }
        }
    )
}

@Composable
fun CalendarEventDetailDialog(
    event: CalendarEventEntity,
    activeUser: UserEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.getCommentsForCalendarEvent(event.id).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = { Text(event.title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (event.description.isNotBlank()) {
                    Text(event.description, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

           
                Text("Autor: ${event.authorName} • ${event.editCount}x upraveno", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Diskuse k události (${comments.size}):", fontWeight = FontWeight.Bold, color = Color(0xFF00c73c), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                val greenColorsHex = listOf("#00c73c","#c3ed07","#1eff00","#047000","#12db70")
                fun getUserColor(id: String): Color {
                    val idx = (id.hashCode() % greenColorsHex.size + greenColorsHex.size) % greenColorsHex.size
                    return try { Color(android.graphics.Color.parseColor(greenColorsHex[idx])) } catch (e: Exception) { Color(0xFF00c73c) }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(130.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(comments) { c ->
                        val userCol = getUserColor(c.authorId)
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0F172A)).border(1.5.dp, userCol, RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Column {
                                Text(c.authorName, fontWeight = FontWeight.Bold, color = userCol, fontSize = 11.sp)
                                Text(c.text, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Přidat poznámku...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addCalendarComment(event.id, commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF00c73c))
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
