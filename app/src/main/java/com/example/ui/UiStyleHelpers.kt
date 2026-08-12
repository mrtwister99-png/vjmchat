package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun priorityColor(priority: String): Color = when (priority.uppercase()) {
    "HIGH" -> Color(0xFFEF4444)
    "MEDIUM" -> Color(0xFFF97316)
    "LOW" -> Color(0xFFEAB308)
    else -> Color(0xFF475569)
}

fun priorityBorderWidth(priority: String): Dp = when (priority.uppercase()) {
    "HIGH" -> 2.5.dp
    "MEDIUM" -> 2.dp
    "LOW" -> 1.5.dp
    else -> 1.dp
}

fun prioritySymbol(priority: String): String = when (priority.uppercase()) {
    "HIGH" -> "🔴"
    "MEDIUM" -> "🟠"
    "LOW" -> "🟡"
    else -> "⚪"
}

fun priorityPrefix(priority: String): String = when (priority.uppercase()) {
    "HIGH" -> "🔴"
    "MEDIUM" -> "🟠"
    "LOW" -> "🟡"
    else -> "⚪"
}

fun parseCategoryColor(hex: String?, fallback: Color = Color(0xFF3B82F6)): Color = try {
    if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}

@Composable
fun PriorityBadge(
    priority: String,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val bg = if (selected) Color(0xFF0F172A) else Color(0xFF334155)
    val strokeColor = priorityColor(priority)
    val strokeWidth = if (selected) priorityBorderWidth(priority) else 1.dp
    Box(
        modifier = modifier
            .size(size)
            .background(bg, RoundedCornerShape(10.dp))
            .border(strokeWidth, strokeColor, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(prioritySymbol(priority), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}