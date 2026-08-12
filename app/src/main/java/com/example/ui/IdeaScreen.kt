package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IdeaEntity
import com.example.data.UserEntity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaScreen(
    viewModel: MainViewModel,
    ideas: List<IdeaEntity>,
    activeUser: UserEntity?,
    onBack: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onIdeasOpened()
    }
    var selectedTab by remember { mutableStateOf("LIST") } // "LIST", "REALIZACE_DRAFT", "REALIZACE_FINAL"

    var showAddIdeaDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddFactMenu by remember { mutableStateOf(false) }
    var showHotovoDialog by remember { mutableStateOf(false) }
    var showAddFactDialog by remember { mutableStateOf(false) }
    var addFactType by remember { mutableStateOf("Fakt") }
    var addFactText by remember { mutableStateOf("") }
    var selectedDraftIdeaId by remember { mutableStateOf<Long?>(null) }
    val pendingWidgetIdea by viewModel.pendingWidgetIdea.collectAsState()
    androidx.compose.runtime.LaunchedEffect(pendingWidgetIdea) {
        if (pendingWidgetIdea != null) showAddIdeaDialog = true
    }
    var selectedIdeaForProposal by remember { mutableStateOf<IdeaEntity?>(null) }
    var selectedIdeaForFinale by remember { mutableStateOf<IdeaEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val users by viewModel.users.collectAsState()
    val localContext = androidx.compose.ui.platform.LocalContext.current

    val currentTabIdeas = remember(ideas, selectedTab) {
        ideas.filter { it.stage == selectedTab }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nápady & Realizace", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF1E293B)).border(1.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text("?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
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
            // Stage Tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == "LIST",
                    onClick = { selectedTab = "LIST" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Nápady (${ideas.count { it.stage == "LIST" }})", fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = selectedTab == "REALIZACE_DRAFT",
                    onClick = { selectedTab = "REALIZACE_DRAFT" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Předrealizace (${ideas.count { it.stage == "REALIZACE_DRAFT" }})", fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = selectedTab == "REALIZACE_FINAL",
                    onClick = { selectedTab = "REALIZACE_FINAL" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Realizace (${ideas.count { it.stage == "REALIZACE_FINAL" }})", fontSize = 12.sp)
                }
            }

            // Main List Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentTabIdeas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Zatím žádné nápady v této sekci", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(currentTabIdeas) { idea ->
                            IdeaCardItem(
                                idea = idea,
                                activeUser = activeUser,
                                viewModel = viewModel,
                                onToggleStar = { viewModel.toggleStar(idea.id) },
                                onSendProposal = { selectedIdeaForProposal = idea },
                                onFinaleClick = { selectedIdeaForFinale = idea }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val hasDraft = ideas.any { it.stage == "REALIZACE_DRAFT" }
            val hasFinalConfirmed = ideas.any { it.stage == "REALIZACE_FINAL" }

            if (selectedTab == "LIST") {
                Button(
                    onClick = { showAddIdeaDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Přidat nový nápad", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            if (selectedTab == "REALIZACE_DRAFT") {
                Button(
                    onClick = { if (hasDraft) showAddFactMenu = true },
                    enabled = hasDraft,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasDraft) Color(0xFF3B82F6) else Color(0xFF334155), disabledContainerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Přidat", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (selectedTab == "REALIZACE_FINAL") {
                Button(
                    onClick = { if (hasFinalConfirmed) showHotovoDialog = true },
                    enabled = hasFinalConfirmed,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasFinalConfirmed) Color(0xFF22C55E) else Color(0xFF334155), disabledContainerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("HOTOVO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add Idea Dialog (with Potenciál Selection)
    if (showAddIdeaDialog) {
        val widgetIdeaTitle = pendingWidgetIdea ?: ""
        var ideaTitle by remember(pendingWidgetIdea) { mutableStateOf(widgetIdeaTitle) }
        var ideaDesc by remember { mutableStateOf("") }
        var selectedPotential by remember { mutableIntStateOf(0) }
        var selectedPriority by remember { mutableStateOf("NONE") }

        AlertDialog(
            onDismissRequest = { showAddIdeaDialog = false; viewModel.clearPendingWidgetIdea() },
            containerColor = Color(0xFF1E293B),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("Přidat nový nápad", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = ideaTitle,
                        onValueChange = { ideaTitle = it },
                        label = { Text("Název nápadu", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ideaDesc,
                        onValueChange = { ideaDesc = it },
                        label = { Text("Popis nápadu...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Potenciál:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("NONE", "LOW", "MEDIUM", "HIGH").forEach { priVal ->
                            PriorityBadge(
                                priority = priVal,
                                selected = selectedPriority == priVal,
                                onClick = { selectedPriority = priVal },
                                size = 38.dp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        if (ideaTitle.isNotBlank()) {
                            val mappedPotential = when(selectedPriority) {
                                "HIGH" -> 3
                                "MEDIUM" -> 2
                                "LOW" -> 1
                                else -> 0
                            }
                            viewModel.addIdea(ideaTitle, ideaDesc, mappedPotential, selectedPriority, context)
                            showAddIdeaDialog = false
                            viewModel.clearPendingWidgetIdea()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))
                ) {
                    Text("Uložit nápad", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIdeaDialog = false; viewModel.clearPendingWidgetIdea() }) {
                    Text("Zrušit", color = Color.Gray)
                }
            }
        )
    }

    // Proposal Form Dialog
    if (selectedIdeaForProposal != null) {
        val idea = selectedIdeaForProposal!!
        var propName by remember { mutableStateOf(idea.proposalName.ifEmpty { idea.title }) }
        var propDesc by remember { mutableStateOf(idea.proposalDesc.ifEmpty { idea.description }) }

        AlertDialog(
            onDismissRequest = { selectedIdeaForProposal = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Poslat návrh požadavku k potvrzení", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Tento návrh pošle ČERVENOU notifikaci ostatním členům!", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = propName,
                        onValueChange = { propName = it },
                        label = { Text("Název požadavku/projektu") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = propDesc,
                        onValueChange = { propDesc = it },
                        label = { Text("Podrobný popis & specifikace") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitIdeaProposal(idea.id, propName, propDesc, "[]")
                        selectedIdeaForProposal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Poslat k potvrzení 🔴", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedIdeaForProposal = null }) {
                    Text("Storno", color = Color.Gray)
                }
            }
        )
    }

    // HELP ? - vysvětlivka
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Jak fungují Nápady?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("NÁPADY = sem dáš nápad. Když 2 lidi dají ⭐ jde do Realizace?\n\nREALIZACE? = doplníš fakty, soubory, fotky. Když 3 lidi dají 👑 jde do Realizace.\n\nREALIZACE = finální schvalování. Po schválení tlačítko HOTOVO ukáže finál.", color = Color(0xFFE2E8F0), fontSize = 13.sp) },
            confirmButton = { Button(onClick = { showHelpDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) { Text("OK") } }
        )
    }

    // MENU pro Přidat v Realizace?
    if (showAddFactMenu) {
        AlertDialog(
            onDismissRequest = { showAddFactMenu = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Přidat do nápadu", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Fakt", "Obrázek", "Soubor", "Odkaz").forEach { type ->
                        Button(onClick = { addFactType = type; addFactText = ""; showAddFactDialog = true; showAddFactMenu = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) { Text(type, color = Color.White) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddFactMenu = false }) { Text("Zrušit", color = Color.Gray) } }
        )
    }

    // DIALOG pro zadání faktu / odkazu
    if (showAddFactDialog) {
        val draftIdeas = ideas.filter { it.stage == "REALIZACE_DRAFT" }
        AlertDialog(
            onDismissRequest = { showAddFactDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Přidat $addFactType", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (draftIdeas.size > 1) {
                        Text("Vyber nápad:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        draftIdeas.forEach { di ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { selectedDraftIdeaId = di.id }, colors = CardDefaults.cardColors(containerColor = if (selectedDraftIdeaId == di.id) Color(0xFF3B82F6) else Color(0xFF0F172A))) {
                                Text(di.title, modifier = Modifier.padding(8.dp), color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (draftIdeas.isNotEmpty()) {
                        selectedDraftIdeaId = draftIdeas.first().id
                    }
                    OutlinedTextField(value = addFactText, onValueChange = { addFactText = it }, label = { Text(when(addFactType){"Fakt"->"Text faktu"; "Obrázek"->"URL obrázku / popis"; "Soubor"->"Název souboru pdf/doc"; "Odkaz"->"https://..."; else->"Hodnota"}) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val targetId = selectedDraftIdeaId ?: draftIdeas.firstOrNull()?.id
                    if (targetId != null && addFactText.isNotBlank()) {
                        when (addFactType) {
                            "Fakt" -> viewModel.addInfoToIdea(targetId, "FAKT: $addFactText")
                            "Odkaz" -> viewModel.addInfoToIdea(targetId, "ODKAZ: $addFactText")
                            "Obrázek" -> viewModel.attachFileToIdea(targetId, "IMG:$addFactText")
                            "Soubor" -> viewModel.attachFileToIdea(targetId, addFactText)
                        }
                    }
                    showAddFactDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) { Text("Uložit") }
            },
            dismissButton = { TextButton(onClick = { showAddFactDialog = false }) { Text("Zrušit", color = Color.Gray) } }
        )
    }

    // HOTOVO - zelené finální
    if (showHotovoDialog) {
        val finalIdeas = ideas.filter { it.stage == "REALIZACE_FINAL" }
        AlertDialog(
            onDismissRequest = { showHotovoDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("HOTOVO - Finální nápady", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (finalIdeas.isEmpty()) Text("Žádný finální nápad", color = Color.Gray)
                    else finalIdeas.forEach { fi -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) { Column(modifier = Modifier.padding(10.dp)) { Text(fi.title, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(fi.proposalDesc.ifEmpty { fi.description }, color = Color.White, fontSize = 12.sp) } } }
                }
            },
            confirmButton = { Button(onClick = { showHotovoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))) { Text("Zavřít", color = Color.White) } }
        )
    }

    // Finale Idea Email Summary Dialog - bere reálné maily z profilů
    if (selectedIdeaForFinale != null) {
        val idea = selectedIdeaForFinale!!
        val realEmails = users.filter { it.email.isNotBlank() && it.email.contains("@") }

        AlertDialog(
            onDismissRequest = { selectedIdeaForFinale = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REALIZACE PROJEKTU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Kompletní podklady o schváleném nápadu budou zaslány na e-maily:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (realEmails.isEmpty()) {
                        Text("• Žádné e-maily nenastaveny! Nastav v profilu.", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        realEmails.forEach { u ->
                            Text("• ${u.nickname}: ${u.email}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Projekt: ${idea.title}", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(idea.proposalDesc.ifEmpty { idea.description }, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (realEmails.isNotEmpty()) {
                            val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:")
                                putExtra(android.content.Intent.EXTRA_EMAIL, realEmails.map { it.email }.toTypedArray())
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "REALIZACE PROJEKTU: ${idea.title}")
                                putExtra(android.content.Intent.EXTRA_TEXT, "Projekt: ${idea.title}\n\nPopis: ${idea.proposalDesc.ifEmpty { idea.description }}\n\nAutor: ${idea.authorName}\n\nPosláno z VJM Chat")
                            }
                            try {
                                localContext.startActivity(android.content.Intent.createChooser(emailIntent, "Odeslat e-mail"))
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Žádný e-mail klient nenalezen") }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Nejdřív nastav e-maily v profilech!")
                            }
                        }
                        selectedIdeaForFinale = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Odeslat e-maily", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedIdeaForFinale = null }) {
                    Text("Zavřít", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdeaCardItem(
    idea: IdeaEntity,
    activeUser: UserEntity?,
    viewModel: MainViewModel,
    onToggleStar: () -> Unit,
    onSendProposal: () -> Unit,
    onFinaleClick: () -> Unit
) {
    val activeUserId = activeUser?.id ?: ""
    var showLikeConfirm by remember { mutableStateOf(false) }
    var showApproveConfirm by remember { mutableStateOf(false) }
    val isAdmin = activeUserId == "admin"
    var isExpandedComments by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showAddInfoDialog by remember { mutableStateOf(false) }
    var infoInputText by remember { mutableStateOf("") }

    val starsList = remember(idea.starsJson) {
        try {
            val arr = JSONArray(idea.starsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList<String>() }
    }

    val crownsList = remember(idea.crownsJson) {
        try {
            val arr = JSONArray(idea.crownsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList<String>() }
    }

    val potentialsMap = remember(idea.potentialsJson) {
        try {
            val obj = JSONObject(idea.potentialsJson)
            val map = mutableMapOf<String, Int>()
            obj.keys().forEach { k -> map[k] = obj.optInt(k, 0) }
            map
        } catch (e: Exception) { emptyMap<String, Int>() }
    }

    val infoList = remember(idea.infoListJson) {
        try {
            val arr = JSONArray(idea.infoListJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList<String>() }
    }

    val attachmentsList = remember(idea.attachmentsJson) {
        try {
            val arr = JSONArray(idea.attachmentsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList<String>() }
    }

    val approvalsList = remember(idea.approvalsJson) {
        try {
            val arr = JSONArray(idea.approvalsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList<String>() }
    }

    val hasStarred = starsList.contains(activeUserId)
    val hasCrown = crownsList.contains(activeUserId)
    val isAuthor = idea.authorId == activeUserId

    val comments by viewModel.getCommentsForIdea(idea.id).collectAsState(initial = emptyList())

    val priorityBorder = try {
        when (idea.priority) {
            "HIGH" -> androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFEF4444))
            "MEDIUM" -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF97316))
            "LOW" -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
            else -> null
        }
    } catch (e: Exception) { null }

        Card(
        modifier = Modifier.fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (!isAuthor) {
                            showLikeConfirm = true
                        }
                    },
                    onDoubleTap = {
                        showApproveConfirm = true
                    },
                    onTap = {}
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = priorityBorder ?: when {
            starsList.size >= 3 -> androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFEF4444))
            starsList.size == 2 -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF97316))
            starsList.size == 1 -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
            else -> androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
        }
    ) {
        var showEditIdeaDialog by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                val avgPot = if (potentialsMap.isNotEmpty()) potentialsMap.values.average().toInt() else 0
                val potColor = when(avgPot) {
                    3 -> Color(0xFFEF4444)
                    2 -> Color(0xFFF97316)
                    1 -> Color(0xFFEAB308)
                    else -> Color(0xFF64748B)
                }
                Box(
                    modifier = Modifier.padding(top = 4.dp, end = 6.dp).size(12.dp).clip(CircleShape).background(potColor).border(1.dp, Color.White.copy(alpha=0.3f), CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = idea.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    if (idea.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = idea.description, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAuthor) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, Color(0xFF60A5FA), CircleShape).clickable { showEditIdeaDialog = true }, contentAlignment = Alignment.Center) {
                                Text("✎", color = Color(0xFF60A5FA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B)).border(1.dp, Color(0xFFEAB308), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(text = "${if (idea.stage == "LIST") 1 + approvalsList.size else approvalsList.size}/3", color = Color(0xFFEAB308), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "(Autor: ${idea.authorName})", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (showEditIdeaDialog) {
                var editTitle by remember { mutableStateOf(idea.title) }
                var editDesc by remember { mutableStateOf(idea.description) }
                AlertDialog(
                    onDismissRequest = { showEditIdeaDialog = false },
                    containerColor = Color(0xFF1E293B),
                    title = { Text("Upravit nápad", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Název") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Popis") }, modifier = Modifier.fillMaxWidth().height(90.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.updateIdea(idea.id, editTitle, editDesc); showEditIdeaDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))) { Text("Uložit", color = Color.Black) }
                    },
                    dismissButton = { TextButton(onClick = { showEditIdeaDialog = false }) { Text("Zrušit", color = Color.Gray) } }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // REALIZACE? (REALIZACE_DRAFT stage layout)
            if (idea.stage == "REALIZACE_DRAFT") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Korunky pro postup (${crownsList.size}/3 👑):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEAB308),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { viewModel.toggleCrownOnIdea(idea.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasCrown) Color(0xFFEAB308) else Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (hasCrown) "👑 Uděleno" else "+ Dát 👑",
                                    color = if (hasCrown) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Display attached info notes
                        if (infoList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Doplňující informace:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            infoList.forEach { infoStr ->
                                Text("• $infoStr", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // Display attached files
                        if (attachmentsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Připojené soubory (${attachmentsList.size}):", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            attachmentsList.forEach { fileStr ->
                                Text("📄 $fileStr", fontSize = 11.sp, color = Color(0xFF60A5FA))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action buttons: Left + (Add info), Right + (Add photo/file)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showAddInfoDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Info", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.attachFileToIdea(idea.id, "Dokument_${System.currentTimeMillis()}.pdf")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Soubor", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // REALIZACE (FINAL stage layout)
            if (idea.stage == "REALIZACE_FINAL") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                        if (isAdmin) {
                            Button(
                                onClick = onSendProposal,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Složit požadavky & poslat na potvrzení", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (idea.approvalStatus) {
                                            "CONFIRMED" -> Color(0xFF22C55E)
                                            "REJECTED" -> Color(0xFFEF4444)
                                            "PROPOSED" -> Color(0xFFEF4444)
                                            else -> Color(0xFF64748B)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (idea.approvalStatus) {
                                        "CONFIRMED" -> "Schváleno všemi 🟢 - HOTOVO"
                                        "REJECTED" -> "Zamítnuto 🔴"
                                        "PROPOSED" -> "Čeká na potvrzení (${approvalsList.size}/3) 🔴"
                                        else -> "Čeká na potvrzení - bez návrhu ⚪"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isAdmin) {
                            if (idea.approvalStatus == "PROPOSED") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.respondToProposal(idea.id, true, "Schváleno") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Potvrdit", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { showRejectDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Zamítnout", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (idea.approvalStatus == "CONFIRMED") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onFinaleClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Odeslat podklady e-mailem", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Comments Toggle Section (PURPLE #8B5CF6 theme)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isExpandedComments = !isExpandedComments }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Diskuse & Komentáře (${comments.size})",
                    fontSize = 12.sp,
                    color = Color(0xFF8B5CF6), // Purple color requirement!
                    fontWeight = FontWeight.Bold
                )
            }

            if (isExpandedComments) {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.runtime.LaunchedEffect(comments.size) {
                    if (comments.isNotEmpty()) {
                        listState.animateScrollToItem(comments.size - 1)
                    }
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                       .fillMaxWidth()
                       .heightIn(max = 200.dp)
                       .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                                  items(comments.size) { idx ->
                        val c = comments[idx]
                        Box(
                            modifier = Modifier
                               .fillMaxWidth()
                               .padding(vertical = 2.dp)
                               .clip(RoundedCornerShape(6.dp))
                               .background(Color(0xFF0F172A))
                               .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                               .padding(8.dp)
                        ) {
                            Column {
                                Text(c.authorName, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), fontSize = 11.sp)
                                Text(c.text, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        val commentContext = androidx.compose.ui.platform.LocalContext.current
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Napiš komentář...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8B5CF6)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (commentInput.isNotBlank()) {
                                        viewModel.addProjectComment(idea.id, commentInput, null, commentContext)
                                        commentInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Info Dialog
    if (showAddInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAddInfoDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Přidat informaci k nápadu", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = infoInputText,
                    onValueChange = { infoInputText = it },
                    label = { Text("Informace (např. 'Je na léto', 'Složitá údržba')...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (infoInputText.isNotBlank()) {
                            viewModel.addInfoToIdea(idea.id, infoInputText)
                            infoInputText = ""
                            showAddInfoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInfoDialog = false }) { Text("Storno", color = Color.Gray) }
            }
        )
    }

    if (showLikeConfirm) {
        AlertDialog(
            onDismissRequest = { showLikeConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Líbí se vám tento nápad?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Opravdu se vám tento nápad líbí?", color = Color(0xFFE2E8F0)) },
            confirmButton = {
                Button(onClick = { onToggleStar(); showLikeConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))) {
                    Text("ANO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLikeConfirm = false }) { Text("NE", color = Color.Gray) }
            }
        )
    }

    // Reject reason dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Důvod zamítnutí", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rejectionReasonInput,
                    onValueChange = { rejectionReasonInput = it },
                    label = { Text("Napište důvod zamítnutí...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.respondToProposal(idea.id, false, rejectionReasonInput)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Potvrdit zamítnutí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Storno", color = Color.Gray) }
            }
        )
    }
}
