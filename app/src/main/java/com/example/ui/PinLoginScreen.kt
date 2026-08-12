package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity

@Composable
fun PinLoginScreen(
    users: List<UserEntity>,
    onLoginSuccess: (UserEntity) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // PINY FINAL - TOM(admin)=2242, Adélka(kamaradka)=2221, Michal(tata)=3331 - zadne stare
    val pinMap = mapOf(
        "2242" to "admin",
        "2221" to "kamaradka",
        "3331" to "tata"
    )

    fun checkPin() {
        if (pin.length == 4) {
            val userId = pinMap[pin]
            val user = users.find { it.id == userId }
            if (user!= null) {
                onLoginSuccess(user)
                pin = ""
                error = ""
            } else {
                error = "Špatný PIN: $pin"
                pin = ""
            }
        }
    }

    Scaffold(containerColor = Color(0xFF0B0F19), contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.ime) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).imePadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔐 VJMchat", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Zadej 4-místný PIN", fontSize = 16.sp, color = Color(0xFFE2E8F0))
            Spacer(Modifier.height(32.dp))

            // 4 tecky
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0..3) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pin.length > i) Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White))
                        else Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF334155)))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (error.isNotEmpty()) Text(error, color = Color(0xFFEF4444), fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            // Numericka klavesnice - hned otevrena
            val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    row.forEach { key ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (key.isNotEmpty()) {
                                PinKeyButton(
                                    keyLabel = key,
                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                    containerColor = Color(0xFF1E293B),
                                    onClick = {
                                        if (key == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                        else { if (pin.length < 4) { pin += key; if (pin.length == 4) checkPin() } }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PinKeyButton(
    keyLabel: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    onClick: () -> Unit
) {
    var flashToken by remember { mutableIntStateOf(0) }
    var flashVisible by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (flashVisible) 0.95f else 0f,
        animationSpec = tween(durationMillis = if (flashVisible) 60 else 480),
        label = "pinFlash"
    )

    LaunchedEffect(flashToken) {
        if (flashToken == 0) return@LaunchedEffect
        flashVisible = true
        flashVisible = false
    }

    Button(
        onClick = {
            flashToken += 1
            onClick()
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(keyLabel, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .graphicsLayer { alpha = flashAlpha }
            )
            Text(keyLabel, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}