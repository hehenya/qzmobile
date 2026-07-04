package com.example.toolbox.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.data.ActiveDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapDialog(
    activeDays: List<ActiveDay>,
    yearMonth: YearMonth,
    onDismiss: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value 
    val activeDaysMap = activeDays.associate { it.date to it.msgCount }
    val maxMsgCount = activeDays.maxOfOrNull { it.msgCount } ?: 1
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Text("◀", fontSize = 18.sp)
                }
                Text(
                    text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = onNextMonth,
                    enabled = yearMonth.isBefore(YearMonth.from(today))
                ) {
                    Text("▶", fontSize = 18.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星期标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 热力图网格
                val totalCells = firstDayOfWeek - 1 + daysInMonth
                val rows = (totalCells + 6) / 7
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayOfMonth = cellIndex - (firstDayOfWeek - 1) + 1
                                
                                if (col < 7) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val date = yearMonth.atDay(dayOfMonth)
                                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val msgCount = activeDaysMap[dateStr] ?: 0
                                            val isFuture = date.isAfter(today)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when {
                                                            isFuture -> Color.Gray.copy(alpha = 0.1f)
                                                            msgCount > 0 -> getHeatmapColor(msgCount, maxMsgCount)
                                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                        }
                                                    )
                                                    .then(
                                                        if (date == today) {
                                                            Modifier.border(
                                                                2.dp,
                                                                MaterialTheme.colorScheme.primary,
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayOfMonth.toString(),
                                                    fontSize = 11.sp,
                                                    color = when {
                                                        isFuture -> Color.Gray.copy(alpha = 0.3f)
                                                        msgCount > maxMsgCount * 0.7f -> Color.White
                                                        msgCount > 0 -> Color.White.copy(alpha = 0.9f)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontWeight = if (msgCount > 0) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 图例
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(0.1f, 0.3f, 0.5f, 0.7f, 1f).forEach { intensity ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Color(
                                        red = 0.1f,
                                        green = 0.6f,
                                        blue = 0.1f,
                                        alpha = intensity
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("多", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

fun getHeatmapColor(msgCount: Int, maxCount: Int): Color {
    if (maxCount == 0) return Color(0.1f, 0.6f, 0.1f, 0.1f)
    
    val intensity = (msgCount.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1.0f)
    
    // 从浅绿到深绿
    return Color(
        red = 0.1f,
        green = 0.6f,
        blue = 0.1f,
        alpha = intensity
    )
}