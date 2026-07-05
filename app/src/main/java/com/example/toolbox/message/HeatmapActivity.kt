package com.example.toolbox.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.TokenManager
import com.example.toolbox.data.ActiveDay
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val chatType = intent.getIntExtra("chat_type", 1)
        val chatId = intent.getIntExtra("chat_id", 0)
        val token = TokenManager.get(this) ?: ""
        val dateString = intent.getStringExtra("date_string") ?: ""

        val initialYearMonth = parseYearMonth(dateString)

        setContent {
            ToolBoxTheme {
                HeatmapScreen(
                    token = token,
                    chatType = chatType,
                    chatId = chatId,
                    initialYearMonth = initialYearMonth,
                    onBack = { finish() }
                )
            }
        }
    }

    private fun parseYearMonth(dateString: String): YearMonth {
        if (dateString.isBlank()) return YearMonth.now()
        return try {
            // 解析 "7月1日" 格式（没有年份，用当前年份）
            val cleaned = dateString
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
            val parts = cleaned.split("-").filter { it.isNotBlank() }

            if (parts.size == 2) {
                // "7-1" → 月份和日期
                val month = parts[0].toIntOrNull() ?: return YearMonth.now()
                val now = YearMonth.now()
                YearMonth.of(now.year, month)
            } else if (parts.size >= 3) {
                // "2026-7-1" → 年月日
                val year = parts[0].toIntOrNull() ?: return YearMonth.now()
                val month = parts[1].toIntOrNull() ?: return YearMonth.now()
                YearMonth.of(year, month)
            } else {
                YearMonth.now()
            }
        } catch (e: Exception) {
            YearMonth.now()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    token: String,
    chatType: Int,
    chatId: Int,
    initialYearMonth: YearMonth,
    onBack: () -> Unit
) {
    val viewModel: MessageDetailViewModel = viewModel(
        factory = MessageDetailViewModelFactory(token, chatType, chatId)
    )

    var currentYearMonth by remember { mutableStateOf(initialYearMonth) }
    val activeDays by viewModel.activeDays.collectAsState()
    val isLoading by viewModel.isLoadingActiveDays.collectAsState()

    val today = LocalDate.now()
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value
    val activeDaysMap = remember(activeDays) { activeDays.associate { it.date to it.msgCount } }
    val maxMsgCount = remember(activeDays) { activeDays.maxOfOrNull { it.msgCount } ?: 1 }

    LaunchedEffect(currentYearMonth) {
        viewModel.loadActiveDays(currentYearMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            currentYearMonth = currentYearMonth.minusMonths(1)
                        }) {
                            Text("◀", fontSize = 20.sp)
                        }

                        Text(
                            text = currentYearMonth.format(
                                DateTimeFormatter.ofPattern("yyyy年M月")
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                val nextMonth = currentYearMonth.plusMonths(1)
                                if (!nextMonth.isAfter(YearMonth.from(today))) {
                                    currentYearMonth = nextMonth
                                }
                            },
                            enabled = currentYearMonth.isBefore(YearMonth.from(today))
                        ) {
                            Text("▶", fontSize = 20.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val date = currentYearMonth.atDay(dayOfMonth)
                                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val msgCount = activeDaysMap[dateStr] ?: 0
                                            val isFuture = date.isAfter(today)
                                            val isToday = date == today

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when {
                                                            isFuture -> Color.Transparent
                                                            msgCount == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                            else -> getHeatColor(msgCount, maxMsgCount)
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday) {
                                                            Modifier.border(
                                                                2.dp,
                                                                MaterialTheme.colorScheme.primary,
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                        } else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayOfMonth.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isFuture -> Color.Transparent
                                                        msgCount > maxMsgCount * 0.6f -> Color.White
                                                        msgCount > 0 -> Color.White.copy(alpha = 0.85f)
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 图例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("少", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        // 4 个渐变色块
                        for (i in 1..4) {
                            val intensity = i / 4f
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(getHeatColor((maxMsgCount * intensity).toInt(), maxMsgCount))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("多", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * 根据消息数量返回颜色深度（绿色系，类似 TG）
 */
private fun getHeatColor(msgCount: Int, maxCount: Int): Color {
    if (maxCount == 0) return Color(0x3365BC7A)
    val ratio = (msgCount.toFloat() / maxCount.toFloat()).coerceIn(0.15f, 1f)
    // 类似 Telegram 的绿色渐变
    val alpha = (0.2f + ratio * 0.8f).coerceIn(0f, 1f)
    return Color(0x0065BC7A).copy(alpha = alpha)
}