package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserEntity

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileSettingsDialog(
    user: UserEntity?,
    allUsers: List<UserEntity> = emptyList(),
    onSave: (nickname: String, avatarEmoji: String, borderHexColor: String, email: String, pin: String) -> Unit,
    onSaveBubbleStyleForUser: (targetId: String, bubbleColor: String, bubbleShape: String?) -> Unit = { _, _, _ -> },
    onResetPin: (targetId: String, newPin: String) -> Unit = { _, _ -> },
    onResetAllData: () -> Unit = {},
    onOpenSecretChat: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (user == null) return

    var nickname by remember(user) { mutableStateOf(user.nickname) }
    var email by remember(user) { mutableStateOf(user.email) }
    var pinInput by remember(user) { mutableStateOf(user.pin) }
    var selectedAvatar by remember(user) { mutableStateOf(user.avatarEmoji) }
    var selectedBorderColor by remember(user) { mutableStateOf(user.borderHexColor) }
    var selectedBubbleShape by remember(user) { mutableStateOf(user.chatBubbleShape) }
    var bubbleColorsByUser by remember(user, allUsers) {
        mutableStateOf(allUsers.associate { it.id to it.chatBubbleColorHex })
    }
    var resetTarget by remember { mutableStateOf<UserEntity?>(null) }
    var resetPinValue by remember { mutableStateOf("") }
    var showDataResetConfirm by remember { mutableStateOf(false) }

    val isAdmin = user.id == "admin"
    val otherUsers = allUsers.filter { it.id != user.id && (it.id == "kamaradka" || it.id == "tata") }
    val isMichal = user.id == "tata" || user.defaultName.contains("Michal", true) || user.nickname.contains("Michal", true)
    val isSecretChatUnlocked = !isMichal

    val avatarOptions = listOf("👑", "🌸", "🛠️", "🧑‍💼", "🚀", "⚡", "🌟", "🔥", "🎯", "🦊", "🦁", "☕")
    val bubbleColorOptions = listOf("#DC2626", "#2563EB", "#22C55E", "#A855F7", "#0EA5E9", "#F59E0B", "#111827", "#64748B")

    val colorOptions = when (user.id) {
        "admin" -> listOf("#0086cf", "#1fceff", "#003df2", "#000000", "#FFFFFF", "#64748B")
        "tata" -> listOf("#09c702", "#6dff12", "#00851d", "#000000", "#FFFFFF", "#64748B")
        "kamaradka" -> listOf("#a8006d", "#fa0079", "#ff26ed", "#000000", "#FFFFFF", "#64748B")
        else -> listOf("#3B82F6", "#000000", "#FFFFFF", "#64748B")
    }

    fun saveAll() {
        if (nickname.isBlank()) return
        val finalPin = if (pinInput.isNotBlank()) pinInput else user.pin
        val finalEmail = email
        onSave(nickname, selectedAvatar, selectedBorderColor, finalEmail, finalPin)

        allUsers.forEach { member ->
            val selectedColor = bubbleColorsByUser[member.id] ?: member.chatBubbleColorHex
            val selectedShape = if (member.id == user.id) selectedBubbleShape else member.chatBubbleShape
            onSaveBubbleStyleForUser(member.id, selectedColor, selectedShape)
        }

        onDismiss()
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Image(painter = painterResource(id = R.drawable.vjmchat1), contentDescription = "VJM chat", modifier = Modifier.height(36.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, parseHex(user.borderHexColor, Color(0xFF3B82F6)), CircleShape)
                        ) {
                            Text(user.avatarEmoji, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayName = user.nickname.takeIf { it.lowercase() != "secret" } ?: user.defaultName
                        Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Zpět", color = Color(0xFF93C5FD)) }
                },
                actions = {
                    TextButton(onClick = { saveAll() }) { Text("Uložit", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold) }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(3.dp, parseHex(selectedBorderColor, Color(0xFF3B82F6)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedAvatar, fontSize = 28.sp)
                    }

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Přezdívka", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail", fontSize = 12.sp) },
                        placeholder = { Text("vas.email@firma.cz", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors()
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { inputVal -> pinInput = inputVal.filter { it.isDigit() }.take(4) },
                        label = { Text("PIN", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = fieldColors()
                    )

                    Text("Avatar", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        avatarOptions.forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatar == em) Color(0xFF3B82F6) else Color(0xFF1E293B))
                                    .clickable { selectedAvatar = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 16.sp)
                            }
                        }
                    }

                    Text("Obrys ikonky", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        colorOptions.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(parseHex(hex, Color.White))
                                    .border(if (selectedBorderColor == hex) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedBorderColor = hex }
                            )
                        }
                    }

                    Text("Tvar mojí bubliny", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShapeChoice(
                            modifier = Modifier.weight(1f),
                            label = "Zaoblené",
                            selected = selectedBubbleShape == "ROUNDED",
                            onClick = { selectedBubbleShape = "ROUNDED" }
                        )
                        ShapeChoice(
                            modifier = Modifier.weight(1f),
                            label = "Hranaté",
                            selected = selectedBubbleShape == "SQUARE",
                            onClick = { selectedBubbleShape = "SQUARE" }
                        )
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Barvy bublin všech lidí", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Defaultně vidíš aktuální barvy všech 3 osob. Můžeš je ručně změnit pro kohokoliv.", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    allUsers.forEach { member ->
                        val selected = bubbleColorsByUser[member.id] ?: member.chatBubbleColorHex
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(member.avatarEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(member.nickname, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHex(selected, Color(0xFF2563EB)))
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                bubbleColorOptions.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(parseHex(hex, Color.Gray))
                                            .border(if (selected == hex) 3.dp else 1.dp, if (selected == hex) Color.White else Color(0xFF334155), CircleShape)
                                            .clickable {
                                                bubbleColorsByUser = bubbleColorsByUser.toMutableMap().apply { put(member.id, hex) }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ukázka chatu", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Náhled je live podle vybraných barev.", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    val meColor = bubbleColorsByUser[user.id] ?: user.chatBubbleColorHex
                    val others = allUsers.filter { it.id != user.id }

                    PreviewBubble(
                        name = "Já",
                        emoji = selectedAvatar,
                        text = "Ahoj, takhle bude vypadat moje bublina.",
                        bubbleColor = parseHex(meColor, Color(0xFFDC2626)),
                        isMe = true,
                        isSquare = selectedBubbleShape == "SQUARE"
                    )

                    others.forEach { member ->
                        PreviewBubble(
                            name = member.nickname,
                            emoji = member.avatarEmoji,
                            text = "A tohle je náhled pro ${member.nickname}.",
                            bubbleColor = parseHex(
                                bubbleColorsByUser[member.id] ?: member.chatBubbleColorHex,
                                Color(0xFF2563EB)
                            ),
                            isMe = false,
                            isSquare = member.chatBubbleShape == "SQUARE"
                        )
                    }
                }
            }

            if (isSecretChatUnlocked && onOpenSecretChat != null) {
                Button(
                    onClick = {
                        onOpenSecretChat.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Vstoupit do SECRET chatu", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                }
            }

            if (isAdmin && user.id != "tata") {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Admin - Reset PINu pro ostatní", fontWeight = FontWeight.Bold, color = Color(0xFFEAB308), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (otherUsers.isNotEmpty()) {
                            otherUsers.forEach { other ->
                                val defaultReset = when {
                                    other.id == "kamaradka" || other.defaultName.contains("Adel", true) -> "2221"
                                    other.id == "tata" -> "3331"
                                    else -> "1234"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(other.avatarEmoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(other.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("PIN: ${other.pin}", color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                    Button(onClick = { resetTarget = other; resetPinValue = defaultReset }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(8.dp)) {
                                        Text("Reset na $defaultReset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showDataResetConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                        ) {
                            Text("DATA RESET - smazat úkoly/poznámky + nápady + chaty + události", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Text("ZRUŠIT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE2E8F0))
                }
                Button(
                    onClick = { saveAll() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ULOŽIT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                }
            }
        }
    }

    if (showDataResetConfirm) {
        AlertDialog(
            onDismissRequest = { showDataResetConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("OPRAVDU SMAZAT VŠECHNA DATA?", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
            text = { Text("Smažou se VŠECHNY úkoly/poznámky + nápady + chaty (team i secret) + události v kalendáři. Nevratné!", color = Color.White, fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { onResetAllData(); showDataResetConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                    Text("ANO, SMAZAT VŠE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataResetConfirm = false }) { Text("Zrušit", color = Color.Gray) }
            }
        )
    }

    if (resetTarget != null) {
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("Resetovat PIN pro ${resetTarget?.nickname}?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Opravdu chceš resetovat PIN pro ${resetTarget?.nickname} na $resetPinValue?", color = Color(0xFFCBD5E1), fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { resetTarget?.let { onResetPin(it.id, resetPinValue) }; resetTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                    Text("Ano, resetovat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTarget = null }) { Text("Zrušit", color = Color(0xFF94A3B8)) }
            }
        )
    }
}

@Composable
private fun ShapeChoice(modifier: Modifier = Modifier, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFF334155))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PreviewBubble(
    name: String,
    emoji: String,
    text: String,
    bubbleColor: Color,
    isMe: Boolean,
    isSquare: Boolean
) {
    val shape = if (isSquare) {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        else RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    } else {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        else RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(if (isMe) Alignment.CenterStart else Alignment.CenterEnd),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(name, color = Color(0xFFFFE082), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text, color = Color.White, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.5.dp, Color.White, CircleShape)
                .align(if (isMe) Alignment.TopEnd else Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 14.sp)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF3B82F6),
    unfocusedBorderColor = Color(0xFF334155),
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0F172A)
)

private fun parseHex(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
